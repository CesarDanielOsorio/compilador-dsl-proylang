package com.compilador;

/**
 * Representa un proyecto completo: su nombre y la tabla de simbolos con
 * todas sus tareas. El analizador semantico produce una lista de estos.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class Proyecto {
    public final String nombre;
    public final int linea;
    public final TablaSimbolos tabla = new TablaSimbolos();

    // Resultados del calculo de la ruta critica (CPM):
    public int duracionTotal;   // duracion total del proyecto

    public Proyecto(String nombre, int linea) {
        this.nombre = nombre;
        this.linea = linea;
    }
}
