package ucu.edu.aed.telecom.model;

import java.util.*;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraph;
import ucu.edu.aed.tda.grafo.Impl.VistaFiltrada;

/**
 * Red de telecomunicaciones: dueña del catálogo de nodos y enlaces y de los dos grafos permanentes.
 * <ul>
 *     <li><b>Ruteo</b> (dirigido): 2 arcos por enlace, con la latencia de cada sentido.</li>
 *     <li><b>Físico</b> (no dirigido): 1 arista por enlace, con su costo.</li>
 * </ul>
 * Los dos grafos comparten las mismas instancias de {@link Nodo} y {@link Enlace}, así que una
 * falla se ve en ambos. Las simulaciones ("¿qué pasa si cae X?") usan {@link #vistaActivaSin}
 * y no modifican la red.
 * <p>
 * {@code version} se incrementa con cada cambio real (alta de nodos o enlaces, fallas y
 * restauraciones); sirve para invalidar cachés.
 */
public class Red {
    private final Map<String, Nodo> nodos = new LinkedHashMap<>();
    private final List<Enlace> enlaces = new ArrayList<>();
    // {idA, idB} -> enlace; Set.of hace que la clave no dependa del orden
    private final Map<Set<String>, Enlace> enlacesPorPar = new HashMap<>();
    private final DirectedGraph<Nodo, PesoLatencia> grafoRuteo = new DirectedGraph<>();
    private final UndirectedGraph<Nodo, PesoCosto> grafoFisico = new UndirectedGraph<>();
    private int version = 0;

    /**
     * Agrega el nodo a los dos grafos.
     *
     * @throws IllegalArgumentException si ya existe un nodo con ese id
     */
    public void agregarNodo(Nodo nodo) {
        Objects.requireNonNull(nodo, "El nodo no puede ser null");
        if (nodos.containsKey(nodo.getId())) {
            throw new IllegalArgumentException("Ya existe un nodo con id " + nodo.getId());
        }
        nodos.put(nodo.getId(), nodo);
        grafoRuteo.agregarVertice(nodo);
        grafoFisico.agregarVertice(nodo);
        version++;
    }

    /**
     * Agrega el enlace: 2 arcos en el grafo de ruteo (uno por sentido) y 1 arista en el físico.
     *
     * @throws IllegalArgumentException si algún extremo no es un nodo de esta red, o si ya
     *                                  existe un enlace entre esos dos nodos (en cualquier orden)
     */
    public void agregarEnlace(Enlace enlace) {
        Objects.requireNonNull(enlace, "El enlace no puede ser null");
        Nodo a = enlace.getA();
        Nodo b = enlace.getB();
        // Exigimos la misma instancia: si no, el estado activo del nodo quedaría duplicado
        if (nodos.get(a.getId()) != a || nodos.get(b.getId()) != b) {
            throw new IllegalArgumentException("Los extremos del enlace " + enlace + " deben ser nodos de esta red");
        }
        Set<String> par = Set.of(a.getId(), b.getId());
        if (enlacesPorPar.containsKey(par)) {
            throw new IllegalArgumentException("Ya existe un enlace entre " + a + " y " + b);
        }
        enlacesPorPar.put(par, enlace);
        enlaces.add(enlace);
        grafoRuteo.agregarArista(a, b, new PesoLatencia(enlace, a));
        grafoRuteo.agregarArista(b, a, new PesoLatencia(enlace, b));
        grafoFisico.agregarArista(a, b, new PesoCosto(enlace, enlace.getCosto()));
        version++;
    }

    /**
     * Crea y agrega un enlace entre los nodos con esos ids.
     *
     * @throws IllegalArgumentException si algún id no existe o ya hay un enlace entre esos nodos
     */
    public Enlace agregarEnlace(String idA, String idB, double latAB, double latBA,
                                TipoEnlace tipo, double anchoDeBanda, double costo) {
        Enlace enlace = new Enlace(getNodo(idA), getNodo(idB), latAB, latBA, tipo, anchoDeBanda, costo);
        agregarEnlace(enlace);
        return enlace;
    }

