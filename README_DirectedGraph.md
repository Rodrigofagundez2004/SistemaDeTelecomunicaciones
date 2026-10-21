# Cambios en `DirectedGraph` y sus tests

Este documento describe las mejoras aplicadas a:

- `src/main/java/ucu/edu/aed/tda/grafo/Impl/DirectedGraph.java`
- `src/test/java/TestsHerramientas/DirectedGraphTest.java`

Para cada cambio se explica **qué problema había**, **qué se hizo** y **por qué**.

**Resultado:** antes había 15 tests y fallaba 1 (`testEsConexo`). Ahora hay 42 tests y pasan todos (`mvn test`).

---

## Índice

**Implementación**
1. [`esConexo` era incorrecto y no determinista](#1-esconexo-era-incorrecto-y-no-determinista)
2. [Nueva estructura interna: salientes + entrantes](#2-nueva-estructura-interna-salientes--entrantes)
3. [`vertices()` exponía el estado interno](#3-vertices-exponía-el-estado-interno)
4. [Tipos crudos (raw types)](#4-tipos-crudos-raw-types)
5. [Aristas duplicadas: comportamiento definido y documentado](#5-aristas-duplicadas-comportamiento-definido-y-documentado)
6. [Validación de `null`](#6-validación-de-null)
7. [Orden de iteración reproducible](#7-orden-de-iteración-reproducible)
8. [Recorridos iterativos en lugar de recursivos](#8-recorridos-iterativos-en-lugar-de-recursivos)
9. [Código duplicado y `cantidadDeAristas`](#9-código-duplicado-y-cantidaddearistas)

**Tests**

10. [Reestructuración de los tests](#10-reestructuración-de-los-tests)
11. [Casos nuevos que se prueban](#11-casos-nuevos-que-se-prueban)

**Pendientes**

12. [Cambios recomendados que no se aplicaron](#12-cambios-recomendados-que-no-se-aplicaron)

---

## Implementación

### 1. `esConexo` era incorrecto y no determinista

**Problema.** La versión anterior hacía un DFS desde un vértice "arbitrario" y comprobaba si llegaba a todos:

```java
V start = adjacencyList.keySet().iterator().next();
dfs(start, visited);
return visited.size() == adjacencyList.size();
```

En un grafo **dirigido** lo que se alcanza depende del vértice de inicio. Con una sola arista `A -> B`:

- si el DFS arranca en `A`, llega a `A` y `B` y responde `true`;
- si arranca en `B`, solo llega a `B` y responde `false`.

Como el inicio salía de un `HashMap`, el resultado dependía del hash de los vértices: el mismo grafo podía dar respuestas distintas según los datos. Por eso fallaba `testEsConexo`: esperaba `false` y obtenía `true`.

**Qué se hizo.** Se definió `esConexo` como **fuertemente conexo**: desde cualquier vértice se llega a cualquier otro. Para comprobarlo alcanza con elegir un vértice `s` y verificar dos cosas:

1. desde `s` se alcanzan todos los vértices (DFS sobre el grafo);
2. todos los vértices alcanzan a `s` (DFS sobre el **grafo transpuesto**, con las aristas invertidas).

Si ambas se cumplen, cualquier par `u, v` se conecta pasando por `s` (`u -> s -> v`).

```java
public boolean esConexo() {
    if (salientes.isEmpty()) return true;
    V start = salientes.keySet().iterator().next();
    return alcanzables(start, false).size() == salientes.size()
        && alcanzables(start, true).size()  == salientes.size();
}
```

El método privado `alcanzables(start, transpuesto)` recorre las aristas salientes (`transpuesto = false`) o las entrantes (`transpuesto = true`). Como se exigen ambos sentidos, el resultado ya no depende de qué vértice se elija como inicio.

**Complejidad:** O(V + E), dos recorridos.

> Si para el problema de telecomunicaciones se necesita la conexión **débil** (conexo ignorando las direcciones), se puede agregar un método aparte que recorra salientes y entrantes a la vez en un solo DFS.

---

### 2. Nueva estructura interna: salientes + entrantes

**Problema.** La estructura era `Map<V, Set<DirectedEdge<V, D>>>`, una lista de aristas salientes por vértice. Eso traía tres consecuencias:

- `predecessors(v)` tenía que recorrer **todas** las aristas del grafo para ver cuáles apuntaban a `v`: O(V + E).
- `removerVertice(v)` también recorría todas las aristas para borrar las entrantes: O(V + E).
- `existeArista`/`obtenerArista` recorrían linealmente las aristas salientes del origen.

**Qué se hizo.** Ahora hay dos mapas que se mantienen sincronizados:

```java
// vértice origen -> (vértice destino -> arista)
private final Map<V, Map<V, DirectedEdge<V, D>>> salientes;
// vértice destino -> vértices origen de sus aristas entrantes
private final Map<V, Set<V>> entrantes;
```

| Operación                         | Antes       | Ahora                          |
|-----------------------------------|-------------|--------------------------------|
| `obtenerArista` / `existeArista`* | O(grado)    | O(1)                           |
| `predecessors`                    | O(V + E)    | O(grado de entrada)            |
| `removerVertice`*                 | O(V + E)    | O(grado entrada + grado salida)|
| `eliminarArista`*                 | O(grado)    | O(1)                           |

\* Sin contar `buscarVertice` (ver la sección 12).

Cada operación que modifica el grafo actualiza los dos mapas:

- `agregarArista(u, v)` → `salientes[u].put(v, arista)` y `entrantes[v].add(u)`.
- `eliminarArista(u, v)` → `salientes[u].remove(v)` y `entrantes[v].remove(u)`.
- `removerVertice(v)` → para cada predecesor `p`, `salientes[p].remove(v)`; para cada sucesor `s`, `entrantes[s].remove(v)`; después borra `v` de ambos mapas.
- `vaciar()` → limpia los dos mapas.

**Costo:** algo más de memoria (un `Set<V>` extra por vértice) a cambio de operaciones mucho más rápidas, sobre todo `predecessors`, `gradoDeEntrada` y la eliminación de vértices. Estas últimas son las que se usan al simular fallas de nodos (resiliencia).

---

### 3. `vertices()` exponía el estado interno

**Problema.** `vertices()` devolvía `adjacencyList.keySet()`, que es una **vista modificable** del mapa interno. Desde afuera se podía hacer:

```java
grafo.vertices().remove("A");
```

Eso borraba el vértice `A` **sin** borrar las aristas que apuntaban a él, y el grafo quedaba inconsistente: aristas hacia un vértice que ya no existe. La propia interfaz `IGraph` dice *"preferible devolver vista inmodificable"*.

**Qué se hizo.**

- `vertices()` devuelve `Collections.unmodifiableSet(salientes.keySet())`. Sigue siendo una vista, sin copia, pero cualquier intento de modificarla lanza `UnsupportedOperationException`.
- `aristas()`, `successors()` y `predecessors()` devuelven copias inmodificables. Así nadie puede creer que modifica el grafo cuando en realidad modifica una copia.

---

### 4. Tipos crudos (raw types)

**Problema.** Varias firmas habían perdido los genéricos:

```java
public V buscarVertice(Comparable criteria)
public Set successors(Comparable criteria)
public Set predecessors(Comparable criteria)
```

Con tipos crudos el compilador no puede verificar tipos: emite warnings de operaciones no verificadas (*unchecked*) y permite errores que recién aparecen en ejecución, como un `ClassCastException`.

**Qué se hizo.** Todas las firmas respetan la interfaz:

```java
public V buscarVertice(Comparable<V> criteria)
public Set<V> successors(Comparable<V> criteria)
public Set<V> predecessors(Comparable<V> criteria)
```

`DirectedGraph` ya no genera warnings *unchecked* al compilar.

---

### 5. Aristas duplicadas: comportamiento definido y documentado

**Problema.** `DirectedEdge.equals` compara solo origen y destino, no el dato. Antes, `agregarArista("A","B",5)` seguido de `agregarArista("A","B",9)` devolvía `false` y la arista seguía con peso 5. Funcionaba así por accidente (el `HashSet` rechazaba el duplicado), sin que estuviera documentado.

**Qué se hizo.** Ahora es una decisión explícita:

- Se admite **a lo sumo una arista `u -> v`** (no hay aristas paralelas).
- Si ya existe, `agregarArista` devuelve `false` y **se conserva el dato original**.
- Para cambiar el dato (por ejemplo, actualizar la latencia de un enlace) hay que hacer `eliminarArista` y después `agregarArista`.

Está documentado en el Javadoc de la clase y del método, y hay un test que lo verifica (`aristaDuplicada`).

> `A -> B` y `B -> A` siguen siendo aristas **distintas** (test `sentidoImporta`).

---

### 6. Validación de `null`

**Problema.** `HashMap` acepta claves `null`, así que `agregarVertice(null)` funcionaba. Pero después `agregarArista(null, ...)` explotaba con un `NullPointerException` dentro del constructor de `DirectedEdge`, lejos del error real.

**Qué se hizo.**

- `agregarVertice(null)` lanza `NullPointerException` con un mensaje claro: *"El vértice no puede ser null"*. Es preferible fallar enseguida a que el error aparezca más tarde en otro lado.
- `agregarArista` con origen o destino `null` devuelve `false`, igual que con cualquier vértice inexistente.
- `buscarVertice(null)` devuelve `null` en lugar de lanzar NPE.

---

### 7. Orden de iteración reproducible

**Problema.** Con `HashMap`/`HashSet`, el orden de `vertices()`, `adyacencias()` y de los recorridos de `DirectedGraphAlgorithms` (BEA, BPF, clasificación topológica) dependía del hash de los vértices. Eso tenía dos consecuencias:

- no se podían escribir tests de recorridos con un orden esperado concreto;
- la salida de la aplicación podía cambiar al cambiar los datos de entrada, aunque el grafo fuera "el mismo".

**Qué se hizo.** Se reemplazaron por `LinkedHashMap`/`LinkedHashSet`, que tienen el mismo costo O(1) pero iteran en **orden de inserción**. Por ejemplo, después de `agregarVertices(List.of("A","B","C"))`, `vertices()` itera exactamente `A, B, C` (test `ordenDeInsercion`). Lo mismo pasa con `adyacencias` (test `adyacencias`).

---

### 8. Recorridos iterativos en lugar de recursivos

**Problema.** `esConexo` y `tieneCiclos` usaban un DFS **recursivo**. Cada vértice del camino ocupa un marco de la pila de llamadas de Java. Con una cadena de unos miles de vértices (`0 -> 1 -> 2 -> ... -> n`) aparece un `StackOverflowError`.

**Qué se hizo.**

- `alcanzables` (usado por `esConexo`) es un DFS con una pila explícita (`ArrayDeque`).
- `tieneCiclos` es un DFS iterativo **de tres estados**:
  - *no visitado*: no está en ningún conjunto;
  - *en proceso* (`enProceso`): está en el camino actual de la pila;
  - *terminado* (`terminados`): ya se exploraron todos sus sucesores.

  Si se encuentra una arista hacia un vértice *en proceso* (arista hacia atrás), hay ciclo. Un vértice *terminado* **no** indica ciclo: es justamente el caso del diamante `A->B, A->C, B->D, C->D`, donde `D` se alcanza por dos caminos sin que haya ciclo. Para saber qué sucesor toca explorar, cada entrada de la pila guarda el vértice junto con el iterador de sus sucesores pendientes, que es lo que en la versión recursiva guardaba la pila de llamadas.

El algoritmo es el mismo que el de la versión recursiva (que era correcto); cambia solo la forma de implementarlo. El test `grafoGrandeSinStackOverflow` lo prueba con una cadena de **100.000 vértices**.

---

### 9. Código duplicado y `cantidadDeAristas`

- `existeArista` repetía el código de `obtenerArista`. Ahora es `return obtenerArista(s, t) != null;`.
- `cantidadDeAristas()` usaba el método por defecto de la interfaz, que llama a `aristas().size()` y arma un `Set` con todas las aristas solo para contarlas. Ahora se sobrescribe y suma el tamaño de cada mapa de salientes, sin crear colecciones intermedias.

---

## Tests

### 10. Reestructuración de los tests

| Antes | Ahora | Por qué |
|---|---|---|
| Cada test creaba el grafo y agregaba vértices a mano | `@BeforeEach` crea un grafo con `A`, `B`, `C` | Menos repetición; cada test muestra solo lo que lo distingue |
| 15 tests sueltos | Agrupados con `@Nested` por funcionalidad (`Vertices`, `AgregarArista`, `EliminarArista`, `RemoverVertice`, `ConsultasAristas`, `SucesoresPredecesores`, `EsConexo`, `TieneCiclos`) | El reporte se lee como una especificación |
| Nombres como `testEsConexo` | `@DisplayName` con la regla que se prueba ("un camino sin vuelta no es fuertemente conexo...") | Si un test falla, el mensaje dice qué regla se rompió |
| Se verificaba casi solo el `boolean` devuelto | Se verifica también el **estado** del grafo después de la operación | Un método puede devolver `true` y aun así dejar el grafo mal |
| `contains(...)` y `size()` | `assertEquals(Set.of("B","C"), ...)` con el conjunto exacto | `contains` pasaba aunque sobraran elementos |
| Comentario `// No dirigido` en `existeArista` | Comentario corregido (`// el grafo es dirigido`) | El comentario decía lo opuesto a lo que se probaba |

Ejemplo de la diferencia con `removerVertice`:

```java
// Antes: solo el valor de retorno
assertTrue(graph.removerVertice("A"));

// Ahora: también el estado del grafo
graph.agregarArista("A", "B", 1);
graph.agregarArista("B", "C", 2);
graph.agregarArista("C", "A", 3);
assertTrue(graph.removerVertice("B"));
assertFalse(graph.existeVertice("B"));
assertEquals(Set.of(), graph.successors("A"));   // se borró la arista entrante A->B
assertEquals(Set.of(), graph.predecessors("C")); // se borró la arista saliente B->C
assertEquals(1, graph.cantidadDeAristas());
assertTrue(graph.existeArista("C", "A"));        // lo que no tocaba a B sigue igual
```

### 11. Casos nuevos que se prueban

Cubren lo que pide `src/test/java/README.md`: grafo vacío, un vértice, conexos y no conexos, con y sin ciclos, y vértices inexistentes o sin aristas.

**Vértices**
- duplicados, `null`, búsqueda con `construirComparable`, orden de inserción, `vertices()` inmodificable.

**Aristas**
- vértices inexistentes o `null`, arista duplicada (conserva el dato original), `A->B` distinta de `B->A`, ciclos de un vértice (`A->A`, una arista de un vértice a sí mismo).
- `eliminarArista`: actualiza sucesores y predecesores, borra solo en el sentido indicado, devuelve `false` si la arista no existe entre vértices existentes.
- `obtenerArista`: verifica origen, destino, **dato** y `directed()`; devuelve `null` si no existe.
- `adyacencias`: contenido y orden; vacía para un vértice sin aristas o inexistente.

**Vértices y sus aristas**
- `removerVertice`: limpia entrantes y salientes, funciona con ciclos de un vértice, el vértice se puede volver a agregar "limpio".
- `successors`/`predecessors`: conjuntos exactos, vértice inexistente, copias inmodificables.
- `gradoDeEntrada`/`gradoDeSalida` (antes no se probaban).

**`esConexo`**
- grafo vacío, un vértice, camino sin vuelta (`false`), todos apuntan al primero (`false`, es el caso que la versión anterior respondía mal), ciclo completo (`true`), vértice aislado (`false`).

**`tieneCiclos`** (antes solo había un caso positivo, que un `return true;` fijo hubiera pasado)
- sin aristas, grafo vacío, ciclo de dos vértices, ciclo de un vértice (`B->B`), **diamante** (DAG, `false`), ciclo en una componente que no es la primera, romper un ciclo al eliminar una arista.

**Otros**
- `vaciar`: borra vértices **y** aristas, y el grafo sigue siendo usable.
- Grafo de 100.000 vértices: `esConexo` y `tieneCiclos` no producen `StackOverflowError`.

---

## 12. Cambios recomendados que no se aplicaron

Estos cambios no se aplicaron porque afectan al resto del proyecto o al repositorio y conviene decidirlos en equipo:

- **`buscarVertice` es O(V).** La interfaz recibe un `Comparable<V>` como criterio, así que hay que comparar contra cada vértice, y casi todas las operaciones llaman a `buscarVertice` primero. Un "atajo" con `containsKey(criteria)` solo sería correcto si `compareTo` es consistente con `equals`, y la interfaz no lo garantiza. Para grafos grandes convendría agregar a la interfaz sobrecargas que reciban el vértice `V` directamente.
- **Paquete `Impl` en mayúscula.** Por convención de Java los paquetes van en minúscula (`impl`). Renombrarlo cambia los imports de todo el proyecto y, en Windows (sistema de archivos que no distingue mayúsculas), git necesita un renombrado en dos pasos.
- **Paquete de los tests.** La convención de Maven es que el test esté en el mismo paquete que la clase (`ucu.edu.aed.tda.grafo.Impl`). Se dejó en `TestsHerramientas` para no cambiar la organización que eligió el equipo.
- **`target/` está versionado en git.** Son archivos compilados que cambian en cada build y generan conflictos. Se recomienda crear un `.gitignore` con `target/` y ejecutar `git rm -r --cached target`.
- **`UndirectedGraph`** tiene problemas parecidos (tipos crudos y warnings *unchecked*; `tieneCiclos` arranca solo desde el primer vértice, así que no ve ciclos en otras componentes y falla con el grafo vacío). Conviene revisarlo con el mismo criterio.
