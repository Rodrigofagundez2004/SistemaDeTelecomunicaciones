package ucu.edu.aed.telecom.datos;

import static ucu.edu.aed.telecom.model.TipoEnlace.*;
import static ucu.edu.aed.telecom.model.TipoNodo.*;

import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.TipoEnlace;
import ucu.edu.aed.telecom.model.TipoNodo;

/**
 * Red de ejemplo para la demo y los tests de los servicios: 25 nodos y 26 enlaces.
 * <pre>
 *                      DC-MVD
 *                     /      \
 *   SW-CENTRO -- R-CORE-1 -- R-CORE-2 -- SW-POCITOS
 *                   |            |   \
 *                   |            |    SRV-CACHE
 *                R-CORE-4 -- R-CORE-3 -- SW-NORTE
 *                   :            |
 *          (radio)  :          DC-NORTE
 *                   :
 *                R-ESTE -- DC-ESTE
 *                   |
 *                SW-ESTE
 * </pre>
 * Cada switch atiende a tres clientes (oficinas, edificios o puntos de acceso).
 * <p>
 * Casos incluidos a propósito, para que cada funcionalidad tenga algo que mostrar:
 * <ul>
 *     <li><b>Caminos de igual costo:</b> R-CORE-1 → R-CORE-3 cuesta 4 ms por R-CORE-2 (2 + 2) y por R-CORE-4 (1 + 3).</li>
 *     <li><b>Latencia asimétrica:</b> el radioenlace R-CORE-4 ↔ R-ESTE tiene 8 ms de ida y 12 ms de vuelta.</li>
 *     <li><b>Puentes:</b> R-CORE-4 ↔ R-ESTE (aísla toda la región este) y R-CORE-3 ↔ DC-NORTE.</li>
 *     <li><b>Puntos de articulación:</b> R-CORE-3 (aísla DC-NORTE y la zona norte), R-CORE-4, R-ESTE y cada switch.</li>
 *     <li><b>Data center sobrecargado:</b> DC-ESTE tiene capacidad 200 y la región este suma 250 usuarios.</li>
 *     <li><b>Enlace saturado:</b> ED-POCITOS demanda 120 × 15 = 1800 Mbps sobre un cobre de 1000 Mbps.</li>
 *     <li><b>Candidato a nuevo data center:</b> SRV-CACHE, un servidor en el núcleo sin función de DC.</li>
 * </ul>
 * Unidades: latencia en ms, ancho de banda en Mbps, costo en USD.
 */
public final class RedDeEjemplo {

    private RedDeEjemplo() {
    }

