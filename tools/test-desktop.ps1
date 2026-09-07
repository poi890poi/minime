param([string]$Classes='artifacts/desktop-classes',[string]$Output='artifacts/desktop-results.jsonl',[switch]$SkipCompile)
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if(!$SkipCompile) {
        New-Item -ItemType Directory -Force $Classes | Out-Null
        $sources=@(Get-ChildItem core/src/main/java -Recurse -Filter '*.java' | ForEach-Object FullName)
        & javac -encoding UTF-8 -d $Classes @sources core/src/test/java/dev/minime/core/DesktopEvaluation.java
        if($LASTEXITCODE -ne 0){throw 'Desktop Java compilation failed'}
        & g++ -std=c++17 -O2 -I .tools/native-sources/librime-1.16.1/src tools/desktop_rime.cpp -o artifacts/desktop-rime.exe
        if($LASTEXITCODE -ne 0){throw 'Desktop Rime bridge compilation failed'}
    }
    & java '-Dfile.encoding=UTF-8' -Xmx2g -cp $Classes dev.minime.core.DesktopEvaluation docs/conversation-ranking/corpus/inputs.tsv $Output (Join-Path (Get-Location) 'artifacts/desktop-rime.exe') (Join-Path (Get-Location) '.tools/rime-evaluation/msvc/dist/lib/rime.dll') (Join-Path (Get-Location) 'app/src/main/rimeAssets/rime') (Join-Path (Get-Location) 'artifacts/desktop-rime-user')
    if($LASTEXITCODE -ne 0){throw 'Desktop evaluation failed'}
} finally {Pop-Location}
