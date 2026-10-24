package ucu.edu.aed.telecom.model;

/**
 * Tipos de nodo de la red.
 * Los tipos cliente (oficinas, edificios, puntos de acceso) tienen usuarios y un consumo
 * estimado por usuario; el resto son equipos de infraestructura.
 */
public enum TipoNodo {
    ROUTER(false, 0),
    SWITCH(false, 0),
    SERVIDOR(false, 0),
    DATA_CENTER(false, 0),
    PUNTO_DE_ACCESO(true, 3),
    OFICINA(true, 5),
    EDIFICIO(true, 15);

    private final boolean cliente;
    private final double consumoPorUsuarioMbps;

    TipoNodo(boolean cliente, double consumoPorUsuarioMbps) {
        this.cliente = cliente;
        this.consumoPorUsuarioMbps = consumoPorUsuarioMbps;
    }

    public boolean esCliente() { return cliente; }

    /** Consumo estimado por usuario en Mbps (0 para los tipos que no son cliente). */
    public double getConsumoPorUsuarioMbps() { return consumoPorUsuarioMbps; }
}
