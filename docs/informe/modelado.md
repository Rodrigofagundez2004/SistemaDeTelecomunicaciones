# Modelado — Sistema de Telecomunicaciones (Equipo 3)

Documento base para arrancar el Desafío 2 y 3. Sirve de referencia común: qué es cada grafo, qué clases hay y qué hace cada una. Las justificaciones de acá van después al informe (secciones 2, 3, 4 y 6).

> Los cambios de las revisiones 2 y 3 y su motivo están explicados en [`cambios-modelado.md`](cambios-modelado.md).

---

## 1. Idea central

Usamos **dos grafos permanentes** sobre la red existente y **uno temporal** para planificar cableado.

| Grafo | Tipo | Vértices | Aristas | Peso | Se usa para |
|---|---|---|---|---|---|
| **Ruteo** | Dirigido | Todos los nodos | 2 arcos por enlace físico (A→B y B→A) | Latencia de ese sentido (ms) | Rutas, tabla de ruteo, CDN, fallas, rutas alternativas |
| **Físico** | No dirigido | Todos los nodos | 1 arista por enlace existente | Costo (no se usa para conectividad) | Equipos y enlaces críticos |
| **Planificación** | No dirigido | Red existente + oficinas nuevas | Enlaces existentes (costo 0) + conexiones posibles (su costo) | Costo de instalación | Cableado de menor costo (MST) |

**Por qué no un solo grafo**
- La latencia puede ser **distinta en cada sentido** (congestión, radioenlaces) → el ruteo necesita arcos dirigidos.
- El costo de un cable es **simétrico** y el MST se define sobre grafos no dirigidos.
- Los puntos de articulación están definidos para **grafos no dirigidos**; además una falla física corta los dos sentidos.
- El cableado trabaja sobre conexiones que **todavía no existen**; mezclarlas con la red real rompería ruteo y resiliencia.

**Truco de cableado:** los enlaces ya instalados entran al grafo de planificación con costo 0. Kruskal los usa "gratis" y solo agrega cable donde hace falta → responde *cuánto cuesta sumar las oficinas nuevas a la red actual*.

---

## 2. Supuestos del dominio

- Unidades: latencia en **ms**, ancho de banda en **Mbps**, costo en **USD**.
- Cada enlace físico es bidireccional pero puede tener latencia distinta por sentido (`latAB`, `latBA`). Si no se indica, son iguales.
- **Entre dos nodos hay a lo sumo un enlace físico.** La biblioteca no admite aristas paralelas (`agregarArista` devuelve `false` sin avisar), así que `Red.agregarEnlace` lanza `IllegalArgumentException` si recibe un enlace repetido entre el mismo par (en cualquier orden). Si se quiere modelar un enlace redundante entre A y B, se agrega un nodo intermedio (por ejemplo un `SWITCH`) para que sean dos caminos distintos.
- Los nodos **cliente** (`OFICINA`, `EDIFICIO`, `PUNTO_DE_ACCESO`) tienen `usuarios`. La demanda estimada es `usuarios × consumoPorUsuario(tipo)`:

| Tipo | Consumo por usuario (supuesto) |
|---|---|
| OFICINA | 5 Mbps |
| EDIFICIO (residencial) | 15 Mbps |
| PUNTO_DE_ACCESO | 3 Mbps |

- Los `DATA_CENTER` tienen `capacidad` = usuarios máximos que pueden atender.
- **La asignación de clientes a data centers no respeta la capacidad** (cada cliente va al DC de menor RTT). Es una simplificación declarada: en lugar de repartir la carga, se **reporta** qué DC quedan sobrecargados, y eso alimenta la sugerencia de un nuevo DC.
- La calidad de servicio de la CDN se mide con **RTT = d(cliente→DC) + d(DC→cliente)**: el pedido sube y el contenido baja.
- Un cliente que no alcanza ningún DC activo queda **sin servicio**: no se le asigna DC, se lista aparte y **no entra** en el promedio de latencia (si entrara, el promedio sería infinito).
- Hay dos tipos de falla:
  - **Falla real** (la registra el usuario desde el menú): marca el nodo o enlace como `activo = false` y queda así hasta que se restaura. No se borra nada.
  - **Simulación** ("¿qué pasa si cae X?"): **no modifica la red**. Se arma una vista temporal que oculta X y se consulta sobre ella.
