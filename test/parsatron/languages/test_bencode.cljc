(ns parsatron.languages.test-bencode
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :refer [run]]
            [parsatron.languages.bencode :refer [ben-integer ben-bytestring ben-list ben-dictionary]]
            #?(:clj  [clojure.test :refer [deftest are is]]
               :cljs [clojure.test :refer [run-tests] :refer-macros [deftest are is]]))
  #?(:clj (:import [clojure.lang ExceptionInfo])))

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
  (is (thrown? ExceptionInfo (run (ben-dictionary) "di42e4:spam4:spami42ee"))))

#?(:cljs (run-tests))
