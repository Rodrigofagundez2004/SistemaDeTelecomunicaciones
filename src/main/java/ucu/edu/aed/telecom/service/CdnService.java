package ucu.edu.aed.telecom.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import ucu.edu.aed.tda.grafo.Impl.DirectedGraphAlgorithms;
import ucu.edu.aed.tda.grafo.model.result.IFloydWarshallResult;
import ucu.edu.aed.telecom.model.AsignacionCdn;
import ucu.edu.aed.telecom.model.CargaDataCenter;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.TipoNodo;

/**
 * Distribución de contenido (CDN): qué data center atiende a cada cliente, con qué latencia, y
 * dónde convendría instalar un data center nuevo.
 * <p>
 * La latencia de atención es el RTT = d(cliente→DC) + d(DC→cliente), medido sobre la vista activa
 * de la red: los equipos y enlaces caídos no cuentan. Cada consulta calcula Floyd una sola vez.
 * <p>
 * No modifica la red.
 */
public class CdnService {

    private final Red red;

    public CdnService(Red red) {
        this.red = Objects.requireNonNull(red, "La red no puede ser null");
    }

    /**
     * Asigna a cada cliente activo el data center activo con menor RTT. Los clientes que no
     * alcanzan ningún data center activo quedan en {@code sinServicio}; los clientes caídos no
     * aparecen en ninguno de los dos conjuntos.
     */
    public AsignacionCdn asignacionDataCenters() {
        return asignar(tabla());
    }

    /**
     * Usuarios asignados a cada data center activo frente a su capacidad. Un DC sobrecargado
     * tiene {@link CargaDataCenter#sobrecargado()} en true.
     */
    public List<CargaDataCenter> cargaDataCenters() {
        AsignacionCdn asignacion = asignar(tabla());

        Map<Nodo, Integer> usuariosPorDc = new HashMap<>();
        for (Map.Entry<Nodo, Nodo> entrada : asignacion.dcPorCliente().entrySet()) {
            usuariosPorDc.merge(entrada.getValue(), entrada.getKey().getUsuarios(), Integer::sum);
        }

        List<CargaDataCenter> carga = new ArrayList<>();
        for (Nodo dc : dataCentersActivos()) {
            carga.add(new CargaDataCenter(dc, usuariosPorDc.getOrDefault(dc, 0), dc.getCapacidad()));
        }
        return carga;
    }

    /**
     * Latencia promedio que sienten los usuarios: Σ usuarios × RTT / Σ usuarios, solo sobre los
     * clientes con servicio. Los clientes sin servicio no entran porque su RTT es infinito.
     *
     * @return +∞ si ningún usuario tiene servicio
     */
    public double latenciaPromedioPonderada() {
        IFloydWarshallResult<Nodo> tabla = tabla();
        AsignacionCdn asignacion = asignar(tabla);

        double sumaPonderada = 0;
        long usuarios = 0;
        for (Map.Entry<Nodo, Nodo> entrada : asignacion.dcPorCliente().entrySet()) {
            Nodo cliente = entrada.getKey();
            sumaPonderada += cliente.getUsuarios() * rtt(tabla, cliente, entrada.getValue());
            usuarios += cliente.getUsuarios();
        }
        return usuarios == 0 ? Double.POSITIVE_INFINITY : sumaPonderada / usuarios;
    }

