package ucu.edu.aed.telecom.model;

/**
 * Nodo de la red. Esqueleto: definir qué atributos necesita el modelo
 * (capacidad, usuarios atendidos, ubicación, etc.) y justificarlos en el informe.
 */
public class Nodo {

    private final String id;
    private final TipoNodo tipo;

    public Nodo(String id, TipoNodo tipo) {
        this.id = id;
        this.tipo = tipo;
    }

    public String getId() { return id; }
    public TipoNodo getTipo() { return tipo; }

    // TODO: capacidad, cantidad de usuarios atendidos, etc.

    @Override
    public String toString() {
        return id;
    }
}
