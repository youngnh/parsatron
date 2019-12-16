(ns parsatron.languages.bf-test
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :as sut]
            [parsatron.languages.bf :refer [bf]]
            #?(:clj [clojure.test :refer [deftest are]]
               :cljs [cljs.test :refer [deftest are] :include-macros true])))

(deftest test-accepts-valid-brainf*ck
  (are [input] (try
                 (sut/run (bf) input)
                 true
                 (catch #?(:clj Throwable
                           :cljs js/Error) _
                   false))
       ">"
       "<"
       "+"
       "-"
       "."
       ","
       "[+]"
       ",>++++++[<-------->-],[<+>-]<."))

(deftest test-rejects-invalid-brainf*ck
  (are [input] (thrown? #?(:clj RuntimeException :cljs js/Error) (sut/run (bf) input))
       "a"
       "abc"
       "[+"
       "]"
       "[+>[+]"))
