package ucu.edu.aed.telecom.service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.telecom.model.AsignacionCdn;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.UsoEnlace;

public class ConsultasService {

    /** Par de nodos cuya latencia de ida y de vuelta difieren. */
    public record ParAsimetrico(Nodo a, Nodo b, double ida, double vuelta) {
        public double diferenciaPct() {
            return Math.abs(ida - vuelta) / Math.min(ida, vuelta) * 100;
        }
    }

    private final Red red;
    private final RuteoService ruteo;
    private final CdnService cdn;
    private final ResilienciaService resiliencia;

    public ConsultasService(Red red) {
        this.red = Objects.requireNonNull(red, "La red no puede ser null");
        this.ruteo = new RuteoService(red);
        this.cdn = new CdnService(red);
        this.resiliencia = new ResilienciaService(red);
    }

    /**
     * Consulta 2 — Enlaces saturados. Cada cliente con servicio descarga su demanda estimada desde
     * su data center por la ruta de menor latencia DC → cliente; se acumula esa demanda en cada
     * enlace de la ruta. Devuelve los enlaces con utilización mayor al umbral, de mayor a menor.
     * Algoritmos: Floyd (asignación y caminos) + recorrer cada camino. O(V³ + C·L).
     *
     * @param umbral utilización mínima a reportar (1.0 = demanda igual al ancho de banda)
     */
    public List<UsoEnlace> enlacesSaturados(double umbral) {
        if (umbral < 0) throw new IllegalArgumentException("El umbral no puede ser negativo");
        IFloydWarshallResult<Nodo> tabla = ruteo.tablaDeRuteo();
        AsignacionCdn asignacion = cdn.asignacionDataCenters();

        Map<Enlace, Double> demandaPorEnlace = new HashMap<>();
        for (Map.Entry<Nodo, Nodo> entrada : asignacion.dcPorCliente().entrySet()) {
            Nodo cliente = entrada.getKey();
            List<Nodo> camino = tabla.getPath(entrada.getValue(), cliente);
            for (int i = 0; i + 1 < camino.size(); i++) {
                Enlace enlace = red.getEnlace(camino.get(i).getId(), camino.get(i + 1).getId());
                demandaPorEnlace.merge(enlace, cliente.demandaEstimadaMbps(), Double::sum);
            }
        }

        List<UsoEnlace> saturados = new ArrayList<>();
        for (Map.Entry<Enlace, Double> entrada : demandaPorEnlace.entrySet()) {
            UsoEnlace uso = new UsoEnlace(entrada.getKey(), entrada.getValue());
            if (uso.utilizacion() > umbral) saturados.add(uso);
        }
        saturados.sort(Comparator.comparingDouble(UsoEnlace::utilizacion).reversed());
        return saturados;
    }

    /**
     * Consulta 3 — Ranking de criticidad. Para cada punto de articulación, cuántos usuarios
     * quedan sin acceso a ningún data center si ese equipo cae. Ordenado de mayor a menor.
     * Algoritmos: puntos de articulación O(V+E) + un BFS por equipo crítico: O(A·(V+E)).
     */
    public List<Map.Entry<Nodo, Integer>> rankingCriticidad() {
        List<Map.Entry<Nodo, Integer>> ranking = new ArrayList<>();
        for (Nodo critico : resiliencia.equiposCriticos()) {
            int usuarios = 0;
            for (Nodo afectado : resiliencia.incomunicadosSiFallaNodo(critico.getId())) {
                usuarios += afectado.getUsuarios();
            }
            ranking.add(new AbstractMap.SimpleEntry<>(critico, usuarios));
        }
        ranking.sort(Map.Entry.<Nodo, Integer>comparingByValue().reversed());
        return ranking;
    }

    /**
     * Consulta 4 — Clientes fuera de SLA: clientes activos cuyo RTT hacia su data center supera
     * el máximo. Los clientes sin servicio también están fuera (RTT infinito).
     * Algoritmo: Floyd + mínimo por cliente. O(V³ + C·D).
     *
     * @return cliente → RTT actual en ms (infinito si no tiene servicio), de peor a mejor
     */
    public Map<Nodo, Double> clientesFueraDeSla(double maxRttMs) {
        if (maxRttMs < 0) throw new IllegalArgumentException("El RTT máximo no puede ser negativo");
        IFloydWarshallResult<Nodo> tabla = ruteo.tablaDeRuteo();
        AsignacionCdn asignacion = cdn.asignacionDataCenters();

        List<Map.Entry<Nodo, Double>> fuera = new ArrayList<>();
        for (Map.Entry<Nodo, Nodo> entrada : asignacion.dcPorCliente().entrySet()) {
            double rtt = ruteo.latenciaIdaYVuelta(tabla, entrada.getKey(), entrada.getValue());
            if (rtt > maxRttMs) fuera.add(new AbstractMap.SimpleEntry<>(entrada.getKey(), rtt));
        }
        for (Nodo sinServicio : asignacion.sinServicio()) {
            fuera.add(new AbstractMap.SimpleEntry<>(sinServicio, Double.POSITIVE_INFINITY));
        }
        fuera.sort(Map.Entry.<Nodo, Double>comparingByValue().reversed());

        Map<Nodo, Double> resultado = new LinkedHashMap<>();
        for (Map.Entry<Nodo, Double> e : fuera) resultado.put(e.getKey(), e.getValue());
        return resultado;
    }

    /**
     * Consulta 5 — Rutas asimétricas: pares de nodos activos conectados cuya latencia de ida y de
     * vuelta difieren más de {@code toleranciaPct}. Justifica haber modelado el ruteo con un grafo
     * dirigido. Algoritmo: Floyd + recorrer los pares. O(V³ + V²).
     */
    public List<ParAsimetrico> rutasAsimetricas(double toleranciaPct) {
        if (toleranciaPct < 0) throw new IllegalArgumentException("La tolerancia no puede ser negativa");
        IFloydWarshallResult<Nodo> tabla = ruteo.tablaDeRuteo();
        List<Nodo> activos = red.getNodos().stream().filter(Nodo::isActivo).toList();

        List<ParAsimetrico> asimetricos = new ArrayList<>();
        for (int i = 0; i < activos.size(); i++) {
            for (int j = i + 1; j < activos.size(); j++) {
                Nodo a = activos.get(i);
                Nodo b = activos.get(j);
                double ida = tabla.getCost(a, b);
                double vuelta = tabla.getCost(b, a);
                if (Double.isInfinite(ida) || Double.isInfinite(vuelta) || Math.min(ida, vuelta) == 0) continue;
                ParAsimetrico par = new ParAsimetrico(a, b, ida, vuelta);
                if (par.diferenciaPct() > toleranciaPct) asimetricos.add(par);
            }
        }
        asimetricos.sort(Comparator.comparingDouble(ParAsimetrico::diferenciaPct).reversed());
        return asimetricos;
    }
}