- Un nodo caído **no se cuenta como incomunicado**: se informa por separado como "caído". Incomunicados son los nodos que siguen funcionando pero no alcanzan ningún DC.
- No se simulan protocolos ni configuraciones de equipos (lo pide la letra).

---

## 3. Datos: `RedDeEjemplo` (`telecom.datos`)

**No se carga nada desde archivos.** La red se arma en código con `RedDeEjemplo.crear()`, que llama a `red.agregarNodo(...)` y `red.agregarEnlace(...)`. Cada llamada devuelve una red nueva, así cada test o demo puede fallar nodos sin afectar a los demás.

Red de ejemplo: 25 nodos (3 data centers, 5 routers, 4 switches, 1 servidor, 12 clientes) y 26 enlaces. Incluye a propósito, cada caso con su test en `RedDeEjemploTest`:
- dos caminos de igual costo (R-CORE-1 → R-CORE-3: 4 ms por R-CORE-2 y por R-CORE-4);
- un par con latencia asimétrica (radioenlace R-CORE-4 ↔ R-ESTE: 8 ms de ida, 12 de vuelta);
- puentes (R-CORE-4 ↔ R-ESTE, R-CORE-3 ↔ DC-NORTE) y puntos de articulación (R-CORE-3, R-CORE-4, R-ESTE, los switches);
- un DC sobrecargado (DC-ESTE: capacidad 200, la región este suma 250 usuarios);
- un enlace saturado (ED-POCITOS demanda 1800 Mbps sobre un cobre de 1000);
- un candidato a nuevo data center (SRV-CACHE, servidor en el núcleo).

El diagrama de la red está en el javadoc de `RedDeEjemplo`.

**Datos para el cableado.** Las oficinas nuevas y las conexiones posibles también se arman en código, en `RedDeEjemplo` (se implementan junto con `CableadoService`):
- `List<Nodo> oficinasNuevas()` — nodos que todavía no están conectados; **no** se agregan a la `Red` (no entran a ruteo ni físico).
- `List<Enlace> conexionesPosibles(Red red, List<Nodo> oficinasNuevas)` — enlaces que se podrían instalar, con su costo. Pueden unir una oficina nueva con un nodo de la red o dos oficinas nuevas entre sí. Tienen que incluir al menos una oficina que **no** se pueda conectar, para probar `noConectables`.

---

## 4. Clases

```
ucu.edu.aed.tda.grafo        ← biblioteca (Desafío 1)
ucu.edu.aed.tda.grafo.Impl   ← + VistaFiltrada
ucu.edu.aed.telecom.model    ← dominio
ucu.edu.aed.telecom.service  ← lógica (usa la biblioteca)
ucu.edu.aed.telecom.datos    ← RedDeEjemplo (la red armada en código)
ucu.edu.aed.telecom.App      ← menú/demo por consola
```

> **Cambio respecto al esqueleto (justificar en la sección 3 del informe).** El esqueleto proponía `Enlace extends WeightedEdge` con la latencia como peso, y servicios que reciben el grafo y objetos `Nodo`. Lo cambiamos porque:
> - un mismo enlace necesita **dos pesos distintos** (latencia para ruteo, costo para cableado) y además latencias distintas por sentido; `WeightedEdge` tiene un solo `weight`;
> - los servicios necesitan más que un grafo (catálogo de nodos, data centers, fallas), por eso reciben la `Red`;
> - el usuario identifica los nodos por `id`, por eso los servicios reciben `String`.

### 4.1 Modelo (`telecom.model`)

**`TipoNodo`** (enum)
- Valores: `ROUTER, SWITCH, SERVIDOR, DATA_CENTER, PUNTO_DE_ACCESO, OFICINA, EDIFICIO`
- Atributos: `boolean cliente`, `double consumoPorUsuarioMbps`
- Métodos: `esCliente()`, `getConsumoPorUsuarioMbps()`

