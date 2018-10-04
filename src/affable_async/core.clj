(ns affable-async.core
  (:require [clojure.core.async :as a])
  (:import [java.util.concurrent.atomic AtomicIntegerArray AtomicInteger]))

(defn u-pipeline-async [{:keys [n to af from close?] :or {close? true}}]
  "Unordered Pipeline for async operations.
  Takes elements from the 'from' channel and applies 'af' function then
  puts the transformed elements onto 'to' channel. The function 'af'
  must be of two arguments the first being the input value and the second
  being a channel. It is assumed that 'af' will be asynchronous sending the
  result of the async operation to the provided channel. Outputs will be 
  placed onto 'to' channel in no particular order.
  The 'to' channel will be closed when the 'from' channel closes, but can
  be determined by the 'close?' parameter. The pipeline will stop consuming
  the 'from' channel if the 'to' channel closes.

  'n': the number of parallel ops
  'from': channel to take elements from
  'af': async function taking an element and channel
  'to': channel to put values generated by 'af'"

  (assert (pos-int? n))
  (assert (some? to))
  (assert (some? from))
  (let [done (AtomicInteger. 0)]
    (dotimes [_ n]
      (a/take!
        (a/go-loop [p (a/chan 1)]
          (if-some [e (a/<! from)]
            (do (af e p)
                (when-some [af-result (a/<! p)]
                  (if (a/>! to af-result)
                    (recur (a/chan 1))
                    (a/close! from))))
            (.incrementAndGet done)))
        (fn [_]
          (when (and (= (.get done) n) close?)
            (a/close! to)))))))

(defn u-reduce [f init channels & {:keys [close?] :or {close? true}}]
  "Takes a collection of channels and reduces them according to function f.
  Returns a channel that has the result of the reduction. Each channel in
  channels collection will have at most one value taken from it. A closed
  channel will be discarded and skipped. If 'closed?' is true the channel
  taken from will be closed. If channels is an empty or nil the 'init' value
  will be returned.

  'f': two argument function taking an accumulator and current value
  'init': initial accumulator value
  'channels': collection of core.async channels with at most one message in them
  'close?': optional boolean argument that is true by default"

  (a/go-loop [ret init channels channels]
    (if (empty? channels)
      ret
      (let [[v ch] (a/alts! channels)
            channels' (remove #(identical? ch %) channels)]
        (when close? (a/close! ch))
        (if (nil? v)
          (recur ret channels')
          (let [ret' (f ret v)]
            (if (reduced? ret')
              @ret'
              (recur ret' channels'))))))))
