# ============================================================
#  build.ps1  -  Compila el compilador ProyLang
#  1) Genera el lexer/parser desde la gramatica .g4 (ANTLR)
#  2) Compila todo el codigo Java
#  Autor: Cesar Daniel Osorio Polanco
# ============================================================
$ErrorActionPreference = "Stop"

$JAVA  = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe"
$JAVAC = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\javac.exe"
$ROOT  = $PSScriptRoot
$ANTLR = Join-Path $ROOT "lib\antlr-4.13.2-complete.jar"
$GEN   = Join-Path $ROOT "src\main\java\com\compilador"
$BUILD = Join-Path $ROOT "build"

Write-Host "==> [1/2] Generando lexer y parser con ANTLR..." -ForegroundColor Cyan
& $JAVA -jar $ANTLR -Dlanguage=Java -visitor -package com.compilador `
        -o $GEN (Join-Path $ROOT "grammar\ProyLang.g4")
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR generando con ANTLR" -ForegroundColor Red; exit 1 }

Write-Host "==> [2/2] Compilando codigo Java..." -ForegroundColor Cyan
$sources = Get-ChildItem -Recurse -Path (Join-Path $ROOT "src") -Filter *.java | ForEach-Object { $_.FullName }
& $JAVAC -encoding UTF-8 -cp $ANTLR -d $BUILD $sources
if ($LASTEXITCODE -ne 0) { Write-Host "ERROR compilando Java" -ForegroundColor Red; exit 1 }

Write-Host "==> Compilacion exitosa. Usa run.ps1 para ejecutar." -ForegroundColor Green
