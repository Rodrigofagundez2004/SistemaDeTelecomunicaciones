package TestsHerramientas;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public class UndirectedGraphAlgorithmsTest {

    // Grafo usado en los tests:  A - B - D
    //                            A - C

    @Test
    void bep() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));
        g.agregarArista("B", "D", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new UndirectedGraphAlgorithms().bep(g, visitados::add);

        assertEquals(List.of("A", "B", "D", "C"), visitados);   // baja hasta D antes de ir a C
    }

    @Test
    void bea() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));
        g.agregarArista("B", "D", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new UndirectedGraphAlgorithms().bea(g, visitados::add);

        assertEquals(List.of("A", "B", "C", "D"), visitados);   // por niveles
    }

    @Test
    void bepNoConexoVisitaTodo() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "X"));             // X está aislado
        g.agregarArista("A", "B", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new UndirectedGraphAlgorithms().bep(g, visitados::add);

        assertEquals(List.of("A", "B", "X"), visitados);
    }

    @Test
    void beaConCicloNoRepite() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "C", new WeightedEdge(1));
        g.agregarArista("C", "A", new WeightedEdge(1));        // ciclo

        List<String> visitados = new ArrayList<>();
        new UndirectedGraphAlgorithms().bea(g, visitados::add);

        assertEquals(List.of("A", "B", "C"), visitados);
    }

    @Test
    void beaGrafoVacio() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();

        List<String> visitados = new ArrayList<>();
        new UndirectedGraphAlgorithms().bea(g, visitados::add);

        assertTrue(visitados.isEmpty());
    }
}