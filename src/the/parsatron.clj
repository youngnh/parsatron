(ns the.parsatron
  (:refer-clojure :exclude [char])
  (:require [clojure.string :as str]))

(defrecord InputState [input pos])
(defrecord SourcePos [line column])

(defrecord Cont [fn])
(defrecord Ok [item])
(defrecord Err [errmsg])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; position
(defn inc-sourcepos
  "Increment the source position by a single character, c. On newline,
   increments the SourcePos's line number and resets the column, on
   all other characters, increments the column"
  [{:keys [line column]} c]
  (if (= c \newline)
    (SourcePos. (inc line) 1)
    (SourcePos. line (inc column))))

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
  (ParseError. pos [(str "Unexpected " msg)]))

(defn expect-error [msg pos]
  (ParseError. pos [(str "Expected " msg)]))

(defn merge-errors [{:keys [pos] :as err} other-err]
  (ParseError. pos (flatten (concat (:msgs err) (:msgs other-err)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m

(defn always
  "A parser that always succeeds with the value given and consumes no
   input"
  [x]
  (fn [state cok cerr eok eerr]
    (Cont. #(eok x state))))

(defn bind
  "Parse p, and then q. The function f must be of one argument, it
   will be given the value of p and must return the q to follow p"
  [p f]
  (fn [state cok cerr eok eerr]
    (letfn [(pcok [item state]
              (let [q (f item)]
                (Cont. #(q state cok cerr cok cerr))))
            (peok [item state]
              (let [q (f item)]
                (Cont. #(q state cok cerr eok eerr))))]
      (Cont. #(p state pcok cerr peok eerr)))))

(defn nxt
  "Parse p and then q, returning q's value and discarding p's"
  [p q]
  (bind p (fn [_] q)))

(defmacro defparser
  "Defines a new parser. Parsers are simply functions that accept the
   5 arguments state, cok, cerr, eok, eerr but this macro takes care
   of writing that ceremony for you and wraps the body in a >>"
  [name args & body]
  `(defn ~name ~args
     (fn [state# cok# cerr# eok# eerr#]
       (let [p# (>> ~@body)]
         (Cont. #(p# state# cok# cerr# eok# eerr#))))))

(defmacro >>
  "Expands into nested nxt forms"
  ([m] m)
  ([m n] `(nxt ~m ~n))
  ([m n & ms] `(nxt ~m (>> ~n ~@ms))))

(defmacro let->>
  "Expands into nested bind forms"
  [[& bindings] & body]
  (let [[bind-form p] (take 2 bindings)]
    (if (= 2 (count bindings))
      `(bind ~p (fn [~bind-form] ~@body))
      `(bind ~p (fn [~bind-form] (let->> ~(drop 2 bindings) ~@body))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m+
(defn never
  "A parser that always fails, consuming no input"
  []
  (fn [state cok cerr eok eerr]
    (Cont. #(eerr (unknown-error state)))))

(defn either
  "A parser that tries p, upon success, returning its value, and upon
   failure (if no input was consumed) tries to parse q"
  [p q]
  (fn [state cok cerr eok eerr]
    (letfn [(peerr [err-from-p]
              (letfn [(qeerr [err-from-q]
                        (Cont. #(eerr (merge-errors err-from-p err-from-q))))]
                (Cont. #(q state cok cerr eok qeerr))))]
      (Cont. #(p state cok cerr eok peerr)))))

(defn attempt
  "A parser that will attempt to parse p, and upon failure never
   consume any input"
  [p]
  (fn [state cok cerr eok eerr]
    (Cont. #(p state cok eerr eok eerr))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; token
(defn token
  "Consume a single item from the head of the input if (consume? item)
   is not nil. This parser will fail to consume if either the consume?
   test returns nil or if the input is empty"
  [consume?]
  (fn [{:keys [input pos] :as state} cok cerr eok eerr]
    (if-let [tok (first input)]
      (if (consume? tok)
        (Cont. #(cok tok (InputState. (rest input) (inc-sourcepos pos tok))))
        (Cont. #(eerr (unexpect-error (str "token '" tok "'") pos))))
      (Cont. #(eerr (unexpect-error "end of input" pos))))))

(defn many
  "Consume zero or more p. A RuntimeException will be thrown if this
   combinator is applied to a parser that accepts the empty string, as
   that would cause the parser to loop forever"
  [p]
  (letfn [(many-err [_ _]
            (throw (RuntimeException. "Combinator '*' is applied to a parser that accepts an empty string")))
          (safe-p [state cok cerr eok eerr]
            (p state cok cerr many-err eerr))]
    (either
     (let->> [x safe-p
              xs (many safe-p)]
       (always (cons x xs)))
     (always []))))

(defn times
  "Consume exactly n number of p"
  [n p]
  (if (= n 0)
    (always [])
    (fn [state cok cerr eok eerr]
      (letfn [(pcok [item state]
                (let [q (times (dec n) p)]
                  (letfn [(qcok [items state]
                            (Cont. #(cok (cons item items) state)))]
                    (Cont. #(q state qcok cerr qcok eerr)))))
              (peok [item state]
                (Cont. #(eok (repeat n item) state)))]
        (Cont. #(p state pcok cerr peok eerr))))))

(defn lookahead
  "A parser that upon success consumes no input, but returns what was
   parsed"
  [p]
  (fn [state cok cerr eok eerr]
    (letfn [(ok [item _]
              (Cont. #(eok item state)))]
      (Cont. #(p state ok cerr eok eerr)))))

(defn choice
  "A varargs version of either that tries each given parser in turn,
   returning the value of the first one that succeeds"
  [& parsers]
  (if (empty? parsers)
    (never)
    (let [p (first parsers)]
      (either p (apply choice (rest parsers))))))

(defn eof
  "A parser to detect the end of input. If there is nothing more to
   consume from the underlying input, this parser suceeds with a nil
   value, otherwise it fails"
  []
  (fn [{:keys [input pos] :as state} cok cerr eok eerr]
    (if (empty? input)
      (Cont. #(eok nil state))
      (Cont. #(eerr (expect-error "end of input" pos))))))

(defn char
  "Consume the given character"
  [c]
  (token #(= c %)))

(defn any-char
  "Consume any character"
  []
  (token #(char? %)))

(defn digit
  "Consume a digit [0-9] character"
  []
  (token #(Character/isDigit %)))

(defn letter
  "Consume a letter [a-zA-Z] character"
  []
  (token #(Character/isLetter %)))

(defn between
  "Parse p after parsing open and before parsing close, returning the
   value of p and discarding the values of open and close"
  [open close p]
  (let->> [_ open
           x p
           _ close]
    (always x)))

(defn many1
  "Consume 1 or more p"
  [p]
  (let->> [x p
           xs (many p)]
    (always (cons x xs))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; run parsers
(defn run-parser
  "Execute a parser p, given some state. Returns Ok or Err"
  [p state]
  (letfn [(cok [item _]
            (Ok. item))
          (cerr [err]
            (Err. (show-error err)))
          (eok [item _]
            (Ok. item))
          (eerr [err]
            (Err. (show-error err)))]
    (p state cok cerr eok eerr)))

(defn run
  "Run a parser p over some input. The input can be a string or a seq
   of tokens, if the parser produces an error, its message is wrapped
   in a RuntimeException and thrown, and if the parser succeeds, its
   value is returned"
  [p input]
  (let [state (InputState. input (SourcePos. 1 1))]
    (loop [result (run-parser p state)]
      (condp = (class result)
        Cont (recur ((:fn result)))
        Ok (:item result)
        Err (throw (RuntimeException. (:errmsg result)))))))
