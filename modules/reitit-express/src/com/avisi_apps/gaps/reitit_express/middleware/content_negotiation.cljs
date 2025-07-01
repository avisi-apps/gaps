(ns com.avisi-apps.gaps.reitit-express.middleware.content-negotiation
  (:require
    [cljs-bean.core :refer [->js]]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.core :as core]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.json :as json]
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.transit :as transit]
    [hyperfiddle.rcf :refer [tests]]
    ["negotiator" :as negotiator]))

(defn extract-accept-header-ring "Extract accept header from ring request" [request] (get (:headers request) "accept"))

(defn content-type-str->media-type [accept-header supported-media-types]
  (let [media-type (.mediaType ^js (negotiator/Negotiator #js {:headers #js {:accept accept-header}}))]
    (if (= media-type "*/*") (first supported-media-types) (get (set supported-media-types) media-type))))

(tests
  "Basic application/json test"
    (content-type-str->media-type "application/json" ["application/json" "application/transit+json"])
  "Basic application/json test with utf-8 encoding"
    (content-type-str->media-type "application/json; charset=utf-8" ["application/json" "application/transit+json"])
  := "application/json"
  "Accept any */*" (content-type-str->media-type "*/*" ["application/json" "application/transit+json"])
  := "application/json"
  "Respects q-factor weighting"
    (content-type-str->media-type
      "application/*;q=0.8, application/transit+json;q=0.9"
      ["application/json" "application/transit+json"])
  := "application/transit+json"
  "Returns nil when nothing matches"
    (content-type-str->media-type "application/html" ["application/json" "application/transit+json"])
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
        content-type (->
                       (extract-content-type-ring request)
                       (content-type-str->media-type (keys formats)))]
    (cond-> request
      (and body content-type (contains? formats content-type))
        (assoc (if (= content-type "application/x-www-form-urlencoded") :form-params :body-params)
          (format-request body content-type formats)))))

(tests
  "Should handle transit"
    (->
      (negotiate-request
        {:headers {"content-type" "application/transit+json"}
         :body "[[\"^ \",\"~:id\",1],[\"^ \",\"^0\",2],[\"^ \",\"^0\",3]]"}
        default-formats)
      :body-params)
  := [{:id 1} {:id 2}
      {:id 3}]
  "Should handle json"
    (->
      (negotiate-request
        {:headers {"content-type" "application/json"}
         :body (->js [{:id 1} {:id 2} {:id 3}])}
        default-formats)
      :body-params)
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
                       (content-type-str->media-type (keys formats)))
          {:keys [encoder]
           :as format}
            (get formats (or media-type default-format))]
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
