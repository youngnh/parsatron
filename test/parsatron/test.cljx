(ns parsatron.test
  #+clj
  (:require
   [the.parsatron :as p]
   [clojure.test :refer :all])
  #+cljs
  (:require
   [the.parsatron :as p :include-macros true]
   [cemerick.cljs.test])
  #+cljs
  (:require-macros [cemerick.cljs.test :refer [deftest testing is are]])
  #+clj
  (:import [the.parsatron SourcePos]))

(defn parser-result? [expected p input]
  (= expected (p/run p input)))

(deftest test-always
  (is (parser-result? 5 (p/always 5) ""))
  (is (parser-result? 5 (p/always 5) "abc")))

(deftest test-nxt
  (is (parser-result? 5 (p/nxt (p/always 3)
                               (p/always 5)) "")))

(deftest test-bind
  (is (parser-result? 8 (p/bind (p/always 3)
                                (fn [x]
                                  (p/always (+ x 5)))) "")))

(deftest test-never
  (is (thrown? #+clj RuntimeException #+cljs js/Error
               (p/run (p/never) "")))
  (is (thrown? #+clj RuntimeException #+cljs js/Error
               (p/run (p/never) "abc"))))

(deftest test-either
  (testing "first parser succeeds"
    (is (parser-result? 5 (p/either (p/always 5) (p/always 3)) "")))

  (testing "second parser succeeds, when first fails with empty"
    (is (parser-result? 5 (p/either (p/never) (p/always 5)) "")))

  (testing "when neither succeed, errors are merged"
    (is (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                          #"Unexpected token 'c', Unexpected token 'c'"
                          (p/run (p/either (p/char \a) (p/char \b)) "c")))))

(deftest test-attempt
  (testing "success returns value of p"
    (is (parser-result? \a (p/attempt (p/char \a)) "a")))

  (testing "failure is same as never"
    (is (thrown? #+clj RuntimeException #+cljs js/Error
                 (p/run (p/attempt (p/char \a)) "b")))
    (is (parser-result? \c (p/either (p/attempt (p/>> (p/char \a) (p/char \b)))
                                     (p/>> (p/char \a) (p/char \c))) "ac"))))

(deftest test-token
  (testing "throws error on empty input"
    (is (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                          #"Unexpected end of input"
                          (p/run (p/token (constantly true)) ""))))

  (testing "consume? determines parser's behavior, show-f used in error message"
    (is (parser-result? \a (p/token (constantly true)) "a"))
    (is (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                          #"Unexpected token 'a'"
                          (p/run (p/token (constantly false)) "a")))))

(deftest test-many
  (testing "throws an exception if parser does not consume"
    (is (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                          #"Combinator '\*' is applied to a parser that accepts an empty string"
                          (p/run (p/many (p/always 5)) ""))))

  (testing "returns empty list when no input consumed"
    (is (parser-result? [] (p/many (p/char \a)) "")))

  (testing "parser returns list of consumed items"
    (is (parser-result? [\a \a \b \a \b \b]
                        (p/many (p/either (p/char \a)
                                          (p/char \b)))
                        "aababbc")))

  (testing "does not blow the stack"
    (is (parser-result? (take 1000 (repeat \a))
                        (p/many (p/char \a))
                        (apply str (take 1000 (repeat \a)))))))

(deftest test-times
  (testing "0 times returns [], and does not consume"
    (is (parser-result? [] (p/times 0 (p/char \a)) "")))

  (testing "throws an error (from underlying parser) if fewer than specified"
    (are [input] (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                                   #"Unexpected end of input"
                                   (p/run (p/times 3 (p/char \a)) input))
      ""
      "a"
      "aa"))

  (testing "returns a list with the results"
    (is (parser-result? [\a \a \a] (p/times 3 (p/char \a)) "aaa"))
    (is (parser-result? [5 5 5] (p/times 3 (p/always 5)) "")))

  (testing "does not blow the stack"
    (is (parser-result? (take 10000 (repeat \a))
                        (p/times 10000 (p/char \a))
                        (apply str (take 10000 (repeat \a)))))))

(deftest test-lookahead
  (testing "returns value of p on success"
    (is (parser-result? \a (p/lookahead (p/char \a)) "a")))

  (testing "does not consume input on success"
    (is (parser-result? \a (p/>> (p/lookahead (p/char \a)) (p/char \a)) "a"))))

(deftest test-choice
  (testing "choice with no choices throws an exception"
    (is (thrown? #+clj RuntimeException #+cljs js/Error (p/run (p/choice) ""))))

  (testing "first parser to succeed returns result"
    (are [input] (parser-result? (first input) (p/choice (p/char \a) (p/char \b) (p/char \c)) input)
      "a"
      "b"
      "c")))

(deftest test-eof
  (testing "parser succeeds, returns nil when no more input left"
    (is (parser-result? nil (p/eof) ""))
    (is (parser-result? nil (p/>> (p/char \a) (p/eof)) "a")))

  (testing "parser fails with message when input if left"
    (is (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                          #"Expected end of input" (p/run (p/eof) "a")))
    (is (thrown-with-msg? #+clj RuntimeException #+cljs js/Error
                          #"Expected end of input" (p/run (p/>> (p/char \a) (p/eof)) "ab")))))
