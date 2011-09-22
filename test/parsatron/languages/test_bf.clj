(ns parsatron.languages.test-bf
  (:refer-clojure :exclude [char])
  (:use [the.parsatron]
        [parsatron.languages.bf]
        [clojure.test]))

(deftest test-accepts-valid-brainf*ck
  (are [input] (try
                 (run (bf) input)
                 true
                 (catch Exception _
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
  (are [input] (thrown? RuntimeException (run (bf) input))
       "a"
       "abc"
       "[+"
       "]"
       "[+>[+]"))