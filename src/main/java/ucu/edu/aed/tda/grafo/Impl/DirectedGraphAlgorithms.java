package ucu.edu.aed.tda.grafo.Impl;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

import ucu.edu.aed.tda.grafo.IDirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.model.IGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.tda.grafo.model.result.Path;


public class DirectedGraphAlgorithms implements IDirectedGraphAlgorithms {

    @Override
    public <V, D> void recorridoEnProfundidad(IGraph<V, D> grafo, Comparable<V> origen, Consumer<V> consumer) {
        V v = grafo.buscarVertice(origen);
        if (v == null)
            return;
        bpf(grafo, v, new HashSet<>(), consumer);
    }

    @Override
    public <V, D> void recorridoEnProfundidad(IGraph<V, D> grafo, Consumer<V> consumer) {
        Set<V> visitados = new HashSet<>();
        for (V v : grafo.vertices()) {
            if (!visitados.contains(v))
                bpf(grafo, v, visitados, consumer);
        }
    }

    @Override 
    public <V, D> void recorridoEnAmplitud(IGraph<V, D> grafo, Comparable<V> origen, Consumer<V> consumer) {
        V v = grafo.buscarVertice(origen);
        if (v == null)
            return;
        bea(grafo, v, new HashSet<>(), consumer);
    }

    @Override
    public <V, D> void recorridoEnAmplitud(IGraph<V, D> grafo, Consumer<V> consumer) {
        Set<V> visitados = new HashSet<>();
        for (V v : grafo.vertices()) {
            if (!visitados.contains(v))
                bea(grafo, v, visitados, consumer);
        }
    }

    @Override
    public <V, D> List<V> calcularClasificacionTopologica(IDirectedIGraph<V, D> grafo) {
        if (grafo.tieneCiclos())
            throw new IllegalStateException("El grafo tiene ciclos, no admite clasificacion topologica");
        Set<V> visitados = new HashSet<>();
        LinkedList<V> lista = new LinkedList<>();
        for (V v : grafo.vertices()) {
            if (!visitados.contains(v))
                clasificacionTopologicaAux(grafo, v, visitados, lista);
        }
        return lista;
    }

    @Override
    public <V, D extends WeightedEdge> List<Path<V>> obtenerTodosLosCaminos(Comparable<V> source, Comparable<V> target, IGraph<V, D> grafo) {
        List<Path<V>> caminos = new ArrayList<>();
        V origen = grafo.buscarVertice(source);
        V destino = grafo.buscarVertice(target);
        if (origen == null || destino == null)
            return caminos;
        todosLosCaminosAux(grafo, origen, destino, new HashSet<>(), new LinkedList<>(), 0, caminos);
        return caminos;
    }

    @Override
    public <V, D extends WeightedEdge> IDijkstraResult<V> dijkstra(Comparable<V> source, IDirectedIGraph<V, D> grafo) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> IFloydWarshallResult<V> floyd(IDirectedIGraph<V, D> grafo) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> IFloydWarshallResult<V> warshall(IDirectedIGraph<V, D> grafo) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> V obtenerCentroGrafo(IDirectedIGraph<V, D> grafo) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> double obtenerExcentricidad(IDirectedIGraph<V, D> grafo, Comparable<V> vertexCriteria) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    private <V, D> void bpf(IGraph<V, D> grafo, V v, Set<V> visitados, Consumer<V> consumer) {
        visitados.add(v);
        consumer.accept(v);
        for (Edge<V, D> e : grafo.adyacencias(grafo.construirComparable(v))) {
            V w = vecino(e, v);
            if (!visitados.contains(w))
                bpf(grafo, w, visitados, consumer);
        }
    }

    private <V, D> V vecino(Edge<V, D> e, V actual) {
        return e.source().equals(actual) ? e.target() : e.source();
    }

    private <V, D> void bea(IGraph<V, D> grafo, V origen, Set<V> visitados, Consumer<V> consumer) {
        Queue<V> cola = new ArrayDeque<>();
        cola.add(origen);
        visitados.add(origen);
        while (!cola.isEmpty()) {
            V x = cola.poll();
            consumer.accept(x);
            for (Edge<V, D> e : grafo.adyacencias(grafo.construirComparable(x))) {
                V y = vecino(e, x);
                if (!visitados.contains(y)) {
                    visitados.add(y);
                    cola.add(y);
                }
            }
        }
    }

    private <V, D> void clasificacionTopologicaAux(IDirectedIGraph<V, D> grafo, V v, Set<V> visitados, LinkedList<V> lista) {
        visitados.add(v);
        for (Edge<V, D> e : grafo.adyacencias(grafo.construirComparable(v))) {
            V w = vecino(e, v);
            if (!visitados.contains(w))
                clasificacionTopologicaAux(grafo, w, visitados, lista);
        }
        lista.addFirst(v);
    }
    
    private <V, D extends WeightedEdge> void todosLosCaminosAux(IGraph<V, D> grafo, V nodo, V destino, Set<V> enCamino, LinkedList<V> camino, double costo, List<Path<V>> caminos) {
        enCamino.add(nodo);
        camino.addLast(nodo);
        if (nodo.equals(destino)) {
            caminos.add(new Path<>(new ArrayList<>(camino), costo));
        } else {
            for (Edge<V, D> e : grafo.adyacencias(grafo.construirComparable(nodo))) {
                V w = vecino(e, nodo);
                if (!enCamino.contains(w))
                    todosLosCaminosAux(grafo, w, destino, enCamino, camino, costo + e.dato().getWeight(), caminos);
            }
        }
        enCamino.remove(nodo);
        camino.removeLast();
    }
}
