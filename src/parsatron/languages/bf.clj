(ns parsatron.languages.bf
  (:refer-clojure :exclude [char])
  (:use [the.parsatron]))

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
