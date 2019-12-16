(ns the.parsatron-test
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :as sut #?@(:clj [:refer [>>]])]
            #?(:clj [clojure.test :refer [deftest is are testing]]
               :cljs [cljs.test :refer [deftest is are testing] :include-macros true]))
  #?(:cljs (:require-macros [the.parsatron :refer [>>]])))

(defn parser-result? [expected p input]
  (= expected (sut/run p input)))

(deftest test-always
  (is (parser-result? 5 (sut/always 5) ""))
  (is (parser-result? 5 (sut/always 5) "abc")))

(deftest test-nxt
  (is (parser-result? 5 (sut/nxt (sut/always 3)
                                 (sut/always 5)) "")))

(deftest test-bind
  (is (parser-result? 8 (sut/bind (sut/always 3)
                                  (fn [x]
                                    (sut/always (+ x 5)))) "")))

(deftest test-never
  (is (thrown? #?(:clj RuntimeException
                  :cljs js/Error) (sut/run (sut/never) "")))
  (is (thrown? #?(:clj RuntimeException
                  :cljs js/Error) (sut/run (sut/never) "abc"))))

(deftest test-either
  (testing "first parser succeeds"
    (is (parser-result? 5 (sut/either (sut/always 5) (sut/always 3)) "")))

  (testing "second parser succeeds, when first fails with empty"
    (is (parser-result? 5 (sut/either (sut/never) (sut/always 5)) "")))

  (testing "when neither succeed, errors are merged"
    (is (thrown-with-msg? #?(:clj RuntimeException
                             :cljs js/Error) #"Unexpected token 'c', Unexpected token 'c'"
                          (sut/run (sut/either (sut/char \a) (sut/char \b)) "c")))))

(deftest test-attempt
  (testing "success returns value of p"
    (is (parser-result? \a (sut/attempt (sut/char \a)) "a")))

  (testing "failure is same as never"
    (is (thrown? #?(:clj RuntimeException
                    :cljs js/Error) (sut/run (sut/attempt (sut/char \a)) "b")))
    (is (parser-result? \c (sut/either (sut/attempt (>> (sut/char \a) (sut/char \b)))
                                       (>> (sut/char \a) (sut/char \c))) "ac"))))

(deftest test-token
  (testing "throws error on empty input"
    (is (thrown-with-msg? #?(:clj RuntimeException
                             :cljs js/Error) #"Unexpected end of input"
                          (sut/run (sut/token (constantly true)) ""))))

  (testing "consume? determines parser's behavior, show-f used in error message"
    (is (parser-result? \a (sut/token (constantly true)) "a"))
    (is (thrown-with-msg? #?(:clj RuntimeException
                             :cljs js/Error) #"Unexpected token 'a'"
                          (sut/run (sut/token (constantly false)) "a")))))

(deftest test-many
  (testing "throws an exception if parser does not consume"
    (is (thrown-with-msg? #?(:clj RuntimeException
                             :cljs js/Error) #"Combinator '\*' is applied to a parser that accepts an empty string"
                          (sut/run (sut/many (sut/always 5)) ""))))

  (testing "returns empty list when no input consumed"
    (is (parser-result? [] (sut/many (sut/char \a)) "")))

  (testing "parser returns list of consumed items"
    (is (parser-result? [\a \a \b \a \b \b]
                        (sut/many (sut/either (sut/char \a)
                                              (sut/char \b)))
                        "aababbc")))

  (testing "does not blow the stack"
    (is (parser-result? (take 1000 (repeat \a))
                        (sut/many (sut/char \a))
                        (apply str (take 1000 (repeat \a)))))))

(deftest test-times
  (testing "0 times returns [], and does not consume"
    (is (parser-result? [] (sut/times 0 (sut/char \a)) "")))

  (testing "throws an error (from underlying parser) if fewer than specified"
    (are [input] (thrown-with-msg? #?(:clj RuntimeException
                                      :cljs js/Error) #"Unexpected end of input"
                                   (sut/run (sut/times 3 (sut/char \a)) input))
      ""
      "a"
      "aa"))

  (testing "returns a list with the results"
    (is (parser-result? [\a \a \a] (sut/times 3 (sut/char \a)) "aaa"))
    (is (parser-result? [5 5 5] (sut/times 3 (sut/always 5)) "")))

  (testing "does not blow the stack"
    (is (parser-result? (take 10000 (repeat \a))
                        (sut/times 10000 (sut/char \a))
                        (apply str (take 10000 (repeat \a)))))))

(deftest test-lookahead
  (testing "returns value of p on success"
    (is (parser-result? \a (sut/lookahead (sut/char \a)) "a")))

  (testing "does not consume input on success"
    (is (parser-result? \a (>> (sut/lookahead (sut/char \a)) (sut/char \a)) "a"))))

(deftest test-choice
  (testing "choice with no choices throws an exception"
    (is (thrown? #?(:clj RuntimeException
                    :cljs js/Error) (sut/run (sut/choice) ""))))

  (testing "first parser to succeed returns result"
    (are [input] (parser-result? (first input) (sut/choice (sut/char \a) (sut/char \b) (sut/char \c)) input)
      "a"
      "b"
      "c")))

(deftest test-eof
  (testing "parser succeeds, returns nil when no more input left"
    (is (parser-result? nil (sut/eof) ""))
    (is (parser-result? nil (>> (sut/char \a) (sut/eof)) "a")))

  (testing "parser fails with message when input if left"
    (is (thrown-with-msg? #?(:clj RuntimeException
                             :cljs js/Error) #"Expected end of input"
                          (sut/run (sut/eof) "a")))
    (is (thrown-with-msg? #?(:clj RuntimeException
                             :cljs js/Error) #"Expected end of input"
                          (sut/run (>> (sut/char \a) (sut/eof)) "ab")))))