**`TipoEnlace`** (enum): `FIBRA, COBRE, RADIOENLACE`

**`Nodo`**
- Atributos: `String id`, `TipoNodo tipo`, `int usuarios`, `int capacidad`, `boolean activo = true`
- Métodos: getters, `esCliente()`, `esDataCenter()`, `demandaEstimadaMbps()`, `setActivo(boolean)`
- `equals`/`hashCode` **por `id`** (los dos grafos comparten las mismas instancias como claves de sus `LinkedHashMap`)

**`Enlace`** — único lugar donde viven los atributos de un enlace. **No** extiende `WeightedEdge`.
- Atributos: `Nodo a`, `Nodo b`, `double latAB`, `double latBA`, `double anchoDeBanda`, `double costo`, `TipoEnlace tipo`, `boolean activo = true`
- Métodos: getters, `latenciaDesde(Nodo origen)` (devuelve `latAB` o `latBA`), `otroExtremo(Nodo n)`, `setActivo(boolean)`

**`PesoLatencia extends WeightedEdge`** — dato de las aristas del grafo de ruteo.
- Atributos: `Enlace enlace`
- Constructor: `PesoLatencia(Enlace e, Nodo origen)` → `super(e.latenciaDesde(origen))`
- Métodos: `getEnlace()`

**`PesoCosto extends WeightedEdge`** — dato de las aristas de los grafos físico y de planificación.
- Atributos: `Enlace enlace`
- Constructor: `PesoCosto(Enlace e, double costo)` (permite costo 0 para enlaces ya instalados)
- Métodos: `getEnlace()`

> Por qué los envoltorios: cada grafo necesita un peso distinto y `WeightedEdge` tiene un solo `weight`. Así ambos grafos apuntan al **mismo** `Enlace` y una falla (`activo=false`) se ve en los dos.

**`Red`** — dueña de los grafos y del catálogo.
- Atributos:
  - `Map<String, Nodo> nodos` (búsqueda por id en O(1), en vez de `buscarVertice` O(V))
  - `List<Enlace> enlaces`
  - `DirectedGraph<Nodo, PesoLatencia> grafoRuteo`
  - `UndirectedGraph<Nodo, PesoCosto> grafoFisico`
  - `int version` (se incrementa en cada cambio real; lo usa el caché del Desafío 3)
- Métodos:
  - `agregarNodo(Nodo n)` → agrega el vértice a los dos grafos
  - `agregarEnlace(Enlace e)` → 2 arcos en ruteo + 1 arista en físico; lanza `IllegalArgumentException` si ya hay un enlace entre esos nodos
  - `getNodo(String id)`, `getNodos()`, `getDataCenters()`, `getClientes()`, `getEnlace(String a, String b)`
  - `getGrafoRuteo()`, `getGrafoFisico()`
  - `vistaActiva()` → `VistaFiltrada` del ruteo que oculta nodos y enlaces inactivos
  - `vistaActivaSin(Set<Nodo> nodosOcultos, Set<Enlace> enlacesOcultos)` → igual que `vistaActiva()` pero oculta además lo indicado. **No modifica la red ni `version`**; es lo que usan las simulaciones y la ruta alternativa
  - `fallarNodo(String id)`, `restaurarNodo(String id)`, `fallarEnlace(String a, String b)`, `restaurarEnlace(String a, String b)` → fallas reales (sí incrementan `version`)
  - `getVersion()`

**Records de resultado** (en `telecom.model`)
- `Ruta(List<Nodo> nodos, double latencia)` — vacía + `Double.POSITIVE_INFINITY` si no hay ruta
- `ResultadoCableado(List<Enlace> aInstalar, double costoTotal, Set<Nodo> noConectables)`
- `AsignacionCdn(Map<Nodo, Nodo> dcPorCliente, Set<Nodo> sinServicio)`
- `CargaDataCenter(Nodo dc, int usuariosAsignados, int capacidad)` — con `boolean sobrecargado()`
- `RutaAnchoDeBanda(List<Nodo> nodos, double cuelloDeBotellaMbps)` — vacía + `0` si no hay ruta
- `UsoEnlace(Enlace enlace, double demandaMbps)` — con `double utilizacion()` = demanda / ancho de banda

