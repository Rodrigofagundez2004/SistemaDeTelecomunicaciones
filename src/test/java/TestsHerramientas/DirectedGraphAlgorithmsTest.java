package TestsHerramientas;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.tda.grafo.Impl.DirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.Path;

public class DirectedGraphAlgorithmsTest {

    // Grafo usado en los tests:  A -> B -> D
    //                            A -> C -> D

    @Test
    void recorridoEnProfundidadDesdeA() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));
        g.agregarArista("B", "D", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnProfundidad(g, g.construirComparable("A"), visitados::add);

        assertEquals(List.of("A", "B", "D", "C"), visitados);   // baja hasta D antes de ir a C
    }

    @Test
    void recorridoEnProfundidadSinOrigenVisitaTodo() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "X"));                // X está aislado
        g.agregarArista("A", "B", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnProfundidad(g, visitados::add);

        assertEquals(List.of("A", "B", "X"), visitados);
    }

    @Test
    void recorridoEnProfundidadConCicloNoRepite() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "C", new WeightedEdge(1));
        g.agregarArista("C", "A", new WeightedEdge(1));           // ciclo

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnProfundidad(g, g.construirComparable("A"), visitados::add);

        assertEquals(List.of("A", "B", "C"), visitados);
    }

    @Test
    void recorridoEnProfundidadOrigenInexistente() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertice("A");

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnProfundidad(g, g.construirComparable("Z"), visitados::add);

        assertTrue(visitados.isEmpty());
    }

    @Test
    void recorridoEnAmplitudDesdeA() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));
        g.agregarArista("B", "D", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnAmplitud(g, g.construirComparable("A"), visitados::add);

        assertEquals(List.of("A", "B", "C", "D"), visitados);   // por niveles: A, después B y C, después D
    }

    @Test
    void recorridoEnAmplitudSinOrigenVisitaTodo() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "X"));
        g.agregarArista("A", "B", new WeightedEdge(1));

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnAmplitud(g, visitados::add);

        assertEquals(List.of("A", "B", "X"), visitados);
    }

    @Test
    void recorridoEnAmplitudGrafoVacio() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();

        List<String> visitados = new ArrayList<>();
        new DirectedGraphAlgorithms().recorridoEnAmplitud(g, visitados::add);

        assertTrue(visitados.isEmpty());
    }

    @Test
    void clasificacionTopologica() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(1));
        g.agregarArista("B", "D", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(1));

        List<String> orden = new DirectedGraphAlgorithms().calcularClasificacionTopologica(g);

        assertEquals(4, orden.size());
        assertEquals("A", orden.get(0));                      // A no depende de nadie
        assertEquals("D", orden.get(3));                      // D depende de todos
        assertTrue(orden.indexOf("B") < orden.indexOf("D"));
        assertTrue(orden.indexOf("C") < orden.indexOf("D"));
    }

    @Test
    void clasificacionTopologicaConCicloLanzaExcepcion() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("B", "A", new WeightedEdge(1));

        assertThrows(IllegalStateException.class,
                () -> new DirectedGraphAlgorithms().calcularClasificacionTopologica(g));
    }

    @Test
    void clasificacionTopologicaGrafoVacio() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();

        assertTrue(new DirectedGraphAlgorithms().calcularClasificacionTopologica(g).isEmpty());
    }

    @Test
    void todosLosCaminos() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B", "C", "D"));
        g.agregarArista("A", "B", new WeightedEdge(1));
        g.agregarArista("A", "C", new WeightedEdge(5));
        g.agregarArista("B", "D", new WeightedEdge(1));
        g.agregarArista("C", "D", new WeightedEdge(1));

        List<Path<String>> caminos = new DirectedGraphAlgorithms()
                .obtenerTodosLosCaminos(g.construirComparable("A"), g.construirComparable("D"), g);

        assertEquals(2, caminos.size());
        assertEquals(List.of("A", "B", "D"), caminos.get(0).getPath());
        assertEquals(2, caminos.get(0).getCost());
        assertEquals(List.of("A", "C", "D"), caminos.get(1).getPath());
        assertEquals(6, caminos.get(1).getCost());
    }

    @Test
    void todosLosCaminosSinCamino() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertices(List.of("A", "B"));
        g.agregarArista("A", "B", new WeightedEdge(1));

        List<Path<String>> caminos = new DirectedGraphAlgorithms()
                .obtenerTodosLosCaminos(g.construirComparable("B"), g.construirComparable("A"), g);

        assertTrue(caminos.isEmpty());                        // no hay arco de B hacia A
    }

    @Test
    void todosLosCaminosOrigenIgualDestino() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertice("A");

        List<Path<String>> caminos = new DirectedGraphAlgorithms()
                .obtenerTodosLosCaminos(g.construirComparable("A"), g.construirComparable("A"), g);

        assertEquals(1, caminos.size());
        assertEquals(List.of("A"), caminos.get(0).getPath());
        assertEquals(0, caminos.get(0).getCost());
    }

    @Test
    void todosLosCaminosVerticeInexistente() {
        DirectedGraph<String, WeightedEdge> g = new DirectedGraph<>();
        g.agregarVertice("A");

        List<Path<String>> caminos = new DirectedGraphAlgorithms()
                .obtenerTodosLosCaminos(g.construirComparable("A"), g.construirComparable("Z"), g);

        assertTrue(caminos.isEmpty());
    }
}