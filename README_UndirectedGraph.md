# Cambios en `UndirectedGraph` y sus tests

Este documento describe las mejoras aplicadas a:

- `src/main/java/ucu/edu/aed/tda/grafo/Impl/UndirectedGraph.java`
- `src/test/java/TestsHerramientas/UndirectedGraphTest.java` (**nuevo**: antes no había ningún test de esta clase)

Para cada cambio se explica **qué problema había**, **qué se hizo** y **por qué**. Los cambios siguen los mismos criterios que se aplicaron a `DirectedGraph` (ver `README_DirectedGraph.md`).

**Resultado:**
- Se crearon 35 tests para `UndirectedGraph`. Con la implementación nueva pasan todos.
- **Contra la implementación original fallan 15 de esos 35**: los tests detectan cada uno de los errores descritos abajo.
- La suite completa del proyecto tiene 77 tests y pasan todos (`mvn test`).

---

## Índice

**Errores corregidos**
1. [`tieneCiclos` fallaba con el grafo vacío y solo revisaba la primera componente](#1-tieneciclos-fallaba-con-el-grafo-vacío-y-solo-revisaba-la-primera-componente)
2. [`adyacencias` y `obtenerArista` no respetaban el contrato de `source()`](#2-adyacencias-y-obtenerarista-no-respetaban-el-contrato-de-source)
3. [Los ciclos de un vértice rompían `agregarArista`, `eliminarArista` y `removerVertice`](#3-los-ciclos-de-un-vértice-rompían-agregararista-eliminararista-y-removervertice)
4. [`vertices()` exponía el estado interno](#4-vertices-exponía-el-estado-interno)
5. [Validación de `null`](#5-validación-de-null)
6. [Recursión: `StackOverflowError` en grafos grandes](#6-recursión-stackoverflowerror-en-grafos-grandes)

**Mejoras de calidad**

7. [Nueva estructura interna: mapa vecino -> arista](#7-nueva-estructura-interna-mapa-vecino---arista)
8. [Tipos crudos (raw types)](#8-tipos-crudos-raw-types)
9. [Orden de iteración reproducible](#9-orden-de-iteración-reproducible)
10. [Aristas duplicadas: comportamiento documentado](#10-aristas-duplicadas-comportamiento-documentado)
11. [Código duplicado y `cantidadDeAristas`](#11-código-duplicado-y-cantidaddearistas)

**Tests**

12. [`UndirectedGraphTest`: estructura y casos](#12-undirectedgraphtest-estructura-y-casos)

**Pendientes**

13. [Cambios recomendados que no se aplicaron](#13-cambios-recomendados-que-no-se-aplicaron)

---

## Errores corregidos

Todos estos errores se confirmaron ejecutando la implementación original.

| # | Caso | Antes | Ahora |
|---|---|---|---|
| 1 | `tieneCiclos()` con el grafo vacío | `NoSuchElementException` | `false` |
| 1 | Ciclo B–C–D con A aislado | `false` | `true` |
| 2 | `adyacencias("B")` después de agregar {A,B} | `source() = A` | `source() = B` |
| 3 | `agregarArista("A","A")` | `false`, aunque la agregaba | `true` |
| 3 | `eliminarArista("A","A")` | `false`, aunque la eliminaba | `true` |
| 3 | `removerVertice` de un vértice con un ciclo de un vértice | `ConcurrentModificationException` | `true` |
| 4 | `vertices().remove("B")` | dejaba la arista {A,B} "colgada" | `UnsupportedOperationException` |
| 5 | `agregarVertice(null)` | `true` (y luego NPE en otro lado) | `NullPointerException` inmediato |
| 5 | `buscarVertice(null)` | `NullPointerException` | `null` |
| 6 | `esConexo()` en una cadena de 100.000 vértices | `StackOverflowError` | `true` |

### 1. `tieneCiclos` fallaba con el grafo vacío y solo revisaba la primera componente

**Problema.**
```java
return tieneCiclosDFS(null, adjacencyList.keySet().iterator().next(), visited);
```
- Si el grafo está vacío, `next()` lanza `NoSuchElementException`.
- El DFS arranca **solo desde el primer vértice**. Si el ciclo está en otra componente conexa, no lo ve. Ejemplo: con `A` aislado y el triángulo `B–C–D`, respondía `false`.

**Qué se hizo.** El DFS se lanza desde **cada vértice todavía no visitado**, así recorre todas las componentes. Con el grafo vacío el ciclo no se ejecuta y devuelve `false`.

La regla para detectar el ciclo no cambió, porque era correcta: en un grafo no dirigido, si desde un vértice `u` se encuentra un vecino **ya visitado que no es el padre** de `u` (el vértice desde el que se llegó a `u`), hay un ciclo. Se excluye el padre porque la arista `{u, padre}` es la misma por la que se llegó, y recorrerla de vuelta no es un ciclo. El test `unaArista` verifica que `A–B` sola **no** cuente como ciclo.

Un ciclo de un vértice (arista `{u, u}`) cuenta como ciclo: el vecino es `u` mismo, que ya está visitado y no es su propio padre.

### 2. `adyacencias` y `obtenerArista` no respetaban el contrato de `source()`

**Problema.** La interfaz `IGraph` dice:

> *"En caso de que sea un grafo no dirigido, el método `source()` referencia al vértice `verticeCriteria`"*

Pero la arista `{A, B}` se guardaba una sola vez con `source = A`, y `adyacencias("B")` la devolvía tal cual: `source() = A`, `target() = B`. Quien hiciera lo natural, usar `e.target()` como "el vecino de B", obtenía **B mismo**.

Hoy `DirectedGraphAlgorithms` lo esquivaba con un método auxiliar `vecino(e, actual)`. Pero cualquier algoritmo nuevo (Prim, Kruskal, Dijkstra) o código del sistema de telecomunicaciones que confíe en el contrato iba a **recorrer mal el grafo, sin lanzar ningún error**. Por eso era el error más peligroso.

**Qué se hizo.** Se agregó un método privado `orientar(edge, origen)` que devuelve la arista con `source() == origen`. Si ya está así orientada devuelve el mismo objeto; si no, crea un `UndirectedEdge(origen, otro, dato)` con el mismo dato.

- `adyacencias(v)`: todas las aristas tienen `source() == v` y `target() ==` el vecino.
- `obtenerArista(u, v)`: devuelve la arista con `source() == u`.

Como el `equals` de `UndirectedEdge` es simétrico, la arista orientada sigue siendo **igual** a la guardada (`obtenerArista("A","B").equals(obtenerArista("B","A"))` es `true`), así que no se rompe nada que compare aristas.

### 3. Los ciclos de un vértice rompían `agregarArista`, `eliminarArista` y `removerVertice`

**Problema.** Un ciclo de un vértice (una arista `{A, A}` de un vértice a sí mismo) tiene los dos extremos iguales, así que las "dos" listas de adyacencia son **la misma**:

- **`agregarArista`**: el primer `add` devolvía `true` y el segundo `false` (ya estaba). Con `addedToSource && addedToTarget` el método respondía `false` **aunque la arista sí se había agregado**.
- **`eliminarArista`**: pasaba lo mismo con los dos `remove`. Respondía `false` aunque la arista sí se había eliminado.
- **`removerVertice`**: recorría `adjacencyList.get(vertex)` y dentro del ciclo hacía `adjacencyList.get(neighbor).remove(edge)`. Con un ciclo de un vértice, `neighbor == vertex`, así que **modificaba el conjunto que estaba recorriendo** y lanzaba `ConcurrentModificationException`.

**Qué se hizo.** Se decidió **permitir ciclos de un vértice**, igual que en `DirectedGraph`, y manejarlos bien:

- `agregarArista` primero comprueba si la arista ya existe y después hace los dos `put`. En un ciclo de un vértice el segundo `put` pisa la misma entrada, sin efecto, y el resultado es `true`.
- `eliminarArista` decide el resultado con el **primer** `remove`. El segundo, en un ciclo de un vértice, simplemente no encuentra nada.
- `removerVertice` salta el caso `vecino == vertex` (el ciclo de un vértice se borra junto con el vértice) y nunca modifica el mapa que está recorriendo.

### 4. `vertices()` exponía el estado interno

**Problema.** `vertices()` devolvía `adjacencyList.keySet()`, una vista **modificable**. `grafo.vertices().remove("B")` borraba `B` pero la arista `{A, B}` seguía en las adyacencias de `A`: una arista hacia un vértice inexistente.

**Qué se hizo.** `vertices()` devuelve `Collections.unmodifiableSet(...)`, y `aristas()` devuelve una copia inmodificable. La interfaz `IGraph` ya lo recomendaba.

### 5. Validación de `null`

**Problema.** `agregarVertice(null)` devolvía `true` (`HashMap` acepta claves `null`), y después `agregarArista` explotaba con NPE dentro de `UndirectedEdge`. `buscarVertice(null)` también lanzaba NPE.

**Qué se hizo.**
- `agregarVertice(null)` lanza `NullPointerException` con el mensaje *"El vértice no puede ser null"*, en el momento del error.
- `agregarArista` con un extremo `null` devuelve `false`, igual que con un vértice inexistente.
- `buscarVertice(null)` devuelve `null`.

### 6. Recursión: `StackOverflowError` en grafos grandes

**Problema.** `esConexo` (`dfs`) y `tieneCiclos` (`tieneCiclosDFS`) eran recursivos, y en una cadena de 100.000 vértices desbordaban la pila de Java.

**Qué se hizo.**
- `esConexo`: DFS con una pila explícita (`ArrayDeque`).
- `tieneCiclos`: DFS iterativo. Cada elemento de la pila es un `record Marco(vertice, padre, pendientes)`, donde `pendientes` es el iterador de los vecinos que faltan explorar. Es exactamente la información que en la versión recursiva guardaba cada llamada: el vértice actual, el parámetro `parent` y el punto del `for` donde iba.

El test `grafoGrandeSinStackOverflow` lo verifica con 100.000 vértices.

---

## Mejoras de calidad

### 7. Nueva estructura interna: mapa vecino -> arista

**Antes:** `Map<V, Set<UndirectedEdge<V, D>>>`. Para saber si existía `{u, v}` había que recorrer todas las aristas de `u`, con esta condición confusa:
```java
if (edge.target().equals(target) || edge.source().equals(target))
```
Funcionaba solo porque `u` ya estaba en esa lista de adyacencia.

**Ahora:** `Map<V, Map<V, UndirectedEdge<V, D>>>`: para cada vértice, un mapa **vecino -> arista**. Una arista `{u, v}` se guarda (el mismo objeto) en `adyacentes[u][v]` y en `adyacentes[v][u]`. Un ciclo de un vértice `{u, u}` queda en una única entrada, `adyacentes[u][u]`.

| Operación                                           | Antes    | Ahora      |
|-----------------------------------------------------|----------|------------|
| `existeArista` / `obtenerArista` / `eliminarArista`* | O(grado) | O(1)       |
| `removerVertice`*                                   | O(grado) | O(grado)   |

\* Sin contar `buscarVertice` (ver la sección 13).

Además, `eliminarArista` ya no necesita crear una arista "falsa" con dato `null` solo para buscarla con `remove(edge)`. Ese truco dependía de que `UndirectedEdge.equals` ignorara el dato.

### 8. Tipos crudos (raw types)

`buscarVertice`, `eliminarArista`, `removerVertice`, `existeArista`, `obtenerArista` y `adyacencias` recibían `Comparable` sin tipo. Ahora todos usan `Comparable<V>`, como la interfaz. **El proyecto completo compila sin warnings *unchecked*.** Antes, `UndirectedGraph` era la única clase que todavía los generaba.

### 9. Orden de iteración reproducible

Se reemplazaron `HashMap`/`HashSet` por `LinkedHashMap`/`LinkedHashSet`. `vertices()`, `adyacencias()` y los recorridos de `UndirectedGraphAlgorithms` (`bea`, `bep`) iteran en **orden de inserción**. Así se pueden escribir tests de recorridos con un orden esperado y la salida no cambia con los hashes.

### 10. Aristas duplicadas: comportamiento documentado

Como el grafo es no dirigido, `{A,B}` y `{B,A}` son **la misma arista**. Si ya existe, `agregarArista` (en cualquier orden) devuelve `false` y **conserva el dato original**. Para cambiar el dato hay que eliminarla y volver a agregarla. Es el mismo criterio que en `DirectedGraph`, ahora documentado en el Javadoc y verificado por el test `aristaDuplicadaEnOrdenInverso`.

### 11. Código duplicado y `cantidadDeAristas`

- `existeArista` ahora es `return obtenerArista(s, t) != null;`. Antes repetía el mismo ciclo.
- `cantidadDeAristas()` se sobrescribe para contar sin crear el `Set` de `aristas()`. Cada arista común aparece en sus dos extremos y cada ciclo de un vértice en una sola entrada, así que el total es `aristasComunes / 2 + ciclosDeUnVertice`.

---

## Tests

### 12. `UndirectedGraphTest`: estructura y casos

Usa la misma estructura que `DirectedGraphTest`: un `@BeforeEach` con los vértices `A`, `B`, `C`, grupos `@Nested` por funcionalidad y `@DisplayName` con la regla que se prueba. Los tests verifican **el estado del grafo**, no solo el valor devuelto. Un método auxiliar `vecinos(v)` obtiene los vecinos de `v` a partir de `adyacencias(v)`, así que también comprueba el contrato de `target()`.

**Casos propios de un grafo no dirigido**
- `{A,B}` se ve desde los dos extremos: `existeArista` en ambos sentidos y `vecinos` de ambos.
- Agregar `{B,A}` cuando existe `{A,B}` se rechaza y se conserva el dato.
- `eliminarArista("B","A")` borra la arista creada como `{A,B}`, de **ambos** extremos.
- `adyacencias(v)` y `obtenerArista(u, v)` respetan el contrato de `source()` (`adyacenciasOrientadas`, `obtenerArista`).
- `aristas()` devuelve cada arista una sola vez.

**Ciclos de un vértice**
- Agregar, eliminar y remover un vértice con un ciclo de un vértice (antes: `false`, `false` y `ConcurrentModificationException`).

**`esConexo`**
- Vacío, un vértice, camino con aristas agregadas "hacia atrás" (`true`), vértice aislado (`false`), dos componentes (`false`).

**`tieneCiclos`**
- Vacío (antes `NoSuchElementException`), sin aristas, **una sola arista** (volver al padre no es un ciclo), árbol, triángulo, ciclo de un vértice, ciclo en una componente que no es la primera (antes `false`) y romper un ciclo eliminando una arista.

**Robustez**
- `null`, `vertices()` y `aristas()` inmodificables, vértice inexistente, volver a agregar un vértice eliminado, `vaciar` y un grafo de 100.000 vértices.

**Validación de los tests.** Se ejecutaron los 35 tests contra la implementación **original** y fallan 15: uno o más por cada error de la tabla de arriba. Eso confirma que los tests detectan esos errores y no pasan "de casualidad".

---

## 13. Cambios recomendados que no se aplicaron

- **`buscarVertice` sigue siendo O(V).** La interfaz recibe un `Comparable<V>` como criterio y obliga a comparar contra cada vértice. Es la misma limitación que en `DirectedGraph`.
- **Código compartido con `DirectedGraph`.** `buscarVertice`, `vertices`, `vaciar`, `existeArista` y la validación de vértices son casi idénticos en las dos clases. Se podría extraer una clase base abstracta (`AbstractGraph<V, D>`). No se hizo para no cambiar la jerarquía de clases del proyecto base.
- **`DirectedGraphAlgorithms.vecino(e, actual)`.** Ese auxiliar existía para esquivar el error 2. Ahora que `adyacencias` respeta el contrato, `vecino` ya no hace falta: alcanza con `e.target()`. Se dejó porque sigue funcionando y no es parte de esta clase.
- **`IUndirectedGraph` está vacía.** Se le podría agregar un `default int grado(Comparable<V> v)` (cantidad de vecinos), útil por ejemplo en resiliencia para detectar nodos críticos.
- Las mismas recomendaciones de `README_DirectedGraph.md` sobre el paquete `Impl`, el paquete de los tests y `target/` en git.
