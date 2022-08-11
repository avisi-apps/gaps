(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.core.server :as server]
            [clojure.tools.deps.alpha :as deps]
            [clojure.java.io :as io]
            [org.corfield.build :as bb])
  (:import [java.io File]))

(def version (format "0.0.%s-SNAPSHOT" (b/git-count-revs nil)))
(def modules-folder (b/resolve-path "modules"))
(def modules (->>
               (.listFiles ^File modules-folder)
               (filter #(.isDirectory ^File %))
               (mapv (fn [^File file] (symbol "com.avisi-apps.gaps" (.getName file))))))

(defn local-lib->mvn-lib [[k {:deps/keys [manifest] :as v}]]
  (if (= manifest :deps)
    [k {:mvn/version version
        :deps/manifest :mvn
        :parents #{[]}}]
    [k v]))

(defn module-dir [{:keys [lib]}]
  (format "./modules/%s" (name lib)))

(defn build-module [{:keys [lib] :as opts}]
  (with-bindings {#'clojure.tools.build.api/*project-root* (module-dir opts)}
    (let [basis (-> (b/create-basis)
                  (update :libs #(into {} (map local-lib->mvn-lib) %)))]
      (-> opts
        (assoc :version version
               :basis basis)
        (bb/jar)
        (bb/install)))))

(defn build [opts]
  (run!
    (fn [module]
      (println "Building module " module)
      (build-module {:lib module})
      (println "Built module " module))
    modules)
  opts)

(defn clean [opts]
  (run!
    (fn [module]
      (println "Cleaning module " module)
      (bb/clean {:target (str (module-dir {:lib module}) "/target")})
      (println "Cleaned module " module))
    modules)
  opts)

(defn release-module [{:keys [lib] :as opts}]
  (with-bindings {#'clojure.tools.build.api/*project-root* (module-dir opts)}
    (->
      (assoc opts
             :version version
             :artifact (str (module-dir opts) "/" (bb/default-jar-file lib version)))
      (bb/deploy))))

(defn release [opts]
  (clean opts)
  (run!
    (fn [module]
      (->
        opts
        (assoc :lib module)
        (build-module)
        (release-module))) modules))

(defn repl [{:keys [port] :or {port 5555}}]
  (server/start-server {:name "build repl"
                        :port port
                        :accept 'clojure.core.server/repl})
  (println "Started repl server on port: " port)
  @(promise))

(comment
  (build {})

  (release-module {:lib 'avisi-apps/rcf})


  (build-module {:lib 'avisi-apps/log})
  )
