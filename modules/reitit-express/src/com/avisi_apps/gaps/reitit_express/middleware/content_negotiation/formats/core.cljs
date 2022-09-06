(ns com.avisi-apps.gaps.reitit-express.middleware.content-negotiation.formats.core)

(defprotocol Decode
  (decode [this data]))

(defprotocol Encode
  (encode [this data]))

(defrecord Format [name encoder decoder return matches])
