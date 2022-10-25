(ns com.avisi-apps.gaps.rollbar.api
  (:require
    ["axios" :as axios]
    ["agentkeepalive" :refer [HttpsAgent]]
    [promesa.core :as p]
    [cljs-bean.core :refer [bean]]
    [clojure.string :as str]))

(def token "ff24d39dcc4b46478685f282d8c1ce15")

(def https-agent
  (HttpsAgent.
    #js
      {:keepAlive true
      :maxSockets 100
      :maxFreeSockets 10
      :timeout 60000
      :freeSocketTimeout 30000}))

(def axios-instance
  (axios/create
    #js
      {:maxContentLength (* 50 1000 1000)
       :validateStatus (fn [status] (and (>= status 200) (< status 400)))
       :maxRedirects 0
       :httpsAgent https-agent}))

(defn sendToRollbar [severity message]
(p/let [response (->
    (.request
      ^js axios-instance
      #js
      {:method "post"
       :url "https://api.rollbar.com/api/1/item/"
       :headers (clj->js {:Content-Type "application/json"
                          :X-Rollbar-Access-Token token})
       :data (clj->js {:data {
                       :environment "production"
                       :body {
                              :message {
                                        :level (str/lower-case severity)
                                        :body  message}}
                       :level (str/lower-case severity)}})})
    (p/then bean))] response))
