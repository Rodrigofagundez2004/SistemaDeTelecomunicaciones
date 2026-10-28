package service;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.telecom.service.ResilienciaService;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.Ruta;
import ucu.edu.aed.telecom.model.TipoNodo;
import ucu.edu.aed.telecom.model.TipoEnlace;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResilienciaServiceTest {

    private Nodo nodo(String id, TipoNodo tipo) {
        return new Nodo(id, tipo);
    }

    private Red crearRedConPuente() {
        Red red = new Red();

        red.agregarNodo(nodo("A", TipoNodo.DATA_CENTER));
        red.agregarNodo(new Nodo("B", TipoNodo.OFICINA, 10, 0));
        red.agregarNodo(new Nodo("C", TipoNodo.OFICINA, 20, 0));
        red.agregarNodo(new Nodo("D", TipoNodo.ROUTER));
        red.agregarNodo(new Nodo("E", TipoNodo.OFICINA, 30, 0));
        red.agregarNodo(new Nodo("F", TipoNodo.OFICINA, 40, 0));

        red.agregarEnlace("A", "B", 1, 1, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("B", "C", 1, 1, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("C", "D", 1, 1, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("D", "E", 1, 1, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("E", "F", 1, 1, TipoEnlace.values()[0], 100, 10);

        return red;
    }

    @Test
    void detectaEquiposCriticos() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        Set<Nodo> criticos = service.equiposCriticos();

        assertTrue(criticos.contains(red.getNodo("B")));
        assertTrue(criticos.contains(red.getNodo("C")));
        assertTrue(criticos.contains(red.getNodo("D")));
        assertTrue(criticos.contains(red.getNodo("E")));
    }

    @Test
    void detectaEnlacesCriticos() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        Set<Enlace> criticos = service.enlacesCriticos();

        assertEquals(5, criticos.size());
    }

    @Test
    void detectaNodosIncomunicadosSiFallaUnNodo() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        Set<Nodo> incomunicados =
                service.incomunicadosSiFallaNodo("D");

        assertTrue(incomunicados.contains(red.getNodo("E")));
        assertTrue(incomunicados.contains(red.getNodo("F")));
        assertFalse(incomunicados.contains(red.getNodo("A")));
    }

    @Test
    void detectaNodosIncomunicadosSiFallaUnEnlace() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        Set<Nodo> incomunicados =
                service.incomunicadosSiFallaEnlace("D", "E");

        assertTrue(incomunicados.contains(red.getNodo("E")));
        assertTrue(incomunicados.contains(red.getNodo("F")));
        assertFalse(incomunicados.contains(red.getNodo("A")));
    }

    @Test
    void encuentraRutaAlternativaCuandoExisteOtroCamino() {
        Red red = new Red();

        red.agregarNodo(nodo("A", TipoNodo.DATA_CENTER));
        red.agregarNodo(nodo("B", TipoNodo.ROUTER));
        red.agregarNodo(nodo("C", TipoNodo.ROUTER));
        red.agregarNodo(new Nodo("D", TipoNodo.OFICINA, 10, 0));

        red.agregarEnlace("A", "B", 1, 1, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("B", "D", 1, 1, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("A", "C", 2, 2, TipoEnlace.values()[0], 100, 10);
        red.agregarEnlace("C", "D", 2, 2, TipoEnlace.values()[0], 100, 10);

        ResilienciaService service = new ResilienciaService(red);

        Ruta ruta = service.rutaAlternativa("A", "D");

        assertTrue(ruta.existe());
        assertEquals("A", ruta.nodos().get(0).getId());
        assertEquals("D", ruta.nodos().get(ruta.nodos().size() - 1).getId());
    }

    @Test
    void devuelveRutaInexistenteSiNoHayAlternativa() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        Ruta ruta = service.rutaAlternativa("A", "F");

        assertFalse(ruta.existe());
    }

    @Test
    void rechazaRutaAlternativaConMismoOrigenYDestino() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.rutaAlternativa("A", "A")
        );
    }

    @Test
    void calculaRankingDeCriticidad() {
        Red red = crearRedConPuente();
        ResilienciaService service = new ResilienciaService(red);

        List<Map.Entry<Nodo, Integer>> ranking =
                service.rankingCriticidad();

        assertFalse(ranking.isEmpty());

        for (int i = 0; i + 1 < ranking.size(); i++) {
            assertTrue(
                    ranking.get(i).getValue()
                            >= ranking.get(i + 1).getValue()
            );
        }
    }
}
