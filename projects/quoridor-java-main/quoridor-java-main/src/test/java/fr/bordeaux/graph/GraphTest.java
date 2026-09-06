package fr.bordeaux.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphTest {

  private Graph graph;

  @BeforeEach
  public void setUp() {
    Map<Integer, List<Integer>> adj = new HashMap<>();
    adj.put(0, new ArrayList<>(List.of(1)));
    adj.put(1, new ArrayList<>(List.of(0, 2)));
    adj.put(2, new ArrayList<>(List.of(1)));

    graph = new Graph(adj);
  }

  @Test
  public void testHasEdge() {
    assertTrue(graph.hasEdge(0, 1));
    assertTrue(graph.hasEdge(1, 0));
    assertFalse(graph.hasEdge(0, 2));
  }

  @Test
  public void testGetNeighbors() {
    List<Integer> neighbors = graph.getNeighbors(1);
    assertEquals(2, neighbors.size());
    assertTrue(neighbors.contains(0));
    assertTrue(neighbors.contains(2));
  }

  @Test
  public void testRemoveEdgeIsSymmetric() {
    graph.removeEdge(0, 1);

    assertFalse(graph.hasEdge(0, 1));
    assertFalse(graph.hasEdge(1, 0));
    assertTrue(graph.hasEdge(1, 2));
  }

  @Test
  public void testAddEdgeIsSymmetric() {
    assertFalse(graph.hasEdge(0, 2));

    graph.addEdge(0, 2);

    assertTrue(graph.hasEdge(0, 2));
    assertTrue(graph.hasEdge(2, 0));

    assertTrue(graph.getNeighbors(0).contains(2));
    assertTrue(graph.getNeighbors(2).contains(1));
    assertTrue(graph.getNeighbors(2).contains(0));
  }

  @Test
  public void testAddEdgeDoesNotDuplicate() {
    int initialSize = graph.getNeighbors(0).size();
    graph.addEdge(0, 1);

    assertEquals(initialSize, graph.getNeighbors(0).size());
  }

  @Test
  public void testHasEdgeWithInvalidVertex() {
    assertThrows(
        NullPointerException.class,
        () -> {
          graph.hasEdge(99, 1);
        });
  }
}
