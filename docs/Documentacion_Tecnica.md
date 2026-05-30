# Documentación Técnica — Compilador ProyLang

**Proyecto:** Diseño e implementación de un compilador para un DSL
**Variante:** Lenguaje para gestión de proyectos (tareas, tiempos, dependencias)
**Autor:** Cesar Daniel Osorio Polanco
**Carné:** 0900-16-6607
**Herramientas:** ANTLR 4.13.2 + Java 17

---

## Índice

1. [Definición del lenguaje](#1-definición-del-lenguaje)
2. [Gramática formal (EBNF / BNF)](#2-gramática-formal-ebnf--bnf)
3. [Fase 1 — Análisis léxico](#3-fase-1--análisis-léxico)
4. [Fase 2 — Análisis sintáctico](#4-fase-2--análisis-sintáctico)
5. [Fase 3 — Análisis semántico](#5-fase-3--análisis-semántico)
6. [Fase 4 — Generación de código](#6-fase-4--generación-de-código)
7. [Manejo de errores](#7-manejo-de-errores)
8. [Ejemplos de programas](#8-ejemplos-de-programas)
9. [Cómo construir y ejecutar](#9-cómo-construir-y-ejecutar)

---

## 1. Definición del lenguaje

**ProyLang** es un lenguaje de dominio específico (DSL) cuyo propósito es **describir
proyectos**: un proyecto contiene **tareas**, y cada tarea tiene atributos como su
duración, costo, responsable y las tareas de las que **depende**.

A partir de esa descripción, el compilador calcula automáticamente el **cronograma**
del proyecto usando el **Método de la Ruta Crítica (CPM)**: cuándo puede iniciar y
terminar cada tarea, cuánta **holgura** tiene y cuál es la **duración total** del proyecto.

### Estructura general de un programa

```
proyecto "Nombre del proyecto" {
    tarea identificador {
        atributo = valor;
        ...
    }
    ...
}
```

### Palabras reservadas

| Palabra | Significado |
|---|---|
| `proyecto` | Declara un proyecto |
| `tarea` | Declara una tarea |
| `nombre` | Nombre descriptivo de la tarea (texto) |
| `duracion` | Duración en días (entero positivo) — **obligatorio** |
| `costo` | Costo monetario (número ≥ 0) |
| `responsable` | Persona responsable (texto) |
| `depende` | Lista de tareas predecesoras |

### Tipos de datos

| Tipo | Descripción | Ejemplo |
|---|---|---|
| **Número** | Enteros o decimales; admite expresiones aritméticas | `5`, `3.5`, `10 * 1000 + 500` |
| **Texto (string)** | Cadena entre comillas dobles | `"Diseño de la interfaz"` |
| **Identificador** | Nombre de una tarea | `diseno`, `backend` |

### Operadores

`+`  `-`  `*`  `/`  y paréntesis `( )`, con la precedencia matemática habitual
(primero `* /`, luego `+ -`; el `-` unario tiene la mayor precedencia).

### Comentarios

```
// comentario de una línea
/* comentario
   de varias líneas */
```

---

## 2. Gramática formal (EBNF / BNF)

### Reglas sintácticas (EBNF)

```ebnf
programa   ::= proyecto+ EOF

proyecto   ::= "proyecto" STRING "{" declTarea* "}"

declTarea  ::= "tarea" ID "{" atributo* "}"

atributo   ::= "nombre"      "=" STRING   ";"
             | "duracion"    "=" expr     ";"
             | "costo"       "=" expr     ";"
             | "responsable" "=" STRING   ";"
             | "depende"     "=" listaIds ";"

listaIds   ::= ID ("," ID)*

expr       ::= "-" expr                 (* menos unario *)
             | expr ("*" | "/") expr
             | expr ("+" | "-") expr
             | "(" expr ")"
             | NUMERO
```

### Reglas léxicas (tokens)

```ebnf
NUMERO     ::= [0-9]+ ("." [0-9]+)?
STRING     ::= '"' (cualquier carácter excepto comilla o salto de línea)* '"'
ID         ::= [a-zA-Z_] [a-zA-Z_0-9]*
WS         ::= (" " | "\t" | "\r" | "\n")+          -> se ignora
COMENTARIO ::= "//" ...  |  "/*" ... "*/"           -> se ignora
```

La gramática completa está en el archivo
[`grammar/ProyLang.g4`](../grammar/ProyLang.g4).

---

## 3. Fase 1 — Análisis léxico

El **lexer** (generado por ANTLR a partir de la sección léxica de la gramática)
recorre el texto de entrada carácter por carácter y lo agrupa en **tokens**
(las "palabras" del lenguaje): palabras reservadas, identificadores, números,
cadenas, símbolos, etc. Los espacios y comentarios se descartan.

**Ejemplo.** La línea `duracion = 5;` produce los tokens:

| Token | Texto |
|---|---|
| `KW_DURACION` | `duracion` |
| `ASIGNA` | `=` |
| `NUMERO` | `5` |
| `PYC` | `;` |

Si aparece un carácter que no pertenece al lenguaje (por ejemplo `@` o `#`),
se reporta un **error léxico** indicando línea y columna. Esto se implementó con
una regla *catch-all* (`CARACTER_INVALIDO`) que se inspecciona en `Main.java`.

Para ver la tabla de tokens de un archivo:

```powershell
.\run.ps1 ejemplos\valido1.proy --tokens
```

---

## 4. Fase 2 — Análisis sintáctico

El **parser** (también generado por ANTLR) toma la secuencia de tokens y verifica
que cumpla la **gramática**, construyendo un **árbol de sintaxis abstracta (AST)**.
Aquí se comprueba que la estructura sea correcta: que cada `tarea` tenga sus llaves,
que cada atributo termine en `;`, que los proyectos estén bien formados, etc.

Si la estructura es inválida (falta un `;`, una llave `}`, etc.), ANTLR genera un
**error sintáctico** que nuestro `ErrorListenerSintactico` captura y reporta con
línea y columna.

---

## 5. Fase 3 — Análisis semántico

Una vez que el programa es léxica y sintácticamente correcto, el analizador
semántico ([`AnalizadorSemantico.java`](../src/main/java/com/compilador/AnalizadorSemantico.java))
recorre el árbol, construye la **tabla de símbolos** y verifica que el programa
**tenga sentido**.

### Tabla de símbolos

Es un diccionario (`LinkedHashMap`) que asocia cada identificador de tarea con un
objeto `Tarea` que guarda sus atributos. Se usa, por ejemplo, para comprobar que
una dependencia apunte a una tarea que realmente existe.

### Validaciones implementadas

| # | Validación | Mensaje de error |
|---|---|---|
| 1 | Tarea con identificador duplicado | `La tarea 'a' ya fue declarada antes.` |
| 2 | Atributo repetido en la misma tarea | `El atributo 'duracion' está repetido…` |
| 3 | Atributo obligatorio `duracion` ausente | `…no tiene el atributo obligatorio 'duracion'.` |
| 4 | `duracion` debe ser entero positivo | `La 'duracion' debe ser mayor que 0…` |
| 5 | `costo` no puede ser negativo | `El 'costo' no puede ser negativo…` |
| 6 | Dependencia hacia tarea no declarada | `…depende de 'fantasma', que no está declarada.` |
| 7 | Auto-dependencia | `…no puede depender de sí misma.` |
| 8 | Dependencia duplicada | `Dependencia duplicada…` |
| 9 | **Dependencia circular** | `Dependencia circular detectada: c -> d -> c` |

La detección de **ciclos** (validación 9) usa una búsqueda en profundidad (DFS)
con coloreo de nodos (blanco/gris/negro): si durante el recorrido se llega a un
nodo que está "en proceso" (gris), existe un ciclo.

---

## 6. Fase 4 — Generación de código

Es la **traducción dirigida por la sintaxis**. Con el programa ya validado, se
genera la salida en la carpeta `salida/`:

### 6.1. Código intermedio de tres direcciones

Las expresiones aritméticas de `duracion` y `costo` se descomponen en
instrucciones simples con variables temporales (`t1`, `t2`, …). Por ejemplo,
`costo = 10 * 1000 + 500` se traduce a:

```
t1 = 10 * 1000
t2 = t1 + 500
backend.costo = t2
```

### 6.2. Cronograma en JSON (Método de la Ruta Crítica)

Se calcula, para cada tarea:

- **Inicio temprano (ES)** y **fin temprano (EF)** — recorrido hacia adelante.
- **Inicio tardío (LS)** y **fin tardío (LF)** — recorrido hacia atrás.
- **Holgura** = LS − ES. Si es 0, la tarea es **crítica**.
- **Duración total** del proyecto = mayor fin temprano.
- **Ruta crítica** = secuencia de tareas con holgura 0.

Fragmento del JSON generado para `valido1.proy`:

```json
{
  "proyecto": "App Movil",
  "duracionTotal": 21,
  "rutaCritica": ["diseno", "backend", "pruebas", "despliegue"],
  "tareas": [
    {
      "id": "backend", "duracion": 10, "costo": 10500,
      "inicioTemprano": 5, "finTemprano": 15,
      "holgura": 0, "critica": true,
      "dependencias": ["diseno"]
    }
    // ...
  ]
}
```

### 6.3. Plan legible (ejecución simulada)

Una tabla con todas las tareas, su cronograma, el costo total, la ruta crítica
y el orden sugerido de ejecución día por día. Ejemplo:

```
 EJECUCION SIMULADA (orden por fecha de inicio):
   dia 0  : iniciar 'diseno' (termina dia 5)
   dia 5  : iniciar 'backend' [tras diseno] (termina dia 15)
   dia 5  : iniciar 'frontend' [tras diseno] (termina dia 13)
   dia 15 : iniciar 'pruebas' [tras backend,frontend] (termina dia 19)
   dia 19 : iniciar 'despliegue' [tras pruebas] (termina dia 21)
```

---

## 7. Manejo de errores

El compilador **se detiene en la primera fase que encuentra errores** (como un
compilador real) y los reporta todos juntos con su **fase, línea y columna**,
gracias a la clase central `GestorErrores`.

- **Errores léxicos** → caracteres no válidos.
- **Errores sintácticos** → estructura mal formada (faltan `;`, `}`, etc.).
- **Errores semánticos** → las 9 validaciones de la sección 5.

---

## 8. Ejemplos de programas

### 8.1. Programa VÁLIDO

```
proyecto "App Movil" {
    tarea diseno {
        nombre      = "Diseño de la interfaz";
        duracion    = 5;
        costo       = 5 * 800;
        responsable = "Ana Lopez";
    }
    tarea backend {
        duracion = 10;
        costo    = 10 * 1000 + 500;
        depende  = diseno;
    }
}
```

**Resultado:** compila correctamente y genera el cronograma, el JSON y el código
de tres direcciones.

### 8.2. Programa INVÁLIDO (errores semánticos)

```
proyecto "Demo Semantico" {
    tarea a { duracion = 5; depende = fantasma; }   // 'fantasma' no existe
    tarea b { costo = -100; }                        // costo negativo y falta duracion
    tarea a { duracion = 2; }                        // 'a' duplicada
    tarea c { duracion = 3; depende = d; }
    tarea d { duracion = 4; depende = c; }           // ciclo c <-> d
}
```

**Resultado (salida real del compilador):**

```
>> FASE 3 - Analisis Semantico
   7 error(es) semantico(s):
   [Semantico] linea 24:10  ->  La tarea 'a' ya fue declarada antes.
   [Semantico] linea 16:19  ->  La tarea 'a' depende de 'fantasma', que no esta declarada.
   [Semantico] linea 20:8   ->  El 'costo' no puede ser negativo (se obtuvo -100.0).
   [Semantico] linea 29:8   ->  La 'duracion' debe ser mayor que 0 (se obtuvo 0).
   [Semantico] linea 30:19  ->  La tarea 'x' no puede depender de si misma.
   [Semantico] linea 19:0   ->  La tarea 'b' no tiene el atributo obligatorio 'duracion'.
   [Semantico] linea 33:0   ->  Dependencia circular detectada: c -> d -> c
```

Los demás ejemplos (`error_lexico.proy`, `error_sintactico.proy`) están en la
carpeta `ejemplos/`.

---

## 9. Cómo construir y ejecutar

```powershell
# 1) Compilar el compilador (genera parser + compila Java)
.\build.ps1

# 2) Ejecutar sobre un archivo .proy
.\run.ps1 ejemplos\valido1.proy

# 3) Ver además la tabla de tokens
.\run.ps1 ejemplos\valido1.proy --tokens
```

Las salidas se generan en la carpeta `salida/`.

---

## Resumen de la arquitectura

```
archivo.proy
     │
     ▼
[ Lexer ]  ──tokens──▶  [ Parser ]  ──AST──▶  [ Analizador Semántico ]
   Fase 1                 Fase 2                  Fase 3 (tabla símbolos)
                                                       │
                                                       ▼
                                            [ Generador de Código ]
                                                   Fase 4
                                       ┌───────────┼────────────┐
                                       ▼           ▼            ▼
                                  3 direcciones  JSON (CPM)   Plan
```

Cada fase alimenta a la siguiente; si una falla, el proceso se detiene y se
reportan los errores.
