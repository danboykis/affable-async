(ns affable-async.retries
  (:require [clojure.core.async :as a]))

(defn retry [op-fn ok? {retry-fn :retry-fn err-log :err-log
                        :or {retry-fn #(repeat 1000)
                             err-log #(println "waiting for:" %1 " retry number:" %2 " failed because: " %3)}}]
  (a/go
    (loop [[retry & retries] (retry-fn)
           retry-num 0
           result (a/<! (op-fn))]
      (if (or (ok? result) (nil? retry))
        result
        (do
          (err-log retry (inc retry-num) result)
          (a/<! (a/timeout retry))
          (recur retries (inc retry-num) (a/<! (op-fn))))))))
