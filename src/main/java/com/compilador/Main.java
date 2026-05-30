package com.compilador;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.util.List;

/**
 * ============================================================
 *  COMPILADOR  ProyLang  -  Programa principal
 *  Lenguaje DSL para Gestion de Proyectos
 *  Autor: Cesar Daniel Osorio Polanco  (Carne 0900-16-6607)
 * ============================================================
 *
 *  Ejecuta las cuatro fases del compilador en orden:
 *    1. Analisis lexico     (tokens)
 *    2. Analisis sintactico (gramatica / parser)
 *    3. Analisis semantico  (tabla de simbolos y validaciones)
 *    4. Generacion de codigo (JSON, 3 direcciones, plan)
 *
 *  Si una fase encuentra errores, se reportan y NO se continua a
 *  la siguiente (igual que un compilador real).
 *
 *  Uso:  java com.compilador.Main  archivo.proy  [--tokens]
 */
public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Uso: java com.compilador.Main <archivo.proy> [--tokens]");
            return;
        }
        String ruta = args[0];
        boolean mostrarTokens = args.length > 1 && args[1].equals("--tokens");

        System.out.println("============================================================");
        System.out.println(" COMPILADOR ProyLang  -  Gestion de Proyectos");
        System.out.println(" Archivo: " + ruta);
        System.out.println("============================================================\n");

        GestorErrores gestor = new GestorErrores();

        // ---------- FASE 1: ANALISIS LEXICO ----------
        CharStream entrada = CharStreams.fromFileName(ruta);
        ProyLangLexer lexer = new ProyLangLexer(entrada);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new ErrorListenerSintactico(gestor));

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();   // forzamos la lectura de todos los tokens

        // Detectamos caracteres invalidos (token CARACTER_INVALIDO de la gramatica).
        for (Token t : tokens.getTokens()) {
            if (t.getType() == ProyLangLexer.CARACTER_INVALIDO) {
                gestor.agregar(GestorErrores.Fase.LEXICO, t.getLine(),
                        t.getCharPositionInLine(),
                        "Caracter no valido: '" + t.getText() + "'");
            }
        }

        if (mostrarTokens) imprimirTokens(tokens, lexer);

        System.out.println(">> FASE 1 - Analisis Lexico");
        if (gestor.hayErrores(GestorErrores.Fase.LEXICO)) {
            System.out.println("   " + gestor.contar(GestorErrores.Fase.LEXICO) + " error(es) lexico(s):");
            gestor.imprimir(GestorErrores.Fase.LEXICO);
            System.out.println("\nCompilacion detenida en la fase lexica.");
            return;
        }
        System.out.println("   OK - sin errores lexicos.\n");

        // ---------- FASE 2: ANALISIS SINTACTICO ----------
        ProyLangParser parser = new ProyLangParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ErrorListenerSintactico(gestor));
        ProyLangParser.ProgramaContext arbol = parser.programa();

        System.out.println(">> FASE 2 - Analisis Sintactico");
        if (gestor.hayErrores(GestorErrores.Fase.SINTACTICO)) {
            System.out.println("   " + gestor.contar(GestorErrores.Fase.SINTACTICO) + " error(es) sintactico(s):");
            gestor.imprimir(GestorErrores.Fase.SINTACTICO);
            System.out.println("\nCompilacion detenida en la fase sintactica.");
            return;
        }
        System.out.println("   OK - la gramatica es correcta.\n");

        // ---------- FASE 3: ANALISIS SEMANTICO ----------
        AnalizadorSemantico semantico = new AnalizadorSemantico(gestor);
        List<Proyecto> proyectos = semantico.analizar(arbol);

        System.out.println(">> FASE 3 - Analisis Semantico");
        if (gestor.hayErrores(GestorErrores.Fase.SEMANTICO)) {
            System.out.println("   " + gestor.contar(GestorErrores.Fase.SEMANTICO) + " error(es) semantico(s):");
            gestor.imprimir(GestorErrores.Fase.SEMANTICO);
            System.out.println("\nCompilacion detenida en la fase semantica.");
            return;
        }
        System.out.println("   OK - tabla de simbolos valida (" +
                contarTareas(proyectos) + " tareas en " + proyectos.size() + " proyecto(s)).\n");

        // ---------- FASE 4: GENERACION DE CODIGO ----------
        System.out.println(">> FASE 4 - Generacion de Codigo\n");
        GeneradorCodigo generador = new GeneradorCodigo("salida");
        generador.generar(proyectos);

        System.out.println("\n============================================================");
        System.out.println(" COMPILACION EXITOSA.");
        System.out.println("============================================================");
    }

    /** Muestra la lista de tokens (util para la fase lexica). */
    private static void imprimirTokens(CommonTokenStream tokens, ProyLangLexer lexer) {
        System.out.println(">> TABLA DE TOKENS (analisis lexico)");
        System.out.printf("   %-18s %-15s %s%n", "TIPO", "TEXTO", "POSICION");
        System.out.println("   --------------------------------------------------");
        for (Token t : tokens.getTokens()) {
            if (t.getType() == Token.EOF) continue;
            String tipo = lexer.getVocabulary().getSymbolicName(t.getType());
            System.out.printf("   %-18s %-15s linea %d:%d%n",
                    tipo, "'" + t.getText() + "'", t.getLine(), t.getCharPositionInLine());
        }
        System.out.println();
    }

    private static int contarTareas(List<Proyecto> proyectos) {
        int n = 0;
        for (Proyecto p : proyectos) n += p.tabla.cantidad();
        return n;
    }
}
