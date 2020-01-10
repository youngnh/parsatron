(ns parsatron.languages.bf
  (:refer-clojure :exclude [char])
  (:require #?(:clj  [the.parsatron :refer [choice char many between eof defparser always let->>]]
               :cljs [the.parsatron :refer [choice char many between eof always]
                      :refer-macros [defparser let->>]])))

(defparser instruction []
  (choice (char \>)
          (char \<)
          (char \+)
          (char \-)
          (char \.)
          (char \,)
          (between (char \[) (char \]) (many (instruction)))))

(defparser bf []
  (let->> [result (many (instruction))
           _ (eof)]
    (always result)))
