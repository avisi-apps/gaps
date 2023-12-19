(ns com.avisi-apps.gaps.google-cloud.pubsub
  (:require
    ["@google-cloud/pubsub" :refer [PubSub]]
    [promesa.core :as p]
    [com.avisi-apps.gaps.log :as log]))

(defn publish-message! [topic-id {:keys [data attributes]}]
  (->
    (p/let [js-payload (js/Buffer.from (js/JSON.stringify (clj->js data)))
            message-id (->
                         ^js (PubSub.)
                         (.topic topic-id)
                         (.publishMessage
                           ; despite the google docs saying otherwise we have to add both props even if they are null,
                           ; otherwise the client-library throws an error
                           #js
                            {:data js-payload
                             :attributes (clj->js attributes)}))]
      message-id)
    (p/then
      (fn [message-id]
        (log/info
          {:message "Published message"
           :data data
           :attributes attributes
           :message-id message-id})))
    (p/catch
      (fn [err]
        (log/error
          err
          {:message "Could not publish message"
           :data data
           :attributes attributes})))))
