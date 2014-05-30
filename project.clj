(defproject the/parsatron "0.0.8-SNAPSHOT"
  :description "Clojure parser combinators"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2227"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]

  :global-vars {*warn-on-reflection* false}

  :cljsbuild {:builds [{:source-paths ["src/cljs" "test/cljs"]
                        :compiler {:output-to "test/resources/parsatron_test.js"}}]})
