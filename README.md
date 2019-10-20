# affable-async

```[com.danboykis/affable-async "0.1.0"]```

Provide functionality useful for working with core.async.

`u-pipeline-async` unordered pipeline that can execute n concurrent requests subject to 'af' asynchronous function

## Usage

```clojure
(require '[affable-async.core :as aff]
	 '[clojure.core.async :as a])

(def to (a/chan 10))
(def from (a/chan 10))
(aff/u-pipeline-async {:n 10 :to to :from from
		       :af (fn [v ch] (when (= v 5) (a/<!! (a/timeout 5000))) (a/put! ch (* 2 v)) (a/close! ch))})
(a/go-loop []
  (if-some [v (a/<! to)]
    (do (println v)
    	(recur))
    (println "done reading!")))

(dotimes [i 10] (a/put! from i))
```
