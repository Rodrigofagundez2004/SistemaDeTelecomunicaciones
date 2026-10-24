package ucu.edu.aed.telecom.model;

import java.util.Objects;

import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

/**
 * Dato de las aristas de los grafos físico y de planificación: el peso es un costo.
 * El costo se pasa aparte (no siempre es el del enlace) para poder cargar los enlaces ya
 * instalados con costo 0 en el grafo de planificación.
 */
public class PesoCosto extends WeightedEdge {

    private final Enlace enlace;

    public PesoCosto(Enlace enlace, double costo) {
        super(costo);
        this.enlace = Objects.requireNonNull(enlace);
    }

    public Enlace getEnlace() { return enlace; }
}
