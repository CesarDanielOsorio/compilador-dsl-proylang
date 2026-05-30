package com.compilador;

import java.util.ArrayList;
import java.util.List;

/**
 * Genera CODIGO INTERMEDIO DE TRES DIRECCIONES para las expresiones
 * aritmeticas (los valores de 'duracion' y 'costo').
 *
 * El codigo de tres direcciones descompone una expresion compleja en
 * instrucciones simples con a lo sumo un operador, usando variables
 * temporales (t1, t2, ...). Por ejemplo:
 *
 *      costo = 10 * 1000 + 500
 *   se traduce a:
 *      t1 = 10 * 1000
 *      t2 = t1 + 500
 *
 * Es un Visitor que devuelve el "lugar" (place) donde quedo el resultado
 * de cada sub-expresion: un numero literal o un temporal.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class GeneradorTresDirecciones extends ProyLangBaseVisitor<String> {

    private int contadorTemporales = 0;
    private final List<String> instrucciones = new ArrayList<>();

    /** Genera un nuevo nombre de temporal: t1, t2, t3, ... */
    private String nuevoTemporal() {
        return "t" + (++contadorTemporales);
    }

    public List<String> getInstrucciones() {
        return instrucciones;
    }

    @Override
    public String visitNumero(ProyLangParser.NumeroContext ctx) {
        return ctx.NUMERO().getText();   // un literal es su propio "lugar"
    }

    @Override
    public String visitParentesis(ProyLangParser.ParentesisContext ctx) {
        return visit(ctx.expr());        // los parentesis no generan codigo
    }

    @Override
    public String visitNegativo(ProyLangParser.NegativoContext ctx) {
        String valor = visit(ctx.expr());
        String temp = nuevoTemporal();
        instrucciones.add(temp + " = - " + valor);
        return temp;
    }

    @Override
    public String visitMulDiv(ProyLangParser.MulDivContext ctx) {
        String izq = visit(ctx.expr(0));
        String der = visit(ctx.expr(1));
        String op = ctx.MUL() != null ? "*" : "/";
        String temp = nuevoTemporal();
        instrucciones.add(temp + " = " + izq + " " + op + " " + der);
        return temp;
    }

    @Override
    public String visitSumaResta(ProyLangParser.SumaRestaContext ctx) {
        String izq = visit(ctx.expr(0));
        String der = visit(ctx.expr(1));
        String op = ctx.MAS() != null ? "+" : "-";
        String temp = nuevoTemporal();
        instrucciones.add(temp + " = " + izq + " " + op + " " + der);
        return temp;
    }
}
