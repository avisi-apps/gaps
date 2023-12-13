(ns com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.json
  (:require
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.core :as core]
    [goog.object :as gobj]
    [hyperfiddle.rcf :refer [tests]]
    [cljs-bean.core :refer [->js ->clj]]))

(defn json-encoder []
  (reify
    core/Encode
      (encode [_ data] (->js data))))

(defn json-decoder []
  (reify
    core/Decode
      (decode [_ data] (->clj data))))

(def json-format
  (core/map->Format
    {:name "application/json"
     :encoder [json-encoder]
     :decoder [json-decoder]}))

(tests
  "decodes to json (we decode this straight from a js object since expressjs handles this for us)"
  (core/decode (json-decoder) #js {:foo "bar"})
  :=
  {:foo "bar"}
  "for encoding we should return a js object (again express does this for us)"
  (core/encode (json-encoder) {:foo "bar encoded"})
  (gobj/equals *1 #js {:foo "bar encoded"})
  :=
  true)
