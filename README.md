# ProyLang — Compilador para un DSL de Gestión de Proyectos

**Autor:** Cesar Daniel Osorio Polanco
**Carné:** 0900-16-6607
**Variante asignada:** Lenguaje para gestión de proyectos (tareas, tiempos, dependencias)
**Tecnología:** ANTLR 4.13.2 + Java 17

---

## ¿Qué hace?

`ProyLang` es un lenguaje de dominio específico (DSL) para **describir proyectos**:
sus tareas, duraciones, costos, responsables y dependencias. El compilador lee un
archivo `.proy` y ejecuta las **cuatro fases** de un compilador:

1. **Análisis léxico** — convierte el texto en *tokens*.
2. **Análisis sintáctico** — verifica la gramática (parser).
3. **Análisis semántico** — tabla de símbolos y validaciones (tipos, dependencias, ciclos…).
4. **Generación de código** — produce:
   - Código intermedio de **3 direcciones** (de las expresiones).
   - Un **cronograma en JSON** calculado con el método de la **Ruta Crítica (CPM)**.
   - Un **plan legible** con la ejecución simulada del proyecto.

En cada fase se **detectan y reportan errores** con su línea y columna.

---

## Requisitos (ya instalados en esta máquina)

- **JDK 17** — `C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot`
- **ANTLR 4.13.2** — `lib\antlr-4.13.2-complete.jar`

---

## Cómo compilar el compilador

Desde PowerShell, dentro de la carpeta del proyecto:

```powershell
.\build.ps1
```

Esto: (1) genera el lexer/parser desde la gramática `grammar\ProyLang.g4`, y
(2) compila todo el código Java a la carpeta `build\`.

## Cómo ejecutar el compilador

```powershell
.\run.ps1 ejemplos\valido1.proy
```

Para ver además la tabla de tokens (fase léxica):

```powershell
.\run.ps1 ejemplos\valido1.proy --tokens
```

Las salidas (JSON, 3 direcciones, plan) se generan en la carpeta `salida\`.

---

## Ejemplos incluidos

| Archivo | Qué demuestra |
|---|---|
| `ejemplos\valido1.proy` | Proyecto válido completo (app móvil) |
| `ejemplos\valido2.proy` | Proyecto válido simple (evento) |
| `ejemplos\error_lexico.proy` | Caracteres inválidos (`@`, `#`) |
| `ejemplos\error_sintactico.proy` | Falta `;` y falta `}` |
| `ejemplos\error_semantico.proy` | 8 errores semánticos distintos |

---

## Estructura del proyecto

```
compilador-dsl\
├── grammar\
│   └── ProyLang.g4              # Gramática (léxico + sintaxis)
├── src\main\java\com\compilador\
│   ├── Main.java                # Orquesta las 4 fases
│   ├── GestorErrores.java       # Manejo de errores de todas las fases
│   ├── ErrorListenerSintactico.java
│   ├── EvaluadorExpr.java       # Evalúa expresiones (semántico)
│   ├── AnalizadorSemantico.java # Tabla de símbolos + validaciones
│   ├── TablaSimbolos.java
│   ├── Tarea.java / Proyecto.java
│   ├── GeneradorTresDirecciones.java
│   ├── GeneradorCodigo.java     # CPM + JSON + plan
│   └── ProyLang*.java           # (generados por ANTLR)
├── ejemplos\                    # Programas de prueba
├── salida\                      # Salidas generadas
├── lib\antlr-4.13.2-complete.jar
├── build.ps1 / run.ps1
└── docs\Documentacion_Tecnica.md
```

La documentación técnica detallada está en
[docs/Documentacion_Tecnica.md](docs/Documentacion_Tecnica.md).
