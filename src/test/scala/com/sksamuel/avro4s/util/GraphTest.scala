package com.sksamuel.avro4s.util

import org.scalatest.{Matchers, WordSpec}

class GraphTest extends WordSpec with Matchers {

  "graph" should {
    "add edge" in {
      val g = Graph[String]().addEdge("a", "b")
      g.outgoing("a") shouldBe Seq("b")
      g.incoming("a") shouldBe Seq.empty
      g.incoming("b") shouldBe Seq("a")
      g.outgoing("b") shouldBe Seq.empty
    }

    "remove edge" in {
      val edges = Seq(
        "a" -> "b"
      )

      val g = Graph(edges).removeEdge("a", "b")
      g.outgoing("a") shouldBe Seq.empty
      g.incoming("b") shouldBe Seq.empty
    }

    "return roots" in {
      val edges = Seq(
        "a" -> "b",
        "b" -> "c"
      )

      Graph(edges).roots shouldBe Seq("a")
    }
  }
}
