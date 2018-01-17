package com.sksamuel.avro4s.util

import org.scalatest.{Matchers, WordSpec}

class TopologicalSortTest extends WordSpec with Matchers {

  "toposort" should {
    "return sorted nodes" in {
      val edges = Seq(
        "b" -> "a",
        "c" -> "e",
        "d" -> "a",
        "e" -> "d"
      )

      val g = Graph(edges)

      TopologicalSort.sort(g) shouldBe Seq("b", "c", "e", "d", "a")
    }

    "throw exception for cyclic graph" in {
      val edges = Seq(
        "b" -> "a",
        "c" -> "a"
      )

      val g = Graph(edges)

      a[RuntimeException] should be thrownBy (TopologicalSort.sort(g) shouldBe Seq("c", "b", "a"))

    }
  }

}
