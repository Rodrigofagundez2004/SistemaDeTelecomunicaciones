package ucu.edu.aed.telecom.model;

import java.util.Map;
import java.util.Set;

/** Data center que atiende a cada cliente, y los clientes que no alcanzan ningún data center activo. */
public record AsignacionCdn(Map<Nodo, Nodo> dcPorCliente, Set<Nodo> sinServicio) {

    public AsignacionCdn {
        dcPorCliente = Map.copyOf(dcPorCliente);
        sinServicio = Set.copyOf(sinServicio);
    }
}
