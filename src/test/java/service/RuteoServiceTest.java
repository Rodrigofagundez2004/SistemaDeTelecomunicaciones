package service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}