(defproject org.clojars.quoll/parsatron "0.0.10"
  :description "Clojure parser combinators"

  :dependencies [[org.clojure/clojure "1.10.2"]]

  :plugins [[lein-cljsbuild "1.1.8"]]

  :source-paths ["src"]
  :test-paths ["test"]

  :global-vars {*warn-on-reflection* false}

  :profiles {
    :dev {
      :dependencies [[org.clojure/clojurescript "1.10.773"]]}}

  :cljsbuild {
    :builds {
      :dev {
        :source-paths ["src"]
        :compiler {:optimizations :simple
                   :target :nodejs
                   :output-to "target/parsatron.js"}}
      :test {
        :source-paths ["src" "test"]
        :compiler {:optimizations :simple
                   :target :nodejs
                   :output-to "test/resources/parsatron_test.js"}} }
    :test-commands {"unit" ["node" "test/resources/parsatron_test.js"]}})
