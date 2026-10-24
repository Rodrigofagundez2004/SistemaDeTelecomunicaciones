# Cambios en el modelado (revisión 2)

Resumen para el equipo de qué cambió en [`modelado.md`](modelado.md) y **por qué**. Cada punto tiene: qué decía antes, qué problema tenía (con un ejemplo), qué cambiamos y una frase para explicarlo rápido.

No se tocó código: son decisiones de diseño a tener en cuenta cuando implementemos. El formato de los CSV queda como estaba en nuestro modelado.

---

## Cambios que modifican el diseño

### 1. Ruta alternativa: ahora se ocultan también los enlaces de la ruta principal

- **Antes:** la ruta alternativa se calculaba ocultando solo los **nodos intermedios** de la ruta principal.
- **Problema:** si la ruta principal es un enlace directo, por ejemplo `A → B`, no tiene nodos intermedios. No se oculta nada, Dijkstra vuelve a encontrar `A → B` y la "alternativa" es la misma ruta.
- **Cambio:** se ocultan los nodos intermedios **y** los enlaces de la ruta principal. Así la alternativa no comparte ni equipos ni cables con la principal.
- **Limitación que declaramos en el informe:** calcular una ruta y después otra sin la primera no siempre encuentra dos rutas disjuntas aunque existan, porque la primera puede pasar justo por donde tenía que pasar la segunda. El algoritmo exacto es Suurballe; no lo implementamos, pero lo mencionamos para mostrar que conocemos el límite.
- **En una frase:** *"Si solo ocultábamos nodos, una ruta directa A→B se devolvía a sí misma como alternativa."*

### 2. A lo sumo un enlace entre dos nodos (y el cargador lo controla)

- **Antes:** no se decía nada sobre dos enlaces entre los mismos dos nodos.
- **Problema:** nuestra biblioteca **no admite aristas paralelas**. Si agregamos un segundo enlace entre A y B, `agregarArista` devuelve `false` y el enlace **desaparece sin aviso**. Justo en una red de telecomunicaciones, dos cables entre los mismos equipos es un caso típico de redundancia, y lo perderíamos sin darnos cuenta.
- **Cambio:** queda como supuesto que hay a lo sumo un enlace por par, y `CargadorRed` / `Red.agregarEnlace` lanzan `IllegalArgumentException` si encuentran uno repetido (indicando la línea del CSV). Si alguien quiere modelar un enlace redundante, agrega un nodo intermedio (un `SWITCH`) y quedan dos caminos distintos.
- **En una frase:** *"La biblioteca descartaba en silencio el segundo cable entre dos equipos; ahora es un error explícito."*

### 3. Las simulaciones de fallas ya no modifican la red

- **Antes:** "¿qué nodos quedan incomunicados si cae X?" se resolvía así: marcar X como caído, hacer el BFS y restaurar X.
- **Problemas:**
  1. Si algo falla en el medio (una excepción), X **queda caído para siempre** y todas las consultas siguientes dan resultados incorrectos.
  2. Cada simulación cambia `version` dos veces (caer y restaurar), así que **invalida el caché** de la tabla de ruteo del Desafío 3 aunque la red en realidad no cambió.
  3. Mezcla dos cosas distintas: una falla real que registra el usuario y una pregunta del tipo "¿qué pasaría si…?".
- **Cambio:** se distinguen dos tipos de falla:
  - **Falla real** (`fallarNodo`, `fallarEnlace`): sí modifica la red, como antes.
  - **Simulación**: usa el método nuevo `red.vistaActivaSin(nodosOcultos, enlacesOcultos)`, que arma una `VistaFiltrada` temporal ocultando X. **No toca la red ni `version`.**
- Lo usan las simulaciones de fallas, la ruta alternativa y el ranking de criticidad. Se agregó a las convenciones: *ningún servicio de consulta modifica la red*.
- **En una frase:** *"Preguntar qué pasaría si cae algo no debería poder romper la red ni el caché."*

### 4. Desafío 3: primero buscar vértices en O(1), después Floyd vs Dijkstra

- **Antes:** la optimización principal era comparar Floyd con V × Dijkstra, y arreglar `adyacencias` quedaba "si da el tiempo".
- **Problema:** con la biblioteca actual, cada llamada a `adyacencias(comparable)` cuesta O(V), porque `buscarVertice` recorre todos los vértices. Por eso **cada Dijkstra ya cuesta O(V²)**, y V Dijkstras cuestan O(V³) o más, lo mismo que Floyd o peor. El experimento iba a mostrar que la "optimización" no mejora nada y no íbamos a poder defender la hipótesis.
- **Cambio:** el orden queda así:
  1. **Optimización 1:** buscar vértices en O(1) cuando el criterio es "igual a este vértice". Las implementaciones sobreescriben `construirComparable` para devolver un comparable que lleva el vértice adentro, y `buscarVertice` lo reconoce y busca directo en el mapa. No cambia la interfaz ni los algoritmos. De paso se reemplaza `indexOf` por un mapa de índices en Floyd. Se mide antes y después.
  2. **Optimización 2:** Floyd vs V × Dijkstra, que ahora sí tiene sentido. `tablaDeRuteoDijkstra()` devuelve también un `IFloydWarshallResult` para que el resto del código no cambie.
  3. El caché queda como extra.
- Ganamos además **dos** comparaciones medibles en lugar de una.
- **En una frase:** *"Sin arreglar la búsqueda de vértices, Dijkstra ya es cuadrático y comparar contra Floyd no muestra nada."*

---

## Cambios de consistencia

