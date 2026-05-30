package com.compilador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GENERACION DE CODIGO (Traduccion dirigida por la sintaxis).
 *
 * Esta es la ultima fase. Una vez que el programa es valido, produce
 * la salida del compilador:
 *
 *   1. Codigo intermedio de TRES DIRECCIONES de las expresiones.
 *   2. Un CRONOGRAMA en formato JSON, calculando con el metodo de la
 *      Ruta Critica (CPM) el inicio/fin de cada tarea, su holgura y
 *      la duracion total del proyecto.
 *   3. Un PLAN legible (pseudocodigo) con la ejecucion simulada.
 *
 * Autor: Cesar Daniel Osorio Polanco
 */
public class GeneradorCodigo {

    private final Path dirSalida;

    public GeneradorCodigo(String dirSalida) {
        this.dirSalida = Paths.get(dirSalida);
    }

    /** Genera todas las salidas para todos los proyectos. */
    public void generar(List<Proyecto> proyectos) throws IOException {
        Files.createDirectories(dirSalida);
        for (Proyecto p : proyectos) {
            calcularRutaCritica(p);
            String base = nombreSeguro(p.nombre);

            String tac = generarTresDirecciones(p);
            String json = generarJson(p);
            String plan = generarPlan(p);

            escribir(base + "_3direcciones.txt", tac);
            escribir(base + "_cronograma.json", json);
            escribir(base + "_plan.txt", plan);

            // Tambien mostramos un resumen en consola.
            System.out.println(plan);
            System.out.println("   Archivos generados en la carpeta 'salida/':");
            System.out.println("     - " + base + "_cronograma.json");
            System.out.println("     - " + base + "_3direcciones.txt");
            System.out.println("     - " + base + "_plan.txt");
        }
    }

    // ============================================================
    //  1) METODO DE LA RUTA CRITICA (CPM)
    // ============================================================
    private void calcularRutaCritica(Proyecto p) {
        TablaSimbolos tabla = p.tabla;

        // Construir sucesores e indegree (grado de entrada) para orden topologico.
        Map<String, List<String>> sucesores = new HashMap<>();
        Map<String, Integer> gradoEntrada = new HashMap<>();
        for (Tarea t : tabla.todas()) {
            sucesores.put(t.id, new ArrayList<>());
            gradoEntrada.put(t.id, 0);
        }
        for (Tarea t : tabla.todas()) {
            for (String dep : t.dependencias) {
                sucesores.get(dep).add(t.id);          // arista dep -> t
                gradoEntrada.put(t.id, gradoEntrada.get(t.id) + 1);
            }
        }

        // Orden topologico (algoritmo de Kahn).
        Deque<String> cola = new ArrayDeque<>();
        for (Tarea t : tabla.todas()) {
            if (gradoEntrada.get(t.id) == 0) cola.add(t.id);
        }
        List<String> ordenTopo = new ArrayList<>();
        Map<String, Integer> grado = new HashMap<>(gradoEntrada);
        while (!cola.isEmpty()) {
            String id = cola.poll();
            ordenTopo.add(id);
            for (String suc : sucesores.get(id)) {
                grado.put(suc, grado.get(suc) - 1);
                if (grado.get(suc) == 0) cola.add(suc);
            }
        }

        // Paso hacia adelante: inicio/fin temprano (ES/EF).
        for (String id : ordenTopo) {
            Tarea t = tabla.obtener(id);
            int es = 0;
            for (String dep : t.dependencias) {
                es = Math.max(es, tabla.obtener(dep).finTemprano);
            }
            t.inicioTemprano = es;
            t.finTemprano = es + t.duracion;
        }

        // Duracion total = el mayor fin temprano.
        int duracionTotal = 0;
        for (Tarea t : tabla.todas()) duracionTotal = Math.max(duracionTotal, t.finTemprano);
        p.duracionTotal = duracionTotal;

        // Paso hacia atras: inicio/fin tardio (LS/LF) y holgura.
        List<String> ordenInverso = new ArrayList<>(ordenTopo);
        Collections.reverse(ordenInverso);
        for (String id : ordenInverso) {
            Tarea t = tabla.obtener(id);
            if (sucesores.get(id).isEmpty()) {
                t.finTardio = duracionTotal;
            } else {
                int lf = Integer.MAX_VALUE;
                for (String suc : sucesores.get(id)) {
                    lf = Math.min(lf, tabla.obtener(suc).inicioTardio);
                }
                t.finTardio = lf;
            }
            t.inicioTardio = t.finTardio - t.duracion;
            t.holgura = t.inicioTardio - t.inicioTemprano;
            t.critica = (t.holgura == 0);
        }
    }