### 4.2 Vista filtrada (`tda.grafo.Impl`)

**`VistaFiltrada<V, D> implements IDirectedIGraph<V, D>`** — envuelve un grafo y oculta lo que no cumple los predicados. Es de solo lectura.
- Atributos: `IDirectedIGraph<V, D> base`, `Predicate<V> nodoOk`, `Predicate<D> aristaOk`
- Métodos que filtran: `vertices()`, `aristas()`, `adyacencias()`, `successors()`, `predecessors()`, `buscarVertice()`, `existeArista()`, `obtenerArista()`
- **Regla obligatoria:** una arista se ve solo si `aristaOk` **y** sus dos extremos cumplen `nodoOk`. No es un detalle: `floyd()` recorre `aristas()` y busca el índice de cada extremo en `vertices()`; si apareciera una arista con un extremo oculto, el índice sería -1 y se rompería con `ArrayIndexOutOfBoundsException`. Va con test propio.
- `esConexo()` y `tieneCiclos()`: **reutilizan los de `DirectedGraph`**. La vista arma un `DirectedGraph` con solo lo visible (`copiaVisible()`, O(V+E)) y le hace la pregunta. No se puede delegar directo en el grafo base porque respondería sobre el grafo completo, incluidos los ocultos; y los métodos de `DirectedGraph` leen sus mapas internos, así que no hay forma de pasarles el filtro.
- Los mutadores (`agregar…`, `remover…`, `vaciar`) lanzan `UnsupportedOperationException`.

Un solo mecanismo resuelve cuatro cosas, y los algoritmos de la biblioteca no se tocan porque solo ven la interfaz:
- **Fallas reales:** `p -> p.getEnlace().isActivo()` y `n -> n.isActivo()`
- **Simulación de fallas:** lo anterior + `n -> !nodosOcultos.contains(n)` y `p -> !enlacesOcultos.contains(p.getEnlace())`
- **Ancho de banda mínimo:** `p -> p.getEnlace().getAnchoDeBanda() >= minimo`
- **Ruta alternativa:** ocultar los nodos intermedios **y** los enlaces de la ruta principal

### 4.3 Servicios (`telecom.service`)

**`RuteoService`** — siempre trabaja sobre `red.vistaActiva()`, así respeta las fallas.
- `Ruta rutaMenorLatencia(String origen, String destino)` → Dijkstra
- `Ruta rutaConAnchoDeBandaMinimo(String origen, String destino, double mbps)` → Dijkstra sobre la vista con filtro de bw
- `IFloydWarshallResult<Nodo> tablaDeRuteo()` → Floyd
- `double rtt(Nodo a, Nodo b)` → usa la tabla

**`CableadoService`**
- `ResultadoCableado disenarCableado(Red red, List<Nodo> oficinasNuevas, List<Enlace> posibles)`
  1. Arma el grafo de planificación: nodos de la red + oficinas nuevas; enlaces existentes con `PesoCosto(e, 0)`; posibles con `PesoCosto(e, e.getCosto())`
  2. Kruskal
  3. Devuelve solo las aristas con costo > 0 (lo que hay que instalar), su suma y las oficinas que quedaron sin conectar

**`ResilienciaService`** — las simulaciones usan `red.vistaActivaSin(...)`: **nunca** modifican la red.
- `Set<Nodo> equiposCriticos()` → puntos de articulación sobre `grafoFisico`. Analiza la **red instalada completa**, sin importar las fallas reales actuales: responde "qué equipos son puntos únicos de falla por diseño".
- `Set<Enlace> enlacesCriticos()` → puentes sobre `grafoFisico` (mismo criterio)
- `Set<Nodo> incomunicadosSiFallaNodo(String id)` → vista sin ese nodo + **un único BFS con varios orígenes** (todos los DC activos en la cola inicial); devuelve los nodos activos no alcanzados, sin incluir al nodo caído
- `Set<Nodo> incomunicadosSiFallaEnlace(String a, String b)` → ídem ocultando un enlace
- `Ruta rutaAlternativa(String origen, String destino)`:
  1. Calcula la ruta principal con Dijkstra.
  2. Oculta sus **nodos intermedios y sus enlaces**, y corre Dijkstra de nuevo. Ocultar los enlaces es necesario: si la ruta principal es un enlace directo A→B no tiene intermedios, y sin ocultar el enlace Dijkstra devolvería la misma ruta.
  3. El resultado comparte solo origen y destino con la principal, así que sobrevive a cualquier falla única de un equipo intermedio o de un enlace de la ruta principal.
  - **Limitación declarada:** este método de dos pasos puede no encontrar una alternativa aunque exista, porque la primera ruta puede "bloquear" la única segunda ruta posible. El algoritmo exacto para dos caminos disjuntos es Suurballe; no lo implementamos y lo mencionamos en el informe.

