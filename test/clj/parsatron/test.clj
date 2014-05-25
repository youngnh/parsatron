(ns parsatron.test
  (:refer-clojure :exclude [char])
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
    (is (parser-result? 5 (either (never) (always 5)) "")))

  (testing "when neither succeed, errors are merged"
    (is (thrown-with-msg? RuntimeException #"Unexpected token 'c', Unexpected token 'c'"
          (run (either (char \a) (char \b)) "c")))))

(deftest test-attempt
  (testing "success returns value of p"
    (is (parser-result? \a (attempt (char \a)) "a")))

  (testing "failure is same as never"
    (is (thrown? RuntimeException (run (attempt (char \a)) "b")))
    (is (parser-result? \c (either (attempt (>> (char \a) (char \b)))
                                   (>> (char \a) (char \c))) "ac"))))

(deftest test-token
  (testing "throws error on empty input"
    (is (thrown-with-msg? RuntimeException #"Unexpected end of input"
          (run (token (constantly true)) ""))))

  (testing "consume? determines parser's behavior, show-f used in error message"
    (is (parser-result? \a (token (constantly true)) "a"))
    (is (thrown-with-msg? RuntimeException #"Unexpected token 'a'"
          (run (token (constantly false)) "a")))))

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
                        "aababbc")))

  (testing "does not blow the stack"
    (is (parser-result? (take 1000 (repeat \a))
                        (many (char \a))
                        (apply str (take 1000 (repeat \a)))))))

(deftest test-times
  (testing "0 times returns [], and does not consume"
    (is (parser-result? [] (times 0 (char \a)) "")))

  (testing "throws an error (from underlying parser) if fewer than specified"
    (are [input] (thrown-with-msg? RuntimeException #"Unexpected end of input"
                   (run (times 3 (char \a)) input))
         ""
         "a"
         "aa"))

  (testing "returns a list with the results"
    (is (parser-result? [\a \a \a] (times 3 (char \a)) "aaa"))
    (is (parser-result? [5 5 5] (times 3 (always 5)) "")))

  (testing "does not blow the stack"
    (is (parser-result? (take 10000 (repeat \a))
                        (times 10000 (char \a))
                        (apply str (take 10000 (repeat \a)))))))

(deftest test-lookahead
  (testing "returns value of p on success"
    (is (parser-result? \a (lookahead (char \a)) "a")))

  (testing "does not consume input on success"
    (is (parser-result? \a (>> (lookahead (char \a)) (char \a)) "a"))))

(deftest test-choice
  (testing "choice with no choices throws an exception"
    (is (thrown? RuntimeException (run (choice) ""))))

  (testing "first parser to succeed returns result"
    (are [input] (parser-result? (first input) (choice (char \a) (char \b) (char \c)) input)
         "a"
         "b"
         "c")))

(deftest test-eof
  (testing "parser succeeds, returns nil when no more input left"
    (is (parser-result? nil (eof) ""))
    (is (parser-result? nil (>> (char \a) (eof)) "a")))

  (testing "parser fails with message when input if left"
    (is (thrown-with-msg? RuntimeException #"Expected end of input"
          (run (eof) "a")))
    (is (thrown-with-msg? RuntimeException #"Expected end of input"
          (run (>> (char \a) (eof)) "ab")))))
