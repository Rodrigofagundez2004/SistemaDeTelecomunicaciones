package service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.telecom.model.TipoEnlace;
import ucu.edu.aed.telecom.model.TipoNodo;
import ucu.edu.aed.telecom.datos.RedDeEjemplo;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.Ruta;
import ucu.edu.aed.telecom.service.RuteoService;

@DisplayName("RuteoService")
public class RuteoServiceTest {

    private Red red;
    private RuteoService ruteo;

    @BeforeEach
    void setUp() {
        red = RedDeEjemplo.crear();          // red nueva en cada test
        ruteo = new RuteoService(red);
    }

    private List<String> ids(Ruta ruta) {
        return ruta.nodos().stream().map(Nodo::getId).toList();
    }

    private RuteoService ruteoTestsAnchoDeBanda() {
        Red rombo = new Red();
        rombo.agregarNodo(new Nodo("A", TipoNodo.ROUTER));
        rombo.agregarNodo(new Nodo("B", TipoNodo.ROUTER));
        rombo.agregarNodo(new Nodo("C", TipoNodo.ROUTER));
        rombo.agregarNodo(new Nodo("D", TipoNodo.ROUTER));
        rombo.agregarEnlace("A", "B", 1, 1, TipoEnlace.COBRE, 100, 1500);
        rombo.agregarEnlace("B", "D", 1, 1, TipoEnlace.COBRE, 100, 1500);
        rombo.agregarEnlace("A", "C", 5, 5, TipoEnlace.FIBRA, 1000, 8000);
        rombo.agregarEnlace("C", "D", 5, 5, TipoEnlace.FIBRA, 1000, 8000);
        return new RuteoService(rombo);
    }

    @Test
    @DisplayName("Menor latencia: devuelve los nodos intermedios y la latencia total")
    void rutaDeMenorLatenciaIda() {
        Ruta ruta = ruteo.rutaDeMenorLatencia("OF-CENTRO", "ED-PUNTA-DEL-ESTE");

        assertEquals(List.of("OF-CENTRO", "SW-CENTRO", "R-CORE-1", "R-CORE-4",
                "R-ESTE", "SW-ESTE", "ED-PUNTA-DEL-ESTE"), ids(ruta));
        assertEquals(14.5, ruta.latencia());   // 2 + 1 + 1 + 8 + 1 + 1.5
    }

    @Test
    @DisplayName("Latencia asimétrica: la vuelta cuesta distinto que la ida")
    void rutaDeMenorLatenciaVuelta() {
        Ruta ruta = ruteo.rutaDeMenorLatencia("ED-PUNTA-DEL-ESTE", "OF-CENTRO");

        assertEquals(18.5, ruta.latencia());   // el radioenlace R-ESTE -> R-CORE-4 cuesta 12 en vez de 8
    }

    @Test
    @DisplayName("Origen igual a destino: ruta de un solo nodo y latencia 0")
    void origenIgualDestino() {
        Ruta ruta = ruteo.rutaDeMenorLatencia("OF-CENTRO", "OF-CENTRO");

        assertEquals(List.of("OF-CENTRO"), ids(ruta));
        assertEquals(0.0, ruta.latencia());
    }

    @Test
    @DisplayName("Nodo inexistente: lanza IllegalArgumentException")
    void nodoInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> ruteo.rutaDeMenorLatencia("NO-EXISTE", "OF-CENTRO"));
    }

    @Test
    @DisplayName("Falla de un enlace: recalcula por otro camino")
    void fallaDeEnlaceDesvia() {
        red.fallarEnlace("R-CORE-1", "R-CORE-4");

        Ruta ruta = ruteo.rutaDeMenorLatencia("OF-CENTRO", "ED-PUNTA-DEL-ESTE");

        assertEquals(List.of("OF-CENTRO", "SW-CENTRO", "R-CORE-1", "R-CORE-2", "R-CORE-3",
                "R-CORE-4", "R-ESTE", "SW-ESTE", "ED-PUNTA-DEL-ESTE"), ids(ruta));
        assertEquals(20.5, ruta.latencia());
    }

    @Test
    @DisplayName("Falla de un punto de articulación: no hay ruta")
    void fallaDeNodoSinRuta() {
        red.fallarNodo("R-CORE-4");            // única entrada a la región este

        Ruta ruta = ruteo.rutaDeMenorLatencia("OF-CENTRO", "ED-PUNTA-DEL-ESTE");

        assertFalse(ruta.existe());
        assertEquals(Double.POSITIVE_INFINITY, ruta.latencia());
    }

    @Test
    @DisplayName("Origen caído: no hay ruta")
    void origenCaido() {
        red.fallarNodo("OF-CENTRO");

        assertFalse(ruteo.rutaDeMenorLatencia("OF-CENTRO", "ED-CORDON").existe());
    }

    @Test
    @DisplayName("Ancho de banda: si la ruta más rápida alcanza, se usa esa")
    void anchoDeBandaQueAlcanza() {
        Ruta ruta = ruteoTestsAnchoDeBanda().rutaConAnchoDeBandaMinimo("A", "D", 100);

        assertEquals(List.of("A", "B", "D"), ids(ruta));
        assertEquals(2.0, ruta.latencia());
    }

    @Test
    @DisplayName("Ancho de banda: si la ruta más rápida no alcanza, se desvía por una más lenta")
    void anchoDeBandaObligaADesviar() {
        Ruta ruta = ruteoTestsAnchoDeBanda().rutaConAnchoDeBandaMinimo("A", "D", 500);

        assertEquals(List.of("A", "C", "D"), ids(ruta));
        assertEquals(10.0, ruta.latencia());
    }

    @Test
    @DisplayName("Ancho de banda: el límite es inclusivo (un enlace de 1000 sirve para 1000)")
    void anchoDeBandaLimiteInclusivo() {
        assertTrue(ruteoTestsAnchoDeBanda().rutaConAnchoDeBandaMinimo("A", "D", 1000).existe());
    }

    @Test
    @DisplayName("Ancho de banda: si ningún camino alcanza, no hay ruta")
    void anchoDeBandaSinRuta() {
        assertFalse(ruteoTestsAnchoDeBanda().rutaConAnchoDeBandaMinimo("A", "D", 1001).existe());
    }

    @Test
    @DisplayName("Ancho de banda: en la red de ejemplo, pedir 500 Mbps aísla la región este")
    void anchoDeBandaRadioenlace() {
        // el único acceso al este es un radioenlace de 300 Mbps
        assertFalse(ruteo.rutaConAnchoDeBandaMinimo("OF-CENTRO", "ED-PUNTA-DEL-ESTE", 500).existe());
        assertTrue(ruteo.rutaConAnchoDeBandaMinimo("OF-CENTRO", "ED-PUNTA-DEL-ESTE", 300).existe());
    }

    @Test
    @DisplayName("Ancho de banda: también respeta las fallas de la red")
    void anchoDeBandaRespetaFallas() {
        red.fallarEnlace("R-CORE-1", "R-CORE-4");

        Ruta ruta = ruteo.rutaConAnchoDeBandaMinimo("OF-CENTRO", "ED-PUNTA-DEL-ESTE", 300);

        assertEquals(20.5, ruta.latencia());   // mismo desvío que sin restricción de ancho de banda
    }

    @Test
    @DisplayName("Ancho de banda: un mínimo negativo lanza IllegalArgumentException")
    void anchoDeBandaNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> ruteo.rutaConAnchoDeBandaMinimo("OF-CENTRO", "ED-CORDON", -1));
    }
}