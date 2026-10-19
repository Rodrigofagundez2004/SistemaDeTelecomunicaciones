package ucu.edu.aed.telecom.service;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;

/** Ruteo: menor latencia entre dos nodos y tabla de ruteo completa. */
public class RuteoService {

    /** TODO: ruta de menor latencia entre origen y destino (con nodos intermedios). */
    public IDijkstraResult<Nodo> rutaDeMenorLatencia(IDirectedIGraph<Nodo, Enlace> red, Nodo origen) {
        throw new UnsupportedOperationException("TODO: rutaDeMenorLatencia");
    }

    /** TODO: tabla de ruteo completa (latencia mínima y camino entre todos los pares). */
    public IFloydWarshallResult<Nodo> tablaDeRuteo(IDirectedIGraph<Nodo, Enlace> red) {
        throw new UnsupportedOperationException("TODO: tablaDeRuteo");
    }

    /** TODO (opcional, se valora): ruteo con ancho de banda mínimo requerido. */
}
