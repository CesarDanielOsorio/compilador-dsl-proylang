package com.compilador;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa una tarea del proyecto. Es la informacion que guardamos
 * en la tabla de simbolos para cada identificador de tarea declarado.
 *
 * Incluye:
 *  - Atributos escritos por el usuario (nombre, duracion, costo, etc.)
 *  - Campos calculados por el metodo de la ruta critica (CPM):
 *    inicio/fin temprano y tardio, holgura y si es critica.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class Tarea {

    // ---- Datos declarados por el usuario ----
    public String id;                 // identificador (clave en la tabla)
    public String nombre;             // nombre descriptivo (opcional)
    public int duracion = -1;         // duracion en dias (obligatoria)
    public double costo = 0;          // costo monetario (opcional)
    public String responsable;        // responsable (opcional)
    public String prioridad;          // alta | media | baja (opcional)
    public List<String> dependencias = new ArrayList<>();  // ids de los que depende

    public int lineaDeclaracion;      // donde se declaro (para reportar errores)

    // ---- Arboles de expresion (para generar codigo de 3 direcciones) ----
    public ProyLangParser.ExprContext exprDuracion;
    public ProyLangParser.ExprContext exprCosto;

    // ---- Campos calculados por CPM (ruta critica) ----
    public int inicioTemprano;   // ES - Early Start
    public int finTemprano;      // EF - Early Finish
    public int inicioTardio;     // LS - Late Start
    public int finTardio;        // LF - Late Finish
    public int holgura;          // slack = LS - ES
    public boolean critica;      // true si holgura == 0

    public Tarea(String id, int lineaDeclaracion) {
        this.id = id;
        this.lineaDeclaracion = lineaDeclaracion;
    }
}
