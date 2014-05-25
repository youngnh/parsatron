(defproject the/parsatron "0.0.6-SNAPSHOT"
  :description "Clojure parser combinators"

  :dependencies
  [[org.clojure/clojure "1.6.0"]
   [org.clojure/clojurescript "0.0-2202"]]

  :dev-dependencies
  [[swank-clojure "1.4.2"]]

  :plugins
  [[com.keminglabs/cljx "0.3.1" :exclusions [org.clojure/clojurescript]]
   [com.cemerick/clojurescript.test "0.3.0"]
   [lein-cljsbuild "1.0.3"]]

  :source-paths ["src" "target/generated-src/clj" "target/generated-src/cljs"]
  :test-paths ["src" "target/generated-test"]

  :aliases
  {"run-tests" ["do"
                "test,"
                "cljx" "once,"
                "cljsbuild" "test"]}

  :cljx
  {:builds [{:source-paths ["src/"]
             :output-path "target/generated-src/clj"
             :rules :clj}
            {:source-paths ["src/"]
             :output-path "target/generated-src/cljs"
             :rules :cljs}
            {:source-paths ["test"]
             :output-path "target/generated-test"
             :rules :clj}
            {:source-paths ["test"]
             :output-path "target/generated-test"
             :rules :cljs}]}

  :cljsbuild
  {:builds [{:source-paths ["target/generated-src/cljs" "target/generated-test"]
             :compiler {:output-to "target/cljs/testable.js"
                        ;; Automatically run the ClojureScript test
                        ;; suite on compile.
                        :notify-command ["phantomjs" :cljs.test/runner "target/cljs/testable.js"]
                        :optimizations :simple
                        :pretty-print true}}]
   :test-commands {"unit-tests" ["phantomjs" :cljs.test/runner "target/cljs/testable.js"]}}

  :global-vars {*warn-on-reflection* true})
