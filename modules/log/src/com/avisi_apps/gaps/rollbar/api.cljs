(ns com.avisi-apps.gaps.rollbar.api
  (:require
    ["axios" :as axios]
    [hyperfiddle.rcf :refer [tests]]
    ["agentkeepalive" :refer [HttpsAgent]]
    [promesa.core :as p]
    [cljs-bean.core :refer [bean]]
    [clojure.string :as str]))

(def https-agent
  (HttpsAgent.
    #js
      {:keepAlive true
      :maxSockets 100
      :maxFreeSockets 10
      :timeout 60000
      :freeSocketTimeout 30000}))

(defn validateStatus [status]
  (and (>= status 200) (< status 400)))

(tests
  "TC1: Valid status"
  (validateStatus 200) := true
  "TC2: Invalid token send"
  (validateStatus 401) := false
  "TC3: Invalid no token send"
  (validateStatus 400) := false)

(def axios-instance
  (axios/create
    #js
      {:maxContentLength (* 50 1000 1000)
       :validateStatus (fn [status] (validateStatus status) )
       :maxRedirects 0
       :httpsAgent https-agent}))

(defn send-to-rollbar [configuration severity message]
(p/let [response (->
    (.request
      ^js axios-instance
      #js
      {:method "post"
       :url "https://api.rollbar.com/api/1/item/"
       :headers (clj->js {:Content-Type "application/json"
                          :X-Rollbar-Access-Token      (get configuration :log/token)})
       :data (clj->js {:data {
                       :environment "production"
                       :body {
                              :message {
                                        :level (str/lower-case severity)
                                        :body  message}}
                       :level (str/lower-case severity)}})})
    (p/then bean))] response))
