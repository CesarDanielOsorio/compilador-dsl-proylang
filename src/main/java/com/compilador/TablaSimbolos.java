package com.compilador;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tabla de simbolos del compilador.
 *
 * Es basicamente un diccionario que asocia cada identificador de tarea
 * con su informacion ({@link Tarea}). Usamos LinkedHashMap para conservar
 * el orden en que las tareas fueron declaradas.
 *
 * El analizador semantico la consulta para validar, por ejemplo, que una
 * dependencia se refiera a una tarea que realmente existe.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class TablaSimbolos {

    private final Map<String, Tarea> tareas = new LinkedHashMap<>();

    /** Inserta una tarea. Devuelve false si el id ya existia (duplicado). */
    public boolean insertar(Tarea tarea) {
        if (tareas.containsKey(tarea.id)) {
            return false;
        }
        tareas.put(tarea.id, tarea);
        return true;
    }

    public boolean existe(String id) {
        return tareas.containsKey(id);
    }

    public Tarea obtener(String id) {
        return tareas.get(id);
    }

    public Collection<Tarea> todas() {
        return tareas.values();
    }

    public int cantidad() {
        return tareas.size();
    }
}
