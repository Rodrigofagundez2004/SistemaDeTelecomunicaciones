package ucu.edu.aed.telecom.model;

import java.util.List;
import java.util.Set;

/**
 * Resultado del diseño de cableado: enlaces nuevos a instalar, su costo total y las
 * oficinas que no se pudieron conectar con las conexiones posibles.
 */
public record ResultadoCableado(List<Enlace> aInstalar, double costoTotal, Set<Nodo> noConectables) {

    public ResultadoCableado {
        aInstalar = List.copyOf(aInstalar);
        noConectables = Set.copyOf(noConectables);
    }
}
