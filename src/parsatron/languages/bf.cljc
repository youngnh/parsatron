(ns parsatron.languages.bf
  (:require #?(:clj  [the.parsatron :refer [choice ch many between eof defparser always let->>]]
               :cljs [the.parsatron :refer [choice ch many between eof always]
                      :refer-macros [defparser let->>]])))

(defparser instruction []
  (choice (ch \>)
          (ch \<)
          (ch \+)
          (ch \-)
          (ch \.)
          (ch \,)
          (between (ch \[) (ch \]) (many (instruction)))))

(defparser bf []
  (let->> [result (many (instruction))
           _ (eof)]
    (always result)))