**`CdnService`**
- `AsignacionCdn asignacionDataCenters()` → para cada cliente activo, el DC activo con menor RTT; los que no alcanzan ninguno van a `sinServicio`
- `List<CargaDataCenter> cargaDataCenters()` → usuarios asignados vs capacidad de cada DC; marca los sobrecargados
- `double latenciaPromedioPonderada()` → Σ usuarios × RTT / Σ usuarios (solo clientes con servicio)
- `Nodo sugerirNuevoDataCenter()` → criterio de beneficio ponderado (ver sección 6)
- `Nodo centroDeLaRed()` → `obtenerCentroGrafo` (solo como comparación)

**`ConsultasService`** — las 5 consultas propias (sección 7).

**`App`** — crea la red con `RedDeEjemplo.crear()` y muestra un menú por consola que ejecuta cada operación.

---

## 5. Requerimiento → algoritmo → complejidad

| Requerimiento | Grafo | Algoritmo | Complejidad |
|---|---|---|---|
| Ruta de menor latencia | Ruteo (vista) | Dijkstra | O((V+E) log V) teórico* |
| Tabla de ruteo completa | Ruteo (vista) | Floyd | O(V³) tiempo, O(V²) espacio |
| Ruta con bw mínimo | Ruteo (vista con filtro) | Dijkstra | Igual que Dijkstra |
| Cableado mínimo | Planificación | Kruskal con union-find | O(E log E)** |
| Equipos críticos | Físico | Puntos de articulación | O(V+E) |
| Enlaces críticos | Físico | Puentes (mismo DFS, condición `low[v] > num[u]`) | O(V+E) |
| Incomunicados ante falla | Ruteo (vista) | BFS con varios orígenes (todos los DC) | O(V+E)* |
| Ruta alternativa | Ruteo (vista) | Dijkstra ×2 | 2 × Dijkstra |
| Asignación CDN | Ruteo | Floyd + mínimo por cliente | O(V³ + V·D) |
| Carga de los DC | — | Recorrer la asignación | O(V) |
| Nuevo data center | Ruteo | Floyd + evaluación greedy | O(V³ + V²) |

\*Con la biblioteca actual, cada llamada a `adyacencias(comparable)` cuesta O(V) porque `buscarVertice` recorre todos los vértices. Eso lleva Dijkstra a O(V² + E log V) y BFS a O(V² + E). Es la primera optimización del Desafío 3 (sección 8).

\*\*Solo si Kruskal se implementa con union-find. Si se implementa buscando ciclos con un recorrido, o con `searchMinEdge`, la complejidad es mayor y hay que informar la real.

No usamos `obtenerTodosLosCaminos` para rutas alternativas en la red completa porque es exponencial; solo para una demo en una subred chica.

---

## 6. CDN: criterio para ubicar un nuevo data center

**Asignación:** cada cliente es atendido por el DC activo con menor RTT. La capacidad no se usa para asignar (ver supuestos); después de asignar se informa qué DC quedan sobrecargados.

**Criterio elegido: beneficio ponderado por usuarios.** Para cada candidato `c` (nodos `ROUTER` o `SERVIDOR`):

```
beneficio(c) = Σ_clientes  usuarios(u) × max(0, RTTactual(u) − RTT(u, c))
```

