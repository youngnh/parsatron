(ns parsatron.languages.bencode
  (:refer-clojure :exclude [char])
  (:use [the.parsatron]))

(declare ben-value)

(defparser positive-int []
  (let->> [digits (many1 (digit))]
    (always (read-string (apply str digits)))))

(defparser negative-int []
  (let->> [digits (>> (char \-) (many1 (digit)))]
    (always (read-string (apply str digits)))))

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

(defn comparer [a b]
  (if (and (string? a) (string? b))
    (compare a b)
    (throw (ex-info "Dictionary keys must be strings"
                    (let [faulty-key (if (string? a) b a)]
                      {:key faulty-key :type (type faulty-key)})))))

(defparser ben-dictionary []
  (let [entry (let->> [key (ben-value)
                       val (ben-value)]
                (always [key val]))]
    (between (char \d) (char \e)
             (let->> [entries (many entry)]
               (always (into (sorted-map-by comparer) entries))))))

(defparser ben-value []
  (choice (ben-integer)
          (ben-bytestring)
          (ben-list)
          (ben-dictionary)))
