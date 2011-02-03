(ns twoguys.parsatron
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; macros
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