(ns parsatron.test
  #?(:clj
     (:require [the.parsatron :refer [run always nxt bind never either ch attempt token
                                      many times lookahead choice eof >>]]
               [clojure.test :refer [is are deftest testing]])
     :cljs
     (:require [the.parsatron :refer [run always nxt bind never either ch attempt token
                                      many times lookahead choice eof SourcePos]
                              :refer-macros [>>]]
               [clojure.test :refer [run-tests] :refer-macros [is are deftest testing]]))
  #?(:clj
     (:import (the.parsatron SourcePos)
              (clojure.lang ExceptionInfo))))

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
  (is (thrown? ExceptionInfo (run (never) "")))
  (is (thrown? ExceptionInfo (run (never) "abc"))))

(deftest test-either
  (testing "first parser succeeds"
    (is (parser-result? 5 (either (always 5) (always 3)) "")))

  (testing "second parser succeeds, when first fails with empty"
    (is (parser-result? 5 (either (never) (always 5)) "")))

  (testing "when neither succeed, errors are merged"
    (is (thrown-with-msg? ExceptionInfo #"Unexpected token 'c', Unexpected token 'c'"
          (run (either (ch \a) (ch \b)) "c")))))

(deftest test-attempt
  (testing "success returns value of p"
    (is (parser-result? \a (attempt (ch \a)) "a")))

  (testing "failure is same as never"
    (is (thrown? ExceptionInfo (run (attempt (ch \a)) "b")))
    (is (parser-result? \c (either (attempt (>> (ch \a) (ch \b)))
                                   (>> (ch \a) (ch \c))) "ac"))))

(deftest test-token
  (testing "throws error on empty input"
    (is (thrown-with-msg? ExceptionInfo #"Unexpected end of input"
          (run (token (constantly true)) ""))))

  (testing "consume? determines parser's behavior, show-f used in error message"
    (is (parser-result? \a (token (constantly true)) "a"))
    (is (thrown-with-msg? ExceptionInfo #"Unexpected token 'a'"
          (run (token (constantly false)) "a")))))

(deftest test-many
  (testing "throws an exception if parser does not consume"
    (is (thrown-with-msg? ExceptionInfo #"Combinator '\*' is applied to a parser that accepts an empty string"
          (run (many (always 5)) ""))))

  (testing "returns empty list when no input consumed"
    (is (parser-result? [] (many (ch \a)) "")))

  (testing "parser returns list of consumed items"
    (is (parser-result? [\a \a \b \a \b \b]
                        (many (either (ch \a)
                                      (ch \b)))
                        "aababbc")))

  (testing "does not blow the stack"
    (is (parser-result? (take 1000 (repeat \a))
                        (many (ch \a))
                        (apply str (take 1000 (repeat \a)))))))

(deftest test-times
  (testing "0 times returns [], and does not consume"
    (is (parser-result? [] (times 0 (ch \a)) "")))

  (testing "throws an error (from underlying parser) if fewer than specified"
    (are [input] (thrown-with-msg? ExceptionInfo #"Unexpected end of input"
                   (run (times 3 (ch \a)) input))
         ""
         "a"
         "aa"))

  (testing "returns a list with the results"
    (is (parser-result? [\a \a \a] (times 3 (ch \a)) "aaa"))
    (is (parser-result? [5 5 5] (times 3 (always 5)) "")))

  (testing "does not blow the stack"
    (is (parser-result? (take 10000 (repeat \a))
                        (times 10000 (ch \a))
                        (apply str (take 10000 (repeat \a)))))))

(deftest test-lookahead
  (testing "returns value of p on success"
    (is (parser-result? \a (lookahead (ch \a)) "a")))

  (testing "does not consume input on success"
    (is (parser-result? \a (>> (lookahead (ch \a)) (ch \a)) "a"))))

(deftest test-choice
  (testing "choice with no choices throws an exception"
    (is (thrown? ExceptionInfo (run (choice) ""))))

  (testing "first parser to succeed returns result"
    (are [input] (parser-result? (first input) (choice (ch \a) (ch \b) (ch \c)) input)
         "a"
         "b"
         "c")))

(deftest test-eof
  (testing "parser succeeds, returns nil when no more input left"
    (is (parser-result? nil (eof) ""))
    (is (parser-result? nil (>> (ch \a) (eof)) "a")))

  (testing "parser fails with message when input if left"
    (is (thrown-with-msg? ExceptionInfo #"Expected end of input"
          (run (eof) "a")))
    (is (thrown-with-msg? ExceptionInfo #"Expected end of input"
          (run (>> (ch \a) (eof)) "ab")))))

#?(:cljs (run-tests))
