package com.example.proyecto_iotelito.data.taxista;

/** Estado confirmado del interruptor; un cambio pendiente no habilita servicios. */
public final class DisponibilidadEstado {
    public enum Problema { NINGUNO, CARGANDO, CONEXION, PERMISOS, PERFIL, GUARDADO, SESION }
    public final boolean disponible;
    public final boolean verificada;
    public final boolean guardando;
    public final Problema problema;

    private DisponibilidadEstado(boolean valor, boolean verificada, boolean guardando, Problema problema) {
        this.disponible = valor;
        this.verificada = verificada;
        this.guardando = guardando;
        this.problema = problema;
    }

    public static DisponibilidadEstado inicial() {
        return new DisponibilidadEstado(false, false, false, Problema.CARGANDO);
    }

    public static DisponibilidadEstado local(boolean valor) {
        return new DisponibilidadEstado(valor, true, false, Problema.NINGUNO);
    }

    public boolean editable() { return verificada && !guardando; }
    public boolean permiteAceptar() { return editable() && disponible; }

    public DisponibilidadEstado servidor(Object activo, Object valor) {
        if (!Boolean.TRUE.equals(activo) || !(valor instanceof Boolean)) {
            return new DisponibilidadEstado(disponible, false, guardando, Problema.PERFIL);
        }
        return new DisponibilidadEstado((Boolean) valor, true, guardando, Problema.NINGUNO);
    }

    public DisponibilidadEstado iniciarCambio() {
        if (!editable()) throw new IllegalStateException("Disponibilidad no verificada o cambio pendiente");
        return new DisponibilidadEstado(disponible, true, true, Problema.NINGUNO);
    }

    public DisponibilidadEstado confirmar(boolean valor) {
        return new DisponibilidadEstado(valor, true, false, Problema.NINGUNO);
    }

    public DisponibilidadEstado desconectar(Problema problema) {
        return new DisponibilidadEstado(disponible, false, guardando, problema);
    }

    public DisponibilidadEstado fallar(Problema problema) {
        // Solo un error de escritura conserva la última lectura válida para reintentar.
        return new DisponibilidadEstado(disponible, verificada && problema == Problema.GUARDADO,
                false, problema);
    }
}
