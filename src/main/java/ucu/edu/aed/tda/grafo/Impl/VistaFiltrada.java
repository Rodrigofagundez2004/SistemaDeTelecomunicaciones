package ucu.edu.aed.tda.grafo.Impl;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.model.edge.Edge;

import java.util.*;
import java.util.function.Predicate;

/**
 * Vista de solo lectura sobre un grafo dirigido que oculta los vértices y aristas que no
 * cumplen los predicados. No copia el grafo: cada consulta se resuelve sobre el grafo base,
 * así que los cambios en el base (o en el estado que miran los predicados) se ven enseguida.
 * <p>
 * Una arista es visible solo si cumple {@code aristaOk} <b>y</b> sus dos extremos cumplen
 * {@code nodoOk}. Esto es obligatorio: algoritmos como Floyd recorren {@link #aristas()} y
 * buscan cada extremo en {@link #vertices()}.
 * <p>
 * Los métodos que modifican el grafo lanzan {@link UnsupportedOperationException}.
 */
public class VistaFiltrada<V, D> implements IDirectedIGraph<V, D> {
    private final IDirectedIGraph<V, D> base;
    private final Predicate<V> nodoOk;
    private final Predicate<D> aristaOk;

    public VistaFiltrada(IDirectedIGraph<V, D> base, Predicate<V> nodoOk, Predicate<D> aristaOk) {
        this.base = Objects.requireNonNull(base);
        this.nodoOk = Objects.requireNonNull(nodoOk);
        this.aristaOk = Objects.requireNonNull(aristaOk);
    }

    private boolean esVisible(Edge<V, D> e) {
        return aristaOk.test(e.dato()) && nodoOk.test(e.source()) && nodoOk.test(e.target());
    }

    @Override
    public V buscarVertice(Comparable<V> criterio) {
        if (criterio == null) return null;
        for (V vertex : base.vertices()) {
            if (nodoOk.test(vertex) && criterio.compareTo(vertex) == 0) {
                return vertex;
            }
        }
        return null;
    }

    @Override
    public Set<V> vertices() {
        Set<V> visibles = new LinkedHashSet<>();
        for (V vertex : base.vertices()) {
            if (nodoOk.test(vertex)) visibles.add(vertex);
        }
        return Collections.unmodifiableSet(visibles);
    }

    @Override
    public Set<Edge<V, D>> aristas() {
        Set<Edge<V, D>> visibles = new LinkedHashSet<>();
        for (Edge<V, D> e : base.aristas()) {
            if (esVisible(e)) visibles.add(e);
        }
        return Collections.unmodifiableSet(visibles);
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
        Edge<V, D> e = base.obtenerArista(base.construirComparable(source), base.construirComparable(target));
        return e != null && aristaOk.test(e.dato()) ? e : null;
    }

    @Override
    public List<Edge<V, D>> adyacencias(Comparable<V> verticeCriteria) {
        V vertex = buscarVertice(verticeCriteria);
        if (vertex == null) return List.of();
        return salientesVisibles(vertex);
    }

    @Override
    public Set<V> successors(Comparable<V> criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return Set.of();
        return Collections.unmodifiableSet(sucesoresVisibles(vertex));
    }

    @Override
    public Set<V> predecessors(Comparable<V> criteria) {
        V vertex = buscarVertice(criteria);
        if (vertex == null) return Set.of();
        return Collections.unmodifiableSet(predecesoresVisibles(vertex));
    }

    /**
     * Conexidad fuerte sobre lo visible. Reutiliza {@link DirectedGraph#esConexo()} aplicado a una
     * copia de la vista: llamar a {@code base.esConexo()} respondería sobre el grafo completo,
     * incluidos los vértices y aristas ocultos.
     */
    @Override
    public boolean esConexo() {
        return copiaVisible().esConexo();
    }

    /** Ciclos sobre lo visible. Reutiliza {@link DirectedGraph#tieneCiclos()} igual que {@link #esConexo()}. */
    @Override
    public boolean tieneCiclos() {
        return copiaVisible().tieneCiclos();
    }

    /**
     * Copia en un DirectedGraph nuevo solo los vértices y aristas visibles, en O(V + E).
     * Las aristas copiadas comparten el dato de las originales.
     */
    private DirectedGraph<V, D> copiaVisible() {
        DirectedGraph<V, D> copia = new DirectedGraph<>();
        copia.agregarVertices(vertices());
        for (Edge<V, D> e : aristas()) {
            copia.agregarArista(e.source(), e.target(), e.dato());
        }
        return copia;
    }

    // Precondición de los tres auxiliares: vertex es visible.

    private List<Edge<V, D>> salientesVisibles(V vertex) {
        List<Edge<V, D>> resultado = new ArrayList<>();
        for (Edge<V, D> e : base.adyacencias(base.construirComparable(vertex))) {
            if (esVisible(e)) resultado.add(e);
        }
        return resultado;
    }

    private Set<V> sucesoresVisibles(V vertex) {
        Set<V> resultado = new LinkedHashSet<>();
        for (Edge<V, D> e : salientesVisibles(vertex)) {
            resultado.add(e.target());
        }
        return resultado;
    }

    private Set<V> predecesoresVisibles(V vertex) {
        Set<V> resultado = new LinkedHashSet<>();
        for (V predecesor : base.predecessors(base.construirComparable(vertex))) {
            if (!nodoOk.test(predecesor)) continue;
            Edge<V, D> e = base.obtenerArista(base.construirComparable(predecesor), base.construirComparable(vertex));
            if (e != null && aristaOk.test(e.dato())) resultado.add(predecesor);
        }
        return resultado;
    }

    // ---- Operaciones de modificación: la vista es de solo lectura ----

    @Override
    public boolean agregarVertice(V vertex) {
        throw new UnsupportedOperationException("La vista filtrada es de solo lectura");
    }

    @Override
    public boolean agregarArista(V source, V target, D dato) {
        throw new UnsupportedOperationException("La vista filtrada es de solo lectura");
    }

    @Override
    public boolean eliminarArista(Comparable<V> source, Comparable<V> target) {
        throw new UnsupportedOperationException("La vista filtrada es de solo lectura");
    }

    @Override
    public boolean removerVertice(Comparable<V> criteria) {
        throw new UnsupportedOperationException("La vista filtrada es de solo lectura");
    }

    @Override
    public void vaciar() {
        throw new UnsupportedOperationException("La vista filtrada es de solo lectura");
    }
}
