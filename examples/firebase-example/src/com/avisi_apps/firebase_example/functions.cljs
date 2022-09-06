(ns com.avisi-apps.firebase-example.functions
  (:require
    ["firebase-functions" :as functions]
    [hyperfiddle.rcf :refer [tests]]
    [reitit.ring :as ring]
    [com.avisi-apps.gaps.reitit-express.http :as http]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation :as content-negotiation]
    [com.avisi-apps.gaps.log :as log]))

(tests
  "Check if hyperfiddle is working correctly"
  (+ 1 1) := 2

  "Check if spy is working correctly"
  (log/spy (+ 1 1)) := 2)

(defn not-found-handler [_ respond _]
  (respond {:status 404
            :body "<h1>Not found</h1>"
            :headers {:content-type "text/html"}}))


(defn handler [_ respond _]
  (respond {:status 200
            :body "<h1>hello world</h1>"
            :cookies {"__session" {:value "foobar"
                                   :secure true
                                   :http-only false}}
            :headers {:content-type "text/html"}}))

(defn data-handler [request respond _]
  (respond {:status 200
            :body {:such "data"}
            :headers {:content-type "application/json"}}))

(def router
  (ring/router
    [["/ping" {:get handler
               :post data-handler}]
     ["/data" {:get data-handler
               :post data-handler}]]))

(def ring-handler (ring/ring-handler router not-found-handler {:middleware [content-negotiation/format-middleware]}))

(def app (http/expressjs-app ring-handler))

(log/info {:message "Initialized functions"})

(def cloud-functions
  #js {:handleRequest
            (->
             ^js functions
              ^js (.-https)
              (.onRequest app))})

(comment
  _request

  )
