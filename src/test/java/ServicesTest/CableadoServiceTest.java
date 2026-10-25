package org.example;

import junit.framework.TestCase;
import ucu.edu.aed.telecom.service.CableadoService;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph; 

public class CableadoServiceTest extends TestCase {

    public void testEligeLaMasBarata() {
        UndirectedGraph<Nodo, Enlace> grafo = new UndirectedGraph<>();
        Nodo a = new Nodo("A");
        Nodo b = new Nodo("B");
        Nodo c = new Nodo("C");
        grafo.agregarVertice(a);
        grafo.agregarVertice(b);
        grafo.agregarVertice(c);

        // A-B cuesta 10, B-C cuesta 50
        grafo.agregarArista(a, b, new Enlace(10));
        grafo.agregarArista(b, c, new Enlace(50));

        CableadoService servicio = new CableadoService();
        IUndirectedGraph<Nodo, Enlace> resultado = servicio.disenarCableado(grafo);

        // El grafo resultante debe tener 2 aristas (n-1 para 3 nodos)
        assertEquals(2, resultado.aristas().size());
        
        // Verificamos que la arista A-B (la barata) esté en el resultado
        assertTrue(resultado.existeArista(a, b));
        assertTrue(resultado.existeArista(b, c));
    }

    public void testReportaNoConectables() {
        UndirectedGraph<Nodo, Enlace> grafo = new UndirectedGraph<>();
        Nodo a = new Nodo("A");
        Nodo b = new Nodo("B");
        Nodo c = new Nodo("C");
        grafo.agregarVertice(a);
        grafo.agregarVertice(b);
        grafo.agregarVertice(c);

        // Solo conectamos A con B. C queda aislado.
        grafo.agregarArista(a, b, new Enlace(10));

        CableadoService servicio = new CableadoService();
        IUndirectedGraph<Nodo, Enlace> resultado = servicio.disenarCableado(grafo);

        // Al no poder conectar todo, Kruskal devuelve un "bosque". 
        // El grafo resultante tendrá las aristas que pudo conectar, pero menos de n-1.
        assertTrue(resultado.aristas().size() < 2); 
        // O podés verificar que C no tiene aristas en el resultado
        assertTrue(resultado.adyacencias(c).isEmpty());
    }

    public void testNoRecompraEnlacesExistentes() {
        UndirectedGraph<Nodo, Enlace> grafo = new UndirectedGraph<>();
        Nodo a = new Nodo("A");
        Nodo b = new Nodo("B");
        Nodo c = new Nodo("C");
        grafo.agregarVertice(a);
        grafo.agregarVertice(b);
        grafo.agregarVertice(c);

        // A-B cuesta 100, B-C cuesta 50, A-C cuesta 200
        grafo.agregarArista(a, b, new Enlace(100));
        grafo.agregarArista(b, c, new Enlace(50));
        grafo.agregarArista(a, c, new Enlace(200));

        CableadoService servicio = new CableadoService();
        IUndirectedGraph<Nodo, Enlace> resultado = servicio.disenarCableado(grafo);

        // Kruskal debería elegir B-C (50) y A-B (100), descartando A-C (200) para no hacer ciclo.
        assertEquals(2, resultado.aristas().size());
        assertTrue(resultado.existeArista(b, c));
        assertTrue(resultado.existeArista(a, b));
        assertFalse(resultado.existeArista(a, c)); // No debe estar la más cara
    }
}