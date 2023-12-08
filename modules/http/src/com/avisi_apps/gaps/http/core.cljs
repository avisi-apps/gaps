(ns com.avisi-apps.gaps.http.core
  (:require
    ["axios" :as axios]
    ["qs" :as qs]
    ["agentkeepalive" :refer [HttpsAgent]]
    [promesa.core :as p]
    [cljs-bean.core :refer [->clj ->js]]
    [clojure.string :as str]))

(def ^:private https-agent
  (HttpsAgent.
    #js
            {:keepAlive true
             :maxSockets 100
             :maxFreeSockets 10
             ;; 60 seconds timeout
             :timeout 60000
             ;; Free socket timout 30 seconds
             :freeSocketTimeout 30000}))

(def ^:private axios-instance
  (axios/create
    #js
            {;; cap the maximum content length we'll accept to 50MBs, just in case
             :maxContentLength (* 50 1000 1000)
             :validateStatus (fn [status] (and (>= status 200) (< status 400)))
             :maxRedirects 0
             :httpsAgent https-agent}))

(defn ^:private settings->url
  "
  'indices' set to false will prevent adding an index to query parameters that are arrays. Example:
  {:accountId [a b]}
  'indices true' -> accountId[0]=a&accountId[1]=b
  'indices false' -> accountId=a&accountId=b
  "
  [{::keys [query-params url]}]
  (let [query-string (qs/stringify (->js query-params) #js {"indices" false})]
    (cond-> url (not (str/blank? query-string)) (str "?" query-string))))

(defn request
  "
  Accepts the following options:
  * `::base-url`:
  * `::endpoint`: part of the url after the base-url
  * `::method`: #{:get :post :put :head :delete}
  * `::query-params`: a map
  * `::headers`: a map with headers
  * `::content-type`: #{:json} is :json by default
  * `::body`: The body of the request will get converted to JSON by default
  "
  [{::keys [method body headers url base-url content-type auth query-params]
    :or
    {method :get
     content-type :json
     url "/"
     headers {}}}]
  (->
    (.request
      ^js axios-instance
      (cond->
        {:url
         (settings->url
           {::query-params query-params
            ::url url})
         :baseURL base-url
         :method (name method)
         :headers
         (merge
           (case content-type
             :json {"Content-Type" "application/json"}
             {})
           headers)}
        body (assoc :data body)
        auth
        (assoc :auth
               {:username (::username auth)
                :password (::password auth)})
        :always ->js))
    (p/then ->clj)))
