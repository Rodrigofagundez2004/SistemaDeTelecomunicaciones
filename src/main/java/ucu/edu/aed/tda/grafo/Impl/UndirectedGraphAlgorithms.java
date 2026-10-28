package ucu.edu.aed.tda.grafo.Impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        IUndirectedGraph<V, D> arbol = crearGrafoVacio();
        arbol.agregarVertices(graph.vertices());

        List<Edge<V, D>> aristas = new ArrayList<>(graph.aristas());
        aristas.sort(Comparator.comparingDouble((Edge<V, D> e) -> e.dato().getWeight()));

        UnionFind<V> uf = new UnionFind<>(graph.vertices());
        for (Edge<V, D> e : aristas) {
            if (uf.union(e.source(), e.target())) {
                arbol.agregarArista(e.source(), e.target(), e.dato());
            }
        }
        return arbol;
    }

    @Override
    public <V, D extends WeightedEdge> IUndirectedGraph<V, D> prim(
            IUndirectedGraph<V, D> graph, Comparable<V> source) {

        if (graph.vertices().isEmpty()) {
            return crearGrafoVacio();
        }

        V origen = graph.buscarVertice(source);
        if (origen == null) {
            throw new IllegalArgumentException("El vértice origen no existe en el grafo");
        }

        IUndirectedGraph<V, D> arbol = crearGrafoVacio();
        arbol.agregarVertice(origen);

        Set<V> conectados = new HashSet<>();
        Set<V> pendientes = new HashSet<>(graph.vertices());
        conectados.add(origen);
        pendientes.remove(origen);

        while (!pendientes.isEmpty()) {
            Edge<V, D> e = searchMinEdge(graph, conectados, pendientes);
            if (e == null) {
                break;
            }
            V nuevo = pendientes.contains(e.target()) ? e.target() : e.source();
            arbol.agregarVertice(nuevo);
            arbol.agregarArista(e.source(), e.target(), e.dato());
            conectados.add(nuevo);
            pendientes.remove(nuevo);
        }
        return arbol;
    }

    @Override
    public <V, D extends WeightedEdge> Edge<V, D> searchMinEdge(IUndirectedGraph<V, D> graph,
            Collection<V> U, Collection<V> pendientes) {
        Edge<V, D> minima = null;
        for (V u : U) {
            for (Edge<V, D> e : graph.adyacencias(graph.construirComparable(u))) {
                V w = e.source().equals(u) ? e.target() : e.source();
                if (pendientes.contains(w)
                        && (minima == null || e.dato().getWeight() < minima.dato().getWeight())) {
                    minima = e;
                }
            }
        }
        return minima;
    }

    @Override
    public <V, D extends WeightedEdge> Set<V> puntosDeArticulacion(IUndirectedGraph<V, D> graph) {
        Set<V> resultado = new HashSet<>();
        Map<V, Integer> num = new HashMap<>();
        Map<V, Integer> bajo = new HashMap<>();
        int[] contador = {0};
        for (V v : graph.vertices()) {
            if (!num.containsKey(v)) {
                articulacionAux(graph, v, null, num, bajo, contador, resultado);
            }
        }
        return resultado;
    }

    private <V, D extends WeightedEdge> void articulacionAux(IUndirectedGraph<V, D> g, V v, V padre,
            Map<V, Integer> num, Map<V, Integer> bajo, int[] contador, Set<V> resultado) {
        num.put(v, ++contador[0]);
        bajo.put(v, num.get(v));
        int hijos = 0;
        for (Edge<V, D> e : g.adyacencias(g.construirComparable(v))) {
            V w = e.source().equals(v) ? e.target() : e.source();
            if (!num.containsKey(w)) {                      
                hijos++;
                articulacionAux(g, w, v, num, bajo, contador, resultado);
                bajo.put(v, Math.min(bajo.get(v), bajo.get(w)));
                if (padre != null && bajo.get(w) >= num.get(v)) {
                    resultado.add(v);
                }
            } else if (!w.equals(padre)) {                
                bajo.put(v, Math.min(bajo.get(v), num.get(w)));
            }
        }
        if (padre == null && hijos >= 2) {
            resultado.add(v);
        }
    }

    public <V, D extends WeightedEdge> Set<Edge<V, D>> puentes(IUndirectedGraph<V, D> graph) {
        Set<Edge<V, D>> resultado = new HashSet<>();
        Map<V, Integer> num = new HashMap<>();
        Map<V, Integer> bajo = new HashMap<>();
        int[] contador = {0};
        for (V v : graph.vertices()) {
            if (!num.containsKey(v)) {
                puentesAux(graph, v, null, num, bajo, contador, resultado);
            }
        }
        return resultado;
    }

    private <V, D extends WeightedEdge> void puentesAux(IUndirectedGraph<V, D> g, V v, V padre,
            Map<V, Integer> num, Map<V, Integer> bajo, int[] contador, Set<Edge<V, D>> resultado) {
        num.put(v, ++contador[0]);
        bajo.put(v, num.get(v));
        for (Edge<V, D> e : g.adyacencias(g.construirComparable(v))) {
            V w = e.source().equals(v) ? e.target() : e.source();
            if (!num.containsKey(w)) {                    
                puentesAux(g, w, v, num, bajo, contador, resultado);
                bajo.put(v, Math.min(bajo.get(v), bajo.get(w)));
                if (bajo.get(w) > num.get(v)) {              
                    resultado.add(e);
                }
            } else if (!w.equals(padre)) {                  
                bajo.put(v, Math.min(bajo.get(v), num.get(w)));
            }
        }
    }

    private <V, D> IUndirectedGraph<V, D> crearGrafoVacio() {
        return new UndirectedGraph<>();
    }
}