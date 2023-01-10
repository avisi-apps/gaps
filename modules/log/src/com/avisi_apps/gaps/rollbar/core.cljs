(ns com.avisi-apps.gaps.rollbar.core
  (:require
    [hyperfiddle.rcf :refer [tests]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.avisi-apps.gaps.rollbar.api :as api]
    [com.avisi-apps.gaps.rollbar.track-error :as track-error]
    [com.avisi-apps.gaps.rollbar.config :as config]))

(defn check-error-tracking-permission [configuration]
  (let [state (app/current-state (:app configuration))]
  (get-in state [:error-tracking :track-error?])))

(defn validate-rollbar-config? [configuration]
  (if (and (contains? configuration :log/token) (string? (get configuration :log/token)))
    true
    false))

(tests
  "TC1: Should be valid config"
  (validate-rollbar-config? {:log/token "testToken"}) := true
  "TC2: Should be invalid config, token is wrong dataType"
  (validate-rollbar-config? {:log/token 123}) := false
  "TC3: Should be invalid config, missing the :token key"
  (validate-rollbar-config? {:token "testValue"}) := false
  "TC4: Should be invalid config, the map is empty"
  (validate-rollbar-config? {}) := false)

(defn log-additional-information [severity message]
  (let [configuration (config/get-rollbar-config)]
    (when (and (validate-rollbar-config? configuration) (check-error-tracking-permission configuration))
      (api/send-to-rollbar configuration severity message))))
