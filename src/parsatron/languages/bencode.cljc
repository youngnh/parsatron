(ns parsatron.languages.bencode
  (:refer-clojure :exclude [char char?])
  (:require [the.parsatron #?@(:clj [:refer [defparser >>]]) :as p])
  #?(:cljs (:require-macros [the.parsatron :refer [defparser >>]])))

(declare ben-value)

(defparser positive-int []
  (p/let->> [digits (p/many1 (p/digit))]
            (p/always (#?(:clj Long/parseLong
                          :cljs js/parseFloat) (apply str digits)))))

(defparser negative-int []
  (p/let->> [digits (>> (p/char \-) (p/many1 (p/digit)))]
            (p/always (#?(:clj Long/parseLong
                          :cljs js/parseFloat) (apply str digits)))))

(defparser ben-integer []
  (p/between (p/char \i) (p/char \e)
             (p/either
              (positive-int)
              (negative-int))))

(defparser ben-bytestring []
  (p/let->> [length (positive-int)
             _ (p/char \:)
             chars (p/times length (p/any-char))]
            (p/always (apply str chars))))

(defparser ben-list []
  (p/between (p/char \l) (p/char \e)
             (p/many (ben-value))))

(defparser ben-dictionary []
  (let [entry (p/let->> [key (ben-bytestring)
                         val (ben-value)]
                        (p/always [key val]))]
    (p/between (p/char \d) (p/char \e)
               (p/let->> [entries (p/many entry)]
                         (p/always (into (sorted-map) entries))))))

(defparser ben-value []
  (p/choice (ben-integer)
            (ben-bytestring)
            (ben-list)
            (ben-dictionary)))
