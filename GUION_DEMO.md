# 🎬 Guion de demostración — Compilador ProyLang

Guía paso a paso para presentar el proyecto al ingeniero.
**Autor:** Cesar Daniel Osorio Polanco

---

## 📍 PASO 0 — ¿Desde dónde se corre?

1. Abre **VS Code**.
2. Menú **Archivo → Abrir carpeta** → selecciona
   `C:\Users\PC\Desktop\compilador-dsl`
3. Abre la terminal: menú **Terminal → New Terminal**
   (se abre automáticamente dentro de la carpeta del proyecto).

> Verifica que la terminal diga al inicio de la línea:
> `PS C:\Users\PC\Desktop\compilador-dsl>`
> Si no, escribe:  `cd C:\Users\PC\Desktop\compilador-dsl`

---

## 📍 PASO 1 — Compilar el compilador (una sola vez)

Copia y pega:

```powershell
.\build.ps1
```

Debe terminar con:  `==> Compilacion exitosa.`

---

## 📍 PASO 2 — DEMO en orden (esto es lo que le enseñas al ingeniero)

### ✅ 2.1 — Programa válido (muestra las 4 fases + resultado)

```powershell
.\run.ps1 ejemplos\valido1.proy
```

**Qué señalar mientras corre:**
- "Aquí pasan las 4 fases: léxico, sintáctico, semántico y generación."
- "Calcula automáticamente el cronograma y la **ruta crítica**."
- "Genera 3 archivos en la carpeta `salida\`."

### 🔍 2.2 — Mostrar la fase léxica (tabla de tokens)

```powershell
.\run.ps1 ejemplos\valido1.proy --tokens
```

> "Esto demuestra el análisis léxico: cada token con su tipo y posición."

### ❌ 2.3 — Mostrar el manejo de errores (¡muy importante!)

```powershell
.\run.ps1 ejemplos\error_lexico.proy
.\run.ps1 ejemplos\error_sintactico.proy
.\run.ps1 ejemplos\error_semantico.proy
```

> "Cada tipo de error se detecta en su fase, con línea y columna, y el
> compilador se detiene sin avanzar."

### 📄 2.4 — Mostrar la salida generada

Abre en VS Code el archivo:
`salida\App_Movil_cronograma.json`  → el cronograma en JSON
`salida\App_Movil_3direcciones.txt` → el código de 3 direcciones
`salida\App_Movil_plan.txt`         → el plan legible

---

## 📍 PASO 3 — Si el ingeniero trae SU PROPIA prueba

1. En VS Code, click derecho sobre la carpeta `ejemplos` → **New File**
2. Nómbralo `prueba.proy`
3. Pega un programa (usa la plantilla de abajo) y guarda (Ctrl+S)
4. Córrelo:

```powershell
.\run.ps1 ejemplos\prueba.proy
```

**No hay que cambiar código.** El compilador acepta cualquier archivo `.proy`.

### Plantilla para escribir un programa en vivo

```
proyecto "Nombre del proyecto" {

    tarea tareaA {
        nombre      = "Primera tarea";
        duracion    = 4;
        costo       = 4 * 500;
        responsable = "Juan";
        prioridad   = alta;
    }

    tarea tareaB {
        duracion = 6;
        depende  = tareaA;
    }
}
```

**Reglas a recordar:**
- Cada atributo termina en `;`
- Los textos van entre `"comillas"`
- `duracion` es obligatoria y debe ser > 0
- `prioridad` solo: `alta`, `media` o `baja`
- `depende` solo apunta a tareas ya declaradas (sin ciclos)

---

## 📍 PASO 4 — Si pide modificar el lenguaje en vivo

Patrón de 4 pasos (ejemplo: agregar un atributo nuevo):
1. `grammar\ProyLang.g4` → agregar el token y la regla
2. `.\build.ps1` → **regenerar** el parser
3. `Tarea.java` → agregar el campo
4. `AnalizadorSemantico.java` → validarlo (y `GeneradorCodigo.java` para mostrarlo)

> Ver detalle completo en `docs\Documentacion_Tecnica.md`.

---

## 📍 Resumen de comandos (chuleta rápida)

| Acción | Comando |
|---|---|
| Compilar | `.\build.ps1` |
| Correr un archivo | `.\run.ps1 ejemplos\valido1.proy` |
| Ver tokens | `.\run.ps1 ejemplos\valido1.proy --tokens` |
| Probar errores | `.\run.ps1 ejemplos\error_semantico.proy` |

---

## 📍 Archivos de la presentación

- **PPT:** `docs\Presentacion_ProyLang.pptx`
- **Documento técnico:** `docs\Documentacion_Tecnica.md`
- **Repositorio:** https://github.com/CesarDanielOsorio/compilador-dsl-proylang
