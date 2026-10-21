package ucu.edu.aed.tda.grafo.Impl;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.model.edge.DirectedEdge;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;

/**
 * Grafo dirigido implementado con listas de adyacencia.
 * <p>
 * Se mantienen dos estructuras sincronizadas:
 * <ul>
 *     <li>{@code salientes}: para cada vértice, sus aristas salientes indexadas por vértice destino.</li>
 *     <li>{@code entrantes}: para cada vértice, el conjunto de vértices que tienen una arista hacia él.</li>
 * </ul>
 * Se usan {@link LinkedHashMap}/{@link LinkedHashSet} para que el orden de iteración
 * (y por lo tanto el de los recorridos) sea el orden de inserción, y sea reproducible.
 * <p>
 * No se admiten vértices {@code null} ni aristas paralelas (a lo sumo una arista u -> v).
 */
public class DirectedGraph<V, D> implements IDirectedIGraph<V, D> {
    // vértice origen -> (vértice destino -> arista)
    private final Map<V, Map<V, DirectedEdge<V, D>>> salientes;
    // vértice destino -> vértices origen de sus aristas entrantes
    private final Map<V, Set<V>> entrantes;

    public DirectedGraph() {
        this.salientes = new LinkedHashMap<>();
        this.entrantes = new LinkedHashMap<>();
    }

    /**
     * @throws NullPointerException si el vértice es null
     */
    @Override
    public boolean agregarVertice(V vertex) {
        Objects.requireNonNull(vertex, "El vértice no puede ser null");
        if (salientes.containsKey(vertex)) {
            return false;
        }
        salientes.put(vertex, new LinkedHashMap<>());
        entrantes.put(vertex, new LinkedHashSet<>());
        return true;
    }

    @Override
    public V buscarVertice(Comparable<V> criteria) {
        if (criteria == null) return null;
        for (V vertex : salientes.keySet()) {
            if (criteria.compareTo(vertex) == 0) {
                return vertex;
            }
        }
        return null;
    }

    /**
     * Agrega la arista source -> target.
     * Si ya existe una arista entre esos vértices retorna false y la arista original
     * (con su dato) se mantiene; para cambiar el dato hay que eliminarla y volver a agregarla.
     */
    @Override
    public boolean agregarArista(V source, V target, D dato) {
        if (source == null || target == null
                || !salientes.containsKey(source) || !salientes.containsKey(target)) {
            return false; // Uno de los vértices no existe
        }
        Map<V, DirectedEdge<V, D>> aristasDeSource = salientes.get(source);
        if (aristasDeSource.containsKey(target)) {
            return false; // Ya existe la arista
        }
        aristasDeSource.put(target, new DirectedEdge<>(source, target, dato));
        entrantes.get(target).add(source);
        return true;
    }

    @Override
    public boolean eliminarArista(Comparable<V> source, Comparable<V> target) {
        V sourceVertex = buscarVertice(source);
        V targetVertex = buscarVertice(target);
        if (sourceVertex == null || targetVertex == null) {
            return false;
        }
        if (salientes.get(sourceVertex).remove(targetVertex) == null) {
            return false; // No existía la arista
        }
        entrantes.get(targetVertex).remove(sourceVertex);
        return true;
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) {
            return false;
        }

        // Elimina las aristas entrantes: cada predecesor deja de apuntar al vértice
        for (V predecesor : entrantes.get(vertex)) {
            salientes.get(predecesor).remove(vertex);
        }
        // Elimina las aristas salientes: cada sucesor deja de tenerlo como predecesor
        for (V sucesor : salientes.get(vertex).keySet()) {
            entrantes.get(sucesor).remove(vertex);
        }

