package com.compilador;

/**
 * Recorre el arbol de una expresion aritmetica y devuelve su valor
 * numerico. Se usa en el analisis semantico para conocer, por ejemplo,
 * el valor concreto de 'duracion = 5 * 2' (= 10) y poder validar que
 * sea positivo.
 *
 * Es un Visitor: ANTLR genero la clase base ProyLangBaseVisitor con un
 * metodo por cada regla; aqui sobre-escribimos los que nos interesan.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class EvaluadorExpr extends ProyLangBaseVisitor<Double> {

    @Override
    public Double visitNumero(ProyLangParser.NumeroContext ctx) {
        return Double.parseDouble(ctx.NUMERO().getText());
    }

    @Override
    public Double visitParentesis(ProyLangParser.ParentesisContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public Double visitNegativo(ProyLangParser.NegativoContext ctx) {
        return -visit(ctx.expr());
    }

    @Override
    public Double visitMulDiv(ProyLangParser.MulDivContext ctx) {
        double izq = visit(ctx.expr(0));
        double der = visit(ctx.expr(1));
        if (ctx.MUL() != null) {
            return izq * der;
        } else {
            return der == 0 ? 0 : izq / der;   // evitamos division por cero
        }
    }

    @Override
    public Double visitSumaResta(ProyLangParser.SumaRestaContext ctx) {
        double izq = visit(ctx.expr(0));
        double der = visit(ctx.expr(1));
        return ctx.MAS() != null ? izq + der : izq - der;
    }
}
