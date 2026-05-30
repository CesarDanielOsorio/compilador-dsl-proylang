package com.compilador;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Escucha los errores que ANTLR detecta durante el analisis y los
 * redirige a nuestro {@link GestorErrores}.
 *
 * ANTLR llama a syntaxError() tanto para el lexer como para el parser.
 * Distinguimos uno de otro preguntando si quien reporta es un Lexer.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class ErrorListenerSintactico extends BaseErrorListener {

    private final GestorErrores gestor;

    public ErrorListenerSintactico(GestorErrores gestor) {
        this.gestor = gestor;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int linea,
                            int columna,
                            String mensaje,
                            RecognitionException e) {
        GestorErrores.Fase fase = (recognizer instanceof Lexer)
                ? GestorErrores.Fase.LEXICO
                : GestorErrores.Fase.SINTACTICO;
        gestor.agregar(fase, linea, columna, mensaje);
    }
}
