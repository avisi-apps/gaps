(ns com.avisi-apps.gaps.reitit-express.http
  (:require
    ["express" :as express]
    ["cookie-parser" :as cookie-parser]
    [promesa.core :as p]
    [com.avisi-apps.gaps.log :as log]
    [cljs-bean.core :refer [->js]]
    [clojure.string :as str]
    [cljs.spec.alpha :as s])
  (:import
    [goog Uri]))

(defn- translate-cookie-opts
  "Copied from Macchiato so we don't have to change existing code"
  [{:keys [same-site secure signed max-age expires http-only path domain overwrite?]}]
  (clj->js
    (merge
      (when secure {:secure true})
      (when signed {:signed true})
      (when same-site {:sameSite same-site})
      (when max-age {:maxAge max-age})
      (when path {:path path})
      (when domain {:domain domain})
      (when expires {:expires expires})
      (when (some? http-only) {:httpOnly http-only})
      (when overwrite? {:overwrite overwrite?}))))

(defn add-params
  [{:keys [form-params query-params]
    :as request
    :or
      {form-params {}
       query-params {}}}]
  (assoc request :params (merge form-params query-params)))

(defn express-request->ring-request
  "Turn a express-js node request into a ring spec map based on: https://github.com/ring-clojure/ring/blob/master/SPEC,
  We add the original request in the meta of the map under `:node/request`"
  [^js express-request]
  (let [socket ^js (.-socket express-request)
        address ^js (.address socket)
        body (.-body express-request)
        cookies (.-cookies express-request)
        parsed-uri ^js (Uri/parse (.-originalUrl express-request))]
    (with-meta
      (cond->
        {:server-port (.-port address)
         :server-name (.-address address)
         :remote-addr (.-remoteAddress socket)
         :uri (.-path express-request)
         :original-url (.-originalUrl express-request)
         :scheme
           (some->
             (.-protocol express-request)
             str/lower-case
             keyword)
         :request-method
           (some->
             (.-method express-request)
             str/lower-case
             keyword)
         :protocol
           (str
             (some->
               (.-protocol express-request)
               str/upper-case)
             "/"
             (.-httpVersion express-request))
         :query-params (js->clj (.-query express-request))
         :headers (js->clj (.-headers express-request))}
        (.hasQuery parsed-uri) (assoc :query-string (.getQuery parsed-uri))
        body (assoc :body body)
        cookies (assoc :cookies (js->clj cookies))
        :always add-params)
      {:node/request express-request})))

(defn handle-unexpected-exception [error node-request node-response]
  (log/error
    error
    {:message "Unexpected error"
     :req node-request})
  (->
    (.status node-response 500)
    (.set #js {"content-type" "text/plain"})
    (.send "Unexpected error")))

(s/def ::static-dirs (s/coll-of string?))

(defn expressjs-app
  "Create an express application from a handler which handles (fn [request respond raise]). Typically, http-handler is the
  result of:

  ```
  (def ring-handler (ring/ring-handler router not-found-handler))
  ```

  # Options
  `com.avisi-apps.gaps.reitit-express.http/static-dirs` []
  If you want to service static folders you can give a list of folders that is should make available
   "
  [http-handler & {::keys [static-dirs]}]
  (let [app ^js (express)]
    (.use app (cookie-parser))
    (.use app (.json ^js express))
    (.use app (.raw ^js express #js {:type #js ["application/transit+json" "application/octet-stream"]}))
    (.use app (.text ^js express))
    (.use app (.urlencoded ^js express #js {:extended true}))
    (run! #(.use app (.static ^js express %)) static-dirs)
    (.use
      app
      (fn reitit-express-middleware [node-request node-response _]
        (->
          (p/do
            (let [request (express-request->ring-request node-request)]
              (http-handler
                request
                (fn respond [{:keys [cookies headers body status]}]
                  (run!
                    (fn
                      [[k
                        {:keys [value]
                         :as opts}]]
                      (.cookie node-response (name k) value (translate-cookie-opts opts)))
                    cookies)
                  (->
                    (.status node-response status)
                    (.set (->js headers))
                    (.send body)))
                (fn raise [error] (handle-unexpected-exception error node-request node-response)))))
          (p/catch (fn [e] (handle-unexpected-exception e node-request node-response))))))))

(defn listen! "Start listening on a port, returns an instance of http.Server which can be closed"
  [^js app port]
  (.listen app port))

(defn close! "Close a http.Server (the result from calling (listen! app 3000)" [^js server cb] (.close server cb))
