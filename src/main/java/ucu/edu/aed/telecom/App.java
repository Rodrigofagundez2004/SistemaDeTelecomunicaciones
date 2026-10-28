package ucu.edu.aed.telecom;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import ucu.edu.aed.telecom.datos.RedDeEjemplo;
import ucu.edu.aed.telecom.model.AsignacionCdn;
import ucu.edu.aed.telecom.model.CargaDataCenter;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.ResultadoCableado;
import ucu.edu.aed.telecom.model.Ruta;
import ucu.edu.aed.telecom.model.UsoEnlace;
import ucu.edu.aed.telecom.service.CableadoService;
import ucu.edu.aed.telecom.service.CdnService;
import ucu.edu.aed.telecom.service.ResilienciaService;
import ucu.edu.aed.telecom.service.RuteoService;
import ucu.edu.aed.utils.PrettyGrid;

/** Punto de entrada: menú por consola sobre la red de ejemplo. */
public class App {

    // Formato de celda para PrettyGrid: texto alineado a la izquierda
    private static final PrettyGrid.CellFormat<String> TEXTO = new PrettyGrid.CellFormat<>() {
        @Override
        public String format(String valor) {
            return valor;
        }

        @Override
        public PrettyGrid.Align align() {
            return PrettyGrid.Align.LEFT;
        }
    };

    private final Scanner entrada = new Scanner(System.in);
    private final Red red = RedDeEjemplo.crear();
    private final RuteoService ruteo = new RuteoService(red);
    private final CdnService cdn = new CdnService(red);
    private final CableadoService cableado = new CableadoService();
    private final ResilienciaService resiliencia = new ResilienciaService(red);

    public static void main(String[] args) {
        System.out.println("Sistema de Telecomunicaciones - esqueleto");
        new App().ejecutar();
    }

    private void ejecutar() {
        System.out.println("=== Sistema de Telecomunicaciones ===");
        System.out.printf("Red de ejemplo cargada: %d nodos, %d enlaces%n",
                red.getNodos().size(), red.getEnlaces().size());

        boolean seguir = true;
        while (seguir) {
            mostrarMenu();
            String opcion = leerTexto("Opción");
            try {
                switch (opcion) {
                    case "1" -> verRed();
                    case "0" -> seguir = false;
                    case "2" -> rutaMenorLatencia();
                    case "3" -> rutaConAnchoDeBanda();
                    case "4" -> verAsignacionDataCenters();
                    case "5" -> verCargaDataCenters();
                    case "6" -> verLatenciaPromedio();
                    case "7" -> verSugerenciaDataCenter();
                    case "8" -> verCentroDeLaRed();
                    case "9" -> verCableadoOficinasNuevas();
                    case "10" -> verEquiposCriticos();
                    case "11" -> verEnlacesCriticos();
                    case "12" -> verRutaAlternativa();
                    case "13" -> verEnlacesSaturados();
                    case "14" -> verRankingCriticidad();
                    case "15" -> verClientesFueraDeSla();
                    case "16" -> verRutasAsimetricas();
                    default -> System.out.println("Opción inválida");
                }
            } catch (IllegalArgumentException e) {
                // ids inexistentes, valores negativos, etc.: se informa y se vuelve al menú
                System.out.println("Error: " + e.getMessage());
            }
        }
        System.out.println("Hasta luego");
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("--- Red actual ---");
        System.out.println("  1. Ver nodos y enlaces");
        System.out.println("--- Mejores rutas ---");
        System.out.println("  2. Ruta de menor latencia");
        System.out.println("  3. Ruta con ancho de banda mínimo");
        System.out.println("--- Distribucion de contenido ---");
        System.out.println("  4. Asignación de clientes a data centers");
        System.out.println("  5. Carga de data centers");
        System.out.println("  6. Latencia promedio ponderada");
        System.out.println("  7. Sugerir nuevo data center");
        System.out.println("  8. Centro de la red");
        System.out.println("--- Nuevos Cableados ---");
        System.out.println("  9. Diseñar cableado de las oficinas nuevas de ejemplo");
        System.out.println("--- Criticidad de la red ---");
        System.out.println(" 10. Equipos críticos");
        System.out.println(" 11. Enlaces críticos");
        System.out.println(" 12. Ruta alternativa");
        System.out.println("-------------------");
        System.out.println(" 13. Enlaces saturados");
        System.out.println(" 14. Ranking de criticidad");
        System.out.println(" 15. Clientes fuera de SLA");
        System.out.println(" 16. Rutas asimétricas");
        System.out.println("  0. Salir");
    }

    // ---------- Vistas de Red ----------

