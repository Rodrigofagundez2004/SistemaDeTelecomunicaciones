package TestsHerramientas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@DisplayName("UndirectedGraph")
public class UndirectedGraphTest {

    private UndirectedGraph<String, Integer> graph;

    @BeforeEach
    void setUp() {
        graph = new UndirectedGraph<>();
        graph.agregarVertices(List.of("A", "B", "C"));
    }

    /** Vecinos de un vértice según sus adyacencias (target de cada arista). */
    private Set<String> vecinos(String v) {
        return graph.adyacencias(v).stream().map(Edge::target).collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("agregarVertice / buscarVertice")
    class Vertices {
        @Test
        @DisplayName("agrega un vértice nuevo y rechaza duplicados")
        void agregarVertice() {
            assertTrue(graph.agregarVertice("D"));
            assertFalse(graph.agregarVertice("D"));
            assertEquals(4, graph.cantidadDeVertices());
        }

        @Test
        @DisplayName("no admite vértices null")
        void agregarVerticeNull() {
            assertThrows(NullPointerException.class, () -> graph.agregarVertice(null));
            assertEquals(3, graph.cantidadDeVertices());
        }

        @Test
        @DisplayName("buscarVertice encuentra existentes y devuelve null si no existe")
        void buscarVertice() {
            assertEquals("A", graph.buscarVertice("A"));
            assertEquals("B", graph.buscarVertice(graph.construirComparable("B")));
            assertNull(graph.buscarVertice("Z"));
            assertNull(graph.buscarVertice(null));
        }

        @Test
        @DisplayName("vertices() respeta el orden de inserción")
        void ordenDeInsercion() {
            assertEquals(List.of("A", "B", "C"), List.copyOf(graph.vertices()));
        }

        @Test
        @DisplayName("vertices() es inmodificable desde afuera")
        void verticesInmodificable() {
            graph.agregarArista("A", "B", 1);
            assertThrows(UnsupportedOperationException.class, () -> graph.vertices().remove("B"));
            assertThrows(UnsupportedOperationException.class, () -> graph.vertices().add("Z"));
            assertTrue(graph.existeVertice("B"));
            assertTrue(graph.existeArista("A", "B"));
        }
    }

    @Nested
    @DisplayName("agregarArista")
    class AgregarArista {
        @Test
        @DisplayName("la arista se ve desde ambos extremos")
        void ambosSentidos() {
            assertTrue(graph.agregarArista("A", "B", 5));
            assertTrue(graph.existeArista("A", "B"));
            assertTrue(graph.existeArista("B", "A"));
            assertEquals(Set.of("B"), vecinos("A"));
            assertEquals(Set.of("A"), vecinos("B"));
            assertEquals(1, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("rechaza aristas con vértices inexistentes o null")
        void verticesInexistentes() {
            assertFalse(graph.agregarArista("A", "Z", 5));
            assertFalse(graph.agregarArista("Z", "A", 5));
            assertFalse(graph.agregarArista(null, "A", 5));
            assertFalse(graph.agregarArista("A", null, 5));
            assertEquals(0, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("{B,A} es la misma arista que {A,B}: se rechaza y conserva el dato original")
        void aristaDuplicadaEnOrdenInverso() {
            graph.agregarArista("A", "B", 1);
            assertFalse(graph.agregarArista("A", "B", 9));
            assertFalse(graph.agregarArista("B", "A", 9));
            assertEquals(1, graph.obtenerArista("A", "B").dato());
            assertEquals(1, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("se admiten ciclos de un vértice (A-A) y agregarArista devuelve true")
        void cicloDeUnVertice() {
            assertTrue(graph.agregarArista("A", "A", 1));
            assertTrue(graph.existeArista("A", "A"));
            assertEquals(Set.of("A"), vecinos("A"));
            assertEquals(1, graph.cantidadDeAristas());
            assertFalse(graph.agregarArista("A", "A", 2));
        }
    }

    @Nested
    @DisplayName("eliminarArista")
    class EliminarArista {
        @Test
        @DisplayName("eliminar {B,A} borra la arista creada como {A,B} de ambos extremos")
        void eliminaEnOrdenInverso() {
            graph.agregarArista("A", "B", 5);
            assertTrue(graph.eliminarArista("B", "A"));
            assertFalse(graph.existeArista("A", "B"));
            assertFalse(graph.existeArista("B", "A"));
            assertEquals(Set.of(), vecinos("A"));
            assertEquals(Set.of(), vecinos("B"));
            assertEquals(0, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("eliminar un ciclo de un vértice (A-A) devuelve true")
        void cicloDeUnVertice() {
            graph.agregarArista("A", "A", 1);
            assertTrue(graph.eliminarArista("A", "A"));
            assertFalse(graph.existeArista("A", "A"));
            assertEquals(0, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("devuelve false si la arista o los vértices no existen")
        void inexistente() {
            assertFalse(graph.eliminarArista("A", "B")); // vértices existen, arista no
            assertFalse(graph.eliminarArista("A", "Z")); // vértice inexistente
        }
    }

    @Nested
    @DisplayName("removerVertice")
    class RemoverVertice {
        @Test
        @DisplayName("elimina el vértice y la arista deja de aparecer en sus vecinos")
        void limpiaVecinos() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 2);
            graph.agregarArista("C", "A", 3);

            assertTrue(graph.removerVertice("B"));

            assertFalse(graph.existeVertice("B"));
            assertEquals(Set.of("C"), vecinos("A"));
            assertEquals(Set.of("A"), vecinos("C"));
            assertEquals(1, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("elimina un vértice con un ciclo de un vértice (A-A) y otras aristas sin ConcurrentModificationException")
        void conCicloDeUnVertice() {
            graph.agregarArista("A", "A", 1);
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("A", "C", 1);
            assertTrue(graph.removerVertice("A"));
            assertEquals(0, graph.cantidadDeAristas());
            assertEquals(Set.of(), vecinos("B"));
        }

        @Test
        @DisplayName("devuelve false si el vértice no existe")
        void inexistente() {
            assertFalse(graph.removerVertice("Z"));
            assertEquals(3, graph.cantidadDeVertices());
        }

        @Test
        @DisplayName("el vértice se puede volver a agregar sin aristas")
        void reagregar() {
            graph.agregarArista("A", "B", 1);
            graph.removerVertice("B");
            assertTrue(graph.agregarVertice("B"));
            assertFalse(graph.existeArista("A", "B"));
        }
    }

    @Nested
    @DisplayName("consultas de aristas")
    class ConsultasAristas {
        @BeforeEach
        void aristas() {
            graph.agregarArista("A", "B", 5);
            graph.agregarArista("C", "A", 7);
        }

        @Test
        @DisplayName("aristas() devuelve cada arista una sola vez y es inmodificable")
        void aristasTodas() {
            Set<Edge<String, Integer>> aristas = graph.aristas();
            assertEquals(2, aristas.size());
            assertThrows(UnsupportedOperationException.class, aristas::clear);
            assertEquals(2, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("obtenerArista devuelve la arista orientada desde el primer criterio")
        void obtenerArista() {
            Edge<String, Integer> ab = graph.obtenerArista("A", "B");
            assertEquals("A", ab.source());
            assertEquals("B", ab.target());
            assertEquals(5, ab.dato());
            assertFalse(ab.directed());

            Edge<String, Integer> ba = graph.obtenerArista("B", "A");
            assertEquals("B", ba.source());
            assertEquals("A", ba.target());
            assertEquals(5, ba.dato());
            assertEquals(ab, ba); // es la misma arista no dirigida
        }

        @Test
        @DisplayName("obtenerArista devuelve null si no existe")
        void obtenerAristaInexistente() {
            assertNull(graph.obtenerArista("B", "C"));
            assertNull(graph.obtenerArista("A", "Z"));
        }

        @Test
        @DisplayName("adyacencias: source() es siempre el vértice consultado (contrato de IGraph)")
        void adyacenciasOrientadas() {
            List<Edge<String, Integer>> deA = graph.adyacencias("A");
            assertEquals(List.of("B", "C"), deA.stream().map(Edge::target).toList());
            assertTrue(deA.stream().allMatch(e -> e.source().equals("A")));

            // La arista {C,A} se agregó con C como origen, pero desde A se ve como A -> C
            Edge<String, Integer> desdeA = deA.get(1);
            assertEquals(7, desdeA.dato());

            List<Edge<String, Integer>> deB = graph.adyacencias("B");
            assertEquals(1, deB.size());
            assertEquals("B", deB.get(0).source());
            assertEquals("A", deB.get(0).target());
        }

        @Test
        @DisplayName("adyacencias de un vértice inexistente es vacía")
        void adyacenciasInexistente() {
            assertEquals(List.of(), graph.adyacencias("Z"));
        }
    }

    @Nested
    @DisplayName("esConexo")
    class EsConexo {
        @Test
        @DisplayName("el grafo vacío y un único vértice son conexos")
        void vacioYUnVertice() {
            UndirectedGraph<String, Integer> g = new UndirectedGraph<>();
            assertTrue(g.esConexo());
            g.agregarVertice("A");
            assertTrue(g.esConexo());
        }

        @Test
        @DisplayName("un camino A-B-C es conexo aunque las aristas se agreguen \"hacia atrás\"")
        void camino() {
            graph.agregarArista("B", "A", 1);
            graph.agregarArista("C", "B", 1);
            assertTrue(graph.esConexo());
        }

        @Test
        @DisplayName("un vértice aislado hace que no sea conexo")
        void verticeAislado() {
            graph.agregarArista("A", "B", 1);
            assertFalse(graph.esConexo());
        }

        @Test
        @DisplayName("dos componentes no son conexas")
        void dosComponentes() {
            graph.agregarVertice("D");
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("C", "D", 1);
            assertFalse(graph.esConexo());
        }
    }

    @Nested
    @DisplayName("tieneCiclos")
    class TieneCiclos {
        @Test
        @DisplayName("el grafo vacío no tiene ciclos (antes lanzaba NoSuchElementException)")
        void vacio() {
            graph.vaciar();
            assertFalse(graph.tieneCiclos());
        }

        @Test
        @DisplayName("vértices sin aristas no tienen ciclos")
        void sinAristas() {
            assertFalse(graph.tieneCiclos());
        }

        @Test
        @DisplayName("una única arista no es un ciclo (volver al padre no cuenta)")
        void unaArista() {
            graph.agregarArista("A", "B", 1);
            assertFalse(graph.tieneCiclos());
        }

        @Test
        @DisplayName("un árbol no tiene ciclos")
        void arbol() {
            graph.agregarVertices(List.of("D", "E"));
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("A", "C", 1);
            graph.agregarArista("B", "D", 1);
            graph.agregarArista("B", "E", 1);
            assertFalse(graph.tieneCiclos());
        }

        @Test
        @DisplayName("un triángulo es un ciclo")
        void triangulo() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 1);
            graph.agregarArista("C", "A", 1);
            assertTrue(graph.tieneCiclos());
        }

        @Test
        @DisplayName("un ciclo de un vértice (B-B) es un ciclo")
        void cicloDeUnVertice() {
            graph.agregarArista("B", "B", 1);
            assertTrue(graph.tieneCiclos());
        }

        @Test
        @DisplayName("detecta un ciclo en una componente que no es la primera")
        void cicloEnOtraComponente() {
            graph.agregarVertice("D");
            graph.agregarArista("B", "C", 1);
            graph.agregarArista("C", "D", 1);
            graph.agregarArista("D", "B", 1);
            assertTrue(graph.tieneCiclos()); // "A" queda aislado y es el primer vértice
        }

        @Test
        @DisplayName("al eliminar una arista del ciclo deja de tenerlo")
        void romperCiclo() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 1);
            graph.agregarArista("C", "A", 1);
            graph.eliminarArista("A", "C");
            assertFalse(graph.tieneCiclos());
        }
    }

    @Test
    @DisplayName("vaciar elimina vértices y aristas")
    void vaciar() {
        graph.agregarArista("A", "B", 1);
        graph.vaciar();
        assertTrue(graph.vertices().isEmpty());
        assertTrue(graph.aristas().isEmpty());
        assertEquals(0, graph.cantidadDeAristas());
        assertTrue(graph.agregarVertice("A")); // el grafo sigue siendo usable
    }

    @Test
    @DisplayName("esConexo y tieneCiclos no desbordan la pila en un grafo grande")
    void grafoGrandeSinStackOverflow() {
        UndirectedGraph<Integer, Integer> g = new UndirectedGraph<>();
        int n = 100_000;
        for (int i = 0; i < n; i++) g.agregarVertice(i);
        for (int i = 0; i < n - 1; i++) g.agregarArista(i, i + 1, 1);

        assertTrue(g.esConexo());
        assertFalse(g.tieneCiclos());

        g.agregarArista(n - 1, 0, 1);
        assertTrue(g.tieneCiclos());
    }
}
