(ns parsatron.languages.bf
  (:refer-clojure :exclude [char])
  (:require #?(:clj  [the.parsatron :refer [choice char many between eof defparser]]
               :cljs [the.parsatron :refer [choice char many between eof] :refer-macros [defparser]])))

(defparser instruction []
  (choice (char \>)
          (char \<)
          (char \+)
          (char \-)
          (char \.)
          (char \,)
          (between (char \[) (char \]) (many (instruction)))))

(defparser bf []
  (many (instruction))
  (eof))
