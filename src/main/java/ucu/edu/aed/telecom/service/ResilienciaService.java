package ucu.edu.aed.telecom.service;

import java.util.Set;

import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;

/** Resiliencia: equipos críticos, nodos incomunicados y rutas alternativas. */
public class ResilienciaService {

    /** TODO: nodos cuya falla desconecta la red (puntos de articulación). */
    public Set<Nodo> equiposCriticos(IUndirectedGraph<Nodo, Enlace> red) {
        throw new UnsupportedOperationException("TODO: equiposCriticos");
    }

    // TODO: nodos incomunicados ante la falla de un equipo, enlace o data center.
    // TODO: rutas alternativas entre dos nodos.
}
