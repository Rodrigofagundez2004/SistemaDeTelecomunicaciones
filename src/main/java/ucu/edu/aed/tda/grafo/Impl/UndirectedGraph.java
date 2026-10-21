package ucu.edu.aed.tda.grafo.Impl;

import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.model.edge.UndirectedEdge;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;

/**
 * Grafo no dirigido implementado con listas de adyacencia.
 * <p>
 * Para cada vértice se guarda un mapa vecino -> arista. Una arista {u, v} aparece en el mapa
 * de u (con clave v) y en el de v (con clave u), y es el mismo objeto en ambos.
 * Un ciclo de un vértice (arista {u, u}) aparece una única vez, en el mapa de u con clave u.
 * <p>
 * Se usan {@link LinkedHashMap} para que el orden de iteración (y por lo tanto el de los
 * recorridos) sea el orden de inserción, y sea reproducible.
 * <p>
 * No se admiten vértices {@code null} ni aristas paralelas (a lo sumo una arista {u, v}).
 * Se admiten ciclos de un vértice (aristas {u, u}).
 */
public class UndirectedGraph<V, D> implements IUndirectedGraph<V, D> {
    // vértice -> (vecino -> arista)
    private final Map<V, Map<V, UndirectedEdge<V, D>>> adyacentes;

    public UndirectedGraph() {
        this.adyacentes = new LinkedHashMap<>();
    }

    /**
     * @throws NullPointerException si el vértice es null
     */
    @Override
    public boolean agregarVertice(V vertex) {
        Objects.requireNonNull(vertex, "El vértice no puede ser null");
        if (adyacentes.containsKey(vertex)) {
            return false;
        }
        adyacentes.put(vertex, new LinkedHashMap<>());
        return true;
    }

    @Override
    public V buscarVertice(Comparable<V> criterio) {
        if (criterio == null) return null;
        for (V vertex : adyacentes.keySet()) {
            if (criterio.compareTo(vertex) == 0) {
                return vertex;
            }
        }
        return null;
    }

    /**
     * Agrega la arista {source, target}.
     * Si ya existe una arista entre esos vértices (en cualquier orden) retorna false y la
     * arista original (con su dato) se mantiene; para cambiar el dato hay que eliminarla y
     * volver a agregarla.
     */
    @Override
    public boolean agregarArista(V source, V target, D dato) {
        if (source == null || target == null
                || !adyacentes.containsKey(source) || !adyacentes.containsKey(target)) {
            return false; // Uno o ambos vértices no existen
        }
        if (adyacentes.get(source).containsKey(target)) {
            return false; // Ya existe la arista
        }
        UndirectedEdge<V, D> edge = new UndirectedEdge<>(source, target, dato);
        adyacentes.get(source).put(target, edge);
        adyacentes.get(target).put(source, edge); // En un ciclo de un vértice es la misma entrada
        return true;
    }

    @Override
    public boolean eliminarArista(Comparable<V> source, Comparable<V> target) {
        V sourceVertex = buscarVertice(source);
        V targetVertex = buscarVertice(target);
        if (sourceVertex == null || targetVertex == null) return false;

        if (adyacentes.get(sourceVertex).remove(targetVertex) == null) {
            return false; // No existía la arista
        }
        adyacentes.get(targetVertex).remove(sourceVertex); // En un ciclo de un vértice ya se eliminó
        return true;
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return false;

        // Cada vecino deja de tener la arista hacia el vértice (si es un ciclo de un vértice, se borra junto con el vértice)
        for (V vecino : adyacentes.get(vertex).keySet()) {
            if (!vecino.equals(vertex)) {
                adyacentes.get(vecino).remove(vertex);
            }
        }
        adyacentes.remove(vertex);
        return true;
    }

    /**
     * Vista inmodificable de los vértices: modificarla lanza UnsupportedOperationException.
     */
    @Override
    public Set<V> vertices() {
        return Collections.unmodifiableSet(adyacentes.keySet());
    }