### 5. Justificación del cambio de firmas respecto al esqueleto

- **Problema:** el esqueleto de la cátedra tenía `Enlace extends WeightedEdge` y servicios que reciben `IDirectedIGraph<Nodo, Enlace>` y objetos `Nodo`. Nuestro modelado cambia las tres cosas, y no lo decía en ningún lado.
- **Cambio:** se agregó una nota en la sección 4 con los motivos, para copiarla a la sección 3 del informe:
  - un enlace necesita dos pesos (latencia y costo) y latencias distintas por sentido, pero `WeightedEdge` tiene un solo peso;
  - los servicios necesitan la `Red` completa (catálogo, data centers, fallas), no solo un grafo;
  - el usuario identifica los nodos por `id`.
- **En una frase:** *"Nos apartamos del esqueleto a propósito, y el informe tiene que decir por qué."*

### 6. Ahora se usan la capacidad de los DC y la demanda de los clientes

- **Antes:** definíamos `capacidad` de los data centers, consumo por usuario y `demandaEstimadaMbps()`, pero **ningún servicio ni consulta los usaba**. Un corrector lo va a ver como datos de relleno.
- **Cambio:**
  - **Capacidad:** la asignación de clientes sigue siendo al DC de menor RTT, sin repartir carga. Es una simplificación que declaramos como supuesto. Pero se agregó `cargaDataCenters()`, que informa qué DC quedan **sobrecargados**, y eso refuerza la sugerencia del nuevo data center.
  - **Demanda:** la usa la consulta nueva de **enlaces saturados** (ver punto 8).
- **En una frase:** *"Si definimos un dato, alguna funcionalidad lo tiene que usar."*

### 7. Pendientes de la biblioteca que faltaban

Se agregaron a la sección 9:
- **Dijkstra:** la cola de prioridad ordena leyendo el mapa de costos, que se modifica mientras los vértices están en la cola. Cuando baja el costo de un vértice ya encolado, el heap queda mal ordenado y puede devolver los vértices en orden incorrecto. Hoy da bien en los ejemplos chicos, pero no está garantizado. Se arregla encolando pares `(vértice, distancia)`.
- **Tests** de Dijkstra, Floyd, Warshall, excentricidad y centro. No hay ninguno, y por eso se escapó el bug de `getPath`.
- **`VistaFiltrada`** tiene que implementar también `tieneCiclos()` y `esConexo()`, este último como conexidad **fuerte**, igual que `DirectedGraph`, para que la vista se comporte igual que el grafo real.
- La regla "una arista solo se ve si sus dos extremos son visibles" pasó de detalle a **obligatoria, con test**. Si no se cumple, Floyd busca el índice de un vértice oculto, obtiene -1 y explota con `ArrayIndexOutOfBoundsException`.

---

## Sugerencias aplicadas

### 8. Se reemplazaron dos consultas propias

- **Antes:** la consulta 1 era la "tabla de ruteo de un router" y la 2 el "impacto de la caída de un data center".
- **Problema:** las dos se parecen mucho a funcionalidades **obligatorias** (tabla de ruteo y nodos incomunicados ante la falla de un DC). El docente puede no contarlas como consultas propias.
- **Cambio:**
  1. **Ruta de máximo ancho de banda (widest path):** la ruta cuyo enlace más lento es lo más rápido posible. Es un Dijkstra modificado: en lugar de sumar y minimizar, se toma el mínimo del camino y se maximiza. Es un algoritmo distinto, se justifica fácil y usa el ancho de banda.
  2. **Enlaces saturados:** se suma la demanda de cada cliente sobre la ruta hacia su DC y se compara con el ancho de banda de cada enlace. Usa la demanda, que antes no se usaba.
- Las consultas 3, 4 y 5 quedan igual.
- Se sacaron los records `FilaRuteo` e `ImpactoCaida`, que ya no se usan, y se agregaron `AsignacionCdn`, `CargaDataCenter`, `RutaAnchoDeBanda` y `UsoEnlace`.

### 9. Incomunicados: un solo BFS en lugar de uno por data center

- **Antes:** un BFS desde cada DC, O(D·(V+E)).
- **Cambio:** un único BFS que arranca con **todos** los DC activos en la cola. Un nodo está comunicado si lo alcanza cualquier DC, y eso es justo lo que calcula un BFS con varios orígenes, en O(V+E). Se implementa en el servicio, porque el BFS de la biblioteca arranca desde un solo vértice.

### 10. Casos que no estaban definidos

Se agregaron a los supuestos:
- **El nodo que falla no cuenta como incomunicado:** se informa aparte como "caído". Incomunicados son los que siguen funcionando pero no llegan a ningún DC.
- **Clientes sin servicio** (no alcanzan ningún DC activo): no se les asigna DC, se listan aparte y no entran en el promedio de latencia, porque si entraran el promedio daría infinito. En la sugerencia de nuevo DC, conectar clientes sin servicio tiene prioridad.
- **Equipos y enlaces críticos** analizan la red **instalada completa**, sin importar las fallas actuales: responden "qué es un punto único de falla por diseño".

### 11. Detalles menores

- Decía `HashMap`, pero la implementación usa `LinkedHashMap`. Corregido.
- La complejidad O(E log E) de Kruskal **solo vale con union-find**. Quedó aclarado en la tabla y en los pendientes.
- Se agregaron casos borde a las convenciones (cliente sin servicio, enlace repetido en el CSV, ruta principal de un solo enlace) y requisitos al dataset (un DC sobrecargado y un enlace saturado, para poder mostrar las consultas nuevas).
