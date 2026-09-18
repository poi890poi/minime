param(
    [string]$Baseline='dbe7722',
    [string]$Python='python',
    [string]$Output='artifacts/chinese-recovery-run'
)
$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    # Desktop native runtime must already be built by tools/test-desktop.ps1.
    & tools/test-core.ps1
    if($LASTEXITCODE -ne 0){throw 'Core gate failed'}
    New-Item -ItemType Directory -Force "$Output/baseline" | Out-Null
    $utf8=[Text.UTF8Encoding]::new($false)
    foreach($name in @('PhoneticDictionary','CompositionEngine')) {
        $lines=& git show "${Baseline}:core/src/main/java/dev/minime/core/$name.java"
        if($LASTEXITCODE -ne 0){throw "Cannot read baseline $name"}
        [IO.File]::WriteAllText((Join-Path (Get-Location) "$Output/baseline/$name.java"),($lines -join "`n")+"`n",$utf8)
    }
    & javac -encoding UTF-8 -cp core/build/manual -d "$Output/baseline" "$Output/baseline/PhoneticDictionary.java" "$Output/baseline/CompositionEngine.java"
    if($LASTEXITCODE -ne 0){throw 'Baseline compilation failed'}
    $env:PATH=(Join-Path (Get-Location) '.tools/rime-evaluation/msvc/dist/lib')+';'+$env:PATH
    foreach($profile in @(@('core','none','corpus'),@('core','packs','corpus'),@('native','packs','corpus'),@('core','packs','imprecision'))) {
        $name=$profile -join '-'
        foreach($side in @('before','after')) {
            $classes=if($side -eq 'before'){"$Output/baseline;core/build/manual"}else{'core/build/manual'}
            & java '-Dfile.encoding=UTF-8' -Xmx2g -cp $classes dev.minime.core.ChineseRecoveryEvaluation "docs/chinese-recovery/$($profile[2]).tsv.gz" "$Output/$name-$side.tsv" $profile[0] $profile[1]
            if($LASTEXITCODE -ne 0){throw "$name $side behavioral contracts failed"}
        }
        & $Python -X utf8 tools/summarize_chinese_recovery.py "$Output/$name-before.tsv" "$Output/$name-after.tsv" "$Output/$name.json"
        if($LASTEXITCODE -ne 0){throw "$name report failed"}
        $report=Get-Content "$Output/$name.json" -Raw | ConvertFrom-Json
        if($report.lost_targets.Count -gt 0){throw "$name lost previously reachable whole targets; inspect the report"}
        if($profile[0] -eq 'core' -and $report.changed_space.Count -gt 0){throw "$name changed Space acceptance; inspect the report"}
    }
    Write-Host "Behavioral gates passed. Inspect target coverage and rank regressions; this is not a language quality or release approval. Reports: $Output"
} finally {Pop-Location}
