# ============================================================
#  run.ps1  -  Ejecuta el compilador ProyLang sobre un archivo
#  Uso:   .\run.ps1 ejemplos\valido1.proy
#         .\run.ps1 ejemplos\valido1.proy --tokens
#  Autor: Cesar Daniel Osorio Polanco
# ============================================================
param(
    [Parameter(Mandatory=$true)] [string]$archivo,
    [string]$opcion = ""
)
$JAVA  = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe"
$ROOT  = $PSScriptRoot
$ANTLR = Join-Path $ROOT "lib\antlr-4.13.2-complete.jar"
$BUILD = Join-Path $ROOT "build"

# Nos paramos en la carpeta del proyecto para que 'salida/' se cree alli.
Push-Location $ROOT
try {
    & $JAVA -cp "$BUILD;$ANTLR" com.compilador.Main $archivo $opcion
} finally {
    Pop-Location
}
