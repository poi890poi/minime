param([Parameter(Mandatory=$true)][string]$Serial,[Parameter(Mandatory=$true)][string]$Plan,[string]$SdkDir=$env:ANDROID_HOME,[string]$Output='artifacts/legacy-study')
$ErrorActionPreference='Stop'
$adb=Join-Path $SdkDir 'platform-tools/adb.exe'
$studyPrevious=(& $adb -s $Serial shell settings get secure default_input_method).Trim()
New-Item -ItemType Directory -Force $Output | Out-Null
try {
    & $adb -s $Serial install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    if($LASTEXITCODE -ne 0) { throw 'Test installation failed' }
    & $adb -s $Serial push $Plan /sdcard/Android/data/app.minime.keyboard/files/legacy-plan.json
    if($LASTEXITCODE -ne 0) { throw 'Study plan transfer failed' }
    & $adb -s $Serial shell input keyevent KEYCODE_WAKEUP
    & $adb -s $Serial shell ime set com.google.android.apps.inputmethod.zhuyin/.ZhuyinInputMethodService
    if($LASTEXITCODE -ne 0) { throw 'Select and enable the reference IME first' }
    & $adb -s $Serial shell am force-stop app.minime.keyboard
    $studyResult=& $adb -s $Serial shell am instrument -w -e class dev.minime.ime.LegacyStudyTest app.minime.keyboard.test/android.test.InstrumentationTestRunner
    $studyResult | Write-Output
    & $adb -s $Serial pull /sdcard/Android/data/app.minime.keyboard/files/legacy-observations.json $Output
    if(($studyResult -join "`n") -notmatch 'OK \(1 test\)') { throw 'Reference study failed' }
} finally {
    try { & $adb -s $Serial shell ime set $studyPrevious }
    finally { & $adb -s $Serial shell input keyevent KEYCODE_SLEEP }
}
