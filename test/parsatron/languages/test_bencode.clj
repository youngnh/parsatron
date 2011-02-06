(ns parsatron.languages.test-bencode
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