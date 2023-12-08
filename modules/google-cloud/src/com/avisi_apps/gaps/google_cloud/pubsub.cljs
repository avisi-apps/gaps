(ns com.avisi-apps.gaps.google-cloud.pubsub
  (:require
    ["@google-cloud/pubsub" :refer [PubSub]]
    [promesa.core :as p]
    [com.avisi-apps.gaps.log :as log]))

(defn publish-message! [topic-id payload]
  (->
    (p/let [js-payload (js/Buffer.from (js/JSON.stringify (clj->js payload)))
            message-id (->
                         ^js (PubSub.)
                         (.topic topic-id)
                         (.publish js-payload))]
      message-id)
    (p/then
      (fn [message-id]
        (log/info
          {:message "Published message"
           :body payload
           :message-id message-id})))
    (p/catch
      (fn [err]
        (log/error
          err
          {:message "Could not publish message"
           :body payload})))))
