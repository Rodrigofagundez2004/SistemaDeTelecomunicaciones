package ucu.edu.aed.telecom.model;

import ucu.edu.aed.tda.grafo.model.edge.WeightedEdge;

/**
 * Enlace de la red. Extiende WeightedEdge para poder usar Dijkstra/Floyd/Prim/Kruskal.
 * Decisión a justificar en el informe: el peso del grafo es la latencia.
 * Ancho de banda y costo son atributos adicionales.
 */
public class Enlace extends WeightedEdge {

    private final TipoEnlace tipo;
    private final double anchoDeBanda;
    private final double costo;

    public Enlace(double latencia, TipoEnlace tipo, double anchoDeBanda, double costo) {
        super(latencia);
        this.tipo = tipo;
        this.anchoDeBanda = anchoDeBanda;
        this.costo = costo;
    }

    public double getLatencia() { return getWeight(); }
    public TipoEnlace getTipo() { return tipo; }
    public double getAnchoDeBanda() { return anchoDeBanda; }
    public double getCosto() { return costo; }
}
