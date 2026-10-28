package ucu.edu.aed.tda.grafo;


import java.util.Collection;
import java.util.function.Consumer;

import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public interface IUndirectedGraphAlgorithm {
    /**
     * Implementa el algoritmo Kruskal
     */
    <V, D extends WeightedEdge> IUndirectedGraph<V, D> kruskal(IUndirectedGraph<V, D> graph);

    /**
     * ejecuta el algoritmo Prim sobre el grafo. Si el grafo está vacío devuelve un grafo vacío.
     */
    <V, D extends WeightedEdge> IUndirectedGraph<V, D> prim(IUndirectedGraph<V, D> graph, Comparable<V> source);

    /**
     * Ejecuta prim sin un vértice origen
     */
    default <V, D extends WeightedEdge> IUndirectedGraph<V, D> prim(IUndirectedGraph<V, D> graph) {

        for (V vertex : graph.vertices()) {
            return prim(graph, graph.construirComparable(vertex));
        }

        return new UndirectedGraph<>();
    }


    /**
     * Retorna la mínima arista (u,v) del grafo "graph", tal que u está en U, y v está en V.
     * Este método es útil para implementar "Prim"
     */
    <V, D extends WeightedEdge> Edge<V, D> searchMinEdge(IUndirectedGraph<V, D> graph, Collection<V> U, Collection<V> V);

    /**
     * Implementa el algoritmo de búsqueda en amplitud
     */
    <V, D> void bea(IUndirectedGraph<V, D> graph, Consumer<V> consumer);

    /**
     * Implementa el algoritmo de búsqueda en profundidad.
     * Visita todos los vértices del grafo (recorriendo todas las componentes
     * si el grafo no es conexo) y aplica el consumer sobre cada uno.
     */
    <V, D> void bep(IUndirectedGraph<V, D> graph, Consumer<V> consumer);

    /**
     * Retorna los puntos de articulación del grafo no dirigido.
     * Un punto de articulación es un vértice tal que, al eliminarlo junto con
     * sus aristas incidentes, aumenta la cantidad de componentes conexas.
     */
    <V, D extends WeightedEdge> java.util.Set<V> puntosDeArticulacion(IUndirectedGraph<V, D> graph);
}