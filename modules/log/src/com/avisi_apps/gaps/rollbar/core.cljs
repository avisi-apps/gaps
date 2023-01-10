(ns com.avisi-apps.gaps.rollbar.core
  (:require
    [hyperfiddle.rcf :refer [tests]]
    [com.avisi-apps.gaps.rollbar.api :as api]
    [com.avisi-apps.gaps.rollbar.config :as config]))

(defn validate-rollbar-config? [configuration]
  (if (and (contains? configuration :token) (string? (get configuration :token)))
    true
    false))

(defn log-additional-information [severity message]
  (let [configuration (config/get-rollbar-config)]
    (when (validate-rollbar-config? configuration)
      (api/send-to-rollbar configuration severity message))))

(tests
  "TC1: Should be valid config"
  (validate-rollbar-config? {:token "testToken"}) := true
  "TC2: Should be invalid config, token is wrong dataType"
  (validate-rollbar-config? {:token 123}) := false
  "TC3: Should be invalid config, missing the :token key"
  (validate-rollbar-config? {:randomKey "testValue"}) := false
  "TC4: Should be invalid config, the map is empty"
  (validate-rollbar-config? {}) := false)
