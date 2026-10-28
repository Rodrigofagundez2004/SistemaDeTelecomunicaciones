package ServicesTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.telecom.datos.RedDeEjemplo;
import ucu.edu.aed.telecom.model.AsignacionCdn;
import ucu.edu.aed.telecom.model.CargaDataCenter;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.TipoEnlace;
import ucu.edu.aed.telecom.model.TipoNodo;
import ucu.edu.aed.telecom.service.CdnService;

@DisplayName("CdnService")
class CdnServiceTest {

    private static final double DELTA = 1e-9;

    /**
     * Dos DC y dos clientes. C1 está cerca de DC1 (1 ms) y lejos de DC2 (5 ms); C2 al revés.
     * RTT C1→DC1 = 2 ms, RTT C1→DC2 = 10 ms. DC1 tiene capacidad 5, así que C1 lo sobrecarga.
     * C3 (5 usuarios) solo se conecta a DC1.
     */
    private static Red redDeDosDataCenters() {
        Red red = new Red();
        red.agregarNodo(new Nodo("DC1", TipoNodo.DATA_CENTER, 0, 5));
        red.agregarNodo(new Nodo("DC2", TipoNodo.DATA_CENTER, 0, 100));
        red.agregarNodo(new Nodo("C1", TipoNodo.OFICINA, 10, 0));
        red.agregarNodo(new Nodo("C2", TipoNodo.OFICINA, 20, 0));
        red.agregarNodo(new Nodo("C3", TipoNodo.OFICINA, 5, 0));
        red.agregarEnlace("C1", "DC1", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("C1", "DC2", 5, 5, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("C2", "DC2", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("C2", "DC1", 5, 5, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("C3", "DC1", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        return red;
    }

    /**
     * Un router R cerca de dos clientes (1 ms) y lejos de dos DC (20 ms). Hoy los clientes van a
     * DC1 con RTT 42 ms. Si R fuera un data center, el RTT bajaría a 2 ms.
     */
    private static Red redConRouterCercano(TipoNodo tipoDeR) {
        Red red = new Red();
        red.agregarNodo(new Nodo("DC1", TipoNodo.DATA_CENTER, 0, 1000));
        red.agregarNodo(new Nodo("DC2", TipoNodo.DATA_CENTER, 0, 1000));
        red.agregarNodo(new Nodo("R", tipoDeR, 0, 1000));
        red.agregarNodo(new Nodo("C1", TipoNodo.OFICINA, 10, 0));
        red.agregarNodo(new Nodo("C2", TipoNodo.OFICINA, 10, 0));
        red.agregarEnlace("R", "DC1", 20, 20, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("R", "DC2", 20, 20, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("C1", "R", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("C2", "R", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        return red;
    }

    @Test
    @DisplayName("cada cliente va al data center de menor RTT")
    void asignaPorMenorRtt() {
        Red red = redDeDosDataCenters();

        AsignacionCdn a = new CdnService(red).asignacionDataCenters();

        assertEquals(red.getNodo("DC1"), a.dcPorCliente().get(red.getNodo("C1")));
        assertEquals(red.getNodo("DC2"), a.dcPorCliente().get(red.getNodo("C2")));
        assertFalse(a.sinServicio().contains(red.getNodo("C3")));
    }

    @Test
    @DisplayName("ejemplo: OF-PRADO va a DC-NORTE (RTT 11) y no a DC-MVD (RTT 16)")
    void asignacionEnLaRedDeEjemplo() {
        Red red = RedDeEjemplo.crear();

        AsignacionCdn a = new CdnService(red).asignacionDataCenters();

        assertEquals(red.getNodo("DC-NORTE"), a.dcPorCliente().get(red.getNodo("OF-PRADO")));
        assertEquals(red.getNodo("DC-ESTE"), a.dcPorCliente().get(red.getNodo("ED-PUNTA-DEL-ESTE")));
    }

    @Test
    @DisplayName("si cae un DC, sus clientes se reasignan al siguiente más cercano")
    void siCaeUnDcSusClientesSeReasignan() {
        Red red = redDeDosDataCenters();
        CdnService cdn = new CdnService(red);
        Nodo c1 = red.getNodo("C1");
        Nodo dc2 = red.getNodo("DC2");

        red.fallarNodo("DC1");
        AsignacionCdn a = cdn.asignacionDataCenters();

        assertEquals(dc2, a.dcPorCliente().get(c1));
        assertFalse(a.dcPorCliente().containsValue(red.getNodo("DC1")));
        // C3 solo podía ir a DC1: queda sin servicio, no se le asigna nada
        assertEquals(Set.of(red.getNodo("C3")), a.sinServicio());
    }

    @Test
    @DisplayName("un nodo caído no aparece ni asignado ni sin servicio")
    void nodoCaidoNoCuenta() {
        Red red = RedDeEjemplo.crear();
        red.fallarNodo("OF-CENTRO");

        AsignacionCdn a = new CdnService(red).asignacionDataCenters();

        assertFalse(a.dcPorCliente().containsKey(red.getNodo("OF-CENTRO")));
        assertFalse(a.sinServicio().contains(red.getNodo("OF-CENTRO")));
    }

    @Test
    @DisplayName("la latencia promedio pondera por usuarios y se recalcula tras una falla")
    void latenciaPromedioPonderada() {
        Red red = redDeDosDataCenters();
        CdnService cdn = new CdnService(red);

        // C1 (10 usuarios, RTT 2) y C2 (20, RTT 2) y C3 (5, RTT 2): promedio 2
        assertEquals(2.0, cdn.latenciaPromedioPonderada(), DELTA);

        // Cae DC1: C1 pasa a DC2 (RTT 10), C2 sigue con RTT 2 y C3 queda sin servicio (no entra)
        // (10 × 10 + 20 × 2) / 30 = 140 / 30
        red.fallarNodo("DC1");
        assertEquals(140.0 / 30.0, cdn.latenciaPromedioPonderada(), DELTA);
    }

    @Test
    @DisplayName("la carga marca como sobrecargado al DC que supera su capacidad")
    void cargaDataCenters() {
        List<CargaDataCenter> carga = new CdnService(redDeDosDataCenters()).cargaDataCenters();

        CargaDataCenter dc1 = carga.stream().filter(c -> c.dc().getId().equals("DC1")).findFirst().orElseThrow();
        CargaDataCenter dc2 = carga.stream().filter(c -> c.dc().getId().equals("DC2")).findFirst().orElseThrow();
        // DC1 recibe a C1 (10) y a C3 (5): 15 usuarios sobre capacidad 5
        assertEquals(15, dc1.usuariosAsignados());
        assertTrue(dc1.sobrecargado());
        assertEquals(20, dc2.usuariosAsignados());
        assertFalse(dc2.sobrecargado());
    }

    @Test
    @DisplayName("ejemplo: DC-ESTE recibe 250 usuarios con capacidad 200 y queda sobrecargado")
    void dataCenterSobrecargadoEnLaRedDeEjemplo() {
        List<CargaDataCenter> carga = new CdnService(RedDeEjemplo.crear()).cargaDataCenters();

        CargaDataCenter este = carga.stream().filter(c -> c.dc().getId().equals("DC-ESTE")).findFirst().orElseThrow();
        assertEquals(250, este.usuariosAsignados());
        assertTrue(este.sobrecargado());
    }

    @Test
    @DisplayName("el candidato sugerido mejora la latencia promedio al instalarse como DC")
    void candidatoSugeridoMejoraElPromedio() {
        Red sinDc = redConRouterCercano(TipoNodo.ROUTER);
        CdnService cdn = new CdnService(sinDc);

        Nodo sugerido = cdn.sugerirNuevoDataCenter();

        assertEquals(sinDc.getNodo("R"), sugerido);
        double antes = cdn.latenciaPromedioPonderada();

        // Misma red con R instalado como data center
        double despues = new CdnService(redConRouterCercano(TipoNodo.DATA_CENTER)).latenciaPromedioPonderada();
        assertEquals(42.0, antes, DELTA);
        assertEquals(2.0, despues, DELTA);
        assertTrue(despues < antes);
    }

    @Test
    @DisplayName("sin candidatos que mejoren nada, no sugiere")
    void sinMejoraNoSugiere() {
        // C1 está pegado a DC1; el único router queda más lejos de C1 que DC1
        Red red = new Red();
        red.agregarNodo(new Nodo("DC1", TipoNodo.DATA_CENTER, 0, 1000));
        red.agregarNodo(new Nodo("C1", TipoNodo.OFICINA, 10, 0));
        red.agregarNodo(new Nodo("R", TipoNodo.ROUTER));
        red.agregarEnlace("C1", "DC1", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        red.agregarEnlace("R", "DC1", 10, 10, TipoEnlace.FIBRA, 1000, 100);

        assertNull(new CdnService(red).sugerirNuevoDataCenter());
    }

    @Test
    @DisplayName("sin data centers no hay nadie que atienda: todos quedan sin servicio")
    void todosLosDcCaidos() {
        Red red = RedDeEjemplo.crear();
        red.fallarNodo("DC-MVD");
        red.fallarNodo("DC-NORTE");
        red.fallarNodo("DC-ESTE");
        CdnService cdn = new CdnService(red);

        AsignacionCdn a = cdn.asignacionDataCenters();

        assertTrue(a.dcPorCliente().isEmpty());
        assertEquals(12, a.sinServicio().size());
        assertTrue(cdn.cargaDataCenters().isEmpty());
        assertEquals(Double.POSITIVE_INFINITY, cdn.latenciaPromedioPonderada());
    }

    @Test
    @DisplayName("con todos los DC caídos, la sugerencia conecta a todos los usuarios sin servicio")
    void todosLosDcCaidosSugerenciaConectaATodos() {
        Red red = RedDeEjemplo.crear();
        red.fallarNodo("DC-MVD");
        red.fallarNodo("DC-NORTE");
        red.fallarNodo("DC-ESTE");

        Nodo sugerido = new CdnService(red).sugerirNuevoDataCenter();

        assertNotNull(sugerido);
        assertTrue(sugerido.getTipo() == TipoNodo.ROUTER || sugerido.getTipo() == TipoNodo.SERVIDOR);
        // La red sigue conexa por los routers, así que el candidato alcanza a los 12 clientes
        assertEquals(red.getNodo("R-CORE-2"), sugerido);
        assertNotNull(new CdnService(red).centroDeLaRed());
    }

    @Test
    @DisplayName("el centro de una estrella es el nodo central")
    void centroDeUnaEstrella() {
        Red red = new Red();
        for (String id : List.of("H", "L1", "L2", "L3")) {
            red.agregarNodo(new Nodo(id, TipoNodo.ROUTER));
        }
        for (String hoja : List.of("L1", "L2", "L3")) {
            red.agregarEnlace("H", hoja, 1, 1, TipoEnlace.FIBRA, 1000, 100);
        }

        assertEquals(red.getNodo("H"), new CdnService(red).centroDeLaRed());
    }

    @Test
    @DisplayName("el centro es null si la red está partida")
    void centroDeRedPartida() {
        Red red = new Red();
        red.agregarNodo(new Nodo("A", TipoNodo.ROUTER));
        red.agregarNodo(new Nodo("B", TipoNodo.ROUTER));

        assertNull(new CdnService(red).centroDeLaRed());
    }

    @Test
    @DisplayName("ninguna consulta de la CDN modifica la red")
    void consultasNoModificanLaRed() {
        Red red = RedDeEjemplo.crear();
        CdnService cdn = new CdnService(red);
        int versionAntes = red.getVersion();

        cdn.asignacionDataCenters();
        cdn.cargaDataCenters();
        cdn.latenciaPromedioPonderada();
        cdn.sugerirNuevoDataCenter();
        cdn.centroDeLaRed();

        assertEquals(versionAntes, red.getVersion());
    }
}