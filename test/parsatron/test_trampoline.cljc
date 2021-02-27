(ns parsatron.test-trampoline
  #?(:clj
     (:require [the.parsatron :refer [always bind]]
               [clojure.test :refer [deftest testing is]])
     :cljs
     (:require [the.parsatron :refer [always bind Continue Ok]]
               [clojure.test :refer [run-tests] :refer-macros [deftest testing is]]))
  #?(:clj
     (:import (the.parsatron Continue Ok))))

(deftest test-always
  (testing "always is a fn"
    (is (fn? (always 5))))

  (testing "with no next parser, always returns Ok"
    (let [p (always 5)
          result (p nil nil nil (fn eok [item _] (Ok. item)) nil)]
      (is (= (Ok. 5) result))))

  (testing "bound to a next parser, always returns Continue"
    (let [p (bind (always 5) (fn [x] (always (+ x 2))))
          p-continue (p nil nil nil (fn eok [item _] (Ok. item)) nil)]
      (is (instance? Continue p-continue))
      (let [q-continue ((:fn p-continue))]
        (is (instance? Continue q-continue))
        (let [result ((:fn q-continue))]
          (is (= (Ok. 7) result)))))))

#?(:cljs (run-tests))
