

package com.github.madbrain.parser

object LLParser {

  def apply(grammar: Grammar) = {
    val firsts = Firsts(grammar)

    val follows = Follows(grammar, firsts)

    // TODO the last .toMap doesn't check for conflicts
    val table = grammar.rules.flatMap(rule => {
      val (hasEpsilon, ruleFirsts) = firsts.get(rule.elements.toList)
      val linesFromFirsts = ruleFirsts.map(f => {
        rule.name -> (f, rule)
      })
      val linesFromFollows = if (hasEpsilon) {
        follows(rule.name).map(f => {
          rule.name -> (f.name, rule)
        })
      } else {
        Seq()
      }
      linesFromFirsts ++ linesFromFollows
    }).groupBy(_._1).mapValues(_.map(_._2).toMap)

    new LLParser(grammar.start, table, buildContinuations(grammar, firsts))
  }

  def buildContinuations(grammar: Grammar, firsts: Firsts) = {
    def computeRuleSteps(rule: Rule, steps: Map[String, Int]) = {
      rule.elements.foldLeft(1) {
        case (-1, _) => -1
        case (s, Term(_)) => s
        case (s, NonTerm(t)) =>
          steps.get(t) match {
            case Some(v) => s + v
            case None => -1
          }
      }
    }
    def computeSteps(initialSteps: Map[String, Int]): Map[String, Int] = {
      grammar.rules.foldLeft(initialSteps) { case (currentSteps, rule) =>
        val oldSteps = currentSteps(rule.name)
        val newSteps = computeRuleSteps(rule, currentSteps)
        if (newSteps >= 0 && (oldSteps < 0 || newSteps < oldSteps)) {
          currentSteps.updated(rule.name, newSteps)
        } else {
          currentSteps
        }
      }
    }
    val steps = Iterations.whileChanging(computeSteps, Map[String, Int]().withDefaultValue(-1))
    grammar.rules
      .filter(rule => computeRuleSteps(rule, steps) == steps(rule.name))
      .map(rule => rule.name -> (rule.elements, firsts.firsts(rule.name)))
      .toMap
  }
}

class LLParser(start: String,
               table: Map[String, Map[String, Rule]],
               continuationGrammar: Map[String, (Seq[Element], Set[FirstElement])]) {

  def parse(input: String*) {
    val tokens = scala.collection.mutable.ArrayBuffer(input:_*)
    val stack = scala.collection.mutable.ArrayBuffer[Element]()
    stack.append(NonTerm(start), Term("$"))

    while (stack.nonEmpty) {
      stack.head match {
        case NonTerm(t) =>
          val rule = table(t)(tokens.head)
          println(s"Predict $rule")
          stack.trimStart(1)
          stack.prependAll(rule.elements)
        case Term(t) =>
          println(s"Match $t <=> ${tokens.head}")
          if (t == tokens.head) {
            stack.trimStart(1)
            tokens.trimStart(1)
          } else {
            println(s"Parse error: expecting $t, got ${tokens.head}")
            val (continuation, acceptables) = buildContinuation(stack.toSeq)
            val tokenContinuation = continuation.flatMap { case Term(t) => Some(t) case _ => None }
            println(s"Continuation [ ${tokenContinuation.mkString(" ")} ]")
            println(s"Acceptables $acceptables")
            while (! acceptables.contains(tokens.head)) {
              tokens.trimStart(1)
            }
            val acc = tokens.head
            var i = 0
            while (i < tokenContinuation.size && acc != tokenContinuation(i)) {
              i += 1
            }
            tokens.prependAll(tokenContinuation.slice(0, i))
            println(s"New tokens [ ${tokens.mkString(" ")} ]")
          }
      }
    }
  }

  def buildContinuation(stack: Seq[Element], acc: Set[String] = Set[String]()): (Seq[Element], Set[String]) = {
    def filterTerminal(firsts: Set[FirstElement]) = firsts flatMap { case Terminal(t) => Some(t) case _ => None }
    val (newStack, newAcc) = stack.foldLeft((Seq[Element](), acc)) {
      case ((s, a), NonTerm(t)) =>
        val (e, aa) = continuationGrammar(t)
        (s ++ e, a ++ filterTerminal(aa))
      case ((s, a), Term(t)) =>
        (s :+ Term(t), a + t)
    }
    if (stack == newStack) {
      (newStack, newAcc)
    } else {
      buildContinuation(newStack, newAcc)
    }
  }
}



