package ucu.edu.aed.telecom.service;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Enlace;

public class ResilienciaService {

    private final UndirectedGraphAlgorithms algoritmos =
            new UndirectedGraphAlgorithms();

    public Set<Nodo> equiposCriticos(
            IUndirectedGraph<Nodo, Enlace> red) {

        return algoritmos.puntosDeArticulacion(red);
    }

    public Set<Edge<Nodo, Enlace>> enlacesCriticos(
            IUndirectedGraph<Nodo, Enlace> red) {

        return algoritmos.puentes(red);
    }

    public Set<Nodo> incomunicadosSiFallaNodo(
            IUndirectedGraph<Nodo, Enlace> red, Nodo nodo) {

        verificarExiste(red, nodo);

        return incomunicados(red, nodo, null, null);
    }

    public Set<Nodo> incomunicadosSiFallaEnlace(
            IUndirectedGraph<Nodo, Enlace> red, Nodo a, Nodo b) {

        verificarExiste(red, a);
        verificarExiste(red, b);

        boolean existe = false;

        for (Edge<Nodo, Enlace> e :
                red.adyacencias(red.construirComparable(a))) {

            if (e.source().equals(b) || e.target().equals(b)) {
                existe = true;
                break;
            }
        }

        if (!existe) {
            throw new IllegalArgumentException(
                    "No existe el enlace entre esos equipos");
        }

        return incomunicados(red, null, a, b);
    }

    public List<Nodo> rutaAlternativa(
            IUndirectedGraph<Nodo, Enlace> red,
            Nodo origen,
            Nodo destino,
            List<Nodo> rutaPrincipal) {

        verificarExiste(red, origen);
        verificarExiste(red, destino);

        if (origen.equals(destino)) {
            List<Nodo> camino = new ArrayList<>();
            camino.add(origen);
            return camino;
        }

        Set<Nodo> nodosProhibidos = new HashSet<>();

        if (rutaPrincipal != null) {
            for (int i = 1; i < rutaPrincipal.size() - 1; i++) {
                nodosProhibidos.add(rutaPrincipal.get(i));
            }
        }

        Map<Nodo, Double> costos = new HashMap<>();
        Map<Nodo, Nodo> predecesores = new HashMap<>();

        costos.put(origen, 0.0);

        PriorityQueue<Map.Entry<Nodo, Double>> cola =
                new PriorityQueue<>(Map.Entry.comparingByValue());

        cola.add(new AbstractMap.SimpleEntry<>(origen, 0.0));

        Set<Nodo> procesados = new HashSet<>();

        while (!cola.isEmpty()) {
            Nodo u = cola.poll().getKey();

            if (procesados.contains(u)) {
                continue;
            }

            procesados.add(u);

            if (u.equals(destino)) {
                break;
            }

            for (Edge<Nodo, Enlace> e :
                    red.adyacencias(red.construirComparable(u))) {

                Nodo w;

                if (e.source().equals(u)) {
                    w = e.target();
                } else {
                    w = e.source();
                }

                if (nodosProhibidos.contains(w)) {
                    continue;
                }

                if (enRuta(rutaPrincipal, u, w)) {
                    continue;
                }

                double nuevoCosto =
                        costos.get(u) + e.dato().getWeight();

                double costoAnterior =
                        costos.getOrDefault(
                                w, Double.POSITIVE_INFINITY);

                if (nuevoCosto < costoAnterior) {
                    costos.put(w, nuevoCosto);
                    predecesores.put(w, u);

                    cola.add(
                            new AbstractMap.SimpleEntry<>(w, nuevoCosto));
                }
            }
        }

        if (!costos.containsKey(destino)) {
            return new ArrayList<>();
        }

        LinkedList<Nodo> camino = new LinkedList<>();
        Nodo actual = destino;

        while (actual != null) {
            camino.addFirst(actual);
            actual = predecesores.get(actual);
        }

        return camino;
    }

    private boolean enRuta(
            List<Nodo> ruta, Nodo u, Nodo w) {

        if (ruta == null) {
            return false;
        }

        for (int i = 0; i + 1 < ruta.size(); i++) {
            Nodo a = ruta.get(i);
            Nodo b = ruta.get(i + 1);

            if (a.equals(u) && b.equals(w)) {
                return true;
            }

            if (a.equals(w) && b.equals(u)) {
                return true;
            }
        }

        return false;
    }

    private void verificarExiste(
            IUndirectedGraph<Nodo, Enlace> red, Nodo n) {

        if (red.buscarVertice(red.construirComparable(n)) == null) {
            throw new IllegalArgumentException(
                    "El equipo no existe en la red");
        }
    }

    private Set<Nodo> incomunicados(
            IUndirectedGraph<Nodo, Enlace> red,
            Nodo nodoCaido,
            Nodo extremoA,
            Nodo extremoB) {

        Set<Nodo> visitados = new HashSet<>();
        List<Nodo> mayor = new ArrayList<>();
        List<List<Nodo>> otras = new ArrayList<>();

        for (Nodo inicio : red.vertices()) {

            if (nodoCaido != null && inicio.equals(nodoCaido)) {
                continue;
            }

            if (visitados.contains(inicio)) {
                continue;
            }

            List<Nodo> componente = new ArrayList<>();
            Deque<Nodo> pila = new ArrayDeque<>();

            pila.push(inicio);
            visitados.add(inicio);

            while (!pila.isEmpty()) {
                Nodo u = pila.pop();
                componente.add(u);

                for (Edge<Nodo, Enlace> e :
                        red.adyacencias(red.construirComparable(u))) {

                    Nodo w;

                    if (e.source().equals(u)) {
                        w = e.target();
                    } else {
                        w = e.source();
                    }

                    if (nodoCaido != null && w.equals(nodoCaido)) {
                        continue;
                    }

                    if (visitados.contains(w)) {
                        continue;
                    }

                    boolean enlaceCaido = false;

                    if (extremoA != null && extremoB != null) {
                        if (u.equals(extremoA) && w.equals(extremoB)) {
                            enlaceCaido = true;
                        }

                        if (u.equals(extremoB) && w.equals(extremoA)) {
                            enlaceCaido = true;
                        }
                    }

                    if (enlaceCaido) {
                        continue;
                    }

                    visitados.add(w);
                    pila.push(w);
                }
            }

            if (componente.size() > mayor.size()) {
                if (!mayor.isEmpty()) {
                    otras.add(mayor);
                }

                mayor = componente;
            } else {
                otras.add(componente);
            }
        }

        Set<Nodo> resultado = new HashSet<>();

        for (List<Nodo> componente : otras) {
            resultado.addAll(componente);
        }

        return resultado;
    }
}
