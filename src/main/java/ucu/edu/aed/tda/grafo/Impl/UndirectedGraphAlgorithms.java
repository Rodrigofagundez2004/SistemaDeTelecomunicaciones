package ucu.edu.aed.tda.grafo.Impl;
import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.IUndirectedGraphAlgorithm;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

public class UndirectedGraphAlgorithms implements IUndirectedGraphAlgorithm {

    private final DirectedGraphAlgorithms recorridos = new DirectedGraphAlgorithms();

    @Override
    public <V, D> void bep(IUndirectedGraph<V, D> grafo, Consumer<V> consumer) {
        recorridos.recorridoEnProfundidad(grafo, consumer);
    }

    @Override
    public <V, D> void bea(IUndirectedGraph<V, D> grafo, Consumer<V> consumer) {
        recorridos.recorridoEnAmplitud(grafo, consumer);
    }
    
    @Override
    public <V, D extends WeightedEdge> IUndirectedGraph<V, D> kruskal(IUndirectedGraph<V, D> graph) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> IUndirectedGraph<V, D> prim(IUndirectedGraph<V, D> graph, Comparable<V> source) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> Edge<V, D> searchMinEdge(IUndirectedGraph<V, D> graph, Collection<V> U, Collection<V> V) {
        throw new UnsupportedOperationException("COMPLETAR");
    }

    @Override
    public <V, D extends WeightedEdge> Set<V> puntosDeArticulacion(IUndirectedGraph<V, D> graph) {
        throw new UnsupportedOperationException("COMPLETAR");
    }    

}
