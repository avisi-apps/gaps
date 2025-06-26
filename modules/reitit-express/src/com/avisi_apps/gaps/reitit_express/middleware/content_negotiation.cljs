(ns com.avisi-apps.gaps.reitit-express.middleware.content-negotiation
  (:require
    [cljs-bean.core :refer [->js]]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.core :as core]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.json :as json]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.transit :as transit]
    [hyperfiddle.rcf :refer [tests]]
    ["negotiator" :as negotiator]))

(defn extract-accept-header-ring "Extract accept header from ring request" [request] (get (:headers request) "accept"))

(defn accept-header->media-type [accept-header supported-media-types]
  (let [media-type (.mediaType ^js (negotiator/Negotiator #js {:headers #js {:accept accept-header}}))]
    (if (= media-type "*/*") (first supported-media-types) (get (set supported-media-types) media-type))))

(tests
  "Basic application/json test"
    (accept-header->media-type "application/json" ["application/json" "application/transit+json"])
  "Basic application/json test with utf-8 encoding"
    (accept-header->media-type "application/json; charset=utf-8" ["application/json" "application/transit+json"])
  := "application/json"
  "Accept any */*" (accept-header->media-type "*/*" ["application/json" "application/transit+json"])
  := "application/json"
  "Respects q-factor weighting"
    (accept-header->media-type
      "application/*;q=0.8, application/transit+json;q=0.9"
      ["application/json" "application/transit+json"])
  := "application/transit+json"
  "Returns nil when nothing matches"
    (accept-header->media-type "application/html" ["application/json" "application/transit+json"])
  := nil)

(defn extract-content-type-ring "Extracts content-type from ring-request."
  [request]
  (or (:content-type request) (get (:headers request) "content-type")))

(def default-formats
  {"application/json" json/json-format
   "application/transit+json" transit/json-format})

(def default-config
  {::formats default-formats
   ::default-format "application/json"})

(defn format-request [body content-type formats]
  (when-let [decoder (get-in formats [content-type :decoder])]
    (core/decode (apply (first decoder) (rest decoder)) body)))

(defn negotiate-request [request formats]
  (let [{:keys [body]} request
        content-type (extract-content-type-ring request)]
    (cond-> request
      (and body content-type (contains? formats content-type))
        (assoc :form-params (format-request body content-type formats)))))

(tests
  "Should handle transit"
    (->
      (negotiate-request
        {:headers {"content-type" "application/transit+json"}
         :body "[[\"^ \",\"~:id\",1],[\"^ \",\"^0\",2],[\"^ \",\"^0\",3]]"}
        default-formats)
      :form-params)
  := [{:id 1} {:id 2}
      {:id 3}]
  "Should handle json"
    (->
      (negotiate-request
        {:headers {"content-type" "application/json"}
         :body (->js [{:id 1} {:id 2} {:id 3}])}
        default-formats)
      :form-params)
  := [{:id 1} {:id 2}
      {:id 3}])

(defn negotiate-response
  [request
   {:keys [body]
    :as response}
   {::keys [formats default-format]}]
  (if (or (map? body) (sequential? body))
    (let [media-type (->
                       (extract-accept-header-ring request)
                       (accept-header->media-type (keys formats)))
          {:keys [encoder]
           :as format}
            (get formats media-type default-format)]
      (->
        response
        (update :body #(core/encode (apply (first encoder) (rest encoder)) %))
        (update-in [:headers "content-type"] #(if % % (:name format)))))
    response))

(defn format-middleware ([handler] (format-middleware handler default-config))
  ([handler
    {::keys [formats]
     :as config}]
   (fn [request respond raise]
     (handler (negotiate-request request formats) #(respond (negotiate-response request % config)) raise))))
