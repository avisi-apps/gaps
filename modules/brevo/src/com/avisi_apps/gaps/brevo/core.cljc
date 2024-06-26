(ns com.avisi-apps.gaps.brevo.core
  (:require
    [clojure.string :as str]
    #?(:cljs [com.avisi-apps.gaps.log :as log])
    #?(:clj [clj-http.client :as http])
    #?(:clj [jsonista.core :as json])
    #?(:clj [clj-http.util :as http-util])
    #?(:cljs [goog.string :as gstring])
    #?(:cljs [promesa.core :as p])
    #?(:cljs [com.avisi-apps.gaps.brevo.axios-client :as axios-client])))

(def brevo-api-prefix "https://api.brevo.com/v3")

; Static attributes
(def full-name-attribute "FULL_NAME")

; Atlassian app specific attributes that store installation info
(def acrm-cloud-attribute "ATLASSIAN__CLOUD__ACRM")
(def acrm-serverdc-attribute "ATLASSIAN__SERVERDC__ACRM")
(def gfc-cloud-attribute "ATLASSIAN__CLOUD__GFC")
(def gfc-serverdc-attribute "ATLASSIAN__SERVERDC__GFC")
(def vfc-cloud-attribute "ATLASSIAN__CLOUD__VFC")
(def xsd-cloud-attribute "ATLASSIAN__CLOUD__XSD")
(def xsd-serverdc-attribute "ATLASSIAN__SERVERDC__XSD")
(def tfj-cloud-attribute "ATLASSIAN__CLOUD__TFJ")
(def mcf-cloud-attribute "ATLASSIAN__CLOUD__MCF")
(def mcf-serverdc-attribute "ATLASSIAN__SERVERDC__MCF")
(def uda-serverdc-attribute "ATLASSIAN__SERVERDC__UDA")
(def ut-serverdc-attribute "ATLASSIAN__SERVERDC__UT")


; monday.com app specific attributes that store installation info
(def tracket-attribute "MONDAY__TRACKET")
(def gitlab-for-monday-attribute "MONDAY__GITLABFORMONDAY")
(def github-for-monday-attribute "MONDAY__GITHUBFORMONDAY")

(def license-type-field "LICENSETYPE")
(def license-type-paid "PAID")
(def license-type-trial "TRIAL")
(def license-type-other "OTHER")

(def status-field "STATUS")
(def status-active "ACTIVE")
(def status-inactive "INACTIVE")
(def status-other "OTHER")

(def monday-monetization-enabled-field "MONDAY_MONETIZATION_ENABLED")

; Exceptions
(def document-not-found-code "document_not_found")

(defn create-app-attribute-string
  "Transforms clojure map into a string value for the app specific attribute
   {A 1, B 2} -> A:1__B:2"
  [clj-map]
  (str/join "__" (mapv (fn [[k v]] (str k ":" v)) clj-map)))

(defn read-app-attribute-string
  "Transforms string value into a clojure map for the app specific attribute
   A:1__B:2 -> {A 1, B 2}"
  [app-attribute]
  (if (str/blank? app-attribute)
    {}
    (reduce (fn [attr key-val] (let [[k v] (str/split key-val #":")] (assoc attr k v)))
      {}
      (str/split app-attribute #"__"))))

(defn url-encode [s]
  #?(:clj (http-util/url-encode s)
     :cljs (gstring/urlEncode s)))

(defn get-brevo-id-by-email [api-key email]
  (let [endpoint (str brevo-api-prefix "/contacts/" (url-encode email))]
    #?(:cljs
         (p/then
           (axios-client/get
             {:endpoint endpoint
              :headers {:api-key api-key}})
           (fn [result] (get-in (js->clj result :keywordize-keys true) [:data :id])))
       :clj
         (->
           (http/get
             endpoint
             {:headers {"api-key" api-key}
              :content-type :json})
           :body
           (json/read-value (json/object-mapper {:decode-key-fn true}))
           :id))))

(defn get-email-by-brevo-id [api-key brevo-id]
  (let [endpoint (str brevo-api-prefix "/contacts/" brevo-id)]
    #?(:cljs
         (p/then
           (axios-client/get
             {:endpoint endpoint
              :headers {:api-key api-key}})
           (fn [result] (get-in (js->clj result :keywordize-keys true) [:data :email])))
       :clj
         (->
           (http/get
             endpoint
             {:headers {"api-key" api-key}
              :content-type :json})
           :body
           (json/read-value (json/object-mapper {:decode-key-fn true}))
           :email))))

(defn- get-contact-app-attribute
  "Returns the attributes for app-attribute as a map."
  [api-key endpoint app-attribute]
  #?(:cljs
       (->
         (p/then
           (axios-client/get
             {:endpoint endpoint
              :headers {:api-key api-key}})
           (fn [result]
             (->
               (js->clj result :keywordize-keys true)
               (get-in [:data :attributes (keyword app-attribute)])
               (read-app-attribute-string))))
         (p/catch
           (fn [e]
             (let [code (->
                          (ex-data e)
                          (js->clj :keywordize-keys true)
                          (get-in [:response-data "code"]))]
               (if (= code document-not-found-code) (log/warn {:message "Contact could not be found"}) (throw e))))))
     :clj
       (try
         (->
           (http/get
             endpoint
             {:headers {"api-key" api-key}
              :content-type :json})
           :body
           (json/read-value (json/object-mapper {:decode-key-fn true}))
           :attributes
           (keyword app-attribute)
           (read-app-attribute-string))
         (catch Exception e
           (let [code (->
                        (ex-data e)
                        (json/read-value (json/object-mapper {:decode-key-fn true}))
                        (get-in [:response-data "code"]))]
             (when-not (= code document-not-found-code)
               (throw e)))))))

(defn get-contact-app-attribute-by-id [api-key brevo-id app-attribute]
  (if-not (str/blank? brevo-id)
    (let [endpoint (str brevo-api-prefix "/contacts/" brevo-id)]
      (get-contact-app-attribute api-key endpoint app-attribute))
    {}))

(defn get-contact-app-attribute-by-email [api-key email app-attribute]
  (if-not (str/blank? email)
    (let [endpoint (str brevo-api-prefix "/contacts/" (url-encode email))]
      (get-contact-app-attribute api-key endpoint app-attribute))
    {}))

(defn create-or-update-contact! "Returns the brevo ID of the newly created or updated contact."
  [api-key email attributes-map add-to-lists]
  (let [payload (cond->
                  {:email email
                   :attributes attributes-map
                   :updateEnabled true}
                  (seq add-to-lists) (assoc :listIds add-to-lists))
        endpoint (str brevo-api-prefix "/contacts")]
    #?(:cljs
         (p/then
           (axios-client/post
             {:endpoint endpoint
              :headers {:api-key api-key}
              :data payload})
           (fn [result]
             (or (get-in (js->clj result :keywordize-keys true) [:data :id]) (get-brevo-id-by-email api-key email))))
       :clj
         (or
           (->
             (http/post
               endpoint
               {:headers {"api-key" api-key}
                :content-type :json
                :form-params payload})
             :body
             (json/read-value (json/object-mapper {:decode-key-fn true}))
             :id)
           (get-brevo-id-by-email api-key email)))))

(defn update-contact-by-id! [api-key id attributes-map]
  (let [endpoint (str brevo-api-prefix "/contacts/" id)
        payload {:attributes attributes-map}]
    #?(:cljs
         (axios-client/put
           {:endpoint endpoint
            :headers {:api-key api-key}
            :data payload})
       :clj
         (http/put
           endpoint
           {:headers {"api-key" api-key}
            :content-type :json
            :form-params payload}))))

(defn ping [api-key]
  (let [endpoint (str brevo-api-prefix "/account")]
    #?(:cljs
         (axios-client/get
           {:endpoint endpoint
            :headers {:api-key api-key}})
       :clj (http/get endpoint {:headers {"api-key" api-key}}))))
