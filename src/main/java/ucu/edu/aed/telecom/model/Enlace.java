package ucu.edu.aed.telecom.model;

import java.util.Objects;

/**
 * Enlace físico entre dos nodos: único lugar donde viven sus atributos.
 * <p>
 * No extiende WeightedEdge porque cada grafo necesita un peso distinto (latencia por sentido
 * para ruteo, costo para cableado). Los grafos guardan {@link PesoLatencia} / {@link PesoCosto},
 * que apuntan a esta misma instancia: una falla ({@code activo = false}) se ve en todos.
 * <p>
 * El estado {@code activo} solo lo cambia {@link Red}. La igualdad es por identidad: la red
 * admite a lo sumo un enlace por par de nodos.
 */
public class Enlace {

    private final Nodo a;
    private final Nodo b;
    private final double latAB;
    private final double latBA;
    private final TipoEnlace tipo;
    private final double anchoDeBanda;
    private final double costo;
    private boolean activo = true;

    /**
     * @param latAB latencia en ms en el sentido a -> b
     * @param latBA latencia en ms en el sentido b -> a
     * @throws IllegalArgumentException si a y b son el mismo nodo, o algún valor es inválido
     */
    public Enlace(Nodo a, Nodo b, double latAB, double latBA, TipoEnlace tipo, double anchoDeBanda, double costo) {
        this.a = Objects.requireNonNull(a, "El extremo a no puede ser null");
        this.b = Objects.requireNonNull(b, "El extremo b no puede ser null");
        if (a.equals(b)) throw new IllegalArgumentException("Un enlace no puede unir un nodo consigo mismo: " + a);
        if (latAB < 0 || latBA < 0) throw new IllegalArgumentException("La latencia no puede ser negativa: " + this);
        if (anchoDeBanda <= 0) throw new IllegalArgumentException("El ancho de banda debe ser positivo: " + this);
        if (costo < 0) throw new IllegalArgumentException("El costo no puede ser negativo: " + this);
        this.latAB = latAB;
        this.latBA = latBA;
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser null");
        this.anchoDeBanda = anchoDeBanda;
        this.costo = costo;
    }

    /** Enlace con la misma latencia en los dos sentidos. */
    public Enlace(Nodo a, Nodo b, double latencia, TipoEnlace tipo, double anchoDeBanda, double costo) {
        this(a, b, latencia, latencia, tipo, anchoDeBanda, costo);
    }

    public Nodo getA() { return a; }
    public Nodo getB() { return b; }
    public double getLatAB() { return latAB; }
    public double getLatBA() { return latBA; }
    public TipoEnlace getTipo() { return tipo; }
    public double getAnchoDeBanda() { return anchoDeBanda; }
    public double getCosto() { return costo; }
    public boolean isActivo() { return activo; }

    void setActivo(boolean activo) { this.activo = activo; }

    /**
     * Latencia al recorrer el enlace saliendo desde {@code origen}.
     *
     * @throws IllegalArgumentException si origen no es un extremo del enlace
     */
    public double latenciaDesde(Nodo origen) {
        if (a.equals(origen)) return latAB;
        if (b.equals(origen)) return latBA;
        throw new IllegalArgumentException(origen + " no es extremo del enlace " + this);
    }

    /**
     * @throws IllegalArgumentException si n no es un extremo del enlace
     */
    public Nodo otroExtremo(Nodo n) {
        if (a.equals(n)) return b;
        if (b.equals(n)) return a;
        throw new IllegalArgumentException(n + " no es extremo del enlace " + this);
    }

    /** true si el enlace une x e y, en cualquier orden. */
    public boolean conecta(Nodo x, Nodo y) {
        return (a.equals(x) && b.equals(y)) || (a.equals(y) && b.equals(x));
    }

    @Override
    public String toString() {
        return a + " <-> " + b;
    }
}
