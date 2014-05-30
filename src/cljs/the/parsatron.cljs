(ns the.parsatron
  (:refer-clojure :exclude [char])
  (:require [clojure.string :as str])
  (:require-macros [the.parsatron :refer [defparser >> let->>]]))

(defrecord InputState [input pos])
(defrecord SourcePos [line column])

(defrecord Continue [fn])
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
;; trampoline
(defn parsatron-poline
  "A trampoline for executing potentially stack-blowing recursive
   functions without running out of stack space. This particular
   trampoline differs from clojure.core/trampoline by requiring
   continuations to be wrapped in a Continue record. Will loop until
   the value is no longer a Continue record, returning that."
  [f & args]
  (loop [value (apply f args)]
    (condp instance? value
      Continue (recur ((:fn value)))
      value)))

(defn sequentially [f value]
  (condp instance? value
    Continue (Continue. #(sequentially f ((:fn value))))
    (f value)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; host environment
(defn fail [message]
  (js/Error. message))

(defn char?
  "Test for a single-character string.

   ClojureScript doesn't support a character type, so we pretend it
   does"
  [x]
  (and (string? x) (= (count x) 1)))

(defn digit?
  "Tests if a character is a digit: [0-9]"
  [c]
  (re-matches #"\d" c))

(defn letter?
  "Tests if a character is a letter: [a-zA-Z]"
  [c]
  (re-matches #"[a-zA-Z]" c))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m
(defn always
  "A parser that always succeeds with the value given and consumes no
   input"
  [x]
  (fn [state cok cerr eok eerr]
    (eok x state)))

(defn bind
  "Parse p, and then q. The function f must be of one argument, it
   will be given the value of p and must return the q to follow p"
  [p f]
  (fn [state cok cerr eok eerr]
    (letfn [(pcok [item state]
              (sequentially
               (fn [q] (Continue. #(q state cok cerr cok cerr)))
               (f item)))
            (peok [item state]
              (sequentially
               (fn [q] (Continue. #(q state cok cerr eok eerr)))
               (f item)))]
      (Continue. #(p state pcok cerr peok eerr)))))

(defn nxt
  "Parse p and then q, returning q's value and discarding p's"
  [p q]
  (bind p (fn [_] q)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; m+
(defn never
  "A parser that always fails, consuming no input"
  []
  (fn [state cok cerr eok eerr]
    (eerr (unknown-error state))))

(defn either
  "A parser that tries p, upon success, returning its value, and upon
   failure (if no input was consumed) tries to parse q"
  [p q]
  (fn [state cok cerr eok eerr]
    (letfn [(peerr [err-from-p]
              (letfn [(qeerr [err-from-q]
                        (eerr (merge-errors err-from-p err-from-q)))]
                (Continue. #(q state cok cerr eok qeerr))))]
      (Continue. #(p state cok cerr eok peerr)))))

(defn attempt
  "A parser that will attempt to parse p, and upon failure never
   consume any input"
  [p]
  (fn [state cok cerr eok eerr]
    (Continue. #(p state cok eerr eok eerr))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; interacting with the parser's state

(defn extract
  "Extract information from the Parser's current state. f should be a
   fn of one argument, the parser's current state, and any value that
   it deems worthy of returning will be returned by the entire parser.
   No input is consumed by this parser, and the state itself is not
   altered."
  [f]
  (fn [state _ _ eok _]
    (eok (f state) state)))

(defn examine
  "Return the Parser's current state"
  []
  (extract identity))

(defn lineno
  "A parser that returns the current line number. It consumes no input"
  []
  (extract (comp :line :pos)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; token
(defn token
  "Consume a single item from the head of the input if (consume? item)
   is not nil. This parser will fail to consume if either the consume?
   test returns nil or if the input is empty"
  [consume?]
  (fn [{:keys [input pos] :as state} cok cerr eok eerr]
    (if-not (empty? input)
      (let [tok (first input)]
        (if (consume? tok)
          (cok tok (InputState. (rest input) (inc-sourcepos pos tok)))
          (eerr (unexpect-error (str "token '" tok "'") pos))))
      (eerr (unexpect-error "end of input" pos)))))

(defn many
  "Consume zero or more p. A RuntimeException will be thrown if this
   combinator is applied to a parser that accepts the empty string, as
   that would cause the parser to loop forever"
  [p]
  (letfn [(many-err [_ _]
            (fail "Combinator '*' is applied to a parser that accepts an empty string"))
          (safe-p [state cok cerr eok eerr]
            (Continue. #(p state cok cerr many-err eerr)))]
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
    (let->> [x p
             xs (times (dec n) p)]
      (always (cons x xs)))))

(defn lookahead
  "A parser that upon success consumes no input, but returns what was
   parsed"
  [p]
  (fn [state cok cerr eok eerr]
    (letfn [(ok [item _]
              (eok item state))]
      (Continue. #(p state ok cerr eok eerr)))))

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
      (eok nil state)
      (eerr (expect-error "end of input" pos)))))

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
  (token digit?))

(defn letter
  "Consume a letter [a-zA-Z] character"
  []
  (token letter?))

(defn string
  "Consume the given string"
  [s]
  (reduce nxt (concat (map char s)
                      (list (always s)))))

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
  "Execute a parser p, given some state, Returns Ok or Err"
  [p state]
  (parsatron-poline p state
                    (fn cok [item _]
                      (Ok. item))
                    (fn cerr [err]
                      (Err. (show-error err)))
                    (fn eok [item _]
                      (Ok. item))
                    (fn eerr [err]
                      (Err. (show-error err)))))

(defn run
  "Run a parser p over some input. The input can be a string or a seq
   of tokens, if the parser produces an error, its message is wrapped
   in a RuntimeException and thrown, and if the parser succeeds, its
   value is returned"
  [p input]
  (let [result (run-parser p (InputState. input (SourcePos. 1 1)))]
    (condp instance? result
      Ok (:item result)
      Err (throw (fail ^String (:errmsg result))))))
