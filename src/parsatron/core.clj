(ns parsatron.core)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Base Types and Records

(defrecord InputState [input pos])
(defrecord SourcePos [name line column])

(defprotocol Consumable
  (get-reply [this]))

(deftype Consumed [reply]
  Consumable
  (get-reply [_] reply))
(deftype Empty [reply]
  Consumable
  (get-reply [_] reply))

(defrecord Ok [item state])
(defrecord Err [err])

(defprotocol ShowableError
  (show-error [this]))

(defrecord ParseError [pos msgs]
  ShowableError
  (show-error [_] (str (apply str msgs)
		       " at"
		       " line: " (:line pos)
		       " column: " (:column pos)
		       " in " (:name pos))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Error Handling/Creation fns

(defn unknown-error [{:keys [pos] :as state}]
  (ParseError. pos []))

(defn unexpect-error [msg pos]
  (ParseError. pos [msg]))

(defn merge-error [{:keys [pos] :as p} q]
  (ParseError. pos (concat (:msgs p) (:msgs q))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Raw parsers

(defn always [x]
  (fn [state cok cerr eok eerr]
    (eok x state)))

(defn parser-zero []
  (fn [state cok cerr eok eerr]
    (eerr (unknown-error state))))

(defn token-prim [show-f nextpos-f consume?]
  (fn [{:keys [input pos] :as state} cok cerr eok eerr]
    (if-let [s (seq input)]
      (let [item (first s)
	    rest-of-input (next s)]
	(if (consume? item)
	  (let [newpos (nextpos-f pos item rest-of-input)
		newstate (InputState. rest-of-input newpos)]
	    (cok item newstate))
	  (eerr (unexpect-error (show-f item) pos))))
      (eerr (unexpect-error "" pos)))))

(defn parser-plus [m n]
  (fn [state cok cerr eok eerr]
    (letfn [(meerr [err]
		   (letfn [(neok [item state-prime]
				 (eok item state-prime))
			   (neerr [err-prime]
				  (eerr (merge-error err err-prime)))]
		     (n state cok cerr neok neerr)))]
      (m state cok cerr eok meerr))))

(defn many-accum [p]
  (fn [state cok cerr eok eerr]
    (letfn [(many-err [err] (throw (RuntimeException. "combinator '*' is applied to a parser that accepts an empty string")))
	    (continue [coll item state-prime]
		      (p state-prime (partial continue (cons item coll)) cerr many-err (fn [_] (cok (reverse (cons item coll)) state-prime))))]
      (p state (partial continue (seq [])) cerr many-err (fn [_] (eok [] state))))))

(defn parser-bind [m f]
  (fn [state cok cerr eok eerr]
    (letfn [(mcok [item state]
		  (let [p (f item)]
		    (p state cok cerr cok cerr)))
	    (meok [item state]
		  (let [p (f item)]
		    (p state cok cerr eok eerr)))]
      (m state mcok cerr meok eerr))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Running parsers

(defn run-parser [p state]
  (letfn [(cok [item state-prime]
	       (Consumed. (Ok. item state-prime)))
	  (cerr [err]
		(Consumed. (Err. err)))
	  (eok [item state-prime]
	       (Empty. (Ok. item state-prime)))
	  (eerr [err]
		(Empty. (Err. err)))]
    (p state cok cerr eok eerr)))

(defn run-p [p source-name input]
  (let [result (run-parser p (InputState. input (SourcePos. source-name 1 1)))
	r (get-reply result)]
    (condp = (class r)
	Ok (:item r)
	Err (throw (RuntimeException. (show-error (:err r)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Position-related fns

(defn updatepos-char [{:keys [name line column]} c]
  (case c
	\newline (SourcePos. name (inc line) 1)
	\tab (SourcePos. name line (- (+ column 8) (mod (dec column) 8)))
	(SourcePos. name line (inc column))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Higher-level parsers

(defn satisfy [pred]
  (token-prim str
	      (fn [pos c cs]
		(updatepos-char pos c))
	      pred))

(defn char [c]
  (satisfy #(= c %)))

(defn one-of [cs]
  (satisfy (partial contains? (set cs))))

(defn digit []
  (satisfy #(Character/isDigit %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Macros

(defmacro >>
  ([m] m)
  ([m n] `(parser-bind ~m (fn [_#] ~n)))
  ([m n & more] `(>> (>> ~m ~n) ~@more)))

(defmacro parserlet [[& bindings] & body]
  (let [[bind-form p] (take 2 bindings)]
    (if (= 2 (count bindings))
      `(parser-bind ~p (fn [~bind-form] ~@body))
      `(parser-bind ~p (fn [~bind-form] (parserlet ~(drop 2 bindings) ~@body))))))