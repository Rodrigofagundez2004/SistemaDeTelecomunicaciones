package ucu.edu.aed.telecom.model;

/** Demanda que pasa por un enlace frente a su ancho de banda. */
public record UsoEnlace(Enlace enlace, double demandaMbps) {

    /** demanda / ancho de banda: mayor a 1 significa enlace saturado. */
    public double utilizacion() {
        return demandaMbps / enlace.getAnchoDeBanda();
    }
}
