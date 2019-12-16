(ns parsatron.languages.bencode-test
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :as sut]
            [parsatron.languages.bencode :refer [ben-integer ben-bytestring ben-list ben-dictionary]]
            #?(:clj [clojure.test :refer [deftest is are]]
               :cljs [cljs.test :refer [deftest is are] :include-macros true])))

(deftest test-ben-integer
  (are [expected input] (= expected (sut/run (ben-integer) input))
       42 "i42e"))

(deftest test-ben-bytestring
  (are [expected input] (= expected (sut/run (ben-bytestring) input))
       "spam" "4:spam"))

(deftest test-ben-list
  (are [expected input] (= expected (sut/run (ben-list) input))
       [42 "spam"] "li42e4:spame"))

(deftest test-ben-dictionary
  (are [expected input] (= expected (sut/run (ben-dictionary) input))
       {"42" "spam", "spam" 42} "d2:424:spam4:spami42ee"
       {"spam" ["a" "b"]} "d4:spaml1:a1:bee"
       {"name" "Mary"
        "age" 33
        "children" ["Betty", "Sam"]
        "address" {"street" "1 Home St"
                   "city" "Anywhere"}}
        "d4:name4:Mary3:agei33e8:childrenl5:Betty3:Same7:addressd6:street9:1 Home St4:city8:Anywhereee")
  (is (thrown? #?(:clj RuntimeException :cljs js/Error) (sut/run (ben-dictionary) "di42e4:spam4:spami42ee"))))