Se elige el `c` con mayor beneficio (milisegundos ahorrados, ponderados por usuarios). Para agregar k DC se repite el proceso de forma greedy. El problema exacto (p-mediana) es NP-difícil; greedy es una heurística razonable y se calcula en O(V²) con la tabla de Floyd ya hecha.

Los clientes **sin servicio** (RTT actual infinito) se tratan aparte: si algún candidato los alcanza, gana el que conecte más usuarios sin servicio y el beneficio solo desempata. Así se evita sumar infinitos.

Los DC sobrecargados refuerzan la sugerencia: el informe muestra cuántos usuarios dejarían de ir a un DC sobrecargado si se instala el nuevo.

**Comparación con el centro del grafo:** el centro minimiza el peor caso (excentricidad), pero ignora los DC que ya existen y la cantidad de usuarios de cada nodo. Lo mostramos para justificar por qué no alcanza (pregunta guía 4).

---

## 7. Consultas propias (5)

Elegimos consultas que **no repiten** funcionalidades obligatorias (tabla de ruteo, fallas de DC) y que usan los datos de demanda y ancho de banda, que si no quedarían sin uso.

1. **Ruta de máximo ancho de banda** → `RutaAnchoDeBanda rutaMaximoAnchoDeBanda(String origen, String destino)`: la ruta cuyo enlace más lento (cuello de botella) es lo más rápido posible. Algoritmo: **Dijkstra modificado (widest path)**: en lugar de sumar pesos y minimizar, se toma el mínimo ancho de banda del camino y se maximiza. O((V+E) log V). Se implementa en el servicio, porque el Dijkstra de la biblioteca suma pesos. Es distinta de `rutaConAnchoDeBandaMinimo`: aquella minimiza latencia con un umbral fijo; esta busca la mejor capacidad posible sin umbral.
2. **Enlaces saturados** → `List<UsoEnlace> enlacesSaturados(double umbral)`: para cada cliente con servicio se toma la ruta hacia su DC y se suma su `demandaEstimadaMbps()` a cada enlace de esa ruta. Devuelve los enlaces con `utilizacion() > umbral`, ordenados de mayor a menor. Algoritmos: Floyd (asignación y caminos) + acumulación sobre los caminos. O(V³ + C·L), con C clientes y L el largo de la ruta.
3. **Ranking de criticidad** → `List<Map.Entry<Nodo, Integer>> rankingCriticidad()`: para cada punto de articulación, cuántos usuarios quedan incomunicados si cae. Algoritmos: articulación + BFS con varios orígenes sobre una vista simulada (no modifica la red).
4. **Clientes fuera de SLA** → `List<Nodo> clientesFueraDeSla(double maxRttMs)`. Algoritmo: Floyd.
5. **Rutas asimétricas** → `List<String> rutasAsimetricas(double toleranciaPct)`: pares donde la ruta de ida ≠ la de vuelta o la latencia difiere más del X%. Algoritmo: Floyd. Justifica haber elegido un grafo dirigido.

---

## 8. Desafío 3

Las optimizaciones van **en este orden**, porque la segunda solo tiene sentido después de la primera.

**Optimización 1 (principal): buscar vértices en O(1)**
1. Operación: `buscarVertice`, que llaman `adyacencias`, `obtenerArista`, `successors`, etc. Todos los algoritmos (BFS, DFS, Dijkstra) la llaman en cada paso con `construirComparable(v)`.
2. Medir: tiempo de BFS y Dijkstra sobre redes aleatorias dispersas de distintos tamaños. Esperado: O(V²) por el recorrido lineal.
3. Propuesta: cuando el criterio es "igual a este vértice", buscar directo en el mapa (`containsKey`, O(1)) en lugar de recorrer. Las implementaciones sobreescriben `construirComparable` para devolver un comparable que "lleva" el vértice adentro, y `buscarVertice` lo reconoce y va directo al mapa. Cualquier otro criterio sigue con el recorrido lineal. **No cambia la interfaz ni los algoritmos.** `VistaFiltrada` delega en el grafo base y además aplica `nodoOk`.
   - En la misma línea: `floyd()`, `warshall()` y `FloydWarshallResult` usan `List.indexOf` (O(V)) por cada arista y cada consulta → reemplazar por un `Map<V, Integer>` de índices.