    /**
     * Cada arista aparece una sola vez, con la orientación con la que fue agregada.
     */
    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> edges = new LinkedHashSet<>();
        for (Map<V, UndirectedEdge<V, D>> aristasDelVertice : adyacentes.values()) {
            edges.addAll(aristasDelVertice.values()); // {u,v} y {v,u} son iguales: no se duplican
        }
        return Collections.unmodifiableSet(edges);
    }

    @Override
    public int cantidadDeAristas() {
        int aristasComunes = 0;
        int ciclosDeUnVertice = 0;
        for (Map.Entry<V, Map<V, UndirectedEdge<V, D>>> entry : adyacentes.entrySet()) {
            for (V vecino : entry.getValue().keySet()) {
                if (vecino.equals(entry.getKey())) ciclosDeUnVertice++;
                else aristasComunes++;
            }
        }
        // Cada arista común se guarda en sus dos extremos; cada ciclo de un vértice, una sola vez
        return aristasComunes / 2 + ciclosDeUnVertice;
    }

    @Override
    public boolean existeArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        return obtenerArista(sourceCriteria, targetCriteria) != null;
    }

    /**
     * La arista retornada tiene source() igual al vértice de sourceCriteria.
     */
    @Override
    public Edge<V, D> obtenerArista(Comparable<V> sourceCriteria, Comparable<V> targetCriteria) {
        V source = buscarVertice(sourceCriteria);
        V target = buscarVertice(targetCriteria);
        if (source == null || target == null) return null;

        UndirectedEdge<V, D> edge = adyacentes.get(source).get(target);
        return edge == null ? null : orientar(edge, source);
    }

    /**
     * Como indica el contrato de IGraph, todas las aristas retornadas tienen
     * source() igual al vértice consultado y target() igual al vecino.
     */
    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        V vertex = buscarVertice(verticeCriteria);
        if (vertex == null) return List.of();

        List<Edge<V, D>> resultado = new ArrayList<>();
        for (UndirectedEdge<V, D> edge : adyacentes.get(vertex).values()) {
            resultado.add(orientar(edge, vertex));
        }
        return resultado;
    }

    /**
     * Retorna la arista con source() == origen. Como el equals de UndirectedEdge es
     * simétrico, la arista orientada sigue siendo igual a la guardada.
     */
    private UndirectedEdge<V, D> orientar(UndirectedEdge<V, D> edge, V origen) {
        if (edge.source().equals(origen)) return edge;
        return new UndirectedEdge<>(origen, edge.source(), edge.dato());
    }

    /**
     * Un grafo no dirigido es conexo si desde un vértice cualquiera se alcanzan todos.
     * El grafo vacío se considera conexo.
     */
    @Override
    public boolean esConexo() {
        if (adyacentes.isEmpty()) return true;

        // DFS iterativo (evita StackOverflowError en grafos grandes)
        V start = adyacentes.keySet().iterator().next();
        Set<V> visited = new HashSet<>();
        Deque<V> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            V vertex = stack.pop();
            if (!visited.add(vertex)) continue;
            for (V vecino : adyacentes.get(vertex).keySet()) {
                if (!visited.contains(vecino)) {
                    stack.push(vecino);
                }
            }
        }
        return visited.size() == adyacentes.size();
    }

    @Override
    public void vaciar() {
        adyacentes.clear();
    }

    /**
     * Detecta ciclos con un DFS iterativo que recorre todas las componentes: encontrar un
     * vecino ya visitado que no sea el padre (el vértice desde el que se llegó) indica un ciclo.
     * Un ciclo de un vértice (arista {u, u}) también cuenta como ciclo. El grafo vacío no tiene ciclos.
     */
    @Override
    public boolean tieneCiclos() {
        Set<V> visited = new HashSet<>();

        for (V inicio : adyacentes.keySet()) {
            if (visited.contains(inicio)) continue;

            // Cada marco de la pila guarda el vértice, su padre y el iterador de sus vecinos pendientes
            Deque<Marco<V>> stack = new ArrayDeque<>();
            visited.add(inicio);
            stack.push(new Marco<>(inicio, null, adyacentes.get(inicio).keySet().iterator()));

            while (!stack.isEmpty()) {
                Marco<V> tope = stack.peek();
                if (!tope.pendientes().hasNext()) {
                    stack.pop();
                    continue;
                }
                V vecino = tope.pendientes().next();
                if (vecino.equals(tope.padre())) {
                    continue; // Volver por la arista al padre no es un ciclo
                }
                if (visited.contains(vecino)) {
                    return true; // Encontramos un ciclo
                }
                visited.add(vecino);
                stack.push(new Marco<>(vecino, tope.vertice(), adyacentes.get(vecino).keySet().iterator()));
            }
        }
        return false;
    }

    private record Marco<V>(V vertice, V padre, Iterator<V> pendientes) {
    }
}
