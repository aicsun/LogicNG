package org.logicng.dnnf.datastructures.dtree;

import org.logicng.formulas.Formula;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class MinFillDTreeGenerator extends EliminatingOrderDTreeGenerator {

    @Override
    public DTree generate(final Formula cnf) {
        final Graph graph = new Graph(cnf);
        final List<Literal> ordering = graph.getMinFillOrdering();
        return generateWithEliminatingOrder(cnf, ordering);
    }

    /**
     * Undirected Graph
     */
    private class Graph {
        final int numberOfVertices;
        final int numberOfEdges;

        /**
         * The adjacency matrix (which is symmetric since the graph is undirected)
         */
        final boolean[][] adjMatrix;

        /**
         * The list of vertices
         */
        final ArrayList<Literal> vertices;

        /**
         * The edges of the graph as a list of edges per node ({{2,3},{1},{1}} means that there are the edges 1-2 and 1-3)
         */
        final ArrayList<ArrayList<Integer>> edgeList;

        public Graph(final Formula cnf) {
            /* build vertices */
            this.numberOfVertices = cnf.variables().size();
            this.vertices = new ArrayList<>(this.numberOfVertices);
            final Map<Literal, Integer> varToIndex = new HashMap<>();
            int index = 0;
            for (final Literal variable : cnf.variables()) {
                this.vertices.add(variable);
                varToIndex.put(variable, index++);
            }

            /* build edge list and adjacency matrix */
            this.adjMatrix = new boolean[this.numberOfVertices][this.numberOfVertices];
            this.edgeList = new ArrayList<>(this.numberOfVertices);
            for (int i = 0; i < this.numberOfVertices; i++) {
                this.edgeList.add(new ArrayList<>());
            }

            int numberOfEdges = 0;
            for (final Formula clause : cnf) {
                final SortedSet<Variable> variables = clause.variables();
                final int[] varNums = new int[variables.size()];
                index = 0;
                for (final Literal var : variables) {
                    varNums[index++] = varToIndex.get(var);
                }
                for (int i = 0; i < varNums.length; i++) {
                    for (int j = i + 1; j < varNums.length; j++) {
                        this.edgeList.get(varNums[i]).add(varNums[j]);
                        this.edgeList.get(varNums[j]).add(varNums[i]);
                        this.adjMatrix[varNums[i]][varNums[j]] = true;
                        this.adjMatrix[varNums[j]][varNums[i]] = true;
                        numberOfEdges++;
                    }
                }
            }
            this.numberOfEdges = numberOfEdges;
        }

        private ArrayList<ArrayList<Integer>> getCopyOfEdgeList() {
            final ArrayList<ArrayList<Integer>> result = new ArrayList<>();
            for (final List<Integer> edge : this.edgeList) {
                result.add(new ArrayList<>(edge));
            }
            return result;
        }

        private boolean[][] getCopyOfAdjMatrix() {
            final boolean[][] result = new boolean[this.numberOfVertices][this.numberOfVertices];
            for (int i = 0; i < this.numberOfVertices; i++) {
                result[i] = Arrays.copyOf(this.adjMatrix[i], this.numberOfVertices);
            }
            return result;
        }

        public List<Literal> getMinFillOrdering() {
            final boolean[][] fillAdjMatrix = getCopyOfAdjMatrix();
            final ArrayList<ArrayList<Integer>> fillEdgeList = getCopyOfEdgeList(); // TODO: replace by List<BitSet> ?

            final Literal[] ordering = new Literal[this.numberOfVertices];
            final boolean[] processed = new boolean[this.numberOfVertices];
            int treewidth = 0;

            for (int iteration = 0; iteration < this.numberOfVertices; iteration++) {
                final LinkedList<Integer> possiblyBestVertices = new LinkedList<>();
                int minEdges = Integer.MAX_VALUE;
                for (int currentVertex = 0; currentVertex < this.numberOfVertices; currentVertex++) {
                    if (processed[currentVertex]) {
                        continue;
                    }
                    int edgesAdded = 0;
                    final List<Integer> neighborList = fillEdgeList.get(currentVertex);
                    for (int i = 0; i < neighborList.size(); i++) {
                        final Integer firstNeighbor = neighborList.get(i);
                        if (processed[firstNeighbor]) {
                            continue;
                        }
                        for (int j = i + 1; j < neighborList.size(); j++) {
                            final Integer secondNeighbor = neighborList.get(j);
                            if (processed[secondNeighbor]) {
                                continue;
                            }
                            if (!fillAdjMatrix[firstNeighbor][secondNeighbor]) {
                                edgesAdded++;
                            }
                        }
                    }
                    if (edgesAdded < minEdges) {
                        minEdges = edgesAdded;
                        possiblyBestVertices.clear();
                        possiblyBestVertices.add(currentVertex);
                    } else if (edgesAdded == minEdges) {
                        possiblyBestVertices.add(currentVertex);
                    }
                }

                final int bestVertex = possiblyBestVertices.get(0); // or choose randomly

                final List<Integer> neighborList = fillEdgeList.get(bestVertex);
                for (int i = 0; i < neighborList.size(); i++) {
                    final Integer firstNeighbor = neighborList.get(i);
                    if (processed[firstNeighbor]) {
                        continue;
                    }
                    for (int j = i + 1; j < neighborList.size(); j++) {
                        final Integer secondNeighbor = neighborList.get(j);
                        if (processed[secondNeighbor]) {
                            continue;
                        }
                        if (!fillAdjMatrix[firstNeighbor][secondNeighbor]) {
                            fillAdjMatrix[firstNeighbor][secondNeighbor] = true;
                            fillAdjMatrix[secondNeighbor][firstNeighbor] = true;
                            fillEdgeList.get(firstNeighbor).add(secondNeighbor);
                            fillEdgeList.get(secondNeighbor).add(firstNeighbor);
                        }
                    }
                }

                int currentNumberOfEdges = 0;
                for (int k = 0; k < this.numberOfVertices; k++) {
                    if (fillAdjMatrix[bestVertex][k] && !processed[k]) {
                        currentNumberOfEdges++;
                    }
                }
                if (treewidth < currentNumberOfEdges) {
                    treewidth = currentNumberOfEdges;
                }

                processed[bestVertex] = true;
                ordering[iteration] = this.vertices.get(bestVertex);
            }
            return Arrays.asList(ordering);
        }
    }
}
