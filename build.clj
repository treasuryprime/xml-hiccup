(ns build
  "tools.build entry points for xml-hiccup.

   Usage:
     clojure -T:build clean
     clojure -T:build jar
     clojure -T:build install
     clojure -T:build deploy     ; requires CLOJARS_USERNAME + CLOJARS_PASSWORD env vars"
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def lib 'xml-hiccup)
(def version "0.1.5")
(def class-dir "target/classes")
(def basis (delay (b/create-basis {:project "deps.edn"})))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def scm
  {:url                 "https://github.com/treasuryprime/xml-hiccup"
   :connection          "scm:git:git://github.com/treasuryprime/xml-hiccup.git"
   :developerConnection "scm:git:ssh://git@github.com/treasuryprime/xml-hiccup.git"
   :tag                 (or (System/getenv "GITHUB_SHA") version)})

(def pom-data
  [[:description "Parse XML into hiccup formatted data (updated fork of kuhumcst/xml-hiccup)"]
   [:url "https://github.com/treasuryprime/xml-hiccup"]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url "https://github.com/treasuryprime/xml-hiccup/blob/main/LICENSE"]]]])

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     @basis
                :src-dirs  ["src-clj"]
                :scm       scm
                :pom-data  pom-data})
  (b/copy-dir {:src-dirs ["src-clj"] :target-dir class-dir})
  (b/jar {:class-dir class-dir :jar-file jar-file})
  (println "Wrote" jar-file))

(defn install [_]
  (jar nil)
  (dd/deploy {:installer :local
              :artifact  jar-file
              :pom-file  (b/pom-path {:lib lib :class-dir class-dir})}))

(defn deploy
  "Deploy to GitHub Packages. Env vars required:
     CLOJARS_USERNAME -> GitHub username
     CLOJARS_PASSWORD -> GitHub PAT with `write:packages`"
  [_]
  (jar nil)
  (let [username (System/getenv "CLOJARS_USERNAME")
        password (System/getenv "CLOJARS_PASSWORD")
        blank?   #(or (nil? %) (= "" %))]
    (when (or (blank? username) (blank? password))
      (throw (ex-info "Missing CLOJARS_USERNAME or CLOJARS_PASSWORD env var. Set both to a GitHub username and a PAT with write:packages scope."
                      {:username-set? (not (blank? username))
                       :password-set? (not (blank? password))})))
    (dd/deploy {:installer      :remote
                :artifact       jar-file
                :pom-file       (b/pom-path {:lib lib :class-dir class-dir})
                :sign-releases? false
                :repository     {"github"
                                 {:url      "https://maven.pkg.github.com/treasuryprime/xml-hiccup"
                                  :username username
                                  :password password}}})))