    private void verRed() {
        List<Nodo> nodos = new ArrayList<>(red.getNodos());
        String[][] filasNodos = new String[nodos.size()][];
        List<String> idsNodos = new ArrayList<>();
        for (int i = 0; i < nodos.size(); i++) {
            Nodo n = nodos.get(i);
            idsNodos.add(n.getId());
            filasNodos[i] = new String[] {
                    n.getTipo().toString(),
                    n.esCliente() ? String.valueOf(n.getUsuarios()) : "-",
                    n.esCliente() ? formato(n.demandaEstimadaMbps()) : "-",
                    n.esDataCenter() ? String.valueOf(n.getCapacidad()) : "-",
                    n.isActivo() ? "activo" : "CAIDO"
            };
        }
        System.out.println("\nNODOS");
        PrettyGrid.printLabeled(filasNodos, TEXTO,
                List.of("Tipo", "Usuarios", "Demanda Mbps", "Capacidad", "Estado"), idsNodos, "Nodo");

        List<Enlace> enlaces = red.getEnlaces();
        String[][] filasEnlaces = new String[enlaces.size()][];
        List<String> nombresEnlaces = new ArrayList<>();
        for (int i = 0; i < enlaces.size(); i++) {
            Enlace e = enlaces.get(i);
            nombresEnlaces.add(e.getA().getId() + " <-> " + e.getB().getId());
            filasEnlaces[i] = new String[] {
                    e.getTipo().toString(),
                    formato(e.getLatAB()),
                    formato(e.getLatBA()),
                    formato(e.getAnchoDeBanda()),
                    formato(e.getCosto()),
                    e.isActivo() ? "activo" : "CAIDO"
            };
        }
        System.out.println("\nENLACES (ida = de izquierda a derecha)");
        PrettyGrid.printLabeled(filasEnlaces, TEXTO,
                List.of("Medio", "Ida ms", "Vuelta ms", "Mbps", "Costo USD", "Estado"), nombresEnlaces, "Enlace");
    }

    // ---------- Ruteo ----------

    private void rutaMenorLatencia() {
        String origen = leerNodo("Origen");
        String destino = leerNodo("Destino");
        validarDistintos(origen, destino);

        Ruta ida = ruteo.rutaDeMenorLatencia(origen, destino);
        if (!ida.existe()) {
            System.out.println("No hay ruta de " + origen + " a " + destino + " con la red actual");
            return;
        }
        System.out.println("\nIDA  " + origen + " -> " + destino);
        mostrarRuta(ida);

        Ruta vuelta = ruteo.rutaDeMenorLatencia(destino, origen);
        System.out.println("\nVUELTA  " + destino + " -> " + origen + ": " + formato(vuelta.latencia()) + " ms");
        if (vuelta.latencia() != ida.latencia()) {
            System.out.println("  Latencia asimétrica: la vuelta difiere en "
                    + formato(Math.abs(vuelta.latencia() - ida.latencia())) + " ms");
        }

        String mb = leerTexto("\nMB a enviar de origen a destino (Enter para omitir)");
        if (!mb.isEmpty()) {
            double megabytes = aNumero(mb);
            double segundos = ida.latencia() / 1000 + megabytes * 8 / cuelloDeBotella(ida);
            System.out.println("Tiempo estimado: " + formatoTiempo(segundos)
                    + "  (latencia + MB x 8 / cuello de botella; no modela TCP ni congestión)");
        }
    }

    private void rutaConAnchoDeBanda() {
        String origen = leerNodo("Origen");
        String destino = leerNodo("Destino");
        validarDistintos(origen, destino);
        double mbps = aNumero(leerTexto("Ancho de banda mínimo (Mbps)"));

        Ruta normal = ruteo.rutaDeMenorLatencia(origen, destino);
        Ruta conMinimo = ruteo.rutaConAnchoDeBandaMinimo(origen, destino, mbps);

        if (!conMinimo.existe()) {
            System.out.println("Ningún camino de " + origen + " a " + destino
                    + " garantiza " + formato(mbps) + " Mbps en todos sus enlaces");
            if (normal.existe()) {
                System.out.println("La ruta más rápida tiene un cuello de botella de "
                        + formato(cuelloDeBotella(normal)) + " Mbps:");
                mostrarRuta(normal);
            }
            return;
        }
        if (conMinimo.nodos().equals(normal.nodos())) {
            System.out.println("La ruta más rápida ya cumple con " + formato(mbps) + " Mbps:");
            mostrarRuta(conMinimo);
            return;
        }
        System.out.println("\nRUTA MÁS RÁPIDA (no cumple " + formato(mbps) + " Mbps)");
        mostrarRuta(normal);
        System.out.println("\nRUTA QUE CUMPLE " + formato(mbps) + " Mbps (+"
                + formato(conMinimo.latencia() - normal.latencia()) + " ms)");
        mostrarRuta(conMinimo);
    }

