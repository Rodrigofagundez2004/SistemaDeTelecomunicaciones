package TestsHerramientas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

@DisplayName("DirectedGraph")
public class DirectedGraphTest {

    private DirectedGraph<String, Integer> graph;

    @BeforeEach
    void setUp() {
        graph = new DirectedGraph<>();
        graph.agregarVertices(List.of("A", "B", "C"));
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
            assertNull(graph.buscarVertice("Z"));
            assertNull(graph.buscarVertice(null));
        }

        @Test
        @DisplayName("buscarVertice funciona con un criterio construido por equals")
        void buscarVerticeConComparableConstruido() {
            assertEquals("B", graph.buscarVertice(graph.construirComparable("B")));
        }

        @Test
        @DisplayName("vertices() respeta el orden de inserción")
        void ordenDeInsercion() {
            assertEquals(List.of("A", "B", "C"), List.copyOf(graph.vertices()));
        }

        @Test
        @DisplayName("vertices() es inmodificable desde afuera")
        void verticesInmodificable() {
            assertThrows(UnsupportedOperationException.class, () -> graph.vertices().remove("A"));
            assertThrows(UnsupportedOperationException.class, () -> graph.vertices().add("Z"));
            assertTrue(graph.existeVertice("A"));
        }
    }

    @Nested
    @DisplayName("agregarArista")
    class AgregarArista {
        @Test
        @DisplayName("agrega una arista entre vértices existentes")
        void agregaArista() {
            assertTrue(graph.agregarArista("A", "B", 5));
            assertTrue(graph.existeArista("A", "B"));
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
        @DisplayName("una arista duplicada se rechaza y se conserva el dato original")
        void aristaDuplicada() {
            graph.agregarArista("A", "B", 5);
            assertFalse(graph.agregarArista("A", "B", 9));
            assertEquals(5, graph.obtenerArista("A", "B").dato());
            assertEquals(1, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("A->B y B->A son aristas distintas")
        void sentidoImporta() {
            assertTrue(graph.agregarArista("A", "B", 5));
            assertTrue(graph.agregarArista("B", "A", 7));
            assertEquals(2, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("se admiten lazos (u -> u)")
        void lazo() {
            assertTrue(graph.agregarArista("A", "A", 1));
            assertEquals(Set.of("A"), graph.successors("A"));
            assertEquals(Set.of("A"), graph.predecessors("A"));
        }
    }

    @Nested
    @DisplayName("eliminarArista")
    class EliminarArista {
        @Test
        @DisplayName("elimina la arista y actualiza sucesores y predecesores")
        void eliminaArista() {
            graph.agregarArista("A", "B", 5);
            assertTrue(graph.eliminarArista("A", "B"));
            assertFalse(graph.existeArista("A", "B"));
            assertEquals(Set.of(), graph.successors("A"));
            assertEquals(Set.of(), graph.predecessors("B"));
            assertEquals(0, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("solo elimina en el sentido indicado")
        void soloUnSentido() {
            graph.agregarArista("A", "B", 5);
            graph.agregarArista("B", "A", 5);
            assertTrue(graph.eliminarArista("A", "B"));
            assertTrue(graph.existeArista("B", "A"));
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
        @DisplayName("elimina el vértice junto con sus aristas entrantes y salientes")
        void limpiaAristas() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 2);
            graph.agregarArista("C", "A", 3);

            assertTrue(graph.removerVertice("B"));

            assertFalse(graph.existeVertice("B"));
            assertEquals(Set.of(), graph.successors("A"));
            assertEquals(Set.of(), graph.predecessors("C"));
            assertEquals(1, graph.cantidadDeAristas());
            assertTrue(graph.existeArista("C", "A"));
        }

        @Test
        @DisplayName("elimina correctamente un vértice con lazo")
        void conLazo() {
            graph.agregarArista("A", "A", 1);
            assertTrue(graph.removerVertice("A"));
            assertEquals(0, graph.cantidadDeAristas());
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
            assertEquals(Set.of(), graph.predecessors("B"));
        }
    }

    @Nested
    @DisplayName("consultas de aristas")
    class ConsultasAristas {
        @BeforeEach
        void aristas() {
            graph.agregarArista("A", "B", 5);
            graph.agregarArista("A", "C", 7);
        }

        @Test
        @DisplayName("aristas() devuelve todas y es inmodificable")
        void aristasTodas() {
            Set<Edge<String, Integer>> aristas = graph.aristas();
            assertEquals(2, aristas.size());
            assertThrows(UnsupportedOperationException.class, aristas::clear);
            assertEquals(2, graph.cantidadDeAristas());
        }

        @Test
        @DisplayName("existeArista distingue el sentido")
        void existeArista() {
            assertTrue(graph.existeArista("A", "B"));
            assertFalse(graph.existeArista("B", "A")); // el grafo es dirigido
            assertFalse(graph.existeArista("A", "Z"));
        }

        @Test
        @DisplayName("obtenerArista devuelve origen, destino y dato")
        void obtenerArista() {
            Edge<String, Integer> arista = graph.obtenerArista("A", "B");
            assertNotNull(arista);
            assertEquals("A", arista.source());
            assertEquals("B", arista.target());
            assertEquals(5, arista.dato());
            assertTrue(arista.directed());
        }

        @Test
        @DisplayName("obtenerArista devuelve null si no existe")
        void obtenerAristaInexistente() {
            assertNull(graph.obtenerArista("B", "A"));
            assertNull(graph.obtenerArista("A", "Z"));
        }

        @Test
        @DisplayName("adyacencias devuelve las aristas salientes en orden de inserción")
        void adyacencias() {
            List<Edge<String, Integer>> adyacencias = graph.adyacencias("A");
            assertEquals(2, adyacencias.size());
            assertEquals("B", adyacencias.get(0).target());
            assertEquals("C", adyacencias.get(1).target());
            assertTrue(adyacencias.stream().allMatch(e -> e.source().equals("A")));
        }

        @Test
        @DisplayName("adyacencias de un vértice sin aristas o inexistente es vacía")
        void adyacenciasVacias() {
            assertEquals(List.of(), graph.adyacencias("B"));
            assertEquals(List.of(), graph.adyacencias("Z"));
        }
    }

    @Nested
    @DisplayName("sucesores, predecesores y grados")
    class SucesoresPredecesores {
        @BeforeEach
        void aristas() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("A", "C", 1);
            graph.agregarArista("B", "C", 1);
        }

        @Test
        void successors() {
            assertEquals(Set.of("B", "C"), graph.successors("A"));
            assertEquals(Set.of("C"), graph.successors("B"));
            assertEquals(Set.of(), graph.successors("C"));
            assertEquals(Set.of(), graph.successors("Z"));
        }

        @Test
        void predecessors() {
            assertEquals(Set.of(), graph.predecessors("A"));
            assertEquals(Set.of("A"), graph.predecessors("B"));
            assertEquals(Set.of("A", "B"), graph.predecessors("C"));
            assertEquals(Set.of(), graph.predecessors("Z"));
        }

        @Test
        void grados() {
            assertEquals(2, graph.gradoDeSalida("A"));
            assertEquals(0, graph.gradoDeEntrada("A"));
            assertEquals(0, graph.gradoDeSalida("C"));
            assertEquals(2, graph.gradoDeEntrada("C"));
        }

        @Test
        @DisplayName("los conjuntos devueltos no permiten modificar el grafo")
        void copiasInmodificables() {
            assertThrows(UnsupportedOperationException.class, () -> graph.successors("A").clear());
            assertThrows(UnsupportedOperationException.class, () -> graph.predecessors("C").clear());
            assertEquals(3, graph.cantidadDeAristas());
        }
    }

    @Nested
    @DisplayName("esConexo (conexión fuerte)")
    class EsConexo {
        @Test
        @DisplayName("el grafo vacío es conexo")
        void vacio() {
            graph.vaciar();
            assertTrue(graph.esConexo());
        }

        @Test
        @DisplayName("un único vértice es conexo")
        void unVertice() {
            DirectedGraph<String, Integer> g = new DirectedGraph<>();
            g.agregarVertice("A");
            assertTrue(g.esConexo());
        }

        @Test
        @DisplayName("un camino sin vuelta no es fuertemente conexo, sin importar el inicio")
        void caminoSinVuelta() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 1);
            assertFalse(graph.esConexo());
        }

        @Test
        @DisplayName("todos alcanzan al primero pero no al revés: no es conexo")
        void soloEntrantes() {
            graph.agregarArista("B", "A", 1);
            graph.agregarArista("C", "A", 1);
            assertFalse(graph.esConexo());
        }

        @Test
        @DisplayName("un ciclo que cubre todos los vértices es conexo")
        void cicloCompleto() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 1);
            graph.agregarArista("C", "A", 1);
            assertTrue(graph.esConexo());
        }

        @Test
        @DisplayName("un vértice aislado hace que no sea conexo")
        void verticeAislado() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "A", 1);
            assertFalse(graph.esConexo());
        }
    }

    @Nested
    @DisplayName("tieneCiclos")
    class TieneCiclos {
        @Test
        @DisplayName("grafo vacío y grafo sin aristas no tienen ciclos")
        void sinAristas() {
            assertFalse(graph.tieneCiclos());
            graph.vaciar();
            assertFalse(graph.tieneCiclos());
        }

        @Test
        @DisplayName("ciclo de dos vértices")
        void cicloSimple() {
            graph.agregarArista("A", "B", 5);
            graph.agregarArista("B", "A", 5);
            assertTrue(graph.tieneCiclos());
        }

        @Test
        @DisplayName("un lazo es un ciclo")
        void lazo() {
            graph.agregarArista("B", "B", 1);
            assertTrue(graph.tieneCiclos());
        }

        @Test
        @DisplayName("un diamante (DAG con caminos que convergen) no es un ciclo")
        void diamante() {
            graph.agregarVertice("D");
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("A", "C", 1);
            graph.agregarArista("B", "D", 1);
            graph.agregarArista("C", "D", 1);
            assertFalse(graph.tieneCiclos());
        }

        @Test
        @DisplayName("detecta un ciclo en una componente que no es la primera")
        void cicloEnOtraComponente() {
            graph.agregarVertice("D");
            graph.agregarArista("C", "D", 1);
            graph.agregarArista("D", "C", 1);
            assertTrue(graph.tieneCiclos()); // "A" y "B" quedan aislados
        }

        @Test
        @DisplayName("al eliminar la arista que cierra el ciclo deja de tenerlo")
        void romperCiclo() {
            graph.agregarArista("A", "B", 1);
            graph.agregarArista("B", "C", 1);
            graph.agregarArista("C", "A", 1);
            assertTrue(graph.tieneCiclos());
            graph.eliminarArista("C", "A");
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
        DirectedGraph<Integer, Integer> g = new DirectedGraph<>();
        int n = 100_000;
        for (int i = 0; i < n; i++) g.agregarVertice(i);
        for (int i = 0; i < n - 1; i++) g.agregarArista(i, i + 1, 1);

        assertFalse(g.tieneCiclos());
        assertFalse(g.esConexo());

        g.agregarArista(n - 1, 0, 1);
        assertTrue(g.tieneCiclos());
        assertTrue(g.esConexo());
    }
}
