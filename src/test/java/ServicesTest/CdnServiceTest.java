package org.example;

import junit.framework.TestCase;
import ucu.edu.aed.telecom.service.CdnService;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraph;

public class CdnServiceTest extends TestCase {

    private DirectedGraph<Nodo, Enlace> red;
    private CdnService servicio;
    private Nodo c1, c2, dc1, dc2;

    @Override
    protected void setUp() {
        red = new DirectedGraph<>();
        servicio = new CdnService();

        c1 = new Nodo("Cliente1");
        c2 = new Nodo("Cliente2");
        dc1 = new Nodo("DC1");
        dc2 = new Nodo("DC2");

        red.agregarVertice(c1);
        red.agregarVertice(c2);
        red.agregarVertice(dc1);
        red.agregarVertice(dc2);

        // C1 conectado a DC1 (costo 10) y a DC2 (costo 50)
        red.agregarArista(c1, dc1, new Enlace(10));
        red.agregarArista(c1, dc2, new Enlace(50));

        // C2 conectado solo a DC2 (costo 20)
        red.agregarArista(c2, dc2, new Enlace(20));
    }

    public void testDatacenterMasCercano() {
        // Para C1, el más cercano debe ser DC1 (10 < 50)
        Nodo masCercanoC1 = servicio.datacenterMasCercano(red, c1);
        assertEquals(dc1, masCercanoC1);

        // Para C2, el único alcanzable es DC2 (20)
        Nodo masCercanoC2 = servicio.datacenterMasCercano(red, c2);
        assertEquals(dc2, masCercanoC2);
    }

    public void testSugerirNuevoDatacenter() {
        // Si ponemos un DC en C2, el Cliente2 tendría costo 0.
        // Si lo ponemos en C1, el Cliente1 tendría costo 0, pero el Cliente2 seguiría en 20.
        // Probablemente C1 sea mejor candidato para bajar el promedio general, o C2.
        
        Nodo sugerido = servicio.sugerirNuevoDatacenter(red);
        
        assertNotNull(sugerido);
        
        // El candidato no debería ser un DC existente
        assertNotSame(dc1, sugerido);
        assertNotSame(dc2, sugerido);
        
        // Dependiendo de los costos, debería sugerir C1 o C2
        assertTrue(sugerido.equals(c1) || sugerido.equals(c2));
    }
}