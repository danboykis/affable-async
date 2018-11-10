(ns affable-async.channels
  (:require [clojure.core.async.impl.channels :as ic]
            [clojure.core.async.impl.protocols :as p]))


;; like ManyToManyChannel but a lot less useful
(deftype DummyChannel [closed]
  ic/MMC
  (cleanup [_] nil)
  (abort [_] nil)
  p/Channel
  (close! [_] (reset! closed true) nil)
  (closed? [_] @closed)
  p/ReadPort
  (take! [ _ _] nil)
  p/WritePort
  (put! [_ _ _] (atom (not @closed))))


(defn dummy-chan []
  "Creates a channel that accepts writes but doesn't store messages."
  (DummyChannel. (atom false)))