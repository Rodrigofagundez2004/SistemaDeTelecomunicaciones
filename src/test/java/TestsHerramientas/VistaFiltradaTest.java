package TestsHerramientas;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.tda.grafo.Impl.DirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.Impl.VistaFiltrada;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

@DisplayName("VistaFiltrada")
public class VistaFiltradaTest {

    // Grafo base:  A -> B -> C -> A   (ciclo, fuertemente conexo)
    //              A -> D             (D no vuelve a nadie)
    private DirectedGraph<String, WeightedEdge> g;

    @BeforeEach
    void setUp() {
        g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "C", new WeightedEdge(2));
        g.agregarArista("C", "A", new WeightedEdge(3));
        g.agregarArista("A", "D", new WeightedEdge(4));
    }

    private VistaFiltrada<String, WeightedEdge> sinVertice(String oculto) {
        return new VistaFiltrada<>(g, v -> !v.equals(oculto), d -> true);
    }

    @Test
    @DisplayName("oculta los vértices que no cumplen el predicado")
    void ocultaVertices() {
        VistaFiltrada<String, WeightedEdge> vista = sinVertice("B");

        assertEquals(Set.of("A", "C", "D"), vista.vertices());
        assertNull(vista.buscarVertice(g.construirComparable("B")));
        assertFalse(vista.existeVertice(g.construirComparable("B")));
    }

    @Test
    @DisplayName("aristas() nunca devuelve una arista con un extremo oculto")
    void aristasSinExtremosOcultos() {
        VistaFiltrada<String, WeightedEdge> vista = sinVertice("B");

        for (Edge<String, WeightedEdge> e : vista.aristas()) {
            assertTrue(vista.vertices().contains(e.source()), "origen oculto en " + e);
            assertTrue(vista.vertices().contains(e.target()), "destino oculto en " + e);
        }
        assertEquals(2, vista.cantidadDeAristas());   // C -> A y A -> D
    }

    @Test
    @DisplayName("oculta las aristas que no cumplen el predicado")
    void ocultaAristas() {
        VistaFiltrada<String, WeightedEdge> vista = new VistaFiltrada<>(g, v -> true, d -> d.getWeight() < 3);

        assertTrue(vista.existeArista(g.construirComparable("A"), g.construirComparable("B")));
        assertFalse(vista.existeArista(g.construirComparable("C"), g.construirComparable("A")));
        assertNull(vista.obtenerArista(g.construirComparable("A"), g.construirComparable("D")));
        assertEquals(Set.of("B"), vista.successors(g.construirComparable("A")));
        assertTrue(vista.predecessors(g.construirComparable("A")).isEmpty());
    }

    @Test
    @DisplayName("adyacencias, sucesores y predecesores respetan los vértices ocultos")
    void vecinosFiltrados() {
        VistaFiltrada<String, WeightedEdge> vista = sinVertice("D");

        assertEquals(1, vista.adyacencias(g.construirComparable("A")).size());
        assertEquals(Set.of("B"), vista.successors(g.construirComparable("A")));
        assertEquals(Set.of("C"), vista.predecessors(g.construirComparable("A")));
        assertTrue(vista.adyacencias(g.construirComparable("D")).isEmpty());   // D no está en la vista
    }

    @Test
    @DisplayName("esConexo es conexidad fuerte sobre lo visible")
    void esConexoFuerte() {
        assertFalse(new VistaFiltrada<>(g, v -> true, d -> true).esConexo());   // D no vuelve
        assertTrue(sinVertice("D").esConexo());                                   // queda el ciclo A-B-C
        assertFalse(new VistaFiltrada<>(g, v -> !v.equals("D"), d -> d.getWeight() != 3).esConexo());
        assertTrue(new VistaFiltrada<>(g, v -> false, d -> true).esConexo());   // vista vacía
    }

    @Test
    @DisplayName("tieneCiclos solo mira lo visible")
    void tieneCiclos() {
        assertTrue(new VistaFiltrada<>(g, v -> true, d -> true).tieneCiclos());
        assertFalse(sinVertice("B").tieneCiclos());                                       // se corta el ciclo
        assertFalse(new VistaFiltrada<>(g, v -> true, d -> d.getWeight() != 3).tieneCiclos());
    }

    @Test
    @DisplayName("refleja enseguida los cambios del grafo base")
    void reflejaCambiosDelBase() {
        VistaFiltrada<String, WeightedEdge> vista = sinVertice("B");
        g.agregarVertice("E");
        g.agregarArista("D", "E", new WeightedEdge(1));

        assertTrue(vista.vertices().contains("E"));
        assertTrue(vista.existeArista(g.construirComparable("D"), g.construirComparable("E")));
    }

    @Test
    @DisplayName("los algoritmos de la biblioteca funcionan sobre la vista")
    void algoritmosSobreLaVista() {
        DirectedGraphAlgorithms alg = new DirectedGraphAlgorithms();
        VistaFiltrada<String, WeightedEdge> vista = sinVertice("B");

        // Sin B, A no llega a C; Floyd no debe romperse con las aristas de B ocultas
        assertEquals(Double.POSITIVE_INFINITY, alg.dijkstra(g.construirComparable("A"), vista).getCost("C"));
        assertEquals(Double.POSITIVE_INFINITY, alg.floyd(vista).getCost("A", "C"));
        assertEquals(4, alg.floyd(vista).getCost("A", "D"));
    }

    @Test
    @DisplayName("es de solo lectura")
    void soloLectura() {
        VistaFiltrada<String, WeightedEdge> vista = sinVertice("B");

        assertThrows(UnsupportedOperationException.class, () -> vista.agregarVertice("X"));
        assertThrows(UnsupportedOperationException.class, () -> vista.agregarArista("A", "C", new WeightedEdge(1)));
        assertThrows(UnsupportedOperationException.class,
                () -> vista.eliminarArista(g.construirComparable("A"), g.construirComparable("D")));
        assertThrows(UnsupportedOperationException.class, () -> vista.removerVertice(g.construirComparable("A")));
        assertThrows(UnsupportedOperationException.class, vista::vaciar);
        assertEquals(4, g.cantidadDeVertices());
    }
}
