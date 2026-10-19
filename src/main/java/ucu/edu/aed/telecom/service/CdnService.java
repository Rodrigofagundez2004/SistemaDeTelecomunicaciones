package ucu.edu.aed.telecom.service;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;

/** Distribución de contenido: data center que atiende a cada nodo y ubicación de nuevos data centers. */
public class CdnService {

    /** TODO: data center con menor latencia para un nodo. */
    public Nodo datacenterMasCercano(IDirectedIGraph<Nodo, Enlace> red, Nodo nodo) {
        throw new UnsupportedOperationException("TODO: datacenterMasCercano");
    }

    /** TODO: sugerir dónde instalar un nuevo data center. El criterio debe definirse y justificarse. */
    public Nodo sugerirNuevoDatacenter(IDirectedIGraph<Nodo, Enlace> red) {
        throw new UnsupportedOperationException("TODO: sugerirNuevoDatacenter");
    }
}
