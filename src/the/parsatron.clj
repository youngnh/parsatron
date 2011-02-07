(ns the.parsatron
  (:refer-clojure :exclude [char])
  (:require [clojure.string :as str]))

(defrecord InputState [input pos])
(defrecord SourcePos [line column])

(defrecord Ok [item])
(defrecord Err [errmsg])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; errors
(defprotocol ShowableError
  (show-error [this]))

(defrecord ParseError [pos msgs]
  ShowableError
  (show-error [_] (str (str/join ", " msgs)
                       " at"
                       " line: " (:line pos)
                       " column: " (:column pos))))

(defn unknown-error [{:keys [pos] :as state}]
  (ParseError. pos ["Error"]))

(defn unexpect-error [msg pos]
  (ParseError. pos [msg]))

(defn merge-errors [{:keys [pos] :as err} other-err]
  (ParseError. pos (flatten (concat (:msgs err) (:msgs other-err)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m
(defn always [x]
  (fn [state cok cerr eok eerr]
    (eok x state)))

(defn nxt [p q]
  (fn [state cok cerr eok eerr]
    (letfn [(pcok [item state]
                  (q state cok cerr cok cerr))
            (peok [item state]
                  (q state cok cerr eok eerr))]
      (p state pcok cerr peok eerr))))

(defn bind [p f]
  (fn [state cok cerr eok eerr]
    (letfn [(pcok [item state]
                  (let [q (f item)]
                    (q state cok cerr cok cerr)))
            (peok [item state]
                  (let [q (f item)]
                    (q state cok cerr eok eerr)))]
      (p state pcok cerr peok eerr))))

(defmacro defparser [name args & body]
  `(defn ~name ~args
     (fn [state# cok# cerr# eok# eerr#]
       (let [p# (>> ~@body)]
         (p# state# cok# cerr# eok# eerr#)))))

(defmacro >>
  ([m] m)
  ([m n] `(nxt ~m ~n))
  ([m n & ms] `(nxt ~m (>> ~n ~@ms))))

(defmacro p-let [[& bindings] & body]
  (let [[bind-form p] (take 2 bindings)]
    (if (= 2 (count bindings))
      `(bind ~p (fn [~bind-form] ~@body))
      `(bind ~p (fn [~bind-form] (p-let ~(drop 2 bindings) ~@body))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m+
(defn never []
  (fn [state cok cerr eok eerr]
    (eerr (unknown-error state))))

(defn either [p q]
  (fn [state cok cerr eok eerr]
    (letfn [(peerr [err-from-p]
                   (letfn [(qeerr [err-from-q]
                                  (eerr (merge-errors err-from-p err-from-q)))]
                     (q state cok cerr eok qeerr)))]
      (p state cok cerr eok peerr))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; token
(defn token [consume? nextpos-f show-f]
  (fn [{:keys [input pos] :as state} cok cerr eok eerr]
    (if-let [s (seq input)]
      (let [item (first s)
            rest-of-input (next s)]
        (if (consume? item)
          (let [newpos (nextpos-f pos item rest-of-input)
                newstate (InputState. rest-of-input newpos)]
            (cok item newstate))
          (eerr (unexpect-error (str "Found unexpected " (show-f item)) pos))))
      (eerr (unexpect-error "Input is empty" pos)))))

(defn many [p]
  (fn [state cok cerr eok eerr]
    (letfn [(many-err [_ _]
                      (throw (RuntimeException. "Combinator '*' is applied to a parser that accepts an empty string")))
            (pcok [coll]
                  (fn [item state]
                    (letfn [(exit-cok [_]
                                      (cok (conj coll item) state))]
                      (p state (pcok (conj coll item)) cerr many-err exit-cok))))
            (peerr [_]
                   (eok [] state))]
      (p state (pcok []) cerr many-err peerr))))

(defn times [n p]
  (if (= n 0)
    (always [])
    (fn [state cok cerr eok eerr]
      (letfn [(pcok [item state]
                    (let [q (times (dec n) p)]
                      (letfn [(qcok [items state]
                                    (cok (cons item items) state))]
                        (q state qcok cerr qcok eerr))))
              (peok [item state]
                    (eok (repeat n item) state))]
        (p state pcok cerr peok eerr)))))

(defn choice [& parsers]
  (if (empty? parsers)
    (never)
    (let [p (first parsers)]
      (either p (apply choice (rest parsers))))))

(defn updatepos-char [{:keys [line column]} c]
  (case c
        \newline (SourcePos. (inc line) 1)
        (SourcePos. line (inc column))))

(defn satisfy [pred]
  (token pred
         (fn [pos c cs]
           (updatepos-char pos c))
         str))

(defn char [c]
  (satisfy #(= c %)))

(defn any-char []
  (satisfy (constantly true)))

(defn digit []
  (satisfy #(Character/isDigit %)))

(defn letter []
  (satisfy #(Character/isLetter %)))

(defn between [open close p]
  (p-let [_ open
          x p
          _ close]
         (always x)))

(defn many1 [p]
  (p-let [x p
          xs (many p)]
         (always (cons x xs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; run parsers
(defn run-parser [p state]
  (letfn [(cok [item ztate]
               (Ok. item))
          (cerr [err]
                (Err. err))
          (eok [item state-prime]
               (Ok. item))
          (eerr [err]
                (Err. (show-error err)))]
    (p state cok cerr eok eerr)))

(defn run [p input]
  (let [result (run-parser p (InputState. input (SourcePos. 1 1)))]
    (condp = (class result)
        Ok (:item result)
        Err (throw (RuntimeException. (:errmsg result))))))