    /** Crea una red nueva en cada llamada, así cada test puede fallar nodos sin afectar a otros. */
    public static Red crear() {
        Red red = new Red();

        // Data centers (capacidad = usuarios máximos)
        nodo(red, "DC-MVD", DATA_CENTER, 0, 2000);
        nodo(red, "DC-NORTE", DATA_CENTER, 0, 300);
        nodo(red, "DC-ESTE", DATA_CENTER, 0, 200);

        // Infraestructura
        nodo(red, "R-CORE-1", ROUTER, 0, 0);
        nodo(red, "R-CORE-2", ROUTER, 0, 0);
        nodo(red, "R-CORE-3", ROUTER, 0, 0);
        nodo(red, "R-CORE-4", ROUTER, 0, 0);
        nodo(red, "R-ESTE", ROUTER, 0, 0);
        nodo(red, "SW-CENTRO", SWITCH, 0, 0);
        nodo(red, "SW-POCITOS", SWITCH, 0, 0);
        nodo(red, "SW-NORTE", SWITCH, 0, 0);
        nodo(red, "SW-ESTE", SWITCH, 0, 0);
        nodo(red, "SRV-CACHE", SERVIDOR, 0, 0);

        // Clientes (usuarios)
        nodo(red, "OF-CENTRO", OFICINA, 40, 0);
        nodo(red, "OF-CIUDAD-VIEJA", OFICINA, 25, 0);
        nodo(red, "ED-CORDON", EDIFICIO, 100, 0);
        nodo(red, "ED-POCITOS", EDIFICIO, 120, 0);
        nodo(red, "ED-PUNTA-CARRETAS", EDIFICIO, 90, 0);
        nodo(red, "AP-RAMBLA", PUNTO_DE_ACCESO, 60, 0);
        nodo(red, "OF-PRADO", OFICINA, 30, 0);
        nodo(red, "ED-SAYAGO", EDIFICIO, 70, 0);
        nodo(red, "AP-COLON", PUNTO_DE_ACCESO, 50, 0);
        nodo(red, "ED-PUNTA-DEL-ESTE", EDIFICIO, 150, 0);
        nodo(red, "OF-MALDONADO", OFICINA, 20, 0);
        nodo(red, "AP-PLAYA", PUNTO_DE_ACCESO, 80, 0);

        // Anillo de núcleo: dos caminos de 4 ms entre R-CORE-1 y R-CORE-3
        enlace(red, "R-CORE-1", "R-CORE-2", 2, 2, FIBRA, 10000, 20000);
        enlace(red, "R-CORE-2", "R-CORE-3", 2, 2, FIBRA, 10000, 20000);
        enlace(red, "R-CORE-3", "R-CORE-4", 3, 3, FIBRA, 10000, 20000);
        enlace(red, "R-CORE-4", "R-CORE-1", 1, 1, FIBRA, 10000, 20000);

        // Data centers y servidor. DC-MVD tiene 1.5 ms a cada router para que pasar por él
        // (1.5 + 1.5 = 3) sea peor que el anillo (2) y no rompa el empate de 4 ms
        enlace(red, "DC-MVD", "R-CORE-1", 1.5, 1.5, FIBRA, 10000, 8000);
        enlace(red, "DC-MVD", "R-CORE-2", 1.5, 1.5, FIBRA, 10000, 8000);
        enlace(red, "DC-NORTE", "R-CORE-3", 1, 1, FIBRA, 10000, 8000);    // puente
        enlace(red, "SRV-CACHE", "R-CORE-2", 0.5, 0.5, FIBRA, 10000, 5000);

        // Región este: un único radioenlace asimétrico la une al núcleo (puente)
        enlace(red, "R-CORE-4", "R-ESTE", 8, 12, RADIOENLACE, 300, 4000);
        enlace(red, "R-ESTE", "DC-ESTE", 0.5, 0.5, FIBRA, 10000, 5000);
        enlace(red, "R-ESTE", "SW-ESTE", 1, 1, FIBRA, 10000, 5000);

        // Switches de acceso
        enlace(red, "SW-CENTRO", "R-CORE-1", 1, 1, FIBRA, 10000, 5000);
        enlace(red, "SW-POCITOS", "R-CORE-2", 1, 1, FIBRA, 10000, 5000);
        enlace(red, "SW-NORTE", "R-CORE-3", 1.5, 1.5, FIBRA, 10000, 5000);

        // Última milla
        enlace(red, "OF-CENTRO", "SW-CENTRO", 2, 2, COBRE, 1000, 1500);
        enlace(red, "OF-CIUDAD-VIEJA", "SW-CENTRO", 2.5, 2.5, COBRE, 1000, 1500);
        enlace(red, "ED-CORDON", "SW-CENTRO", 1.5, 1.5, FIBRA, 2000, 5000);
        enlace(red, "ED-POCITOS", "SW-POCITOS", 2, 2, COBRE, 1000, 1500);   // saturado
        enlace(red, "ED-PUNTA-CARRETAS", "SW-POCITOS", 1.5, 1.5, FIBRA, 2000, 5000);
        enlace(red, "AP-RAMBLA", "SW-POCITOS", 4, 4, RADIOENLACE, 300, 4000);
        enlace(red, "OF-PRADO", "SW-NORTE", 3, 3, COBRE, 1000, 1500);
        enlace(red, "ED-SAYAGO", "SW-NORTE", 2, 2, FIBRA, 2000, 5000);
        enlace(red, "AP-COLON", "SW-NORTE", 5, 5, RADIOENLACE, 300, 4000);
        enlace(red, "ED-PUNTA-DEL-ESTE", "SW-ESTE", 1.5, 1.5, FIBRA, 5000, 5000);
        enlace(red, "OF-MALDONADO", "SW-ESTE", 3, 3, COBRE, 1000, 1500);
        enlace(red, "AP-PLAYA", "SW-ESTE", 4, 4, RADIOENLACE, 300, 4000);

        return red;
    }

    private static void nodo(Red red, String id, TipoNodo tipo, int usuarios, int capacidad) {
        red.agregarNodo(new Nodo(id, tipo, usuarios, capacidad));
    }

    private static void enlace(Red red, String a, String b, double latAB, double latBA,
                               TipoEnlace tipo, double anchoDeBanda, double costo) {
        red.agregarEnlace(a, b, latAB, latBA, tipo, anchoDeBanda, costo);
    }
}
