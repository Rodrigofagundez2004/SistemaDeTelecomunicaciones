package telecom;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.Impl.VistaFiltrada;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.telecom.model.*;

@DisplayName("Red")
public class RedTest {

    // Red usada en los tests (latencia en ms / ancho de banda en Mbps):
    //
    //        B                camino corto A-B-D: 1 + 1 = 2 ms, por cobre de 100 Mbps
    //      /   \              camino largo A-C-D: 5 + 5 = 10 ms, por fibra de 1000 Mbps
    //     A     D             A-B tiene latencia asimétrica: 1 ms de ida, 3 ms de vuelta
    //      \   /
    //        C
    private Red red;
    private Nodo a, b, c, d;
    private final DirectedGraphAlgorithms alg = new DirectedGraphAlgorithms();

    @BeforeEach
    void setUp() {
        red = new Red();
        a = new Nodo("A", TipoNodo.ROUTER);
        b = new Nodo("B", TipoNodo.ROUTER);
        c = new Nodo("C", TipoNodo.ROUTER);
        d = new Nodo("D", TipoNodo.DATA_CENTER, 0, 100);
        for (Nodo n : List.of(a, b, c, d)) red.agregarNodo(n);
        red.agregarEnlace("A", "B", 1, 3, TipoEnlace.COBRE, 100, 1500);
        red.agregarEnlace("B", "D", 1, 1, TipoEnlace.COBRE, 100, 1500);
        red.agregarEnlace("A", "C", 5, 5, TipoEnlace.FIBRA, 1000, 8000);
        red.agregarEnlace("C", "D", 5, 5, TipoEnlace.FIBRA, 1000, 8000);
    }

    private boolean hayArco(IDirectedIGraph<Nodo, PesoLatencia> grafo, Nodo x, Nodo y) {
        return grafo.existeArista(grafo.construirComparable(x), grafo.construirComparable(y));
    }

    private IDijkstraResult<Nodo> dijkstraDesdeA(IDirectedIGraph<Nodo, PesoLatencia> grafo) {
        return alg.dijkstra(grafo.construirComparable(a), grafo);
    }

    @Nested
    @DisplayName("construcción")
    class Construccion {
        @Test
        @DisplayName("cada enlace genera 2 arcos de ruteo con la latencia de su sentido y 1 arista física")
        void agregarEnlace() {
            IDirectedIGraph<Nodo, PesoLatencia> ruteo = red.getGrafoRuteo();

            assertEquals(8, ruteo.cantidadDeAristas());
            assertEquals(4, red.getGrafoFisico().cantidadDeAristas());
            assertEquals(1, ruteo.obtenerArista(a, b).dato().getWeight());
            assertEquals(3, ruteo.obtenerArista(b, a).dato().getWeight());
            assertEquals(1500, red.getGrafoFisico().obtenerArista(a, b).dato().getWeight());
        }

        @Test
        @DisplayName("los arcos y la arista apuntan al mismo Enlace")
        void mismoEnlace() {
            Enlace ab = red.getEnlace("A", "B");

            assertSame(ab, red.getGrafoRuteo().obtenerArista(a, b).dato().getEnlace());
            assertSame(ab, red.getGrafoRuteo().obtenerArista(b, a).dato().getEnlace());
            assertSame(ab, red.getGrafoFisico().obtenerArista(a, b).dato().getEnlace());
            assertSame(ab, red.getEnlace("B", "A"));
        }

        @Test
        @DisplayName("rechaza un enlace repetido entre el mismo par, en cualquier orden")
        void enlaceRepetido() {
            assertThrows(IllegalArgumentException.class,
                    () -> red.agregarEnlace("A", "B", 2, 2, TipoEnlace.FIBRA, 1000, 100));
            assertThrows(IllegalArgumentException.class,
                    () -> red.agregarEnlace("B", "A", 2, 2, TipoEnlace.FIBRA, 1000, 100));
            assertEquals(4, red.getEnlaces().size());
        }

        @Test
        @DisplayName("rechaza nodos con id repetido y enlaces con nodos ajenos a la red")
        void nodosInvalidos() {
            assertThrows(IllegalArgumentException.class, () -> red.agregarNodo(new Nodo("A", TipoNodo.SWITCH)));

            Nodo ajeno = new Nodo("X", TipoNodo.ROUTER);
            assertThrows(IllegalArgumentException.class,
                    () -> red.agregarEnlace(new Enlace(a, ajeno, 1, TipoEnlace.FIBRA, 100, 100)));
            // Mismo id que un nodo de la red, pero otra instancia
            Nodo copiaDeA = new Nodo("A", TipoNodo.ROUTER);
            assertThrows(IllegalArgumentException.class,
                    () -> red.agregarEnlace(new Enlace(copiaDeA, c, 1, TipoEnlace.FIBRA, 100, 100)));
        }

        @Test
        @DisplayName("getNodo y getEnlace lanzan IllegalArgumentException con ids inexistentes")
        void idsInexistentes() {
            assertThrows(IllegalArgumentException.class, () -> red.getNodo("Z"));
            assertThrows(IllegalArgumentException.class, () -> red.getEnlace("A", "Z"));
            assertNull(red.getEnlace("A", "D"));   // los dos existen, pero no hay enlace
        }
    }

