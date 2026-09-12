param([Parameter(Mandatory=$true)][ValidateSet('RFCR91GWXLX')][string]$Serial,[string]$SdkDir=$env:ANDROID_HOME,
    [string]$TestClass='dev.minime.ime.EditorIntegrationTest,dev.minime.ime.KeyboardInteractionTest,dev.minime.ime.RimeIntegrationTest',
    [string]$AppApk='app/build/outputs/apk/debug/app-debug.apk',
    [string]$TestApk='app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk',
    [ValidateRange(30,900)][int]$TimeoutSeconds=180,
    [string[]]$Reports=@())
foreach($report in $Reports) {if($report -notmatch '^[a-zA-Z0-9][a-zA-Z0-9_.-]*$'){throw 'Report must be a plain file name'}}
# Resolve install inputs before taking a device lease or issuing any ADB command.
$AppApk=(Resolve-Path -LiteralPath $AppApk -ErrorAction Stop).Path
$TestApk=(Resolve-Path -LiteralPath $TestApk -ErrorAction Stop).Path
. "$PSScriptRoot/phone-lease.ps1"
Invoke-WithPhoneLease {
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
    & $adb -s $Serial install -r -t $AppApk
    if($LASTEXITCODE -ne 0) { throw 'App installation failed' }
    & $adb -s $Serial install -r -t $TestApk
    if($LASTEXITCODE -ne 0) { throw 'Test installation failed' }
    & $adb -s $Serial shell am force-stop app.minime.keyboard
    foreach($name in @('settings','learning')) {
        $xml=& $adb -s $Serial shell run-as app.minime.keyboard cat "shared_prefs/$name.xml" 2>&1
        if($LASTEXITCODE -ne 0) {
            if(($xml -join '') -notmatch 'No such file') {throw "Could not back up $name preferences"}
            $xml='<map />'
        }
        [IO.File]::WriteAllText((Join-Path $prefBackup "$name.xml"),($xml -join "`n"),[Text.UTF8Encoding]::new($false))
        $backedUp+=$name
    }
    & $adb -s $Serial shell input keyevent KEYCODE_WAKEUP
    & $adb -s $Serial shell ime enable app.minime.keyboard/dev.minime.ime.MiniMeService
    & $adb -s $Serial shell ime set app.minime.keyboard/dev.minime.ime.MiniMeService
    $outFile=Join-Path $prefBackup 'instrumentation.txt'
    $errFile=Join-Path $prefBackup 'instrumentation-errors.txt'
    $testProcess=Start-Process -FilePath $adb -ArgumentList @('-s',$Serial,'shell','am','instrument','-w','-e','class',$TestClass,'app.minime.keyboard.test/android.test.InstrumentationTestRunner') -WindowStyle Hidden -PassThru -RedirectStandardOutput $outFile -RedirectStandardError $errFile
    $timer=[Diagnostics.Stopwatch]::StartNew()
    while(!$testProcess.WaitForExit(1000)) {
        if($timer.Elapsed.TotalSeconds -gt $TimeoutSeconds) {
            & $adb -s $Serial shell am force-stop app.minime.keyboard
            if(!$testProcess.WaitForExit(5000)) {$testProcess.Kill()}
            throw "Phone test exceeded $TimeoutSeconds seconds; original preferences will be restored"
        }
    }
    $result=Get-Content -LiteralPath $outFile
    $result | Write-Output
    if($testProcess.ExitCode -ne 0 -or ($result -join "`n") -notmatch 'OK \(\d+ tests?\)') { throw 'Device checks failed; see artifacts/device-tests' }
} finally {
    try {
        & $adb -s $Serial shell am force-stop app.minime.keyboard
        foreach($report in $Reports) {
            & $adb -s $Serial pull "/sdcard/Android/data/app.minime.keyboard/files/$report" (Join-Path $prefBackup $report)
            if($LASTEXITCODE -ne 0){Write-Warning "Test report unavailable: $report"}
        }
        foreach($name in $backedUp) {
            & $adb -s $Serial push (Join-Path $prefBackup "$name.xml") "/sdcard/Android/data/app.minime.keyboard/files/restore-$name.xml" | Out-Null
            if($LASTEXITCODE -ne 0) {throw "Could not transfer $name preference backup"}
            & $adb -s $Serial shell run-as app.minime.keyboard cp "/sdcard/Android/data/app.minime.keyboard/files/restore-$name.xml" "shared_prefs/$name.xml"
            if($LASTEXITCODE -ne 0) {throw "Could not restore $name preferences"}
            $restored=(& $adb -s $Serial shell run-as app.minime.keyboard cat "shared_prefs/$name.xml") -join "`n"
            if($LASTEXITCODE -ne 0 -or $restored.Trim() -cne ([IO.File]::ReadAllText((Join-Path $prefBackup "$name.xml"))).Trim()) {throw "$name preference readback mismatch"}
        }
    } finally { try {
        if($previousIme -and $previousIme -ne 'null') {
            & $adb -s $Serial shell ime set $previousIme
            if($LASTEXITCODE -ne 0 -or (& $adb -s $Serial shell settings get secure default_input_method).Trim() -ne $previousIme) {throw 'Previous IME restoration failed'}
        }
    } finally {
        & $adb -s $Serial shell input keyevent KEYCODE_SLEEP
        try {
            Start-Sleep -Milliseconds 500
            $display=(& $adb -s $Serial shell dumpsys display) -join "`n"
            [IO.File]::WriteAllText((Join-Path $prefBackup 'display-after.txt'),$display)
            if($LASTEXITCODE -ne 0 -or !(Test-PhoneDisplayOff $display)) {throw 'Display OFF could not be verified'}
            Write-Output 'Cleanup verified: preferences, previous IME, display OFF.'
            Write-Output "Session evidence: $prefBackup"
        } finally {Pop-Location}
    } }
}

}
