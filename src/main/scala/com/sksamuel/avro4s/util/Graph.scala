package com.sksamuel.avro4s.util

import com.sksamuel.avro4s.util

case class Graph[N](
  private val incomingEdges: Map[N, Seq[N]] = Map.empty[N, Seq[N]],
  private val outgoingEdges: Map[N, Seq[N]] = Map.empty[N, Seq[N]]
) {
  def addEdge(from: N, to: N): Graph[N] = {
    val newOutgoing = outgoingEdges.updated(from, (outgoingEdges.getOrElse(from, Seq.empty) :+ to).distinct)
    val newIncoming = incomingEdges.updated(to, (incomingEdges.getOrElse(to, Seq.empty) :+ from).distinct)
    Graph(newIncoming, newOutgoing)
  }

  def removeEdge(from: N, to: N): Graph[N] = {
    val newOutgoingEdges = outgoingEdges.getOrElse(from, Seq.empty).filterNot(_ == to)
    val newIncomingEdges = incomingEdges.getOrElse(to, Seq.empty).filterNot(_ == from)

    val newOutgoing = if (newOutgoingEdges.isEmpty) outgoingEdges - from else outgoingEdges.updated(from, newOutgoingEdges)
    val newIncoming = if (newIncomingEdges.isEmpty) incomingEdges - to else incomingEdges.updated(to, newIncomingEdges)

    Graph(newIncoming, newOutgoing)
  }

  def incoming(node: N): Seq[N] = incomingEdges.getOrElse(node, Seq.empty)

  def outgoing(node: N): Seq[N] = outgoingEdges.getOrElse(node, Seq.empty)

  def nodes: Seq[N] = outgoingEdges.keys.toSeq

  def roots: Seq[N] = {
    nodes.filter(node => incomingEdges.get(node).isEmpty).toVector
  }

  def isEmpty: Boolean = {
    incomingEdges.isEmpty && outgoingEdges.isEmpty
  }
}

object Graph {
  def apply[N](edges: Seq[(N,N)]): Graph[N] = {
    edges.foldLeft(util.Graph[N]())((g, e) => g.addEdge(e._1, e._2))
  }
}