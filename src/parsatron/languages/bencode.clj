(ns parsatron.languages.bencode
  (:use [the.parsatron]))

(declare ben-value)

(defparser positive-int []
  (p-let [digits (many1 (digit))]
         (always (read-string (apply str digits)))))

(defparser negative-int []
  (p-let [digits (>> (char \-) (many1 (digit)))]
         (always (read-string (apply str digits)))))

(defparser ben-integer []
  (between (char \i) (char \e)
           (either
            (positive-int)
            (negative-int))))

(defparser ben-bytestring []
  (p-let [length (positive-int)
          _ (char \:)
          chars (times length (any-char))]
         (always (apply str chars))))

(defparser ben-list []
  (between (char \l) (char \e)
           (many (ben-value))))

(defparser ben-dictionary []
  (let [entry (p-let [key (ben-value)
                      val (ben-value)]
                     (always [key val]))]
    (between (char \d) (char \e)
             (p-let [entries (many entry)]
                    (always (into {} entries))))))

(defparser ben-value []
  (choice (ben-integer)
          (ben-bytestring)
          (ben-list)
          (ben-dictionary)))