    // ============================================================
    //  2) CODIGO DE TRES DIRECCIONES
    // ============================================================
    private String generarTresDirecciones(Proyecto p) {
        StringBuilder sb = new StringBuilder();
        sb.append("; Codigo intermedio de tres direcciones\n");
        sb.append("; Proyecto: ").append(p.nombre).append("\n");
        sb.append("; -----------------------------------------\n\n");
        for (Tarea t : p.tabla.todas()) {
            sb.append("; --- Tarea: ").append(t.id).append(" ---\n");
            if (t.exprDuracion != null) {
                GeneradorTresDirecciones g = new GeneradorTresDirecciones();
                String lugar = g.visit(t.exprDuracion);
                for (String ins : g.getInstrucciones()) sb.append(ins).append("\n");
                sb.append(t.id).append(".duracion = ").append(lugar).append("\n");
            }
            if (t.exprCosto != null) {
                GeneradorTresDirecciones g = new GeneradorTresDirecciones();
                String lugar = g.visit(t.exprCosto);
                for (String ins : g.getInstrucciones()) sb.append(ins).append("\n");
                sb.append(t.id).append(".costo = ").append(lugar).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ============================================================
    //  3) CRONOGRAMA EN JSON
    // ============================================================
    private String generarJson(Proyecto p) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"proyecto\": \"").append(escapar(p.nombre)).append("\",\n");
        sb.append("  \"duracionTotal\": ").append(p.duracionTotal).append(",\n");
        sb.append("  \"unidad\": \"dias\",\n");

        // Ruta critica
        List<String> criticas = new ArrayList<>();
        for (Tarea t : p.tabla.todas()) if (t.critica) criticas.add(t.id);
        sb.append("  \"rutaCritica\": [");
        for (int i = 0; i < criticas.size(); i++) {
            sb.append("\"").append(criticas.get(i)).append("\"");
            if (i < criticas.size() - 1) sb.append(", ");
        }
        sb.append("],\n");

        // Tareas
        sb.append("  \"tareas\": [\n");
        List<Tarea> lista = new ArrayList<>(p.tabla.todas());
        for (int i = 0; i < lista.size(); i++) {
            Tarea t = lista.get(i);
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(t.id).append("\",\n");
            sb.append("      \"nombre\": \"").append(escapar(t.nombre == null ? t.id : t.nombre)).append("\",\n");
            sb.append("      \"responsable\": \"").append(escapar(t.responsable == null ? "" : t.responsable)).append("\",\n");
            sb.append("      \"duracion\": ").append(t.duracion).append(",\n");
            sb.append("      \"costo\": ").append(formatoNum(t.costo)).append(",\n");
            sb.append("      \"inicioTemprano\": ").append(t.inicioTemprano).append(",\n");
            sb.append("      \"finTemprano\": ").append(t.finTemprano).append(",\n");
            sb.append("      \"inicioTardio\": ").append(t.inicioTardio).append(",\n");
            sb.append("      \"finTardio\": ").append(t.finTardio).append(",\n");
            sb.append("      \"holgura\": ").append(t.holgura).append(",\n");
            sb.append("      \"critica\": ").append(t.critica).append(",\n");
            sb.append("      \"dependencias\": [");
            for (int j = 0; j < t.dependencias.size(); j++) {
                sb.append("\"").append(t.dependencias.get(j)).append("\"");
                if (j < t.dependencias.size() - 1) sb.append(", ");
            }
            sb.append("]\n");
            sb.append("    }").append(i < lista.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ============================================================
    //  4) PLAN LEGIBLE (pseudocodigo / ejecucion simulada)
    // ============================================================
    private String generarPlan(Proyecto p) {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append(" PLAN DEL PROYECTO: ").append(p.nombre).append("\n");
        sb.append("============================================================\n");
        sb.append(" Tareas: ").append(p.tabla.cantidad())
          .append("   |   Duracion total estimada: ").append(p.duracionTotal).append(" dias\n\n");

        sb.append(String.format(" %-12s %-18s %4s %6s %6s %6s %4s%n",
                "TAREA", "NOMBRE", "DUR", "INI", "FIN", "HOLG", "CRIT"));
        sb.append(" ------------------------------------------------------------\n");
        double costoTotal = 0;
        for (Tarea t : p.tabla.todas()) {
            costoTotal += t.costo;
            String nom = t.nombre == null ? "-" : t.nombre;
            if (nom.length() > 18) nom = nom.substring(0, 17) + ".";
            sb.append(String.format(" %-12s %-18s %4d %6d %6d %6d %4s%n",
                    t.id, nom, t.duracion, t.inicioTemprano, t.finTemprano,
                    t.holgura, t.critica ? "SI" : ""));
        }
        sb.append(" ------------------------------------------------------------\n");
        sb.append(String.format(" Costo total del proyecto: %s%n", formatoNum(costoTotal)));

        // Ruta critica
        List<String> criticas = new ArrayList<>();
        for (Tarea t : p.tabla.todas()) if (t.critica) criticas.add(t.id);
        sb.append(" Ruta critica: ").append(String.join(" -> ", criticas)).append("\n\n");

        // Orden sugerido de ejecucion (por inicio temprano)
        List<Tarea> orden = new ArrayList<>(p.tabla.todas());
        orden.sort((a, b) -> a.inicioTemprano != b.inicioTemprano
                ? Integer.compare(a.inicioTemprano, b.inicioTemprano)
                : a.id.compareTo(b.id));
        sb.append(" EJECUCION SIMULADA (orden por fecha de inicio):\n");
        for (Tarea t : orden) {
            sb.append(String.format("   dia %-3d: iniciar '%s'%s (termina dia %d)%n",
                    t.inicioTemprano, t.id,
                    t.dependencias.isEmpty() ? "" : " [tras " + String.join(",", t.dependencias) + "]",
                    t.finTemprano));
        }
        return sb.toString();
    }

    // ---------- utilidades ----------
    private void escribir(String archivo, String contenido) throws IOException {
        Files.write(dirSalida.resolve(archivo), contenido.getBytes(StandardCharsets.UTF_8));
    }

    private String nombreSeguro(String s) {
        return s.replaceAll("[^a-zA-Z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private String escapar(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String formatoNum(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
