(ns affable-async.channels
  (:require [clojure.core.async.impl.channels :as ic]
            [clojure.core.async.impl.protocols :as p]))


;; like ManyToManyChannel but a lot less useful
(deftype NilChannel [closed xf]
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
    (when (some? xf)
      (locking xf
        ((xf (fn [& _])) nil val)))
      (atom (not @closed)))
  java.lang.Object
  (toString [_] (str "NilChannel<<" "closed:" @closed ">>")))


(defn nil-chan
  ([]
   (nil-chan nil))
  ([xf]
   (NilChannel. (atom false) xf)))