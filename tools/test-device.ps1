param([Parameter(Mandatory=$true)][string]$Serial,[string]$SdkDir=$env:ANDROID_HOME,
    [string]$TestClass='dev.minime.ime.EditorIntegrationTest,dev.minime.ime.KeyboardInteractionTest')
$ErrorActionPreference='Stop'
if (!$SdkDir) { throw 'Pass -SdkDir or set ANDROID_HOME' }
$adb=Join-Path $SdkDir 'platform-tools/adb.exe'
$previousIme=(& $adb -s $Serial shell settings get secure default_input_method).Trim()
if($LASTEXITCODE -ne 0) { throw 'Device is unavailable' }
Push-Location (Split-Path $PSScriptRoot -Parent)
$prefBackup=Join-Path (Get-Location) ('artifacts/device-tests/'+[guid]::NewGuid().ToString())
New-Item -ItemType Directory -Force $prefBackup | Out-Null
$backedUp=@()
try {
    & $adb -s $Serial install -r app/build/outputs/apk/debug/app-debug.apk
    if($LASTEXITCODE -ne 0) { throw 'App installation failed' }
    & $adb -s $Serial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if($LASTEXITCODE -ne 0) { throw 'Test installation failed' }
    & $adb -s $Serial shell am force-stop dev.minime.ime
    foreach($name in @('settings','learning')) {
        $xml=& $adb -s $Serial shell run-as dev.minime.ime cat "shared_prefs/$name.xml" 2>&1
        if($LASTEXITCODE -ne 0) {
            if(($xml -join '') -notmatch 'No such file') {throw "Could not back up $name preferences"}
            $xml='<map />'
        }
        [IO.File]::WriteAllText((Join-Path $prefBackup "$name.xml"),($xml -join "`n"),[Text.UTF8Encoding]::new($false))
        $backedUp+=$name
    }
    & $adb -s $Serial shell input keyevent KEYCODE_WAKEUP
    & $adb -s $Serial shell ime enable dev.minime.ime/.MiniMeService
    & $adb -s $Serial shell ime set dev.minime.ime/.MiniMeService
    $outFile=Join-Path $prefBackup 'instrumentation.txt'
    $errFile=Join-Path $prefBackup 'instrumentation-errors.txt'
    $testProcess=Start-Process -FilePath $adb -ArgumentList @('-s',$Serial,'shell','am','instrument','-w','-e','class',$TestClass,'dev.minime.ime.test/android.test.InstrumentationTestRunner') -WindowStyle Hidden -PassThru -RedirectStandardOutput $outFile -RedirectStandardError $errFile
    $timer=[Diagnostics.Stopwatch]::StartNew()
    while(!$testProcess.WaitForExit(1000)) {
        if($timer.Elapsed.TotalSeconds -gt 180) {
            & $adb -s $Serial shell am force-stop dev.minime.ime
            if(!$testProcess.WaitForExit(5000)) {$testProcess.Kill()}
            throw 'Phone test exceeded 180 seconds; original preferences will be restored'
        }
    }
    $result=Get-Content -LiteralPath $outFile
    $result | Write-Output
    if($testProcess.ExitCode -ne 0 -or ($result -join "`n") -notmatch 'OK \(\d+ tests?\)') { throw 'Device checks failed; see artifacts/device-tests' }
} finally {
    try {
        & $adb -s $Serial shell am force-stop dev.minime.ime
        foreach($name in $backedUp) {
            & $adb -s $Serial push (Join-Path $prefBackup "$name.xml") "/sdcard/Android/data/dev.minime.ime/files/restore-$name.xml" | Out-Null
            if($LASTEXITCODE -ne 0) {throw "Could not transfer $name preference backup"}
            & $adb -s $Serial shell run-as dev.minime.ime cp "/sdcard/Android/data/dev.minime.ime/files/restore-$name.xml" "shared_prefs/$name.xml"
            if($LASTEXITCODE -ne 0) {throw "Could not restore $name preferences"}
        }
    } finally { try {
        if($previousIme -and $previousIme -ne 'null') { & $adb -s $Serial shell ime set $previousIme }
    } finally {
        & $adb -s $Serial shell input keyevent KEYCODE_SLEEP
        Pop-Location
    } }
}
