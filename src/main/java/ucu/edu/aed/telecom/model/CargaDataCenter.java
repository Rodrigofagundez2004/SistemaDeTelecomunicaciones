package ucu.edu.aed.telecom.model;

/** Usuarios asignados a un data center frente a su capacidad. */
public record CargaDataCenter(Nodo dc, int usuariosAsignados, int capacidad) {

    public boolean sobrecargado() {
        return usuariosAsignados > capacidad;
    }
}