    /**
     * Candidato a nuevo data center: un ROUTER o SERVIDOR activo.
     * <p>
     * Criterio, en orden de desempate:
     * <ol>
     *     <li>más usuarios sin servicio que el candidato conectaría;</li>
     *     <li>mayor beneficio: usuarios × RTT ahorrado, sobre los que ya tienen servicio;</li>
     *     <li>menor latencia total hacia los usuarios sin servicio que conectaría.</li>
     * </ol>
     * Un candidato que no conecta a nadie sin servicio ni mejora a nadie se descarta.
     * <p>
     * Nota: la cobertura va antes que el beneficio, así que el promedio de
     * {@link #latenciaPromedioPonderada()} puede subir si se conectan usuarios muy lejanos.
     *
     * @return el candidato sugerido, o null si ninguno mejora la situación actual
     */
    public Nodo sugerirNuevoDataCenter() {
        IFloydWarshallResult<Nodo> tabla = tabla();
        AsignacionCdn actual = asignar(tabla);

        Nodo mejor = null;
        long mejorCobertura = 0;
        double mejorBeneficio = 0;
        double mejorLatenciaSinServicio = Double.POSITIVE_INFINITY;

        for (Nodo candidato : candidatosActivos()) {
            long cobertura = 0;
            double latenciaSinServicio = 0;
            for (Nodo cliente : actual.sinServicio()) {
                double rttNuevo = rtt(tabla, cliente, candidato);
                if (Double.isFinite(rttNuevo)) {
                    cobertura += cliente.getUsuarios();
                    latenciaSinServicio += cliente.getUsuarios() * rttNuevo;
                }
            }

            double beneficio = 0;
            for (Map.Entry<Nodo, Nodo> entrada : actual.dcPorCliente().entrySet()) {
                Nodo cliente = entrada.getKey();
                double ahorro = rtt(tabla, cliente, entrada.getValue()) - rtt(tabla, cliente, candidato);
                beneficio += cliente.getUsuarios() * Math.max(0, ahorro);
            }

            if (cobertura == 0 && beneficio <= 0) {
                continue; // no mejora a nadie
            }

            boolean mejorCoberturaOBeneficio = cobertura > mejorCobertura
                    || (cobertura == mejorCobertura && beneficio > mejorBeneficio);
            boolean mismoEmpate = cobertura == mejorCobertura && beneficio == mejorBeneficio;
            if (mejor == null || mejorCoberturaOBeneficio
                    || (mismoEmpate && latenciaSinServicio < mejorLatenciaSinServicio)) {
                mejor = candidato;
                mejorCobertura = cobertura;
                mejorBeneficio = beneficio;
                mejorLatenciaSinServicio = latenciaSinServicio;
            }
        }
        return mejor;
    }

    /**
     * Centro de la red activa: el nodo de menor excentricidad. Sirve de comparación con
     * {@link #sugerirNuevoDataCenter()}, pero no tiene en cuenta los data centers existentes ni
     * la cantidad de usuarios de cada nodo.
     *
     * @return el centro, o null si la red activa está partida (ningún nodo llega a todos)
     */
    public Nodo centroDeLaRed() {
        return new DirectedGraphAlgorithms().obtenerCentroGrafo(red.vistaActiva());
    }

    private AsignacionCdn asignar(IFloydWarshallResult<Nodo> tabla) {
        List<Nodo> dcs = dataCentersActivos();
        Map<Nodo, Nodo> dcPorCliente = new LinkedHashMap<>();
        Set<Nodo> sinServicio = new LinkedHashSet<>();

        for (Nodo cliente : clientesActivos()) {
            Nodo mejorDc = null;
            double mejorRtt = Double.POSITIVE_INFINITY;
            for (Nodo dc : dcs) {
                double rttDc = rtt(tabla, cliente, dc);
                if (rttDc < mejorRtt) {
                    mejorRtt = rttDc;
                    mejorDc = dc;
                }
            }
            if (mejorDc == null) {
                sinServicio.add(cliente);
            } else {
                dcPorCliente.put(cliente, mejorDc);
            }
        }
        return new AsignacionCdn(dcPorCliente, sinServicio);
    }

    private IFloydWarshallResult<Nodo> tabla() {
        return new DirectedGraphAlgorithms().floyd(red.vistaActiva());
    }

    private double rtt(IFloydWarshallResult<Nodo> tabla, Nodo cliente, Nodo dc) {
        return tabla.getCost(cliente, dc) + tabla.getCost(dc, cliente);
    }

    private List<Nodo> clientesActivos() {
        return red.getClientes().stream().filter(Nodo::isActivo).toList();
    }

    private List<Nodo> dataCentersActivos() {
        return red.getDataCenters().stream().filter(Nodo::isActivo).toList();
    }

    private List<Nodo> candidatosActivos() {
        return red.getNodos().stream()
                .filter(n -> n.isActivo()
                        && (n.getTipo() == TipoNodo.ROUTER || n.getTipo() == TipoNodo.SERVIDOR))
                .toList();
    }
}