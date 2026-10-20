package ucu.edu.aed.tda.grafo.Impl;

import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.UndirectedEdge;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;

public class UndirectedGraph<V, D> implements IUndirectedGraph<V, D> {
    private final Map<V, Set<UndirectedEdge<V, D>>> adjacencyList;

    public UndirectedGraph() {
        this.adjacencyList = new HashMap<>();
    }

    @Override
    public boolean agregarVertice(V vertex) {
        if (!adjacencyList.containsKey(vertex)) {
            adjacencyList.put(vertex, new HashSet<>());
            return true;
        }
        return false;
    }

    @Override
    public V buscarVertice(Comparable criterio) {
        for (V vertex : adjacencyList.keySet()) {
            if (criterio.compareTo(vertex) == 0) {
                return vertex;
            }
        }
        return null;
    }

    @Override
    public boolean agregarArista(V source, V target, D dato) {
        if (!adjacencyList.containsKey(source) || !adjacencyList.containsKey(target)) {
            return false; // Uno o ambos vértices no existen
        }
        UndirectedEdge<V, D> edge = new UndirectedEdge<>(source, target, dato);

        // Añadimos la arista en ambos conjuntos de adyacencia
        boolean addedToSource = adjacencyList.get(source).add(edge);
        boolean addedToTarget = adjacencyList.get(target).add(edge);

        // Retornamos "true" si la arista fue agregada correctamente en ambos lados
        return addedToSource && addedToTarget;
    }

    @Override
    public boolean eliminarArista(Comparable source, Comparable target) {
        V sourceVertex = buscarVertice(source);
        V targetVertex = buscarVertice(target);

        if (sourceVertex == null || targetVertex == null) return false;

        // Eliminamos la arista de ambos conjuntos de adyacencia
        UndirectedEdge<V, D> edge = new UndirectedEdge<>(sourceVertex, targetVertex, null);
        boolean removedFromSource = adjacencyList.get(sourceVertex).remove(edge);
        boolean removedFromTarget = adjacencyList.get(targetVertex).remove(edge);

        return removedFromSource && removedFromTarget;
    }

    @Override
    public boolean removerVertice(Comparable criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return false;

        // Eliminamos todas las aristas asociadas al vértice
        Set<UndirectedEdge<V, D>> edges = adjacencyList.get(vertex);
        for (UndirectedEdge<V, D> edge : edges) {
            V neighbor = edge.source().equals(vertex) ? edge.target() : edge.source();
            adjacencyList.get(neighbor).remove(edge); // Eliminar la arista inversa
        }

        // Finalmente, eliminamos el vértice del grafo
        adjacencyList.remove(vertex);
        return true;
    }

    @Override
    public Set<V> vertices() {
        return adjacencyList.keySet();
    }

    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> edges = new HashSet<>();
        for (Set<UndirectedEdge<V, D>> edgeSet : adjacencyList.values()) {
            edges.addAll(edgeSet); // Añadir todas las aristas (se maneja duplicación en HashSet)
        }
        return edges;
    }

    @Override
    public boolean existeArista(Comparable sourceCriteria, Comparable targetCriteria) {
        V source = buscarVertice(sourceCriteria);
        V target = buscarVertice(targetCriteria);

        if (source == null || target == null) return false;

        // Validamos si existe la arista en las adyacencias del nodo source
        for (UndirectedEdge<V, D> edge : adjacencyList.get(source)) {
            if (edge.target().equals(target) || edge.source().equals(target)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Edge<V, D> obtenerArista(Comparable sourceCriteria, Comparable targetCriteria) {
        V source = buscarVertice(sourceCriteria);
        V target = buscarVertice(targetCriteria);

        if (source == null || target == null) return null;

        // Buscamos la arista en las adyacencias del nodo source
        for (UndirectedEdge<V, D> edge : adjacencyList.get(source)) {
            if (edge.target().equals(target) || edge.source().equals(target)) {
                return edge;
            }
        }
        return null;
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable verticeCriteria) {
        V vertex = buscarVertice(verticeCriteria);
        if (vertex == null) return List.of();

        return new ArrayList<>(adjacencyList.get(vertex));
    }

    @Override
    public boolean esConexo() {
        if (adjacencyList.isEmpty()) return true;

        // Usar DFS desde un nodo arbitrario y verificar si todos los nodos son alcanzables
        V start = adjacencyList.keySet().iterator().next();
        Set<V> visited = new HashSet<>();
        dfs(start, visited);

        return visited.size() == adjacencyList.size();
    }

    private void dfs(V vertex, Set<V> visited) {
        visited.add(vertex);
        for (UndirectedEdge<V, D> edge : adjacencyList.get(vertex)) {
            V neighbor = edge.source().equals(vertex) ? edge.target() : edge.source();
            if (!visited.contains(neighbor)) {
                dfs(neighbor, visited);
            }
        }
    }

    @Override
    public void vaciar() {
        adjacencyList.clear();
    }

    @Override
    public boolean tieneCiclos() {
        Set<V> visited = new HashSet<>();
        return tieneCiclosDFS(null, adjacencyList.keySet().iterator().next(), visited);
    }

    private boolean tieneCiclosDFS(V parent, V current, Set<V> visited) {
        visited.add(current);

        for (UndirectedEdge<V, D> edge : adjacencyList.get(current)) {
            V neighbor = edge.source().equals(current) ? edge.target() : edge.source();

            if (!visited.contains(neighbor)) {
                if (tieneCiclosDFS(current, neighbor, visited)) {
                    return true;
                }
            } else if (!neighbor.equals(parent)) {
                // Si encontramos un vecino que no es el padre, hay un ciclo
                return true;
            }
        }

        return false;
    }
}