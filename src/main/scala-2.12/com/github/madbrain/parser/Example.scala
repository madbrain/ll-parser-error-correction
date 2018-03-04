package com.github.madbrain.parser

object Example {
  def main(args: Array[String]) {
    val grammar = Grammar("Session", Set(
      Rule("Session", Seq(NonTerm("Facts"), NonTerm("Question"))),
      Rule("Session", Seq(Term("("), NonTerm("Session"), Term(")"), NonTerm("Session"))),
      Rule("Facts", Seq()),
      Rule("Facts", Seq(NonTerm("Fact"), NonTerm("Facts"))),
      Rule("Fact", Seq(Term("!"), Term("STRING"))),
      Rule("Question", Seq(Term("?"), Term("STRING")))
    ))

    val parser = LLParser(grammar)

    parser.parse("(", "?", "STRING", "!", "STRING", "$")
  }
}
