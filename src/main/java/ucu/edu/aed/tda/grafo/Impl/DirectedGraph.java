package ucu.edu.aed.tda.grafo.Impl;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.model.edge.DirectedEdge;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;

public class DirectedGraph<V, D> implements IDirectedIGraph<V, D> {
    private final Map<V, Set<DirectedEdge<V, D>>> adjacencyList; // Lista de adyacencia

    public DirectedGraph() {
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
    public V buscarVertice(Comparable criteria) {
        for (V vertex : adjacencyList.keySet()) {
            if (criteria.compareTo(vertex) == 0) {
                return vertex;
            }
        }
        return null;
    }

    @Override
    public boolean agregarArista(V source, V target, D dato) {
        if (!adjacencyList.containsKey(source) || !adjacencyList.containsKey(target)) {
            return false; // Uno de los vértices no existe
        }
        DirectedEdge<V, D> edge = new DirectedEdge<>(source, target, dato);
        return adjacencyList.get(source).add(edge); // Agrega la arista dirigida
    }

    @Override
    public boolean eliminarArista(Comparable source, Comparable target) {
        V sourceVertex = buscarVertice(source);
        V targetVertex = buscarVertice(target);

        if (sourceVertex == null || targetVertex == null) {
            return false;
        }
        return adjacencyList.get(sourceVertex).removeIf(e -> e.target().equals(targetVertex));
    }

    @Override
    public boolean removerVertice(Comparable criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) {
            return false;
        }

        // Elimina todas las aristas entrantes
        for (Set<DirectedEdge<V, D>> edges : adjacencyList.values()) {
            edges.removeIf(edge -> edge.target().equals(vertex));
        }

        // Elimina el vértice y las aristas salientes
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
        for (Set<DirectedEdge<V, D>> edgeSet : adjacencyList.values()) {
            edges.addAll(edgeSet);
        }
        return edges;
    }

    @Override
    public boolean existeArista(Comparable sourceCriteria, Comparable targetCriteria) {
        V source = buscarVertice(sourceCriteria);
        V target = buscarVertice(targetCriteria);
        if (source == null || target == null) return false;

        for (DirectedEdge<V, D> edge : adjacencyList.get(source)) {
            if (edge.target().equals(target)) {
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

        for (DirectedEdge<V, D> edge : adjacencyList.get(source)) {
            if (edge.target().equals(target)) {
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
    public Set successors(Comparable criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return Set.of();

        Set<V> successors = new HashSet<>();
        for (DirectedEdge<V, D> edge : adjacencyList.get(vertex)) {
            successors.add(edge.target());
        }
        return successors;
    }

    @Override
    public Set predecessors(Comparable criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return Set.of();

        Set<V> predecessors = new HashSet<>();
        for (Map.Entry<V, Set<DirectedEdge<V, D>>> entry : adjacencyList.entrySet()) {
            for (DirectedEdge<V, D> edge : entry.getValue()) {
                if (edge.target().equals(vertex)) {
                    predecessors.add(entry.getKey());
                }
            }
        }
        return predecessors;
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
        for (DirectedEdge<V, D> edge : adjacencyList.get(vertex)) {
            if (!visited.contains(edge.target())) {
                dfs(edge.target(), visited);
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
        Set<V> recursionStack = new HashSet<>();

        for (V vertex : adjacencyList.keySet()) {
            if (!visited.contains(vertex)) {
                if (tieneCiclosDFS(vertex, visited, recursionStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tieneCiclosDFS(V vertex, Set<V> visited, Set<V> recursionStack) {
        visited.add(vertex);
        recursionStack.add(vertex);

        for (DirectedEdge<V, D> edge : adjacencyList.get(vertex)) {
            V destination = edge.target();
            if (!visited.contains(destination)) {
                if (tieneCiclosDFS(destination, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(destination)) {
                return true; // Encontramos un ciclo
            }
        }

        recursionStack.remove(vertex);
        return false;
    }
}