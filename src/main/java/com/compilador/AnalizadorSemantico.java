package com.compilador;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ANALISIS SEMANTICO.
 *
 * Despues de que el codigo es lexica y sintacticamente correcto, esta
 * clase verifica que TENGA SENTIDO. Construye la tabla de simbolos y
 * aplica las siguientes validaciones:
 *
 *   1. Tareas con identificador duplicado.
 *   2. Atributos repetidos dentro de una misma tarea.
 *   3. Atributo obligatorio 'duracion' presente.
 *   4. 'duracion' debe ser un entero positivo.
 *   5. 'costo' no puede ser negativo.
 *   6. Una dependencia debe referirse a una tarea declarada.
 *   7. Una tarea no puede depender de si misma.
 *   8. No se permiten dependencias duplicadas.
 *   9. No se permiten dependencias circulares (A -> B -> A).
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class AnalizadorSemantico {

    private final GestorErrores gestor;
    private final EvaluadorExpr evaluador = new EvaluadorExpr();
    private final List<Proyecto> proyectos = new ArrayList<>();

    public AnalizadorSemantico(GestorErrores gestor) {
        this.gestor = gestor;
    }

    /** Punto de entrada: analiza todo el programa y devuelve los proyectos. */
    public List<Proyecto> analizar(ProyLangParser.ProgramaContext arbol) {
        for (ProyLangParser.ProyectoContext pc : arbol.proyecto()) {
            analizarProyecto(pc);
        }
        return proyectos;
    }

    private void analizarProyecto(ProyLangParser.ProyectoContext pc) {
        String nombre = quitarComillas(pc.STRING().getText());
        Proyecto proy = new Proyecto(nombre, pc.STRING().getSymbol().getLine());
        TablaSimbolos tabla = proy.tabla;

        // --- PASO 1: declarar todas las tareas (detecta duplicados) ---
        for (ProyLangParser.DeclTareaContext dt : pc.declTarea()) {
            String id = dt.ID().getText();
            int linea = dt.ID().getSymbol().getLine();
            int col = dt.ID().getSymbol().getCharPositionInLine();
            Tarea t = new Tarea(id, linea);
            if (!tabla.insertar(t)) {
                error(linea, col, "La tarea '" + id + "' ya fue declarada antes.");
            }
        }

        // --- PASO 2: procesar atributos (incluye validar dependencias) ---
        for (ProyLangParser.DeclTareaContext dt : pc.declTarea()) {
            Tarea t = tabla.obtener(dt.ID().getText());
            if (t == null) continue;
            // Solo procesamos los atributos de la PRIMERA declaracion valida.
            // (Si la tarea ya tiene atributos, esta es una redeclaracion.)
            if (t.duracion == -1 && t.dependencias.isEmpty()
                    && t.nombre == null && t.responsable == null) {
                procesarAtributos(dt, t, tabla);
            }
        }

        // --- PASO 3: verificar atributo obligatorio 'duracion' ---
        for (Tarea t : tabla.todas()) {
            if (t.duracion == -1) {
                error(t.lineaDeclaracion, 0,
                        "La tarea '" + t.id + "' no tiene el atributo obligatorio 'duracion'.");
                t.duracion = 0; // valor seguro para que CPM no falle
            }
        }

        // --- PASO 4: detectar dependencias circulares ---
        detectarCiclos(tabla);

        proyectos.add(proy);
    }

    /** Procesa cada atributo de una tarea aplicando su validacion de tipo. */
    private void procesarAtributos(ProyLangParser.DeclTareaContext dt,
                                   Tarea t, TablaSimbolos tabla) {
        Set<String> atributosVistos = new HashSet<>();

        for (ProyLangParser.AtributoContext a : dt.atributo()) {
            int linea = a.getStart().getLine();
            int col = a.getStart().getCharPositionInLine();

            if (a instanceof ProyLangParser.AtrNombreContext) {
                if (!atributosVistos.add("nombre")) duplicado("nombre", linea, col);
                t.nombre = quitarComillas(((ProyLangParser.AtrNombreContext) a).STRING().getText());

            } else if (a instanceof ProyLangParser.AtrResponsableContext) {
                if (!atributosVistos.add("responsable")) duplicado("responsable", linea, col);
                t.responsable = quitarComillas(((ProyLangParser.AtrResponsableContext) a).STRING().getText());

            } else if (a instanceof ProyLangParser.AtrDuracionContext) {
                if (!atributosVistos.add("duracion")) duplicado("duracion", linea, col);
                ProyLangParser.ExprContext e = ((ProyLangParser.AtrDuracionContext) a).expr();
                double valor = evaluador.visit(e);
                if (valor != Math.floor(valor)) {
                    error(linea, col, "La 'duracion' debe ser un numero entero (se obtuvo " + valor + ").");
                } else if (valor <= 0) {
                    error(linea, col, "La 'duracion' debe ser mayor que 0 (se obtuvo " + (int) valor + ").");
                } else {
                    t.duracion = (int) valor;
                    t.exprDuracion = e;
                }

            } else if (a instanceof ProyLangParser.AtrCostoContext) {
                if (!atributosVistos.add("costo")) duplicado("costo", linea, col);
                ProyLangParser.ExprContext e = ((ProyLangParser.AtrCostoContext) a).expr();
                double valor = evaluador.visit(e);
                if (valor < 0) {
                    error(linea, col, "El 'costo' no puede ser negativo (se obtuvo " + valor + ").");
                } else {
                    t.costo = valor;
                    t.exprCosto = e;
                }

            } else if (a instanceof ProyLangParser.AtrDependeContext) {
                if (!atributosVistos.add("depende")) duplicado("depende", linea, col);
                procesarDependencias((ProyLangParser.AtrDependeContext) a, t, tabla);
            }
        }
        // Si por culpa de un error 'duracion' nunca se asigno, queda en -1
        // y el PASO 3 lo reportara como faltante; lo dejamos en 0 si fue invalida.
        if (t.duracion == -1 && tieneAtributo(dt, "duracion")) {
            t.duracion = 0;
        }
    }

    /** Valida la lista de dependencias: existencia, auto-dependencia y duplicados. */
    private void procesarDependencias(ProyLangParser.AtrDependeContext a,
                                      Tarea t, TablaSimbolos tabla) {
        Set<String> yaAgregadas = new HashSet<>();
        for (org.antlr.v4.runtime.tree.TerminalNode idNode : a.listaIds().ID()) {
            String dep = idNode.getText();
            int linea = idNode.getSymbol().getLine();
            int col = idNode.getSymbol().getCharPositionInLine();

            if (dep.equals(t.id)) {
                error(linea, col, "La tarea '" + t.id + "' no puede depender de si misma.");
            } else if (!tabla.existe(dep)) {
                error(linea, col, "La tarea '" + t.id + "' depende de '" + dep
                        + "', que no esta declarada.");
            } else if (!yaAgregadas.add(dep)) {
                error(linea, col, "Dependencia duplicada: '" + t.id + "' ya dependia de '" + dep + "'.");
            } else {
                t.dependencias.add(dep);
            }
        }
    }

    /**
     * Deteccion de ciclos con DFS y coloreo de nodos:
     *   blanco(0) = sin visitar, gris(1) = en la pila actual, negro(2) = terminado.
     * Si llegamos a un nodo gris, encontramos una dependencia circular.
     */
    private void detectarCiclos(TablaSimbolos tabla) {
        Map<String, Integer> color = new HashMap<>();
        for (Tarea t : tabla.todas()) color.put(t.id, 0);
        Set<String> ciclosReportados = new HashSet<>();

        for (Tarea t : tabla.todas()) {
            if (color.get(t.id) == 0) {
                dfs(t.id, tabla, color, new ArrayDeque<>(), ciclosReportados);
            }
        }
    }

    private void dfs(String id, TablaSimbolos tabla, Map<String, Integer> color,
                     Deque<String> pila, Set<String> ciclosReportados) {
        color.put(id, 1);
        pila.push(id);
        for (String dep : tabla.obtener(id).dependencias) {
            if (color.get(dep) == 1) {
                reportarCiclo(dep, pila, tabla, ciclosReportados);
            } else if (color.get(dep) == 0) {
                dfs(dep, tabla, color, pila, ciclosReportados);
            }
        }
        pila.pop();
        color.put(id, 2);
    }

    private void reportarCiclo(String inicio, Deque<String> pila,
                               TablaSimbolos tabla, Set<String> ciclosReportados) {
        // Reconstruimos el ciclo desde 'inicio' hasta el tope de la pila.
        List<String> ruta = new ArrayList<>();
        boolean enCiclo = false;
        List<String> pilaLista = new ArrayList<>(pila); // tope primero
        java.util.Collections.reverse(pilaLista);        // ahora en orden de entrada
        for (String n : pilaLista) {
            if (n.equals(inicio)) enCiclo = true;
            if (enCiclo) ruta.add(n);
        }
        ruta.add(inicio); // cerramos el ciclo
        String clave = String.join("->", ruta);
        if (ciclosReportados.add(clave)) {
            int linea = tabla.obtener(inicio).lineaDeclaracion;
            error(linea, 0, "Dependencia circular detectada: " + String.join(" -> ", ruta));
        }
    }

    // ---------- utilidades ----------

    private boolean tieneAtributo(ProyLangParser.DeclTareaContext dt, String clave) {
        for (ProyLangParser.AtributoContext a : dt.atributo()) {
            if (clave.equals("duracion") && a instanceof ProyLangParser.AtrDuracionContext) return true;
        }
        return false;
    }

    private void duplicado(String atributo, int linea, int col) {
        error(linea, col, "El atributo '" + atributo + "' esta repetido en la misma tarea.");
    }

    private void error(int linea, int col, String mensaje) {
        gestor.agregar(GestorErrores.Fase.SEMANTICO, linea, col, mensaje);
    }

    private String quitarComillas(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
