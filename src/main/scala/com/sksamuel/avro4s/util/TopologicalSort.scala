package com.sksamuel.avro4s.util

object TopologicalSort {

  /**
    * Topological sort of graph nodes using Kahn's algorithm.
    * See here for details: https://en.wikipedia.org/wiki/Topological_sorting
    * @param graph graph to sort
    * @return nodes in topological order
    */
  def sort[N](graph: Graph[N]): Seq[N] = {
    import scala.collection.mutable

    var g = graph
    val sorted = mutable.ArrayBuffer[N]()
    val roots = g.roots.toBuffer

    while(roots.nonEmpty) {
      val n = roots.remove(0)
      sorted += n
      for {
        m <- g.outgoing(n)
      } {
        g = g.removeEdge(n, m)
        if (g.incoming(m).isEmpty) {
          roots += m
        }
      }
    }

    if (g.isEmpty) {
      sorted.toVector
    } else {
      throw new RuntimeException(s"Cyclic dependencies detected")
    }
  }

}
