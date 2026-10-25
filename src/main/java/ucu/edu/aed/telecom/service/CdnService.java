package ucu.edu.aed.telecom.service;
import ucu.edu.aed.telecom.model;
import ucu.edu.aed.tda.grafo.model.result;
import ucu.edu.aed.tda.grafo.IDirectedIGraph;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;

/** Distribución de contenido: data center que atiende a cada nodo y ubicación de nuevos data centers. */
public class CdnService {

    /** TODO: data center con menor latencia para un nodo. */
    public Nodo datacenterMasCercano(IDirectedIGraph<Nodo, Enlace> red, Nodo nodo) {
        IDijkstraResult<Nodo> resultado  = red.dijkstra(nodo);
        nodo mejorDC = null;
        double menorLatencia = Double.POSITIVE_INIFINITY;

        // 2. Recorremos todos los nodos de la red
        for (Nodo PosibleDC : red.vertices()) {
            if (esDataCenter(PosibleDC)) {{
                double latencia = resultado.getCost(posibleDC);

                if (latencia < menorLatencia) {
                    menorLatencia = latencia;
                    mejorDC = posibleDC ;
                }
            }
        }
        return mejorDC;

        }
    }

    /** Sugerir dónde instalar un nuevo data center. */
    public Nodo sugerirNuevoDatacenter(IDirectedIGraph<Nodo, Enlace> red) {
        Nodo mejorCandidato = null;
        double menorPromedio = Double.POSITIVE_INFINITY;

        // 1. Recorremos todos los nodos de la red como posibles candidatos
        for (Nodo candidato : red.vertices()) {
            
            // Si ya es un Data Center, no tiene sentido poner otro ahí
            if (esDataCenter(candidato)) {
                continue;
            }

            double sumaLatencias = 0;
            int cantidadClientes = 0;

            // 2. Simulamos qué pasaría si el 'candidato' fuera un DC
            for (Nodo cliente : red.vertices()) {
                if (!esDataCenter(cliente)) {
                    
                    // Calculamos la latencia al DC original más cercano
                    Nodo dcOriginal = datacenterMasCercano(red, cliente);
                    double latenciaOriginal = dcOriginal != null ? 
                        red.dijkstra(cliente).getCost(dcOriginal) : Double.POSITIVE_INFINITY;

                    // Calculamos la latencia al nuevo candidato
                    double latenciaNueva = red.dijkstra(cliente).getCost(candidato);

                    // El cliente usará el que le dé menor latencia
                    double latenciaFinal = Math.min(latenciaOriginal, latenciaNueva);
                    
                    sumaLatencias += latenciaFinal;
                    cantidadClientes++;
                }
            }

            // 3. Calculamos el promedio para este candidato
            if (cantidadClientes > 0) {
                double promedio = sumaLatencias / cantidadClientes;
                
                // Si este candidato mejora el promedio global, lo guardamos
                if (promedio < menorPromedio) {
                    menorPromedio = promedio;
                    mejorCandidato = candidato;
                }
            }
        }

        return mejorCandidato;
    }

    //Metodo para cheuquera si el nodo es un datacenter
    private boolean esDataCenter(Nodo nodo) {
        return nodo.getTipo() == TipoNodo.DATA_CENTER;
    }
}
