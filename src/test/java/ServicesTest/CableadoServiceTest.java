package ServicesTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ucu.edu.aed.telecom.datos.RedDeEjemplo;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.ResultadoCableado;
import ucu.edu.aed.telecom.model.TipoEnlace;
import ucu.edu.aed.telecom.model.TipoNodo;
import ucu.edu.aed.telecom.service.CableadoService;

@DisplayName("CableadoService")
class CableadoServiceTest {

    private final CableadoService servicio = new CableadoService();

    /** Red chica: R1 y R2 ya están conectados con un enlace de costo 100. */
    private static Red redDeDosRouters() {
        Red red = new Red();
        red.agregarNodo(new Nodo("R1", TipoNodo.ROUTER));
        red.agregarNodo(new Nodo("R2", TipoNodo.ROUTER));
        red.agregarEnlace("R1", "R2", 1, 1, TipoEnlace.FIBRA, 1000, 100);
        return red;
    }

    @Test
    @DisplayName("una oficina con dos conexiones posibles elige la más barata")
    void eligeLaMasBarata() {
        Red red = redDeDosRouters();
        Nodo oficina = new Nodo("O", TipoNodo.OFICINA, 10, 0);
        List<Enlace> posibles = List.of(
                new Enlace(oficina, red.getNodo("R1"), 2, TipoEnlace.FIBRA, 1000, 50),
                new Enlace(oficina, red.getNodo("R2"), 2, TipoEnlace.FIBRA, 1000, 10));

        ResultadoCableado r = servicio.disenarCableado(red, List.of(oficina), posibles);

        assertEquals(1, r.aInstalar().size());
        assertTrue(r.aInstalar().get(0).conecta(oficina, red.getNodo("R2")));
        assertEquals(10, r.costoTotal(), 0.0);
        assertTrue(r.noConectables().isEmpty());
    }

    @Test
    @DisplayName("red de ejemplo: OF-NUEVA-2 se cuelga de OF-NUEVA-1 y OF-NUEVA-3 queda no conectable")
    void redDeEjemplo() {
        Red red = RedDeEjemplo.crear();
        List<Nodo> oficinas = RedDeEjemplo.oficinasNuevas();

        ResultadoCableado r = servicio.disenarCableado(red, oficinas,
                RedDeEjemplo.conexionesPosibles(red, oficinas));

        // OF-NUEVA-1 → SW-CENTRO (1200) + OF-NUEVA-2 → OF-NUEVA-1 (600)
        assertEquals(1800, r.costoTotal(), 0.0);
        assertEquals(2, r.aInstalar().size());
        assertEquals(Set.of(new Nodo("OF-NUEVA-3", TipoNodo.OFICINA)), r.noConectables());
    }

    @Test
    @DisplayName("reporta como no conectables las oficinas que no llegan a la red")
    void reportaNoConectables() {
        Red red = redDeDosRouters();
        Nodo aislada = new Nodo("A", TipoNodo.OFICINA, 5, 0);
        Nodo par1 = new Nodo("P1", TipoNodo.OFICINA, 5, 0);
        Nodo par2 = new Nodo("P2", TipoNodo.OFICINA, 5, 0);
        // P1 y P2 se pueden conectar entre sí, pero ninguna llega a la red: no se instala nada
        List<Enlace> posibles = List.of(new Enlace(par1, par2, 1, TipoEnlace.FIBRA, 1000, 30));

        ResultadoCableado r = servicio.disenarCableado(red, List.of(aislada, par1, par2), posibles);

        assertEquals(Set.of(aislada, par1, par2), r.noConectables());
        assertTrue(r.aInstalar().isEmpty());
        assertEquals(0, r.costoTotal(), 0.0);
    }

    @Test
    @DisplayName("no vuelve a comprar un enlace que ya existe, aunque la conexión posible sea más barata")
    void noRecompraEnlacesExistentes() {
        Red red = redDeDosRouters(); // R1-R2 ya instalado a costo 100
        Nodo oficina = new Nodo("O", TipoNodo.OFICINA, 10, 0);
        List<Enlace> posibles = List.of(
                new Enlace(red.getNodo("R1"), red.getNodo("R2"), 1, TipoEnlace.FIBRA, 1000, 1),
                new Enlace(oficina, red.getNodo("R1"), 2, TipoEnlace.FIBRA, 1000, 7));

        ResultadoCableado r = servicio.disenarCableado(red, List.of(oficina), posibles);

        assertEquals(1, r.aInstalar().size());
        assertTrue(r.aInstalar().get(0).conecta(oficina, red.getNodo("R1")));
        assertEquals(7, r.costoTotal(), 0.0);
    }

    @Test
    @DisplayName("un enlace caído sigue contando como instalado: no se vuelve a comprar")
    void enlaceCaidoNoSeRecompra() {
        Red red = redDeDosRouters();
        red.fallarEnlace("R1", "R2");
        List<Enlace> posibles = List.of(new Enlace(red.getNodo("R1"), red.getNodo("R2"), 1, TipoEnlace.FIBRA, 1000, 1));

        ResultadoCableado r = servicio.disenarCableado(red, List.of(), posibles);

        assertTrue(r.aInstalar().isEmpty());
        assertEquals(0, r.costoTotal(), 0.0);
    }

    @Test
    @DisplayName("sin red no hay nada a qué conectar: todas las oficinas quedan no conectables")
    void redVacia() {
        Red red = new Red();
        Nodo a = new Nodo("A", TipoNodo.OFICINA, 5, 0);
        Nodo b = new Nodo("B", TipoNodo.OFICINA, 5, 0);

        ResultadoCableado r = servicio.disenarCableado(red, List.of(a, b),
                List.of(new Enlace(a, b, 1, TipoEnlace.FIBRA, 1000, 5)));

        assertTrue(r.aInstalar().isEmpty());
        assertEquals(Set.of(a, b), r.noConectables());
    }

    @Test
    @DisplayName("una oficina nueva con id de un nodo de la red lanza IllegalArgumentException")
    void oficinaRepetidaEnLaRed() {
        Red red = redDeDosRouters();
        Nodo duplicado = new Nodo("R1", TipoNodo.OFICINA, 5, 0);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.disenarCableado(red, List.of(duplicado), List.of()));
    }

    @Test
    @DisplayName("una conexión posible con un extremo desconocido lanza IllegalArgumentException")
    void extremoDesconocido() {
        Red red = redDeDosRouters();
        Nodo fantasma = new Nodo("X", TipoNodo.OFICINA, 1, 0);
        List<Enlace> posibles = List.of(new Enlace(red.getNodo("R1"), fantasma, 1, TipoEnlace.FIBRA, 1000, 5));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.disenarCableado(red, List.of(), posibles));
    }
}