    @Nested
    @DisplayName("fallas y vistas")
    class Fallas {
        @Test
        @DisplayName("una falla de enlace oculta los arcos en los dos sentidos")
        void fallaDeEnlaceOcultaAmbosSentidos() {
            red.fallarEnlace("A", "B");
            IDirectedIGraph<Nodo, PesoLatencia> vista = red.vistaActiva();

            assertFalse(hayArco(vista, a, b));
            assertFalse(hayArco(vista, b, a));
            assertTrue(hayArco(vista, b, d));
            // El grafo completo no se toca
            assertTrue(hayArco(red.getGrafoRuteo(), a, b));
            assertTrue(hayArco(red.getGrafoRuteo(), b, a));
        }

        @Test
        @DisplayName("Dijkstra sobre la vista activa evita el nodo caído")
        void dijkstraEvitaNodoCaido() {
            assertEquals(List.of(a, b, d), dijkstraDesdeA(red.vistaActiva()).getPath(d));

            red.fallarNodo("B");
            IDijkstraResult<Nodo> r = dijkstraDesdeA(red.vistaActiva());

            assertEquals(List.of(a, c, d), r.getPath(d));
            assertEquals(10, r.getCost(d));
            assertEquals(Double.POSITIVE_INFINITY, r.getCost(b));
        }

        @Test
        @DisplayName("al restaurar el nodo, la ruta corta vuelve a estar disponible")
        void restaurarNodo() {
            red.fallarNodo("B");
            red.restaurarNodo("B");

            assertEquals(List.of(a, b, d), dijkstraDesdeA(red.vistaActiva()).getPath(d));
        }

        @Test
        @DisplayName("el filtro de ancho de banda mínimo descarta los enlaces lentos")
        void filtroDeAnchoDeBanda() {
            IDirectedIGraph<Nodo, PesoLatencia> vista = new VistaFiltrada<>(red.vistaActiva(),
                    n -> true, p -> p.getEnlace().getAnchoDeBanda() >= 500);

            assertFalse(hayArco(vista, a, b));
            assertEquals(List.of(a, c, d), dijkstraDesdeA(vista).getPath(d));
        }

        @Test
        @DisplayName("la vista activa nunca muestra arcos hacia un nodo caído")
        void sinArcosHaciaNodosCaidos() {
            red.fallarNodo("D");

            for (Edge<Nodo, PesoLatencia> e : red.vistaActiva().aristas()) {
                assertNotEquals(d, e.source());
                assertNotEquals(d, e.target());
            }
            assertEquals(Double.POSITIVE_INFINITY, alg.floyd(red.vistaActiva()).getCost(a, d));
        }

        @Test
        @DisplayName("vistaActivaSin simula una falla sin modificar la red ni su versión")
        void simulacionNoModificaLaRed() {
            int version = red.getVersion();
            IDirectedIGraph<Nodo, PesoLatencia> simulada = red.vistaActivaSin(Set.of(b), Set.of());

            assertEquals(List.of(a, c, d), dijkstraDesdeA(simulada).getPath(d));
            assertTrue(b.isActivo());
            assertEquals(version, red.getVersion());
            assertEquals(List.of(a, b, d), dijkstraDesdeA(red.vistaActiva()).getPath(d));
        }

        @Test
        @DisplayName("vistaActivaSin también oculta enlaces y respeta las fallas reales")
        void simulacionDeEnlace() {
            red.fallarEnlace("C", "D");
            IDirectedIGraph<Nodo, PesoLatencia> simulada = red.vistaActivaSin(Set.of(), Set.of(red.getEnlace("B", "D")));

            assertEquals(Double.POSITIVE_INFINITY, dijkstraDesdeA(simulada).getCost(d));
        }

        @Test
        @DisplayName("la versión cambia solo con cambios reales")
        void version() {
            int inicial = red.getVersion();

            red.fallarNodo("B");
            red.fallarNodo("B");            // ya estaba caído: no cuenta
            assertEquals(inicial + 1, red.getVersion());

            red.restaurarNodo("B");
            red.fallarEnlace("A", "C");
            assertEquals(inicial + 3, red.getVersion());
        }

        @Test
        @DisplayName("fallar un enlace inexistente lanza IllegalArgumentException")
        void fallarEnlaceInexistente() {
            assertThrows(IllegalArgumentException.class, () -> red.fallarEnlace("A", "D"));
        }
    }

    @Nested
    @DisplayName("modelo")
    class Modelo {
        @Test
        @DisplayName("Enlace devuelve la latencia según el sentido y el otro extremo")
        void enlace() {
            Enlace ab = red.getEnlace("A", "B");

            assertEquals(1, ab.latenciaDesde(a));
            assertEquals(3, ab.latenciaDesde(b));
            assertEquals(b, ab.otroExtremo(a));
            assertThrows(IllegalArgumentException.class, () -> ab.latenciaDesde(c));
            assertThrows(IllegalArgumentException.class, () -> new Enlace(a, a, 1, TipoEnlace.FIBRA, 100, 0));
        }

        @Test
        @DisplayName("Nodo: igualdad por id y demanda según el tipo")
        void nodo() {
            assertEquals(a, new Nodo("A", TipoNodo.SWITCH));
            assertEquals(a.hashCode(), new Nodo("A", TipoNodo.SWITCH).hashCode());
            assertEquals(1800, new Nodo("ED", TipoNodo.EDIFICIO, 120, 0).demandaEstimadaMbps());
            assertEquals(0, a.demandaEstimadaMbps());
            assertEquals(List.of(d), red.getDataCenters());
        }
    }
}
