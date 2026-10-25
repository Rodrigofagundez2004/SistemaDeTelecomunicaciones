package ucu.edu.aed.telecom.model;

import java.util.List;

/**
 * Ruta de máximo ancho de banda: el cuello de botella es el ancho de banda del enlace
 * más lento de la ruta. Si no hay ruta: lista vacía y cuello de botella 0.
 */
public record RutaAnchoDeBanda(List<Nodo> nodos, double cuelloDeBotellaMbps) {

    public RutaAnchoDeBanda {
        nodos = List.copyOf(nodos);
    }

    public static RutaAnchoDeBanda inexistente() {
        return new RutaAnchoDeBanda(List.of(), 0);
    }

    public boolean existe() {
        return !nodos.isEmpty();
    }
}
