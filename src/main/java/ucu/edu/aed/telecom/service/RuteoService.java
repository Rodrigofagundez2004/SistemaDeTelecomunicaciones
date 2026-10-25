package ucu.edu.aed.telecom.service;

import java.util.List;

import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.Impl.VistaFiltrada;
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

    /**
     * Ruta de menor latencia que solo usa enlaces con al menos {@code mbpsMinimo} de ancho de banda.
     * Se resuelve con el mismo Dijkstra, sobre una vista que además oculta los enlaces más lentos.
     */
    public Ruta rutaConAnchoDeBandaMinimo(String idOrigen, String idDestino, double mbpsMinimo) {
        if (mbpsMinimo < 0)
            throw new IllegalArgumentException("El ancho de banda minimo no puede ser negativo");
        IDirectedIGraph<Nodo, PesoLatencia> vista = new VistaFiltrada<>(
            red.vistaActiva(), 
            nodo -> true,
            peso -> peso.getEnlace().getAnchoDeBanda() >= mbpsMinimo);
        
        return calcularRuta(vista, idOrigen, idDestino);
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