    /** Imprime la ruta salto por salto: medio, latencia en ese sentido y ancho de banda. */
    private void mostrarRuta(Ruta ruta) {
        List<Nodo> nodos = ruta.nodos();
        String[][] filas = new String[nodos.size() - 1][];
        List<String> saltos = new ArrayList<>();
        for (int i = 0; i + 1 < nodos.size(); i++) {
            Nodo desde = nodos.get(i);
            Nodo hasta = nodos.get(i + 1);
            Enlace e = red.getEnlace(desde.getId(), hasta.getId());
            saltos.add(desde.getId() + " -> " + hasta.getId());
            filas[i] = new String[] {
                    e.getTipo().toString(),
                    formato(e.latenciaDesde(desde)),
                    formato(e.getAnchoDeBanda())
            };
        }
        if (filas.length > 0) {
            PrettyGrid.printLabeled(filas, TEXTO, List.of("Medio", "Latencia ms", "Mbps"), saltos, "Salto");
        }
        System.out.println("Total: " + formato(ruta.latencia()) + " ms | " + (nodos.size() - 1)
                + " saltos | cuello de botella: " + formato(cuelloDeBotella(ruta)) + " Mbps");
    }

    /** Ancho de banda del enlace más angosto de la ruta: limita la velocidad de toda la ruta. */
    private double cuelloDeBotella(Ruta ruta) {
        double minimo = Double.POSITIVE_INFINITY;
        List<Nodo> nodos = ruta.nodos();
        for (int i = 0; i + 1 < nodos.size(); i++) {
            Enlace e = red.getEnlace(nodos.get(i).getId(), nodos.get(i + 1).getId());
            minimo = Math.min(minimo, e.getAnchoDeBanda());
        }
        return minimo;
    }

    // ---------- CDN ----------

    /** Cada cliente activo con el data center de menor RTT; los que no alcanzan ninguno van aparte. */
    private void verAsignacionDataCenters() {
        AsignacionCdn asignacion = cdn.asignacionDataCenters();

        List<String> clientes = new ArrayList<>();
        String[][] filas = new String[asignacion.dcPorCliente().size()][];
        int i = 0;
        for (Map.Entry<Nodo, Nodo> entrada : asignacion.dcPorCliente().entrySet()) {
            clientes.add(entrada.getKey().getId());
            filas[i++] = new String[] {entrada.getValue().getId()};
        }
        System.out.println("\nCLIENTES CON SERVICIO (menor RTT)");
        if (filas.length > 0) {
            PrettyGrid.printLabeled(filas, TEXTO, List.of("Data center"), clientes, "Cliente");
        }
        System.out.println("Sin servicio: " + nombres(asignacion.sinServicio()));
    }

    private void verCargaDataCenters() {
        List<CargaDataCenter> carga = cdn.cargaDataCenters();
        String[][] filas = new String[carga.size()][];
        List<String> dcs = new ArrayList<>();
        for (int i = 0; i < carga.size(); i++) {
            CargaDataCenter c = carga.get(i);
            dcs.add(c.dc().getId());
            filas[i] = new String[] {
                    String.valueOf(c.usuariosAsignados()),
                    String.valueOf(c.dc().getCapacidad()),
                    c.sobrecargado() ? "SOBRECARGADO" : "OK"
            };
        }
        System.out.println("\nCARGA DE DATA CENTERS");
        if (filas.length == 0) {
            System.out.println("No hay data centers activos.");
            return;
        }
        PrettyGrid.printLabeled(filas, TEXTO, List.of("Usuarios", "Capacidad", "Estado"), dcs, "Data center");
    }

    private void verLatenciaPromedio() {
        double promedio = cdn.latenciaPromedioPonderada();
        if (Double.isInfinite(promedio)) {
            System.out.println("Ningún usuario tiene servicio: no hay latencia promedio.");
        } else {
            System.out.println(String.format(Locale.US, "Latencia promedio ponderada: %.2f ms (RTT)", promedio));
        }
    }

    private void verSugerenciaDataCenter() {
        Nodo sugerido = cdn.sugerirNuevoDataCenter();
        if (sugerido == null) {
            System.out.println("Ningún candidato mejora la situación actual.");
        } else {
            System.out.println("Sugerencia de nuevo data center: " + sugerido.getId());
        }
    }

