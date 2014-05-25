(ns parsatron.test-trampoline
  #+clj
  (:require
   [the.parsatron :as p]
   [clojure.test :refer :all])
  #+cljs
  (:require
   [the.parsatron :as p :refer [Continue Ok]]
   [cemerick.cljs.test])
  #+cljs
  (:require-macros [cemerick.cljs.test :refer [deftest testing is]])
  #+clj
  (:import (the.parsatron Continue Ok)))

(deftest test-always
  (testing "always is a fn"
    (is (fn? (p/always 5))))

  (testing "with no next parser, always returns Ok"
    (let [p (p/always 5)
          result (p nil nil nil (fn eok [item _] (Ok. item)) nil)]
      (is (= (Ok. 5) result))))

  (testing "bound to a next parser, always returns Continue"
    (let [p (p/bind (p/always 5) (fn [x] (p/always (+ x 2))))
          p-continue (p nil nil nil (fn eok [item _] (Ok. item)) nil)]
      (is (instance? Continue p-continue))
      (let [q-continue ((:fn p-continue))]
        (is (instance? Continue q-continue))
        (let [result ((:fn q-continue))]
          (is (= (Ok. 7) result)))))))
