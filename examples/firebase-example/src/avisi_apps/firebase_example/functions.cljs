(ns avisi-apps.firebase-example.functions
       (:require
              ["firebase-functions" :as functions]
              [hyperfiddle.rcf :refer [tests]]
              [avisi-apps.gaps.log :as log]))

(tests
  "Check if hyperfiddle is working correctly"
  (+ 1 1) := 2

  "Check if spy is working correctly"
  (log/spy (+ 1 1)) := 2)


(def cloud-functions
  #js {:handleRequest
            (->
              ^js functions
              ^js (.-https)
              (.onRequest (fn [request response]

                            (log/info {:message "hello from logging library"})

                            (.send response "Hello world from firebase and cljs"))))})
