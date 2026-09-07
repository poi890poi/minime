param([ValidateSet('RFCR91GWXLX')][string]$Serial='RFCR91GWXLX',[string]$SdkDir='E:/Android/Sdk',
    [ValidateSet('Matrix','Development','Holdout','All')][string]$Phase='All',
    [string]$Output=('artifacts/human-input/'+[guid]::NewGuid().ToString()))
$ErrorActionPreference='Stop'
$adb=Join-Path $SdkDir 'platform-tools/adb.exe'
if(Test-Path -LiteralPath $Output){throw 'Use a new output directory to keep each run immutable'}
New-Item -ItemType Directory -Path $Output -Force | Out-Null
$rootOutput=(Resolve-Path -LiteralPath $Output).Path
$metadata=@{
    sourceHead=(& git rev-parse HEAD).Trim(); serial=$Serial; phase=$Phase; startedUtc=[DateTime]::UtcNow.ToString('o')
    appSha256=(Get-FileHash app/build/outputs/apk/debug/app-debug.apk -Algorithm SHA256).Hash.ToLower()
    testSha256=(Get-FileHash app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk -Algorithm SHA256).Hash.ToLower()
    fixtureSha256=(Get-FileHash app/src/androidTest/assets/human-input.json -Algorithm SHA256).Hash.ToLower()
}
$metadata | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $rootOutput 'run-metadata.json')
& python -X utf8 "$PSScriptRoot/verify_apk.py" app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk | Tee-Object -FilePath (Join-Path $rootOutput 'apk-verification.txt')
if($LASTEXITCODE -ne 0){throw 'APK or frozen test fixture verification failed before phone testing'}
$phases=if($Phase -eq 'All'){@('Matrix','Development','Holdout')}else{@($Phase)}
foreach($part in $phases) {
    $runId=[guid]::NewGuid().ToString()
    $runFile=Join-Path $rootOutput ($part+'-run-id.txt')
    [IO.File]::WriteAllText($runFile,$runId,[Text.UTF8Encoding]::new($false))
    & $adb -s $Serial push $runFile /sdcard/Android/data/dev.minime.ime/files/human-input-run-id.txt
    if($LASTEXITCODE -ne 0){throw 'Cannot transfer run identity'}
    $classes=switch($part) {
        Matrix {'dev.minime.ime.HumanInputPrecisionTest,dev.minime.ime.CandidateStabilityTest'}
        Development {'dev.minime.ime.HumanInputReplayTest#testDevelopmentReplay'}
        Holdout {'dev.minime.ime.HumanInputReplayTest#testHoldoutReplay,dev.minime.ime.KeyboardInteractionTest#testDoubleTapCapsLock,dev.minime.ime.KeyboardInteractionTest#testSlidesCaseNumbersAndCancellation,dev.minime.ime.KeyboardInteractionTest#testHeldDeleteStopsOnRelease'}
    }
    $files=switch($part) {Matrix {@('portrait','landscape')} Development {@('replay-dev')} Holdout {@('replay-test')}}
    try {
        & "$PSScriptRoot/test-device.ps1" -Serial $Serial -SdkDir $SdkDir -TestClass $classes | Tee-Object -FilePath (Join-Path $rootOutput ($part+'-instrumentation.txt'))
    } finally {
        $power=& $adb -s $Serial shell dumpsys power | Select-String 'mWakefulness='
        $power | Tee-Object -FilePath (Join-Path $rootOutput ($part+'-power.txt'))
        if(($power -join '') -notmatch 'mWakefulness=Dozing|mWakefulness=Asleep'){throw 'Phone display did not sleep'}
        foreach($name in $files) {
            $target=Join-Path $rootOutput ('human-input-'+$name+'.json')
            & $adb -s $Serial pull ('/sdcard/Android/data/dev.minime.ime/files/human-input-'+$name+'.json') $target
            if($LASTEXITCODE -ne 0){throw "Missing report for $part"}
            $record=Get-Content -Raw -LiteralPath $target | ConvertFrom-Json
            if($record.runId -ne $runId){throw "Stale report for $part"}
        }
    }
}
& python -X utf8 "$PSScriptRoot/summarize_human_input.py" $rootOutput
if($LASTEXITCODE -ne 0){throw 'Human input report has failing or incomplete gates'}
Write-Output "Human input evidence: $rootOutput"
