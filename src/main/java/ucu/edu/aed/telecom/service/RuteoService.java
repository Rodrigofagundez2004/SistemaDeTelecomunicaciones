package ucu.edu.aed.telecom.service;

import java.util.List;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.model.result.IDijkstraResult;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.PesoLatencia;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.Ruta;

/** Ruteo: menor latencia entre dos nodos y tabla de ruteo completa. */
public class RuteoService {

    private final Red red;
    private final DirectedGraphAlgorithms algoritmos = new DirectedGraphAlgorithms();

    public RuteoService(Red red) {
        this.red = red;
    }

    /**
     * Ruta de menor latencia de origen a destino usando nodos y enlaces activos.
     * Si no hay ruta (o alguno de los dos está caído) devuelve Ruta.inexistente().
     *
     */
    public Ruta rutaDeMenorLatencia(String idOrigen, String idDestino) {
        return calcularRuta(red.vistaActiva(), idOrigen, idDestino);
    }

    /** TODO: tabla de ruteo completa (latencia mínima y camino entre todos los pares). */
    public Ruta tablaDeRuteo(IDirectedIGraph<Nodo, Enlace> red) {
        throw new UnsupportedOperationException("TODO: tablaDeRuteo");
    }

    /** Corre Dijkstra desde el origen sobre la vista recibida y arma la Ruta hasta el destino. */
    private Ruta calcularRuta(IDirectedIGraph<Nodo, PesoLatencia> vista, String idOrigen, String idDestino) {
        Nodo origen = red.getNodo(idOrigen);
        Nodo destino = red.getNodo(idDestino);

        IDijkstraResult<Nodo> resultado = algoritmos.dijkstra(vista.construirComparable(origen), vista);
        List<Nodo> camino = resultado.getPath(destino);

        if (camino.isEmpty())
            return Ruta.inexistente();

        return new Ruta(camino, resultado.getCost(destino));
    }
}
