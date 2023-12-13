(ns com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.transit
  (:require
    [com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.core :as core]
    [hyperfiddle.rcf :refer [tests]]
    [cognitect.transit :as t]))

(defn json-encoder []
  (let [writer (t/writer :json)]
    (reify
      core/Encode
        (encode [_ data] (t/write writer data)))))

(defn json-decoder []
  (let [reader (t/reader :json)]
    (reify
      core/Decode
        (decode [_ data] (t/read reader data)))))

(def json-format
  (core/map->Format
    {:name "application/transit+json"
     :encoder [json-encoder]
     :decoder [json-decoder]}))

(tests
  "Test roundtrip of transit encoder"
  (core/encode (json-encoder) {:foo ["bar"]})
  (core/decode (json-decoder) *1)
  :=
  {:foo ["bar"]})
