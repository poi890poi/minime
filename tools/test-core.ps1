$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    New-Item -ItemType Directory -Force 'core/build/manual' | Out-Null
    $sources = @(Get-ChildItem 'core/src/main/java','core/src/test/java' -Recurse -Filter '*.java' | ForEach-Object FullName)
    & javac -encoding UTF-8 -d core/build/manual @sources
    if ($LASTEXITCODE -ne 0) { throw 'Core compilation failed' }
    & java '-Dfile.encoding=UTF-8' -Xmx1g -cp core/build/manual dev.minime.core.Regression
    if ($LASTEXITCODE -ne 0) { throw 'Core regression failed' }
} finally { Pop-Location }
