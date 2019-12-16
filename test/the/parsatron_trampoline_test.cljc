(ns the.parsatron-trampoline-test
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :as sut #?@(:cljs [:refer [Continue Ok]])]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing] :include-macros true]))
  #?(:clj (:import [the.parsatron Continue Ok])))

(deftest test-always
  (testing "always is a fn"
    (is (fn? (sut/always 5))))

  (testing "with no next parser, always returns Ok"
    (let [p (sut/always 5)
          result (p nil nil nil (fn eok [item _] (Ok. item)) nil)]
      (is (= (Ok. 5) result))))

  (testing "bound to a next parser, always returns Continue"
    (let [p (sut/bind (sut/always 5) (fn [x] (sut/always (+ x 2))))
          p-continue (p nil nil nil (fn eok [item _] (Ok. item)) nil)]
      (is (instance? Continue p-continue))
      (let [q-continue ((:fn p-continue))]
        (is (instance? Continue q-continue))
        (let [result ((:fn q-continue))]
          (is (= (Ok. 7) result)))))))
