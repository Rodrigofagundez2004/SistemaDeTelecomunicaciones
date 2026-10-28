package ucu.edu.aed.telecom.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.Impl.UnionFind;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.PesoCosto;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.ResultadoCableado;

/**
 * Diseño de cableado: conecta las oficinas nuevas a la red actual al menor costo total.
 * <p>
 * Se arma un grafo de planificación no dirigido con los nodos de la red y las oficinas nuevas.
 * Los enlaces ya instalados entran con costo 0, así Kruskal los usa sin costo y solo elige cable
 * nuevo donde hace falta. Como el grafo no admite aristas paralelas, una conexión posible que
 * repite un par ya ocupado se descarta: no se recompra un enlace existente. Si hay varias
 * opciones para el mismo par, gana la más barata.
 * <p>
 * Solo se instala cable en componentes que llegan a la red: un grupo de oficinas que se conectan
 * entre sí pero no a la red se reporta como no conectable y no genera costo.
 */
public class CableadoService {

    /**
     * @param red            red instalada; se usan todos sus enlaces, también los caídos
     * @param oficinasNuevas oficinas a conectar; no pueden existir ya en la red
     * @param posibles       enlaces que se podrían instalar, con su costo
     * @return los enlaces a instalar, su costo total y las oficinas que no llegan a la red
     * @throws IllegalArgumentException si una oficina ya existe en la red, está repetida, o un
     *                                  extremo de una conexión posible no es nodo de la red ni oficina nueva
     */
    public ResultadoCableado disenarCableado(Red red, List<Nodo> oficinasNuevas, List<Enlace> posibles) {
        IUndirectedGraph<Nodo, PesoCosto> planificacion = new UndirectedGraph<>();
        for (Nodo nodo : red.getNodos()) {
            planificacion.agregarVertice(nodo);
        }
        for (Nodo oficina : oficinasNuevas) {
            if (red.existeNodo(oficina.getId())) {
                throw new IllegalArgumentException("La oficina " + oficina + " ya existe en la red");
            }
            if (!planificacion.agregarVertice(oficina)) {
                throw new IllegalArgumentException("La oficina " + oficina + " está repetida");
            }
        }

        // Enlaces instalados: costo 0. Enlace no redefine equals, así que el conjunto compara identidad.
        Set<Enlace> instalados = new HashSet<>();
        for (Enlace enlace : red.getEnlaces()) {
            planificacion.agregarArista(enlace.getA(), enlace.getB(), new PesoCosto(enlace, 0));
            instalados.add(enlace);
        }

        // Ordenamos por costo: al repetirse un par, agregarArista descarta el segundo, así que gana el más barato
        List<Enlace> ordenados = new ArrayList<>(posibles);
        ordenados.sort(Comparator.comparingDouble(Enlace::getCosto));
        for (Enlace posible : ordenados) {
            if (!planificacion.vertices().contains(posible.getA())
                    || !planificacion.vertices().contains(posible.getB())) {
                throw new IllegalArgumentException("Conexión posible " + posible
                        + ": un extremo no es nodo de la red ni oficina nueva");
            }
            planificacion.agregarArista(posible.getA(), posible.getB(), new PesoCosto(posible, posible.getCosto()));
        }

        IUndirectedGraph<Nodo, PesoCosto> arbol = new UndirectedGraphAlgorithms().kruskal(planificacion);

        // Componentes del bosque: ¿qué nodos llegan a algún nodo de la red?
        UnionFind<Nodo> componentes = new UnionFind<>(planificacion.vertices());
        for (Edge<Nodo, PesoCosto> arista : arbol.aristas()) {
            componentes.union(arista.source(), arista.target());
        }
        Set<Nodo> raicesDeRed = new HashSet<>();
        for (Nodo nodo : red.getNodos()) {
            raicesDeRed.add(componentes.find(nodo));
        }

        List<Enlace> aInstalar = new ArrayList<>();
        double costoTotal = 0;
        for (Edge<Nodo, PesoCosto> arista : arbol.aristas()) {
            Enlace enlace = arista.dato().getEnlace();
            boolean yaExiste = instalados.contains(enlace);
            boolean llegaALaRed = raicesDeRed.contains(componentes.find(arista.source()));
            if (!yaExiste && llegaALaRed) {
                aInstalar.add(enlace);
                costoTotal += enlace.getCosto();
            }
        }

        Set<Nodo> noConectables = new HashSet<>();
        for (Nodo oficina : oficinasNuevas) {
            if (!raicesDeRed.contains(componentes.find(oficina))) {
                noConectables.add(oficina);
            }
        }

        return new ResultadoCableado(aInstalar, costoTotal, noConectables);
    }
}