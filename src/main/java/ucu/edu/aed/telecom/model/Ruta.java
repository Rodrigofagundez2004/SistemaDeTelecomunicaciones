package ucu.edu.aed.telecom.model;

import java.util.List;

/** Ruta entre dos nodos y su latencia total. Si no hay ruta: lista vacía y latencia infinita. */
public record Ruta(List<Nodo> nodos, double latencia) {

    public Ruta {
        nodos = List.copyOf(nodos);
    }

    public static Ruta inexistente() {
        return new Ruta(List.of(), Double.POSITIVE_INFINITY);
    }

    public boolean existe() {
        return !nodos.isEmpty();
    }
}
