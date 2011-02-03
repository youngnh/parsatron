(ns twoguys.parsatron
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

(defn merge-errors [{:keys [pos] :as err} other-err]
  (ParseError. pos (flatten (concat (:msgs err) (:msgs other-err)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m
(defn always [x]
  (fn [state cok cerr eok eerr]
    (eok x state)))

(defn next [p q]
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
;; macros
(defmacro >>
  ([m] m)
  ([m n] `(next ~m ~n))
  ([m n & ms] `(next ~m (>> ~n ~@ms))))

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