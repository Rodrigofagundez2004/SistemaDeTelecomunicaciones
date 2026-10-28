package TestsHerramientas;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public class AlgoritmosMSTTest {

    // Tests: Grafo Vacío
    @Test
    void primGrafoVacio() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().prim(g, "A");

        // Prim debe devolver un grafo vacío si el grafo está vacío
        assertNotNull(resultado);
        assertTrue(resultado.vertices().isEmpty());
        assertTrue(resultado.aristas().isEmpty());
    }

    @Test
    void kruskalGrafoVacio() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().kruskal(g);

        // Kruskal debe devolver un grafo vacío si el grafo está vacío
        assertNotNull(resultado);
        assertTrue(resultado.vertices().isEmpty());
        assertTrue(resultado.aristas().isEmpty());
    }

    @Test
    void primSinOrigenSobreGrafoVacioDevuelveVacio() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().prim(g);

        // La variante sin origen tampoco puede devolver null
        assertNotNull(resultado);
        assertTrue(resultado.vertices().isEmpty());
    }

    // Tests: searchMinEdge (arista mínima entre U y los pendientes)
    @Test
    void searchMinEdgeDevuelveLaMinimaEntreLosConjuntos() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));

        g.agregarArista("A", "B", new WeightedEdge(1)); // dentro de U: no cuenta
        g.agregarArista("A", "C", new WeightedEdge(5)); // U -> pendientes
        g.agregarArista("B", "D", new WeightedEdge(3)); // U -> pendientes: la mínima
        g.agregarArista("C", "D", new WeightedEdge(2)); // dentro de pendientes: no cuenta

        Edge<String, WeightedEdge> e = new UndirectedGraphAlgorithms()
                .searchMinEdge(g, Set.of("A", "B"), Set.of("C", "D"));

        assertNotNull(e);
        assertEquals(3.0, e.dato().getWeight(), 0.0);
        // El origen de la arista tiene que estar en U, y el destino entre los pendientes
        assertEquals("B", e.source());
        assertEquals("D", e.target());
    }

    @Test
    void searchMinEdgeSinAristasEntreConjuntosDevuelveNull() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(2));

        assertNull(new UndirectedGraphAlgorithms().searchMinEdge(g, Set.of("A"), Set.of("C", "D")));
    }

    // Tests: Grafo No Conexo (Bosque)
    @Test
    void kruskalGrafoNoConexoDevuelveBosque() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        
        // Dos islas: A-B y C-D
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(2));

        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().kruskal(g);

        // Kruskal debe conectar las dos islas (4 vértices, 2 aristas en total)
        assertEquals(4, resultado.vertices().size());
        assertEquals(2, resultado.aristas().size());
    }

    @Test
    void primGrafoNoConexoSoloAlcanzaComponente() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        
        // Dos islas: A-B y C-D
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(2));

        // Llamamos a Prim desde A. Solo debe alcanzar A y B.
        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().prim(g, "A");

        assertEquals(2, resultado.vertices().size()); // Solo A y B
        assertEquals(1, resultado.aristas().size());  // Solo la arista A-B
    }

    // Tests: Pesos Iguales
    @Test
    void primPesosIgualesEvitaCiclos() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C"));
        
        // Triángulo con todos los pesos iguales (1)
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "C", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));

        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().prim(g, "A");

        // Debe tomar 2 aristas (V-1), no las 3, para no formar ciclo
        assertEquals(3, resultado.vertices().size());
        assertEquals(2, resultado.aristas().size());
    }

    @Test
    void kruskalPesosIgualesEvitaCiclos() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C"));
        
        // Triángulo con todos los pesos iguales (1)
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "C", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));

        IUndirectedGraph<String, WeightedEdge> resultado = new UndirectedGraphAlgorithms().kruskal(g);

        // Debe tomar 2 aristas (V-1), no las 3, para no formar ciclo
        assertEquals(3, resultado.vertices().size());
        assertEquals(2, resultado.aristas().size());
    }

    // Test: Prim y Kruskal dan el mismo costo total
    @Test
    void primYKruskalMismoCostoTotal() {
        UndirectedGraph<String, WeightedEdge> g = new UndirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "C", new WeightedEdge(2));
        g.agregarArista("C", "D", new WeightedEdge(3));
        g.agregarArista("D", "A", new WeightedEdge(4));
        g.agregarArista("A", "C", new WeightedEdge(5));
        g.agregarArista("B", "D", new WeightedEdge(6));

        UndirectedGraphAlgorithms alg = new UndirectedGraphAlgorithms();
        
        IUndirectedGraph<String, WeightedEdge> resKruskal = alg.kruskal(g);
        IUndirectedGraph<String, WeightedEdge> resPrim = alg.prim(g, "A");

        // Calculamos la suma de los pesos (costo total)
        double costoKruskal = 0;
        for (var arista : resKruskal.aristas()) costoKruskal += arista.dato().getWeight();

        double costoPrim = 0;
        for (var arista : resPrim.aristas()) costoPrim += arista.dato().getWeight();

        // Ambos deben dar el mismo costo mínimo total
        assertEquals(costoKruskal, costoPrim, 0.01);
        
        // El costo mínimo esperado es: A-B(1) + B-C(2) + C-D(3) = 6
        assertEquals(6.0, costoKruskal, 0.01);
        assertEquals(6.0, costoPrim, 0.01);
    }
}