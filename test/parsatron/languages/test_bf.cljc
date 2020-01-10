(ns parsatron.languages.test-bf
  (:refer-clojure :exclude [char])
  (:require [the.parsatron :refer [run]]
        [parsatron.languages.bf :refer [bf]]
        #?(:clj  [clojure.test :refer [deftest are]]
           :cljs [clojure.test :refer [run-tests] :refer-macros [deftest are]]))
  #?(:clj (:import [clojure.lang ExceptionInfo])))

(deftest test-accepts-valid-brainf*ck
  (are [input] (try
                 (run (bf) input)
                 true
                 (catch ExceptionInfo _
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
  (are [input] (thrown? ExceptionInfo (run (bf) input))
       "a"
       "abc"
       "[+"
       "]"
       "[+>[+]"))

#?(:cljs (run-tests))
