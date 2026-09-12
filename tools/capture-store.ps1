param([string]$SdkDir='E:\Android\Sdk')
. "$PSScriptRoot/phone-lease.ps1"
Invoke-WithPhoneLease {
$ErrorActionPreference='Stop'
$serial='RFCR91GWXLX'
$adb=Join-Path $SdkDir 'platform-tools/adb.exe'
$size=(& $adb -s $serial shell wm size) -join "`n"
if($LASTEXITCODE -ne 0){throw 'Authorized phone unavailable'}
[IO.File]::WriteAllText((Join-Path $PSScriptRoot '../artifacts/play-materials/display-size-before.txt'),$size)
$override=if($size -match 'Override size: (\d+x\d+)'){$Matches[1]}else{$null}
try {
    & $adb -s $serial shell wm size 1080x1920
    if($LASTEXITCODE -ne 0){throw 'Could not set capture viewport'}
    & "$PSScriptRoot/test-device.ps1" -Serial $serial -SdkDir $SdkDir -TestClass dev.minime.ime.StoreCaptureTest -Reports store-candidates.json
    & $adb -s $serial pull /sdcard/Android/data/app.minime.keyboard/files/store-candidates.json artifacts/play-materials/store-candidates.json
    if($LASTEXITCODE -ne 0){throw 'Missing visible-candidate capture evidence'}
    foreach($name in @('01-chinese','02-english','03-taiwanese','04-japanese','05-geography')) {
        & $adb -s $serial pull "/sdcard/Android/data/app.minime.keyboard/files/store-$name.png" "docs/play-publishing/kit/screenshots/$name.png"
        if($LASTEXITCODE -ne 0){throw "Missing screenshot $name"}
    }
} finally {
    & $adb -s $serial pull /sdcard/Android/data/app.minime.keyboard/files/store-diagnostic.png artifacts/play-materials/diagnostic.png | Out-Null
    try {
        if($override){& $adb -s $serial shell wm size $override}else{& $adb -s $serial shell wm size reset}
        $after=(& $adb -s $serial shell wm size) -join "`n"
        if($LASTEXITCODE -ne 0 -or $after.Trim() -cne $size.Trim()){throw 'Display size restoration failed'}
        Write-Output 'Original display size restored and verified.'
    } finally {
        & $adb -s $serial shell input keyevent KEYCODE_SLEEP
        Start-Sleep -Milliseconds 500
        $display=(& $adb -s $serial shell dumpsys display) -join "`n"
        if($LASTEXITCODE -ne 0 -or !(Test-PhoneDisplayOff $display)){throw 'Display OFF not verified'}
        Write-Output 'Display OFF verified after viewport restoration.'
    }
}

}
