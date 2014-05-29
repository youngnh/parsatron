(ns parsatron.test
  (:require [the.parsatron :as p])
  (:import [goog.testing TestRunner TestCase]))

(def tr (TestRunner.))
(def test (TestCase. "The Parsatron"))

(defn parser-result? [expected p input]
  (js/assertEquals expected (p/run p input)))

(defn test-always []
  (parser-result? 5 (p/always 5) "")
  (parser-result? 5 (p/always 5) "abc"))

(.add test (TestCase.Test. "test-always" test-always))

(defn test-nxt []
  (parser-result? 5 (p/nxt (p/always 3)
                           (p/always 5)) ""))

(.add test (TestCase.Test. "test-nxt" test-nxt))

(defn test-bind []
  (parser-result? 8 (p/bind (p/always 3)
                            (fn [x]
                              (p/always (+ x 5)))) ""))

(.add test (TestCase.Test. "test-bind" test-bind))

(defn test-never []
  (js/assertThrows #(p/run (p/never) "")))

(.add test (TestCase.Test. "test-never" test-never))

(defn test-either []
  ;; first parser succeeds
  (parser-result? 5 (p/either (p/always 5) (p/always 3)) "")

  ;; second parser succeeds, when first fails with empty
  (parser-result? 5 (p/either (p/never) (p/always 5)) "")

  ;; when neither succeed, errors are merged
  (let [err (js/assertThrows #(p/run (p/either (p/char "a") (p/char "b")) "c"))]
    (js/assertTrue (.test #"Unexpected token 'c', Unexpected token 'c'" (.-message err)))))

(.add test (TestCase.Test. "test-either" test-either))

(.initialize tr test)
(.execute tr)
