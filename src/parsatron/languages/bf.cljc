(ns parsatron.languages.bf
  (:refer-clojure :exclude [char char?])
  (:require [the.parsatron #?@(:clj [:refer [defparser]]) :as p])
  #?(:cljs (:require-macros [the.parsatron :refer [defparser]])))

(defparser instruction []
  (p/choice (p/char \>)
            (p/char \<)
            (p/char \+)
            (p/char \-)
            (p/char \.)
            (p/char \,)
            (p/between (p/char \[) (p/char \]) (p/many (instruction)))))

(defparser bf []
  (p/many (instruction))
  (p/eof))
