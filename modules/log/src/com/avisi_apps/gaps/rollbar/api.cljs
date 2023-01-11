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

(defn validate-status [status]
  (and (>= status 200) (< status 400)))

(tests
  "TC1: Valid status"
  (validate-status 200) := true
  "TC2: Invalid token send"
  (validate-status 401) := false
  "TC3: Invalid no token send"
  (validate-status 400) := false)

(def axios-instance
  (axios/create
    #js
      {:maxContentLength (* 50 1000 1000)
       :validateStatus (fn [status] (validate-status status) )
       :maxRedirects 0
       :httpsAgent https-agent}))

(defn build-rollbar-header [configuration]
  (clj->js {:Content-Type "application/json"
            :X-Rollbar-Access-Token (get configuration :log/token)}))

(def rollbar-url "https://api.rollbar.com/api/1/item/")

(defn send-message-to-rollbar [configuration notifier severity message]
(p/let [response (->
    (.request
      ^js axios-instance
      #js
      {:method "post"
       :url rollbar-url
       :headers (build-rollbar-header configuration)
       :data (clj->js {:data {
                       :environment "production"
                       :notifier notifier
                       :body {
                              :message {:level (str/lower-case severity)
                                        :body  message}}
                       :level (str/lower-case severity)}})})
    (p/then bean))] response))

(defn send-exception-to-rollbar [configuration notifier client severity payload frames]
  (p/let [response (->
    (.request
    ^js axios-instance
    #js
    {:method "post"
    :url rollbar-url
    :headers (build-rollbar-header configuration)
    :data (clj->js
            {:data {
      :environment "production"
      :notifier notifier
      :body {
        :trace {
          :frames frames
          :exception {:class (str "\n Message: " (:err payload))}}}
      :client client
      :level (str/lower-case severity)}})})
    (p/then bean))] response))
