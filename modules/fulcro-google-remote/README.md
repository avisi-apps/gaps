# Fulcro google remote [![Clojars Project](https://img.shields.io/clojars/v/com.avisi-apps.gaps/google-fulcro-remote.svg)](https://clojars.org/com.avisi-apps.gaps/fulcro-google-remote)

A fulcro remote wrapper which handles certain things you can expect from google cloud services. This mostly handles 502 response
status codes which are expected to happen on google cloud and will retry 5 times.

# Usage

See the below example for usage examples:
```clojure
(ns example
  (:require
    [com.avisi-apps.gaps.fulcro.google-remote :as google-remote]
    [com.fulcrologic.fulcro.networking.http-remote :as http-remote]))

;; Create the remote
(def remote
  (->
    (http-remote/fulcro-http-remote
      {:url "/internal/api"
       :response-middleware google-network-wrapper/response-middleware
       :request-middleware
       (http-remote/wrap-fulcro-request
         (fn [r] (assoc-in r [:headers "Accept"] "application/transit+json")))})
    (google-remote/wrap-fulcro-remote)))

;; Use it in fulcro
```