    private void verCentroDeLaRed() {
        Nodo centro = cdn.centroDeLaRed();
        System.out.println(centro == null
                ? "La red activa está partida: no hay centro."
                : "Centro de la red: " + centro.getId());
    }

    // ---------- Cableado ----------

    /** Diseña el cableado de las oficinas nuevas de ejemplo y muestra qué instalar y cuánto cuesta. */
    private void verCableadoOficinasNuevas() {
        List<Nodo> oficinas = RedDeEjemplo.oficinasNuevas();
        List<Enlace> posibles = RedDeEjemplo.conexionesPosibles(red, oficinas);
        ResultadoCableado r = cableado.disenarCableado(red, oficinas, posibles);

        System.out.println("\nENLACES A INSTALAR");
        if (r.aInstalar().isEmpty()) {
            System.out.println("Ninguno.");
        } else {
            String[][] filas = new String[r.aInstalar().size()][];
            List<String> nombres = new ArrayList<>();
            for (int i = 0; i < r.aInstalar().size(); i++) {
                Enlace e = r.aInstalar().get(i);
                nombres.add(e.getA().getId() + " <-> " + e.getB().getId());
                filas[i] = new String[] {e.getTipo().toString(), formato(e.getCosto())};
            }
            PrettyGrid.printLabeled(filas, TEXTO, List.of("Medio", "Costo USD"), nombres, "Enlace");
        }
        System.out.println("Costo total: " + formato(r.costoTotal()) + " USD");
        System.out.println("Oficinas no conectables: " + nombres(r.noConectables()));
    }

    // ---------- Entrada y formato ----------

    /** Pide un id de nodo (sin importar mayúsculas) hasta que exista. Enter vacío cancela. */
    private String leerNodo(String mensaje) {
        while (true) {
            String id = leerTexto(mensaje).toUpperCase();
            if (id.isEmpty()) {
                throw new IllegalArgumentException("operación cancelada");
            }
            if (red.existeNodo(id)) {
                return id;
            }
            List<String> ids = new ArrayList<>();
            for (Nodo n : red.getNodos()) {
                ids.add(n.getId());
            }
            System.out.println("No existe \"" + id + "\". Nodos: " + String.join(", ", ids));
        }
    }

    private static void validarDistintos(String origen, String destino) {
        if (origen.equals(destino)) {
            throw new IllegalArgumentException("origen y destino son el mismo nodo");
        }
    }

        // ---------- Resiliencia ----------

    private void verEquiposCriticos() {
        System.out.println("Equipos críticos (puntos de articulación): " + nombres(resiliencia.equiposCriticos()));
    }

    private void verEnlacesCriticos() {
        List<String> enlaces = new ArrayList<>();
        for (Enlace e : resiliencia.enlacesCriticos()) {
            enlaces.add(e.getA().getId() + " <-> " + e.getB().getId());
        }
        System.out.println("Enlaces críticos (puentes): " + (enlaces.isEmpty() ? "ninguno" : String.join(", ", enlaces)));
    }

    private void verRutaAlternativa() {
        String origen = leerNodo("Origen");
        String destino = leerNodo("Destino");
        validarDistintos(origen, destino);

        Ruta principal = ruteo.rutaDeMenorLatencia(origen, destino);
        if (!principal.existe()) {
            System.out.println("No hay ruta de " + origen + " a " + destino + " con la red actual");
            return;
        }
        System.out.println("\nRUTA PRINCIPAL");
        mostrarRuta(principal);

        Ruta alternativa = resiliencia.rutaAlternativa(origen, destino);
        if (!alternativa.existe()) {
            System.out.println("\nNo hay ruta alternativa sin nodos intermedios ni enlaces en común con la principal");
            return;
        }
        System.out.println("\nRUTA ALTERNATIVA (+" + formato(alternativa.latencia() - principal.latencia()) + " ms)");
        mostrarRuta(alternativa);
    }

    private void verEnlacesSaturados() {
        double umbral = aNumero(leerTexto("Umbral de utilización en % (ej. 80)")) / 100;
        List<UsoEnlace> usos = cdn.enlacesSaturados(umbral);
        if (usos.isEmpty()) {
            System.out.println("Ningún enlace supera ese umbral.");
            return;
        }
        String[][] filas = new String[usos.size()][];
        List<String> nombresEnlaces = new ArrayList<>();
        for (int i = 0; i < usos.size(); i++) {
            UsoEnlace u = usos.get(i);
            nombresEnlaces.add(u.enlace().getA().getId() + " <-> " + u.enlace().getB().getId());
            filas[i] = new String[] {
                    formato(u.demandaMbps()),
                    formato(u.enlace().getAnchoDeBanda()),
                    formato(u.utilizacion() * 100) + " %"
            };
        }
        System.out.println("\nENLACES SATURADOS");
        PrettyGrid.printLabeled(filas, TEXTO, List.of("Demanda Mbps", "Capacidad Mbps", "Uso"), nombresEnlaces, "Enlace");
    }

