package fr.bordeaux.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class GraphBuilderTest {

  private final int SIZE = 9;

  @Test
  public void testGraphHasCorrectNumberOfVertices() {
    Graph graph = GraphBuilder.buildEmptyGrid(SIZE);
    int totalVertices = SIZE * SIZE;

    for (int i = 0; i < totalVertices; i++) {
      assertNotNull(graph.getNeighbors(i));
    }
  }

  @Test
  public void testCornerNeighbors() {
    Graph graph = GraphBuilder.buildEmptyGrid(SIZE);
    int topLeft = 0;
    int bottomRight = (SIZE * SIZE) - 1;

    assertEquals(2, graph.getNeighbors(topLeft).size());
    assertTrue(graph.getNeighbors(topLeft).contains(1));
    assertTrue(graph.getNeighbors(topLeft).contains(SIZE));

    assertEquals(2, graph.getNeighbors(bottomRight).size());
    assertTrue(graph.getNeighbors(bottomRight).contains(bottomRight - 1));
    assertTrue(graph.getNeighbors(bottomRight).contains(bottomRight - SIZE));
  }

  @Test
  public void testCellWithFourNeighbors() {
    Graph graph = GraphBuilder.buildEmptyGrid(SIZE);
    int cellId = SIZE + 1;

    List<Integer> neighbors = graph.getNeighbors(cellId);

    assertEquals(4, neighbors.size());
    assertTrue(neighbors.contains(cellId - 1));
    assertTrue(neighbors.contains(cellId + 1));
    assertTrue(neighbors.contains(cellId - SIZE));
    assertTrue(neighbors.contains(cellId + SIZE));
  }

  @Test
  public void testUndirectedEdges() {
    Graph graph = GraphBuilder.buildEmptyGrid(SIZE);
    int u = 0;
    int v = 1;

    assertTrue(graph.getNeighbors(u).contains(v));
    assertTrue(graph.getNeighbors(v).contains(u));
  }
}
