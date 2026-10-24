package telecom;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.telecom.datos.RedDeEjemplo;
import ucu.edu.aed.telecom.model.*;

@DisplayName("RedDeEjemplo")
public class RedDeEjemploTest {

    private Red red;
    private final DirectedGraphAlgorithms alg = new DirectedGraphAlgorithms();

    @BeforeEach
    void setUp() {
        red = RedDeEjemplo.crear();
    }

    private double latencia(IDirectedIGraph<Nodo, PesoLatencia> grafo, String origen, String destino) {
        return alg.dijkstra(grafo.construirComparable(red.getNodo(origen)), grafo).getCost(red.getNodo(destino));
    }

    @Test
    @DisplayName("carga 25 nodos y 26 enlaces en los dos grafos")
    void cantidades() {
        assertEquals(25, red.getNodos().size());
        assertEquals(26, red.getEnlaces().size());
        assertEquals(25, red.getGrafoRuteo().cantidadDeVertices());
        assertEquals(52, red.getGrafoRuteo().cantidadDeAristas());   // 2 arcos por enlace
        assertEquals(26, red.getGrafoFisico().cantidadDeAristas());
        assertEquals(3, red.getDataCenters().size());
        assertEquals(12, red.getClientes().size());
    }

    @Test
    @DisplayName("un enlace que apunta a un id inexistente lanza IllegalArgumentException")
    void enlaceConIdInexistente() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> red.agregarEnlace("R-CORE-1", "NO-EXISTE", 1, 1, TipoEnlace.FIBRA, 1000, 100));

        assertTrue(e.getMessage().contains("NO-EXISTE"));
        assertEquals(26, red.getEnlaces().size());
    }

    @Test
    @DisplayName("cada llamada crea una red independiente")
    void redesIndependientes() {
        red.fallarNodo("R-CORE-1");

        assertTrue(RedDeEjemplo.crear().getNodo("R-CORE-1").isActivo());
    }

    @Test
    @DisplayName("la red completa es fuertemente conexa")
    void conexa() {
        assertTrue(red.vistaActiva().esConexo());
    }

    // ---- Casos que el dataset incluye a propósito (documentados en RedDeEjemplo) ----

    @Test
    @DisplayName("hay dos caminos de 4 ms entre R-CORE-1 y R-CORE-3")
    void caminosDeIgualCosto() {
        IDirectedIGraph<Nodo, PesoLatencia> vista = red.vistaActiva();

        assertEquals(4, latencia(vista, "R-CORE-1", "R-CORE-3"));
        // Sin cada uno de los intermedios, el otro camino sigue costando 4
        assertEquals(4, latencia(red.vistaActivaSin(Set.of(red.getNodo("R-CORE-2")), Set.of()), "R-CORE-1", "R-CORE-3"));
        assertEquals(4, latencia(red.vistaActivaSin(Set.of(red.getNodo("R-CORE-4")), Set.of()), "R-CORE-1", "R-CORE-3"));
    }

    @Test
    @DisplayName("el radioenlace R-CORE-4 <-> R-ESTE tiene latencia asimétrica")
    void latenciaAsimetrica() {
        Enlace radio = red.getEnlace("R-CORE-4", "R-ESTE");

        assertEquals(8, radio.latenciaDesde(red.getNodo("R-CORE-4")));
        assertEquals(12, radio.latenciaDesde(red.getNodo("R-ESTE")));
    }

    @Test
    @DisplayName("sin el radioenlace (puente) la región este queda aislada")
    void puenteAlEste() {
        IDirectedIGraph<Nodo, PesoLatencia> sinRadio =
                red.vistaActivaSin(Set.of(), Set.of(red.getEnlace("R-CORE-4", "R-ESTE")));

        assertEquals(Double.POSITIVE_INFINITY, latencia(sinRadio, "DC-MVD", "AP-PLAYA"));
        assertTrue(latencia(sinRadio, "DC-ESTE", "AP-PLAYA") < Double.POSITIVE_INFINITY);
    }

    @Test
    @DisplayName("sin R-CORE-3 (punto de articulación) DC-NORTE queda aislado")
    void articulacionNorte() {
        IDirectedIGraph<Nodo, PesoLatencia> sinCore3 = red.vistaActivaSin(Set.of(red.getNodo("R-CORE-3")), Set.of());

        assertEquals(Double.POSITIVE_INFINITY, latencia(sinCore3, "DC-MVD", "DC-NORTE"));
    }

    @Test
    @DisplayName("la región este supera la capacidad de DC-ESTE")
    void dataCenterSobrecargado() {
        int usuariosEste = 0;
        for (String id : new String[]{"ED-PUNTA-DEL-ESTE", "OF-MALDONADO", "AP-PLAYA"}) {
            usuariosEste += red.getNodo(id).getUsuarios();
        }

        assertTrue(usuariosEste > red.getNodo("DC-ESTE").getCapacidad());
    }

    @Test
    @DisplayName("la demanda de ED-POCITOS supera el ancho de banda de su enlace")
    void enlaceSaturado() {
        Nodo pocitos = red.getNodo("ED-POCITOS");
        Enlace acceso = red.getEnlace("ED-POCITOS", "SW-POCITOS");

        assertTrue(new UsoEnlace(acceso, pocitos.demandaEstimadaMbps()).utilizacion() > 1);
    }
}
