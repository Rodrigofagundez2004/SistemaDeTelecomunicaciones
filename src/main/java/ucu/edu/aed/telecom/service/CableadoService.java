package ucu.edu.aed.telecom.service;
import ucu.edu.aed.tda.grafo.model.edge.Edge;
import ucu.edu.aed.tda.grafo.IUndirectedGraph;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;

/** Diseño de cableado: conectar todas las oficinas al menor costo total. */
public class CableadoService {


    /** TODO: devolver el subgrafo de costo mínimo que conecta todos los nodos. */
    public IUndirectedGraph<Nodo, Enlace> disenarCableado(IUndirectedGraph<Nodo, Enlace> posibles) {
        IUndirectedGraph<Nodo, Enlace> resultado = new UndirectedGraph<>();

        // 2. Copiamos todos los vértices al nuevo grafo
        for (Nodo nodo : posibles.vertices()) {
            resultado.agregarVertice(nodo);
        }
        List<Edge<Nodo, Enlace>> mst = posibles.kruskal(); 
        // 4. Agregamos esas aristas al grafo resultado
        for (Edge<Nodo, Enlace> arista : mst) {
            resultado.agregarArista(arista.source(), arista.target(), arista.dato());
        }

        return resultado;

        }
        
    }

