package ucu.edu.aed.telecom.model;

import java.util.Objects;

/**
 * Nodo de la red.
 * <p>
 * {@code equals}/{@code hashCode} son por {@code id}: los grafos de la red usan los nodos como
 * claves de sus mapas. El estado {@code activo} solo lo cambia {@link Red} (por eso el setter es
 * de paquete), así cada falla queda registrada en la versión de la red.
 */
public class Nodo {

    private final String id;
    private final TipoNodo tipo;
    private final int usuarios;
    private final int capacidad;
    private boolean activo = true;

    /**
     * @param usuarios  usuarios atendidos (solo tiene sentido en nodos cliente)
     * @param capacidad usuarios máximos que puede atender (solo tiene sentido en data centers)
     * @throws IllegalArgumentException si el id es vacío o usuarios/capacidad son negativos
     */
    public Nodo(String id, TipoNodo tipo, int usuarios, int capacidad) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El id no puede ser vacío");
        if (usuarios < 0 || capacidad < 0) {
            throw new IllegalArgumentException("Usuarios y capacidad no pueden ser negativos: " + id);
        }
        this.id = id;
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser null");
        this.usuarios = usuarios;
        this.capacidad = capacidad;
    }

    public Nodo(String id, TipoNodo tipo) {
        this(id, tipo, 0, 0);
    }

    public String getId() { return id; }
    public TipoNodo getTipo() { return tipo; }
    public int getUsuarios() { return usuarios; }
    public int getCapacidad() { return capacidad; }
    public boolean isActivo() { return activo; }

    void setActivo(boolean activo) { this.activo = activo; }

    public boolean esCliente() { return tipo.esCliente(); }

    public boolean esDataCenter() { return tipo == TipoNodo.DATA_CENTER; }

    /** usuarios × consumo por usuario del tipo, en Mbps. */
    public double demandaEstimadaMbps() {
        return usuarios * tipo.getConsumoPorUsuarioMbps();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nodo otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
