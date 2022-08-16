(ns build
  (:require [clojure.core.server :as server]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml])
  (:import [java.io File]))

(defn name->module [n]
  (symbol "com.avisi-apps.gaps" n))

(def scm-url "git@github.com:avisi-apps/gaps.git")
(def version (format "0.0.%s-SNAPSHOT" (b/git-count-revs nil)))
(def modules-folder (b/resolve-path "modules"))
(def current-branch (b/git-process {:git-args "branch --show-current"}))
(def release-branch "master")
(def modules (->>
               (.listFiles ^File modules-folder)
               (filter #(.isDirectory ^File %))
               (mapv (fn [^File file] (name->module (.getName file))))))

(defn sha
  [{:keys [dir path] :or {dir "."}}]
  (-> {:command-args (cond-> ["git" "rev-parse" "HEAD"]
                       path (conj "--" path))
       :dir (.getPath (b/resolve-path dir))
       :out :capture}
    b/process
    :out
    str/trim))

(defn local-lib->mvn-lib [[k {:deps/keys [manifest] :as v}]]
  (if (= manifest :deps)
    [k {:mvn/version version
        :deps/manifest :mvn
        :parents #{[]}}]
    [k v]))

(defn module-dir [{:keys [lib]}]
  (format "./modules/%s" (name lib)))

(defn build-module [{:keys [lib] :as opts}]
  (let [dir (module-dir opts)
        module-sha (sha {})]
    (with-bindings {#'clojure.tools.build.api/*project-root* dir}
      (let [basis (-> (b/create-basis)
                    (update :libs #(into {} (map local-lib->mvn-lib) %)))]
        (-> opts
          (assoc :version version
                 :basis basis
                 :scm {:tag module-sha
                       :connection (str "scm:git:" scm-url)
                       :developerConnection (str "scm:git:" scm-url)
                       :url scm-url})
          (bb/jar)
          (bb/install))))))

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
        (assoc
          :lib module)
        (build-module)
        (release-module))) modules))

(defn repl [{:keys [port] :or {port 5555}}]
  (server/start-server {:name "build repl"
                        :port port
                        :accept 'clojure.core.server/repl})
  (println "Started repl server on port: " port)
  @(promise))

(defn changed-files-from-release-branch []
  (when (not= release-branch current-branch)
    (->
      (b/git-process {:git-args "diff master --name-only"})
      (str/split-lines))))

(defn file-path->affected-module [p]
  (some->
    (re-find #"^modules\/([^\/]*)\/.*$" p)
    second))

(comment
  (file-path->affected-module "modules/log/README.md") := "log"
  (file-path->affected-module "README.md") := nil)


(defn gen-workflow-for-module [])

(defn generate-ci-config [opts]
  (let [changed-modules (into #{}
                          (keep file-path->affected-module)
                          (changed-files-from-release-branch))
        base-config (->
                      (io/file ".circleci/continue-config.yml")
                      (slurp)
                      (yaml/parse-string))
        config-with-changed-modules (reduce
                                      (fn [config module]
                                        ;; Here we could possibly add extra build test for certain modules
                                        config)
                                      base-config
                                      changed-modules)]
    (println "Generate generated-config.yml")
    (spit (io/file "generated-config.yml")
      (yaml/generate-string config-with-changed-modules :dumper-options {:flow-style :block}))))

(comment
  (generate-ci-config {})


  (def base-config (->
                     (io/file ".circleci/continue-config.yml")
                     (slurp)
                     (yaml/parse-string)))

  (assoc-in base-config [:workflows] )


  (let [release-branch  "master"
        current-branch (b/git-process {:git-args "branch --show-current"})]
    (when (not= release-branch current-branch)
      (->
        (b/git-process {:git-args "diff master --name-only"})
        (str/split-lines))))

  (into #{}
    (keep file-path->affected-module)
    (changed-files-from-release-branch))

  (build {})

  (release-module {:lib 'avisi-apps/rcf})


  (build-module {:lib 'com.avisi-apps.gaps/log})


  )
