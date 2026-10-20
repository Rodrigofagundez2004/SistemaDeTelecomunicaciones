# Proyecto Integrador 2 - Sistema de Telecomunicaciones

Clase directed graph, ordenes de tiempo de ejecucion de los metodos 

### 1. **Complejidad para cada método**

| Método | Complejidad | Explicación |
| --- | --- | --- |
| **`agregarVertice`** | O(1) | La operación de un tiene complejidad amortizada O(1), ya que estamos solo agregando un elemento. `put``HashMap` |
| **`buscarVertice`** | O(V) | Iteramos sobre las claves del mapa para buscar un nodo que cumpla el criterio. |
| **`agregarArista`** | O(1) | Obtener un conjunto de adyacencias desde el y añadir una arista al tiene tiempo O(1). `Set``HashMap``Set` |
| **`eliminarArista`** | O(E/V) | Accedemos al conjunto asociado al nodo fuente (O(1)) y eliminamos una arista iterando sobre las adyacencias. |
| **`removerVertice`** | O(V + E) | Eliminamos todas las aristas entrantes (O(V + E)) y eliminamos el vértice y su conjunto de aristas salientes. |
| **`vertices`** | O(1) | El método del devuelve las claves en O(1). `keySet``HashMap` |
| **`aristas`** | O(E) | Iteramos por cada entrada del y recopilamos todas las aristas. `HashMap` |
| **`existeArista`** | O(E/V) | Accedemos a las adyacencias del nodo fuente (O(1)), luego iteramos por ese conjunto para buscar la arista. |
| **`obtenerArista`** | O(E/V) | Similar a , buscamos la arista en el conjunto de adyacencias, lo que depende de su tamaño. `existeArista` |
| **`adyacencias`** | O(1) | Obtenemos las aristas salientes de un nodo usando , que tiene tiempo constante O(1). `get` |
| **`successors`** | O(E/V) | Iteramos por todas las aristas salientes del nodo y recopilamos los nodos destino. |
| **`predecessors`** | O(V + E) | Recorremos todos los nodos y todas sus aristas para encontrar aquellas que tienen como destino el nodo objetivo. |
| **`esConexo`** | O(V + E) | Ejecutamos un recorrido DFS para verificar si todos los nodos son alcanzables desde un nodo arbitrario. |
| **`vaciar`** | O(1) | Limpiamos el usando el método `clear`, que tiene tiempo constante amortizado. `HashMap` |
| **`tieneCiclos`** | O(V + E) | Realizamos un DFS con la detección de ciclos, visitando nodos y aristas una sola vez en total. |
### 2. **Convenciones**
- = Cantidad de vértices (nodos) en el grafo. **V**
- **E** = Cantidad de aristas (relaciones entre nodos) en el grafo.
- **E/V** = Promedio de aristas por vértice.

Clase Undirected Graph

### Complejidad de los métodos en `UndirectedGraph`

| **Método** | **Complejidad** | **Explicación** |
| --- | --- | --- |
| **`agregarVertice`** | O(1) | Añadir un nodo implica una sola operación en el . `put``HashMap` |
| **`buscarVertice`** | O(V) | Itera sobre las claves en el (vértices del grafo) para buscar un vértice que cumpla el criterio. `HashMap` |
| **`agregarArista`** | O(1) | Accedemos a los conjuntos de adyacencia de ambos vértices (`source` y `target`) y agregamos la arista en cada uno. Operaciones promedio O(1). |
| **`eliminarArista`** | O(1) | Eliminamos una arista de los conjuntos de adyacencia de ambos vértices (`source` y `target`) usando `remove`, con tiempo O(1) por vértice. |
| **`removerVertice`** | O(E/V) + O(V) = O(E) | Eliminamos todas las aristas asociadas al vértice, para lo cual iteramos sus adyacencias (**O(E/V)** promedio por vértice) y luego, eliminamos el nodo (**O(1)**). |
| **`vertices`** | O(1) | Obtenemos las claves del usando `keySet`, que tiene tiempo O(1). `HashMap` |
| **`aristas`** | O(E) | Iteramos por todas las adyacencias de los vértices en el , recopilando todas las aristas únicas en un . `HashMap``HashSet` |
| **`existeArista`** | O(E/V) | Obtenemos el conjunto de adyacencias del nodo fuente y revisamos si la arista con el nodo destino existe en ese conjunto. |
| **`obtenerArista`** | O(E/V) | Similar a , buscamos directamente la arista en el conjunto de adyacencias del nodo fuente. `existeArista` |
| **`adyacencias`** | O(1) | Obtenemos todas las aristas asociadas a un vértice con el método del , que tiene tiempo O(1). `get``HashMap` |
| **`successors/predecessors`** | No aplicable | En un grafo no dirigido, estos métodos no son relevantes, ya que no existe una relación de dirección entre los nodos. |
| **`esConexo`** | O(V + E) | Usamos DFS para recorrer el grafo desde un nodo inicial y verificamos si todos los nodos son alcanzables. Visitamos cada nodo y arista una vez. |
| **`vaciar`** | O(1) | Limpiamos el con el método `clear`, que tiene tiempo constante amortizado O(1). `HashMap` |
| **`tieneCiclos`** | O(V + E) | DFS con detección de ciclos. Visitamos cada vértice y cada arista una sola vez, por lo que la complejidad sigue siendo O(V + E). |
### Términos utilizados
1. **`V`**: Número de vértices (nodos) en el grafo.
2. **`E`**: Número de aristas en el grafo.
3. **`E/V`**: Promedio de aristas por vértice.

Ambas implementaciones fueron realizadas mediante lista de adyacencia.