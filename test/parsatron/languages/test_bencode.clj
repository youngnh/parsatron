(ns parsatron.languages.test-bencode
  (:refer-clojure :exclude [char])
  (:use [the.parsatron]
        [parsatron.languages.bencode]
        [clojure.test]))

(deftest test-ben-integer
  (are [expected input] (= expected (run (ben-integer) input))
       42 "i42e"))

(deftest test-ben-bytestring
  (are [expected input] (= expected (run (ben-bytestring) input))
       "spam" "4:spam"))

(deftest test-ben-list
  (are [expected input] (= expected (run (ben-list) input))
       [42 "spam"] "li42e4:spame"))

(deftest test-ben-dictionary
  (are [expected input] (= expected (run (ben-dictionary) input))
       {42 "spam", "spam" 42} "di42e4:spam4:spami42ee"))
