(defproject the/parsatron "0.0.9-SNAPSHOT"
  :description "Clojure parser combinators"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :global-vars {*warn-on-reflection* false}

  :cljsbuild {:builds
                {:dev
                 {:source-paths ["src"]
                  :compiler {:optimizations :simple
                             :target :nodejs
                             :output-to "target/parsatron.js"}}
                 :test
                 {:source-paths ["src" "test"]
                  :compiler {:optimizations :simple
                             :target :nodejs
                             :output-to "test/resources/parsatron_test.js"}} }
              :test-commands { "unit" ["node" "test/resources/parsatron_test.js"]}})
