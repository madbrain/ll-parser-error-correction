
LL(1) Parser Error Correction
=============================

Toy implementation of the error correction algorithm described in [1] as presented in [2] (on page 535 §16.6.3).

## Example

```scala
val grammar = Grammar("Session", Set(
  Rule("Session", Seq(NonTerm("Facts"), NonTerm("Question"))),
  Rule("Session", Seq(Term("("), NonTerm("Session"), Term(")"), NonTerm("Session"))),
  Rule("Facts", Seq()),
  Rule("Facts", Seq(NonTerm("Fact"), NonTerm("Facts"))),
  Rule("Fact", Seq(Term("!"), Term("STRING"))),
  Rule("Question", Seq(Term("?"), Term("STRING")))
))

val parser = LLParser(grammar)

parser.parse("(", "?", "STRING", "?", "STRING", "$")
```

This example prints the parsing process including the error correction when parsing the second `?`.

## TODO

Symbols from the first set of the original grammar are used as accepted symbols,
but the continuation may not contains theses symbols as it is constructed from the continuation grammar.
If the input stack contains an acceptable symbol not contained in the continuation, the whole
continuation is inserted and the user input after the accepted symbol won't be used anymore.

From the example grammar, the input string `( ? STRING ! STRING $` produce a parse error on `!` which
is an acceptable symbol but can't be produce by the continuation grammar.
 
A better solution would be to build a continuation which tends to use firsts k symbols from the input stack.
During expansion of non-terminal from the continuation grammar, rule should be chosen if FIRST of its RHS
contains a symbol from the input stack window. If multiple (or none) match choose the one with lowest step count.

## Bibliography

* [1] Röhrich, J. Acta Informatica (1980) 13: 115. https://doi.org/10.1007/BF00263989

* [2] Grune, D. & Jacobs, C.J.H Parsing Techniques: a Practical Guide. https://doi.org/10.1007/978-0-387-68954-8

