///////////////////////////////////////////////////////////////////////////
//                   __                _      _   ________               //
//                  / /   ____  ____ _(_)____/ | / / ____/               //
//                 / /   / __ \/ __ `/ / ___/  |/ / / __                 //
//                / /___/ /_/ / /_/ / / /__/ /|  / /_/ /                 //
//               /_____/\____/\__, /_/\___/_/ |_/\____/                  //
//                           /____/                                      //
//                                                                       //
//               The Next Generation Logic Library                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////
//                                                                       //
//  Copyright 2015-20xx Christoph Zengler                                //
//                                                                       //
//  Licensed under the Apache License, Version 2.0 (the "License");      //
//  you may not use this file except in compliance with the License.     //
//  You may obtain a copy of the License at                              //
//                                                                       //
//  http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                       //
//  Unless required by applicable law or agreed to in writing, software  //
//  distributed under the License is distributed on an "AS IS" BASIS,    //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      //
//  implied.  See the License for the specific language governing        //
//  permissions and limitations under the License.                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////

package org.logicng.graphs.datastructures;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A node in an hypergraph.
 * @param <T> the content type of the graph's nodes
 * @version 1.4.0
 * @since 1.4.0
 */
public class HypergraphNode<T> {

  private final Hypergraph<T> graph;
  private final T content;
  private final LinkedHashSet<HypergraphEdge<T>> edges;

  /**
   * Constructs a new hypergraph node for a given graph and content.
   * @param graph   the graph for this node
   * @param content the content of this node
   */
  public HypergraphNode(final Hypergraph<T> graph, final T content) {
    this.graph = graph;
    this.content = content;
    this.edges = new LinkedHashSet<HypergraphEdge<T>>();
    this.graph.addNode(this);
  }

  /**
   * Returns the hypergraph of this node.
   * @return the hypergraph of this node
   */
  public Hypergraph<T> graph() {
    return this.graph;
  }

  /**
   * Returns the content of this node.
   * @return the content of this node
   */
  public T content() {
    return this.content;
  }

  /**
   * Returns the edges which are connected with this node.
   * @return the edges which are connected with this node
   */
  public Set<HypergraphEdge<T>> edges() {
    return this.edges;
  }

  /**
   * Adds an edge to this node.
   * @param edge the edge
   */
  public void addEdge(final HypergraphEdge<T> edge) {
    this.edges.add(edge);
  }

  /**
   * Computes the tentative new location for this node (see Aloul, Markov, and Sakallah).
   * @param nodeOrdering the node ordering for which the COG is computed
   * @return the tentative new location for this node
   */
  public double computeTentativeNewLocation(final Map<HypergraphNode<T>, Integer> nodeOrdering) {
    double newLocation = 0;
    for (final HypergraphEdge<T> edge : this.edges)
      newLocation += edge.centerOfGravity(nodeOrdering);
    return newLocation / this.edges.size();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final HypergraphNode<?> that = (HypergraphNode<?>) o;
    if (this.graph != null ? !this.graph.equals(that.graph) : that.graph != null) return false;
    return this.content != null ? this.content.equals(that.content) : that.content == null;
  }

  @Override
  public int hashCode() {
    int result = this.graph != null ? this.graph.hashCode() : 0;
    result = 31 * result + (this.content != null ? this.content.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "HypergraphNode{" +
            "content=" + this.content +
            '}';
  }
}
