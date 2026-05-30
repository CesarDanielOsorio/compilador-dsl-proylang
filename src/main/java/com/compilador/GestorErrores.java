package com.compilador;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor central de errores del compilador.
 *
 * Recolecta los errores de las tres fases de analisis (lexico,
 * sintactico y semantico) para poder reportarlos de forma ordenada
 * al usuario. Asi cumplimos el requisito de "manejo de errores en
 * cada fase".
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class GestorErrores {

    /** Las tres fases del compilador que pueden producir errores. */
    public enum Fase {
        LEXICO("Lexico"),
        SINTACTICO("Sintactico"),
        SEMANTICO("Semantico");

        private final String etiqueta;
        Fase(String etiqueta) { this.etiqueta = etiqueta; }
        public String getEtiqueta() { return etiqueta; }
    }

    /** Representa un error individual con su ubicacion. */
    public static class Error {
        final Fase fase;
        final int linea;
        final int columna;
        final String mensaje;

        Error(Fase fase, int linea, int columna, String mensaje) {
            this.fase = fase;
            this.linea = linea;
            this.columna = columna;
            this.mensaje = mensaje;
        }

        @Override
        public String toString() {
            return String.format("[%s] linea %d:%d  ->  %s",
                    fase.getEtiqueta(), linea, columna, mensaje);
        }
    }

    private final List<Error> errores = new ArrayList<>();

    /** Agrega un error a la lista. */
    public void agregar(Fase fase, int linea, int columna, String mensaje) {
        errores.add(new Error(fase, linea, columna, mensaje));
    }

    /** ¿Hubo algun error en cualquier fase? */
    public boolean hayErrores() {
        return !errores.isEmpty();
    }

    /** ¿Hubo errores en una fase especifica? */
    public boolean hayErrores(Fase fase) {
        return errores.stream().anyMatch(e -> e.fase == fase);
    }

    /** Cuenta errores de una fase especifica. */
    public long contar(Fase fase) {
        return errores.stream().filter(e -> e.fase == fase).count();
    }

    public List<Error> getErrores() {
        return errores;
    }

    /** Imprime en consola todos los errores de una fase dada. */
    public void imprimir(Fase fase) {
        for (Error e : errores) {
            if (e.fase == fase) {
                System.out.println("   " + e);
            }
        }
    }
}
