package ucu.edu.aed.telecom.model;

import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

/** Dato de los arcos del grafo de ruteo: el peso es la latencia del enlace en el sentido del arco. */
public class PesoLatencia extends WeightedEdge {

    private final Enlace enlace;

    /** Arco que sale de {@code origen} recorriendo {@code enlace}. */
    public PesoLatencia(Enlace enlace, Nodo origen) {
        super(enlace.latenciaDesde(origen));
        this.enlace = enlace;
    }

    public Enlace getEnlace() { return enlace; }
}