        salientes.remove(vertex);
        entrantes.remove(vertex);
        return true;
    }

    /**
     * Vista inmodificable de los vértices: modificarla lanza UnsupportedOperationException.
     */
    @Override
    public Set<V> vertices() {
        return Collections.unmodifiableSet(salientes.keySet());
    }

    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> edges = new LinkedHashSet<>();
        for (Map<V, DirectedEdge<V, D>> aristasDelVertice : salientes.values()) {
            edges.addAll(aristasDelVertice.values());
        }
        return Collections.unmodifiableSet(edges);
    }

    @Override
    public int cantidadDeAristas() {
        int cantidad = 0;
        for (Map<V, DirectedEdge<V, D>> aristasDelVertice : salientes.values()) {
            cantidad += aristasDelVertice.size();
        }
        return cantidad;
    }

    @Override
    public boolean existeArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        return obtenerArista(sourceCriteria, targetCriteria) != null;
    }

    @Override
    public Edge<V, D> obtenerArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        V source = buscarVertice(sourceCriteria);
        V target = buscarVertice(targetCriteria);
        if (source == null || target == null) return null;
        return salientes.get(source).get(target);
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        V vertex = buscarVertice(verticeCriteria);
        if (vertex == null) return List.of();
        return new ArrayList<>(salientes.get(vertex).values());
    }

    @Override
    public Set<V> successors(Comparable<V> criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return Set.of();
        return Collections.unmodifiableSet(new LinkedHashSet<>(salientes.get(vertex).keySet()));
    }

    @Override
    public Set<V> predecessors(Comparable<V> criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return Set.of();
        return Collections.unmodifiableSet(new LinkedHashSet<>(entrantes.get(vertex)));
    }

    /**
     * Un grafo dirigido es (fuertemente) conexo si desde cualquier vértice se puede
     * llegar a cualquier otro. Alcanza con verificar que desde un vértice cualquiera
     * se llega a todos, y que todos llegan a él (recorriendo el grafo transpuesto).
     * El grafo vacío se considera conexo.
     */
    @Override
    public boolean esConexo() {
        if (salientes.isEmpty()) return true;

        V start = salientes.keySet().iterator().next();
        return alcanzables(start, false).size() == salientes.size()
                && alcanzables(start, true).size() == salientes.size();
    }

    /**
     * DFS iterativo (evita StackOverflowError en grafos grandes).
     *
     * @param transpuesto si es true recorre las aristas en sentido inverso
     * @return los vértices alcanzables desde start, incluido start
     */
    private Set<V> alcanzables(V start, boolean transpuesto) {
        Set<V> visited = new HashSet<>();
        Deque<V> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            V vertex = stack.pop();
            if (!visited.add(vertex)) continue;
            Collection<V> vecinos = transpuesto ? entrantes.get(vertex) : salientes.get(vertex).keySet();
            for (V vecino : vecinos) {
                if (!visited.contains(vecino)) {
                    stack.push(vecino);
                }
            }
        }
        return visited;
    }

    @Override
    public void vaciar() {
        salientes.clear();
        entrantes.clear();
    }

    /**
     * Detecta ciclos con un DFS iterativo de tres estados: un vértice "en proceso" que
     * se vuelve a encontrar indica una arista hacia atrás, es decir, un ciclo.
     * Un lazo (u -> u) también cuenta como ciclo.
     */
    @Override
    public boolean tieneCiclos() {
        Set<V> terminados = new HashSet<>();
        Set<V> enProceso = new HashSet<>();

        for (V inicio : salientes.keySet()) {
            if (terminados.contains(inicio)) continue;

            // Cada entrada de la pila guarda el vértice y el iterador de sus sucesores pendientes
            Deque<Map.Entry<V, Iterator<V>>> stack = new ArrayDeque<>();
            enProceso.add(inicio);
            stack.push(Map.entry(inicio, salientes.get(inicio).keySet().iterator()));

            while (!stack.isEmpty()) {
                Map.Entry<V, Iterator<V>> tope = stack.peek();
                Iterator<V> pendientes = tope.getValue();
                if (pendientes.hasNext()) {
                    V destino = pendientes.next();
                    if (enProceso.contains(destino)) {
                        return true; // Encontramos un ciclo
                    }
                    if (!terminados.contains(destino)) {
                        enProceso.add(destino);
                        stack.push(Map.entry(destino, salientes.get(destino).keySet().iterator()));
                    }
                } else {
                    stack.pop();
                    enProceso.remove(tope.getKey());
                    terminados.add(tope.getKey());
                }
            }
        }
        return false;
    }
}
