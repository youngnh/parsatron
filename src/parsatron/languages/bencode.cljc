(ns parsatron.languages.bencode
  (:refer-clojure :exclude [char])
  (:require
   #?(:clj [the.parsatron
            :refer [defparser digit char any-char many many1 always either between choice let->> >> times]]
      :cljs [the.parsatron
             :refer [digit char any-char many many1 always either between choice times]
             :refer-macros [let->> >> defparser]])))

(declare ben-value)

(def to-int #?(:clj read-string :cljs int))

(defparser positive-int []
  (let->> [digits (many1 (digit))]
    (always (to-int (apply str digits)))))

(defparser negative-int []
  (let->> [digits (>> (char \-) (many1 (digit)))]
    (always (to-int (apply str digits)))))

(defparser ben-integer []
  (between (char \i) (char \e)
           (either
            (positive-int)
            (negative-int))))

(defparser ben-bytestring []
  (let->> [length (positive-int)
           _ (char \:)
           chars (times length (any-char))]
    (always (apply str chars))))

(defparser ben-list []
  (between (char \l) (char \e)
           (many (ben-value))))

(defparser ben-dictionary []
  (let [entry (let->> [key (ben-bytestring)
                       val (ben-value)]
                (always [key val]))]
    (between (char \d) (char \e)
             (let->> [entries (many entry)]
               (always (into (sorted-map) entries))))))

(defparser ben-value []
  (choice (ben-integer)
          (ben-bytestring)
          (ben-list)
          (ben-dictionary)))