    /**
     * @throws IllegalArgumentException si no existe un nodo con ese id
     */
    public Nodo getNodo(String id) {
        Nodo nodo = nodos.get(id);
        if (nodo == null) throw new IllegalArgumentException("No existe el nodo " + id);
        return nodo;
    }

    public boolean existeNodo(String id) {
        return nodos.containsKey(id);
    }

    public Collection<Nodo> getNodos() {
        return Collections.unmodifiableCollection(nodos.values());
    }

    public List<Enlace> getEnlaces() {
        return Collections.unmodifiableList(enlaces);
    }

    public List<Nodo> getDataCenters() {
        return nodos.values().stream().filter(Nodo::esDataCenter).toList();
    }

    public List<Nodo> getClientes() {
        return nodos.values().stream().filter(Nodo::esCliente).toList();
    }

    /**
     * Enlace entre los dos nodos, en cualquier orden, o null si no hay.
     *
     * @throws IllegalArgumentException si algún id no existe
     */
    public Enlace getEnlace(String idA, String idB) {
        getNodo(idA);
        getNodo(idB);
        return enlacesPorPar.get(Set.of(idA, idB));
    }

    /** Grafo de ruteo completo, incluidos nodos y enlaces caídos. No modificarlo desde afuera. */
    public IDirectedIGraph<Nodo, PesoLatencia> getGrafoRuteo() {
        return grafoRuteo;
    }

    /** Grafo físico completo, incluidos nodos y enlaces caídos. No modificarlo desde afuera. */
    public IUndirectedGraph<Nodo, PesoCosto> getGrafoFisico() {
        return grafoFisico;
    }

    /** Grafo de ruteo sin los nodos ni los enlaces caídos. */
    public IDirectedIGraph<Nodo, PesoLatencia> vistaActiva() {
        return new VistaFiltrada<>(grafoRuteo, Nodo::isActivo, p -> p.getEnlace().isActivo());
    }

    /**
     * Como {@link #vistaActiva()}, pero oculta además los nodos y enlaces indicados.
     * No modifica la red ni su versión: es lo que usan las simulaciones de fallas.
     */
    public IDirectedIGraph<Nodo, PesoLatencia> vistaActivaSin(Set<Nodo> nodosOcultos, Set<Enlace> enlacesOcultos) {
        Set<Nodo> nodosSin = Set.copyOf(nodosOcultos);
        Set<Enlace> enlacesSin = Set.copyOf(enlacesOcultos);
        return new VistaFiltrada<>(grafoRuteo,
                n -> n.isActivo() && !nodosSin.contains(n),
                p -> p.getEnlace().isActivo() && !enlacesSin.contains(p.getEnlace()));
    }

    public void fallarNodo(String id) {
        cambiarEstado(getNodo(id), false);
    }

    public void restaurarNodo(String id) {
        cambiarEstado(getNodo(id), true);
    }

    /**
     * @throws IllegalArgumentException si algún id no existe o no hay enlace entre esos nodos
     */
    public void fallarEnlace(String idA, String idB) {
        cambiarEstado(enlaceExistente(idA, idB), false);
    }

    public void restaurarEnlace(String idA, String idB) {
        cambiarEstado(enlaceExistente(idA, idB), true);
    }

    public int getVersion() {
        return version;
    }

    private Enlace enlaceExistente(String idA, String idB) {
        Enlace enlace = getEnlace(idA, idB);
        if (enlace == null) throw new IllegalArgumentException("No existe un enlace entre " + idA + " y " + idB);
        return enlace;
    }

    // La versión solo cambia si el estado cambia de verdad (fallar dos veces cuenta una)
    private void cambiarEstado(Nodo nodo, boolean activo) {
        if (nodo.isActivo() == activo) return;
        nodo.setActivo(activo);
        version++;
    }

    private void cambiarEstado(Enlace enlace, boolean activo) {
        if (enlace.isActivo() == activo) return;
        enlace.setActivo(activo);
        version++;
    }
}
