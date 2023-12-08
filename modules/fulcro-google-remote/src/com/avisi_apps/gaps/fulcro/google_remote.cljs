(ns com.avisi-apps.gaps.fulcro.google-remote
  (:require
    [com.fulcrologic.fulcro.networking.http-remote :as http-remote]
    [com.fulcrologic.fulcro.algorithms.tx-processing :as txn]
    [promesa.core :as p]))

(def default-response-middleware (http-remote/wrap-fulcro-response))

(defn response-middleware
  "Intercepts the status-code property from the response and stores it under :response-status-code as
  Fulcro will change status-code to 500 later if it is not 200."
  [{:keys [status-code]
    :as response}]
  (default-response-middleware (assoc response :response-status-code status-code)))

(defn temporarily-unavailable? [{:keys [response-status-code]}] (= 502 response-status-code))

(defn last-attempt? [tries-left] (= 1 tries-left))

(def amount-of-retry-attempts 5)

(defn wrap-fulcro-remote "Retry requests up to 5 times with and interval of 1 second when Google returns a status 502."
  [fulcro-remote]
  (let [{:keys [transmit!]} fulcro-remote]
    (assoc fulcro-remote
      :transmit!
        (fn retry-transmit! [this send-node]
          #_{:clj-kondo/ignore [:loop-without-recur]}
          (p/loop [tries-left amount-of-retry-attempts]
            (->
              (p/create
                (fn [resolve reject]
                  (transmit!
                    this
                    (->
                      send-node
                      (update
                        ::txn/result-handler
                        (fn [default-result-handler]
                          (fn [result]
                            (if (and (temporarily-unavailable? result) (not (last-attempt? tries-left)))
                              (reject :resolvable)
                              (resolve (default-result-handler result))))))))))
              (p/catch
                (fn [e]
                  (if (= e :resolvable)
                    (p/do!
                      (js/console.warn (str "Trying again in one second, attempts left " (dec tries-left)))
                      (p/delay 1000)
                      (p/recur (dec tries-left)))
                    (js/console.error "Unexpected error in remote" e))))))))))
