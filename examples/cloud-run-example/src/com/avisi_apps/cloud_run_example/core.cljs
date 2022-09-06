(ns com.avisi-apps.cloud-run-example.core
  (:require [reitit.ring :as ring]
    [com.avisi-apps.gaps.reitit-express.http :as http]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation :as content-negotiation]
    [com.avisi-apps.gaps.log :as log]))

(defonce server-ref (volatile! nil))
(defonce ring-handler (volatile! nil))

(defn not-found-handler [_ respond _]
  (respond {:status 404
            :body "<h1>Not found</h1>"
            :headers {:content-type "text/html"}}))


(defn handler [_ respond _]
  (respond {:status 200
            :body "<h1>hello world, from cloud run</h1>"
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

(defn main [& {:keys [done]}]
  (let [ring-handler (vreset! ring-handler (ring/ring-handler router not-found-handler {:middleware [content-negotiation/format-middleware]}))
        app (http/expressjs-app ring-handler)]
    (vreset! server-ref (http/listen! app 3000)))
  (when done (done)))

(defn start "Hook to start. Also used as a hook for hot code reload."
  [done]
  (log/info {:message "start called"})
  (main :done done))

(defn stop "Hot code reload hook to shut down resources so hot code reload can work"
  [done]
  (log/info {:message "stop called"})
  (if-some [srv @server-ref]
    (.close
      srv
      (fn [err]
        (log/info
          {:message "stop completed"
           :error err})
        (vreset! server-ref nil)
        (done)))
    (done)))
