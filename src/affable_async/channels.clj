(ns affable-async.channels
  (:require [clojure.core.async.impl.channels :as ic]
            [clojure.core.async.impl.protocols :as p]))

(defn- ex-handler [ex]
  (let [t (Thread/currentThread)]
    (-> (.getUncaughtExceptionHandler t)
        (.uncaughtException t ex))
    nil))

(defn- handle [exh t]
  (let [result ((or exh ex-handler) t)]
    (when-not (nil? result)
      result)))

(defn- put-handler! [val xf exh]
  (let [retry (volatile! {:val val :xf xf :exh exh})]
    (while @retry
      (let [{:keys [val xf exh] :as m} @retry]
        (vreset! retry nil)
        (when (some? xf)
          (locking xf
            (try
              ((xf (fn [& _])) nil val))
            (catch Throwable t
              (when-some [result (handle exh t)]
                (vreset! retry (assoc m :val result))))))))))

;; like ManyToManyChannel but a lot less useful
(deftype NilChannel [closed xf exh]
  ic/MMC
  (cleanup [_] nil)
  (abort [_] nil)
  p/Channel
  (close! [_] (reset! closed true) nil)
  (closed? [_] @closed)
  p/ReadPort
  (take! [_ _] nil)
  p/WritePort
  (put! [_ val _]
    (put-handler! val xf exh)
    (atom (not @closed)))
  java.lang.Object
  (toString [_] (str "NilChannel<<" "closed:" @closed ">>")))


(defn nil-chan
  ([]
   (nil-chan nil))
  ([xf]
   (NilChannel. (atom false) xf nil))
  ([xf exh]
   (NilChannel. (atom false) xf exh)))
