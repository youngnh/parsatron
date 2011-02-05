(ns parsatron.languages.bf)

(defparser instruction []
  (choice (char \>)
          (char \<)
          (char \+)
          (char \-)
          (char \.)
          (char \,)
          (between (char \[) (char \]) (many (instruction)))))

(defparser bf []
  (many (instruction)))
