package ucu.edu.aed.telecom.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.Impl.UndirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.PesoCosto;
import ucu.edu.aed.telecom.model.PesoLatencia;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.Ruta;

/**
 * Resiliencia: equipos y enlaces críticos, qué queda incomunicado ante una falla y rutas alternativas.
 * Las simulaciones usan {@link Red#vistaActivaSin}: nunca modifican la red.
 */
public class ResilienciaService {

    private final Red red;
    private final RuteoService ruteo;
    private final UndirectedGraphAlgorithms algNoDirigidos = new UndirectedGraphAlgorithms();
    private final DirectedGraphAlgorithms algDirigidos = new DirectedGraphAlgorithms();

    public ResilienciaService(Red red) {
        this.red = Objects.requireNonNull(red, "La red no puede ser null");
        this.ruteo = new RuteoService(red);
    }

    /** Puntos de articulación del grafo físico: equipos cuya caída parte la red instalada. */
    public Set<Nodo> equiposCriticos() {
        return algNoDirigidos.puntosDeArticulacion(red.getGrafoFisico());
    }

    /** Puentes del grafo físico: enlaces cuya caída parte la red instalada. */
    public Set<Enlace> enlacesCriticos() {
        Set<Enlace> criticos = new LinkedHashSet<>();
        for (Edge<Nodo, PesoCosto> arista : algNoDirigidos.puentes(red.getGrafoFisico())) {
            criticos.add(arista.dato().getEnlace());
        }
        return criticos;
    }

    /** Nodos activos que no alcanzarían ningún data center si cae el nodo indicado (no se incluye a sí mismo). */
    public Set<Nodo> incomunicadosSiFallaNodo(String id) {
        Nodo caido = red.getNodo(id);
        return incomunicados(red.vistaActivaSin(Set.of(caido), Set.of()));
    }

    /**
     * Nodos activos que no alcanzarían ningún data center si cae el enlace indicado.
     *
     * @throws IllegalArgumentException si algún id no existe o no hay enlace entre esos nodos
     */
    public Set<Nodo> incomunicadosSiFallaEnlace(String idA, String idB) {
        Enlace enlace = red.getEnlace(idA, idB);
        if (enlace == null) {
            throw new IllegalArgumentException("No existe un enlace entre " + idA + " y " + idB);
        }
        return incomunicados(red.vistaActivaSin(Set.of(), Set.of(enlace)));
    }

    /**
     * Ruta de menor latencia que no comparte nodos intermedios ni enlaces con la ruta principal.
     * Limitación: método de dos pasos; puede no encontrar alternativa aunque exista (exacto: Suurballe).
     *
     * @return la alternativa, o Ruta.inexistente() si no hay ruta principal o no hay alternativa
     */
    public Ruta rutaAlternativa(String idOrigen, String idDestino) {
        if (idOrigen.equals(idDestino)) {
            throw new IllegalArgumentException("Origen y destino son el mismo nodo");
        }
        Ruta principal = ruteo.rutaDeMenorLatencia(idOrigen, idDestino);
        if (!principal.existe()) {
            return Ruta.inexistente();
        }

        List<Nodo> nodos = principal.nodos();
        Set<Nodo> intermedios = new HashSet<>(nodos.subList(1, nodos.size() - 1));
        Set<Enlace> enlacesDeLaRuta = new HashSet<>();
        for (int i = 0; i + 1 < nodos.size(); i++) {
            enlacesDeLaRuta.add(red.getEnlace(nodos.get(i).getId(), nodos.get(i + 1).getId()));
        }
        return ruteo.calcularRuta(red.vistaActivaSin(intermedios, enlacesDeLaRuta), idOrigen, idDestino);
    }

    /**
     * BFS desde cada data center visible; lo que ningún BFS alcanza queda incomunicado.
     * Alcanza con un sentido porque cada enlace da dos arcos y una falla oculta los dos.
     * Si un DC ya fue alcanzado se saltea: su componente ya está recorrida, así el total es O(V+E).
     */
    private Set<Nodo> incomunicados(IDirectedIGraph<Nodo, PesoLatencia> vista) {
        Set<Nodo> alcanzados = new HashSet<>();
        for (Nodo nodo : vista.vertices()) {
            if (nodo.esDataCenter() && !alcanzados.contains(nodo)) {
                algDirigidos.recorridoEnAmplitud(vista, vista.construirComparable(nodo), alcanzados::add);
            }
        }
        Set<Nodo> resultado = new LinkedHashSet<>();
        for (Nodo nodo : vista.vertices()) {
            if (!alcanzados.contains(nodo)) {
                resultado.add(nodo);
            }
        }
        return resultado;
    }
}