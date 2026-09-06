package fr.bordeaux.graph;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Utility class used to build graph instances. */
public final class GraphBuilder {

  /** Private constructor to prevent instantiation. */
  private GraphBuilder() {}

  /**
   * Builds an empty square grid graph of the given size.
   *
   * @param size number of rows and columns in the grid
   * @return a graph representing the empty grid
   */
  public static Graph buildEmptyGrid(final int size) {
    final Map<Integer, List<Integer>> adjacencyList = new ConcurrentHashMap<>();
    final int nbVertices = size * size;

    for (int vertexIndex = 0; vertexIndex < nbVertices; vertexIndex++) {
      adjacencyList.put(vertexIndex, new java.util.concurrent.CopyOnWriteArrayList<>());
    }

    for (int row = 0; row < size; row++) {
      for (int col = 0; col < size; col++) {
        final int currentVertex = row * size + col;

        if (row < size - 1) {
          addUndirectedEdge(adjacencyList, currentVertex, (row + 1) * size + col);
        }
        if (col < size - 1) {
          addUndirectedEdge(adjacencyList, currentVertex, row * size + col + 1);
        }
      }
    }
    return new Graph(adjacencyList);
  }

  /**
   * Adds an undirected edge between two vertices.
   *
   * @param adj adjacency list of the graph
   * @param u first vertex
   * @param v second vertex
   */
  private static void addUndirectedEdge(
      final Map<Integer, List<Integer>> adjacencyList, final int vertexU, final int vertexV) {
    adjacencyList.get(vertexU).add(vertexV);
    adjacencyList.get(vertexV).add(vertexU);
  }
}
