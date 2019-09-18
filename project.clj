(defproject org.clojars.quoll/parsatron "0.0.8"
  :description "Clojure parser combinators. Forked from Nate Young's Master"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]

  :global-vars {*warn-on-reflection* false}

  :cljsbuild {:builds [{:source-paths ["src/cljs" "test/cljs"]
                        :compiler {:optimizations :simple
                                   :target :nodejs
                                   :output-to "test/resources/parsatron_test.js"}}]
              :test-commands { "unit" ["node" "test/resources/parsatron_test.js"]}})
