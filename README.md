# The Parsatron

Born from Haskell's Parsec library, The Parsatron is a functional
parser library. The Parsatron provides a lot of very small functions that can
be combined into larger ones to very quickly write parsers for languages.

Like all parser combinator libraries, The Parsatron produces recursive-descent
parsers that are best suited for LL(1) grammars. However, The Parsatron offers
*infinite lookahead* which means you can try and parse any insane thing you'd
like and if it doesn't work out, fall back to where you started. It's a feature
that's worked out well for others. I'm sure you'll find something useful to do
with it.


## Installation

You can use The Parsatron by including

    [the/parsatron "0.0.7"]

in your `project.clj` dependencies. It's available for download from Clojars.

## ClojureScript Support

The Parsatron has been ported to ClojureScript and is included in the
library distribution. There are a few small differences between ClojureScript
and Clojure that follow The Parsatron into the browser, namely:

* The Parsatron makes liberal use of macros: `>>`, `let->>` and
  `defparser` must be included via `:require-macros`
* ClojureScript has no notion of a character type like Clojure does.
  The Parsatron considers Strings of length 1 to be characters

The Parsatron's ClojureScript tests can be run by first building them:

    lein cljsbuild once

and then opening the html file [test/resources/parsatron_test.html](test/resources/parsatron_test.html)

## Usage

A basic syntax checker for a certain profane esoteric programming language could
be defined as follows:

    (defparser instruction []
      (choice (char \>)
              (char \<)
              (char \+)
              (char \-)
              (char \.)
              (char \,)
              (between (char \[) (char \]) (many (instruction)))))

    (defparser bf []
      (many (instruction))
      (eof))

The `defparser` forms create new parsers that you can combine into other, more
complex parsers. As you can see in this example, those parsers can be recursive.

The `choice`, `char`, `between` and `many` functions you see are themselves
combinators, provided *gratis* by the library. Some, like `choice`, `many`, and
`between`, take parsers as arguments and return you a new one, wholly different,
but exhibiting eerily familiar behavior. Some, like `char`, take less exotic input
(in this case, a humble character) and return more basic parsers, that perform
what is asked of them without hestitation or spite.

You execute a parser over some input via the `run` form.

    (run (bf) ",>++++++[<-------->-],[<+>-]<.")

Currently, The Parsatron only provides character-oriented parsers, but the ideas
it's built on are powerful enough that with the right series of commits, it can
be made to run over sequence of arbitrary "tokens". Clojure's handling of
sequences and sequence-like things is a feature deeply ingrained in the language's
ethos. Look for expansion in this area.

* * * * *

Beyond just verifying that a string is a valid member of some language, The
Parsatron offers you facilities for interacting with and operating on the things
you parse via sequencing of multiple parsers and binding their results. The
macros `>>` and `let->>` embody this facility.

As an example, [bencoded strings](http://en.wikipedia.org/wiki/Bencode) are prefixed by their length and a colon:

    (defparser ben-string []
      (let->> [length (integer)]
        (>> (char \:)
            (times length (any-char)))))

`let->>` allows you to capture and name the result of a parser so it's value may
be used later. `>>` is very similar to Clojure's `do` in that it executes it's
forms in order, but "throws away" all but the value of the last form.

    (run (ben-string) "4:spam") ;; => [\s \p \a \m]

## License

Copyright (C) 2011 Nate Young

Distributed under the Eclipse Public License, the same as Clojure.