    private void verRankingCriticidad() {
        List<Map.Entry<Nodo, Integer>> ranking = resiliencia.rankingCriticidad();
        if (ranking.isEmpty()) {
            System.out.println("No hay equipos críticos.");
            return;
        }
        String[][] filas = new String[ranking.size()][];
        List<String> equipos = new ArrayList<>();
        for (int i = 0; i < ranking.size(); i++) {
            equipos.add(ranking.get(i).getKey().getId());
            filas[i] = new String[] {String.valueOf(ranking.get(i).getValue())};
        }
        System.out.println("\nRANKING DE CRITICIDAD (usuarios sin data center si cae el equipo)");
        PrettyGrid.printLabeled(filas, TEXTO, List.of("Usuarios afectados"), equipos, "Equipo");
    }

    private void verClientesFueraDeSla() {
        double maxRtt = aNumero(leerTexto("RTT máximo (ms)"));
        Map<Nodo, Double> fuera = cdn.clientesFueraDeSla(maxRtt);
        if (fuera.isEmpty()) {
            System.out.println("Todos los clientes cumplen el SLA.");
            return;
        }
        String[][] filas = new String[fuera.size()][];
        List<String> clientes = new ArrayList<>();
        int i = 0;
        for (Map.Entry<Nodo, Double> e : fuera.entrySet()) {
            clientes.add(e.getKey().getId());
            filas[i++] = new String[] {formato(e.getValue()), String.valueOf(e.getKey().getUsuarios())};
        }
        System.out.println("\nCLIENTES FUERA DE SLA (" + formato(maxRtt) + " ms)");
        PrettyGrid.printLabeled(filas, TEXTO, List.of("RTT ms", "Usuarios"), clientes, "Cliente");
    }

        private void verRutasAsimetricas() {
        double tolerancia = aNumero(leerTexto("Tolerancia (%)"));
        List<Map.Entry<Ruta, Ruta>> pares = ruteo.rutasAsimetricas(tolerancia);
        if (pares.isEmpty()) {
            System.out.println("No hay pares con diferencia mayor a " + formato(tolerancia) + " %.");
            return;
        }
        String[][] filas = new String[pares.size()][];
        List<String> nombresPares = new ArrayList<>();
        for (int i = 0; i < pares.size(); i++) {
            Ruta ida = pares.get(i).getKey();
            Ruta vuelta = pares.get(i).getValue();
            Nodo a = ida.nodos().get(0);
            Nodo b = ida.nodos().get(ida.nodos().size() - 1);
            nombresPares.add(a.getId() + " <-> " + b.getId());
            filas[i] = new String[] {
                    formato(ida.latencia()),
                    formato(vuelta.latencia()),
                    formato(RuteoService.diferenciaPct(ida.latencia(), vuelta.latencia())) + " %"
            };
        }
        System.out.println("\nRUTAS ASIMÉTRICAS (" + pares.size() + " pares)");
        PrettyGrid.printLabeled(filas, TEXTO, List.of("Ida ms", "Vuelta ms", "Diferencia"), nombresPares, "Par");
    }

    /** Acepta coma o punto decimal. */
    private static double aNumero(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("\"" + texto + "\" no es un número");
        }
    }

    private String leerTexto(String mensaje) {
        System.out.print(mensaje + ": ");
        if (!entrada.hasNextLine()) {
            System.out.println();
            System.exit(0);             // fin de la entrada (Ctrl+D)
        }
        return entrada.nextLine().trim();
    }

    private static String formatoTiempo(double segundos) {
        if (segundos < 1) {
            return String.format(Locale.US, "%.1f ms", segundos * 1000);
        }
        return String.format(Locale.US, "%.2f s", segundos);
    }

    /** Lista de ids separada por comas, o "ninguno". */
    private static String nombres(Iterable<Nodo> nodos) {
        List<String> ids = new ArrayList<>();
        for (Nodo n : nodos) {
            ids.add(n.getId());
        }
        return ids.isEmpty() ? "ninguno" : String.join(", ", ids);
    }

    /** 14.0 -> "14", 14.5 -> "14.5", infinito -> "sin ruta". */
    private static String formato(double valor) {
        if (Double.isInfinite(valor)) {
            return "sin ruta";
        }
        if (valor == Math.rint(valor)) {
            return String.valueOf((long) valor);
        }
        return String.format(Locale.US, "%.1f", valor);
    }
}