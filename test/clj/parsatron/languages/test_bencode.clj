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
       {"42" "spam", "spam" 42} "d2:424:spam4:spami42ee"
       {"spam" ["a" "b"]} "d4:spaml1:a1:bee"
       {"name" "Mary"
        "age" 33
        "children" ["Betty", "Sam"]
        "address" {"street" "1 Home St"
                   "city" "Anywhere"}}
        "d4:name4:Mary3:agei33e8:childrenl5:Betty3:Same7:addressd6:street9:1 Home St4:city8:Anywhereee")
  (is (thrown?  RuntimeException (run (ben-dictionary) "di42e4:spam4:spami42ee"))))