4. Implementar.
5. Comparar antes y después: V = 50, 100, 200, 500, 1000; 5 corridas por tamaño; tabla y gráfico. Esperado: Dijkstra pasa de O(V² + E log V) a O((V+E) log V).

**Optimización 2: tabla de ruteo — Floyd vs V × Dijkstra**
1. Operación: `RuteoService.tablaDeRuteo()`.
2. Medir: Floyd es O(V³) sin importar cuántas aristas haya.
3. Alternativa: correr Dijkstra desde cada vértice, O(V·(V+E) log V). Las redes de telecomunicaciones son dispersas (E ≈ 2–3 V), así que debería ganar al crecer V. **Solo es cierto después de la optimización 1**: con la biblioteca actual cada Dijkstra ya cuesta O(V²), V veces da O(V³) o más, y el experimento no mostraría ninguna mejora.
4. Implementar `tablaDeRuteoDijkstra()` devolviendo también un `IFloydWarshallResult<Nodo>` (un adaptador sobre los V resultados de Dijkstra), así el resto del código puede usar cualquiera de las dos sin cambios.
5. Comparar con el mismo generador y los mismos tamaños que la optimización 1.

**Extra si da el tiempo: caché de la tabla de ruteo.** Se guarda junto con `red.getVersion()`; solo se recalcula si la red cambió. Como las simulaciones no modifican la red (usan `vistaActivaSin`), no invalidan el caché. Medir 1000 consultas repetidas con y sin caché.

---

## 9. Pendientes de la biblioteca (antes de arrancar servicios)

- [ ] **Bug en `FloydWarshallResult.getPath`:** bucle infinito para cualquier par distinto. `floyd()` guarda "predecesor de j" y `getPath` lo recorre como "siguiente salto". Corrección: reconstruir desde `j` hacia atrás con `curr = predecessorMatrix[i][curr]` y al final invertir la lista.
- [ ] **Dijkstra con cola de prioridad inconsistente:** la `PriorityQueue` ordena con `costos::get`, un mapa que se modifica mientras los elementos están en la cola. Si baja el costo de un vértice que ya está encolado, el heap queda mal ordenado y puede sacar los vértices en un orden incorrecto. Corrección: encolar pares inmutables `(vértice, distancia)` y descartar los pares viejos al sacarlos.
- [ ] Tests de `dijkstra`, `floyd`, `warshall`, `obtenerExcentricidad` y `obtenerCentroGrafo` (hoy no tienen ninguno; así se escapó el bug de `getPath`).
- [ ] `kruskal`, `prim`, `searchMinEdge`, `puntosDeArticulacion` siguen en `"COMPLETAR"`. Kruskal con union-find.
- [ ] Agregar `puentes(IUndirectedGraph)` en `UndirectedGraphAlgorithms` (método extra, fuera de la interfaz).
- [x] `VistaFiltrada` completa: incluye `tieneCiclos()` y `esConexo()` (conexidad fuerte, reutilizando `DirectedGraph`), y un test de que nunca devuelve aristas con un extremo oculto.
- [ ] Confirmar con las filminas la definición de **excentricidad**: hoy es el máximo de las distancias *desde* v; la bibliografía clásica la define como el máximo de las distancias *hacia* v (máximo de la columna de Floyd).

---

## 10. Convenciones

- Tests de dominio en `src/test/java/telecom/`, con una red chica armada en el test para los casos puntuales; `RedDeEjemplo.crear()` para los casos que necesitan la red completa.
- Cada servicio recibe la `Red` por constructor.
- Los servicios buscan nodos por `id` (`String`) usando `red.getNodo(id)`; si no existe, lanzan `IllegalArgumentException`.
- Ningún servicio de consulta o simulación modifica la red: solo `fallar…`/`restaurar…` lo hacen.
- Casos borde a cubrir en cada servicio: red vacía, nodo inexistente, nodo aislado, destino inalcanzable, todos los DC caídos, cliente sin servicio, empates de costo, enlace repetido, ruta principal de un solo enlace.
