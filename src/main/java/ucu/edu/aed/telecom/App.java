package ucu.edu.aed.telecom;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import ucu.edu.aed.telecom.datos.RedDeEjemplo;
import ucu.edu.aed.telecom.model.Enlace;
import ucu.edu.aed.telecom.model.Nodo;
import ucu.edu.aed.telecom.model.Red;
import ucu.edu.aed.telecom.model.Ruta;
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

    public static void main(String[] args) {
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
        System.out.println("--- Red ---");
        System.out.println("  1. Ver nodos y enlaces");
        System.out.println("--- Ruteo ---");
        System.out.println("  2. Ruta de menor latencia");
        System.out.println("  3. Ruta con ancho de banda mínimo");
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