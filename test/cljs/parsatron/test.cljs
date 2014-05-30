(ns parsatron.test
  (:require [the.parsatron :as p])
  (:import [goog.testing TestRunner TestCase])
  (:require-macros [the.parsatron :refer (>>)]))

(def tr (TestRunner.))
(def test (TestCase. "The Parsatron"))

(defn parser-result? [expected p input]
  (js/assertEquals expected (p/run p input)))

(defn throws-with-msg? [re f]
  (let [err (js/assertThrows f)]
    (js/assertTrue (.test re (.-message err)))))

(defn doeach [f & args]
  (doall (map f args)))



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
  (throws-with-msg? #"Unexpected token 'c', Unexpected token 'c'" #(p/run (p/either (p/char "a") (p/char "b")) "c")))

(.add test (TestCase.Test. "test-either" test-either))

(defn test-attempt []
  ;; success returns value of p
  (parser-result? "a" (p/attempt (p/char "a")) "a")

  ;; failure is same as never
  (js/assertThrows #(p/run (p/attempt (char "a")) "b"))
  (parser-result? "c" (p/either (p/attempt (>> (p/char "a") (p/char "b")))
                                (>> (p/char "a") (p/char "c"))) "ac"))

(.add test (TestCase.Test. "test-attempt" test-attempt))

(defn test-token []
  ;; throws error on empty input
  (throws-with-msg? #"Unexpected end of input" #(p/run (p/token (constantly true)) ""))

  ;; consume? determines parser's behavior, show-f used in error message
  (parser-result? "a" (p/token (constantly true)) "a")
  (throws-with-msg? #"Unexpected token 'a'" #(p/run (p/token (constantly false)) "a")))

(.add test (TestCase.Test. "test-token" test-token))

(defn test-many []
  ;; throws an error if parser does not consume
  (throws-with-msg? #"Combinator '\*' is applied to a parser that accepts an empty string" #(p/run (p/many (p/always 5)) ""))

  ;; returns empty list when no input consumed
  (parser-result? [] (p/many (p/char "a")) "")

  ;; parser returns list of consumed items
  (js/assertTrue (= ["a" "a" "b" "a" "b" "b"]
                    (p/run
                        (p/many (p/either (p/char "a")
                                          (p/char "b")))
                        "aababbc")))

  ;; does not blow the stack
  (js/assertTrue (= (take 1000 (repeat "a"))
                    (p/run
                        (p/many (p/char "a"))
                        (apply str (take 1000 (repeat "a")))))))

(.add test (TestCase.Test. "test-many" test-many))

(defn test-times []
  ;; 0 times returns [], and does not consume
  (parser-result? [] (p/times 0 (p/char "a")) "")

  ;; throws an error (from underlying parser) if fewer than specified
  (doeach
   (fn [input]
     (throws-with-msg? #"Unexpected end of input" #(p/run (p/times 3 (p/char "a")) input)))
   ""
   "a"
   "aa")

  ;; returns a list with the results
  (js/assertTrue (= ["a" "a" "a"] (p/run (p/times 3 (p/char "a")) "aaa")))
  (js/assertTrue (= [5 5 5] (p/run (p/times 3 (p/always 5)) "")))

  ;; does not blow the stack
  (js/assertTrue (= (take 10000 (repeat "a"))
                    (p/run
                        (p/times 10000 (p/char "a"))
                        (apply str (take 10000 (repeat "a")))))))

(.add test (TestCase.Test. "test-times" test-times))

(defn test-lookahead []
  ;; returns value of p on success
  (parser-result? "a" (p/lookahead (p/char "a")) "a")

  ;; does not consume input on success
  (parser-result? "a" (>> (p/lookahead (p/char "a")) (p/char "a")) "a"))

(.add test (TestCase.Test. "test-lookahead" test-lookahead))

(defn test-choice []
  ;; choice with no choices throws an exception
  (js/assertThrows #(p/run (p/choice) ""))

  ;; first parser to succeed returns result
  (doeach
   (fn [input]
     (parser-result? (first input) (p/choice (p/char "a") (p/char "b") (p/char "c")) input))
   "a"
   "b"
   "c"))

(.add test (TestCase.Test. "test-choice" test-choice))

(defn test-eof []
  ;; parser succeeds, returns nil when no more input left
  (parser-result? nil (p/eof) "")
  (parser-result? nil (>> (p/char "a") (p/eof)) "a")

  ;; parser fails with message when input if left
  (throws-with-msg? #"Expected end of input"
                    #(p/run (p/eof) "a"))
  (throws-with-msg? #"Expected end of input"
                    #(p/run (>> (p/char "a") (p/eof)) "ab")))

(.add test (TestCase.Test. "test-eof" test-eof))

(.initialize tr test)
(.execute tr)
