(ns parsatron.test
  (:use [the.parsatron]
        [clojure.test])
  (:import (the.parsatron SourcePos)))

(defn parser-result? [expected p input]
  (= expected (run p input)))

(deftest test-always
  (is (parser-result? 5 (always 5) ""))
  (is (parser-result? 5 (always 5) "abc")))

(deftest test-nxt
  (is (parser-result? 5 (nxt (always 3)
                             (always 5)) "")))

(deftest test-bind
  (is (parser-result? 8 (bind (always 3)
                              (fn [x]
                                (always (+ x 5)))) "")))

(deftest test-never
  (is (thrown? RuntimeException (run (never) "")))
  (is (thrown? RuntimeException (run (never) "abc"))))

(deftest test-either
  (testing "first parser succeeds"
    (is (parser-result? 5 (either (always 5) (always 3)) "")))

  (testing "second parser succeeds, when first fails with empty"
    (is (parser-result? 5 (either (never) (always 5)) ""))))

(deftest test-token
  (testing "throws error on empty input"
    (let [consume? (constantly true)
          nxtpos (constantly (SourcePos. 1 2))
          show-token (constantly "a")]
      (is (thrown-with-msg? RuntimeException #"Input is empty"
            (run (token consume? nxtpos show-token) "")))))

  (testing "consume? determines parser's behavior, show-f used in error message"
    (let [consume (constantly true)
          dont-consume (constantly false)
          nxtpos (constantly (SourcePos. 1 2))
          show-token (constantly "a")]
      (is (parser-result? \a (token consume nxtpos show-token) "a"))
      (is (thrown-with-msg? RuntimeException #"Found unexpected a"
            (run (token dont-consume nxtpos show-token) "a"))))))

(deftest test-many
  (testing "throws an exception if parser does not consume"
    (is (thrown-with-msg? RuntimeException #"Combinator '\*' is applied to a parser that accepts an empty string"
          (run (many (always 5)) ""))))

  (testing "returns empty list when no input consumed"
    (is (parser-result? [] (many (char \a)) "")))

  (testing "parser returns list of consumed items"
    (is (parser-result? [\a \a \b \a \b \b]
                        (many (either (char \a)
                                      (char \b)))
                        "aababbc"))))