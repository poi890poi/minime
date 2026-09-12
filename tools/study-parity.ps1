param([Parameter(Mandatory=$true)][ValidateSet('RFCR91GWXLX')][string]$Serial,[Parameter(Mandatory=$true)][string]$Plan,
    [Parameter(Mandatory=$true)][string]$Output,[string]$SdkDir='E:\Android\Sdk')
$ErrorActionPreference='Stop'
$Plan=(Resolve-Path -LiteralPath $Plan).Path
. "$PSScriptRoot/phone-lease.ps1"
Invoke-WithPhoneLease {
$adb=Join-Path $SdkDir 'platform-tools/adb.exe'
New-Item -ItemType Directory -Force $Output | Out-Null
try {
& $adb -s $Serial shell dumpsys package com.google.android.apps.inputmethod.zhuyin | Select-String 'versionCode=|versionName=' | Set-Content (Join-Path $Output 'google-version.txt')
& $adb -s $Serial push $Plan /sdcard/Android/data/app.minime.keyboard/files/parity-plan.json
if($LASTEXITCODE -ne 0){throw 'Plan transfer failed'}
try {
    & "$PSScriptRoot/test-device.ps1" -Serial $Serial -SdkDir $SdkDir -TestClass dev.minime.ime.ParityStudyTest -TimeoutSeconds 900
} finally {
    & $adb -s $Serial pull /sdcard/Android/data/app.minime.keyboard/files/parity-observations.json (Join-Path $Output 'observations.json')
    $cases=@(Get-Content $Plan -Raw | ConvertFrom-Json)
    $records=@(Get-Content (Join-Path $Output 'observations.json') -Raw | ConvertFrom-Json)
    $expected=@()
    foreach($record in $records){
        $case=@($cases | Where-Object id -eq $record.id)[0]
        if(!$case){continue}
        $prefix='parity-'+$record.provider+'-'+$record.id+'-'
        if($record.status -eq 'unavailable'){$expected+=$prefix+'failure.png'}
        for($j=0;$j -lt $case.actions.Count;$j++){
            $action=$case.actions[$j]
            $stage=if($action.label){$action.label}else{'step-'+$j}
            if($action.capture -and @($record.steps | Where-Object stage -eq $stage).Count){$expected+=$prefix+'step-'+$j+'.png'}
            if($action.hold -gt 0 -and @($record.steps | Where-Object stage -eq ('held-'+$action.key)).Count){$expected+=$prefix+'held-'+$action.key+'.png'}
        }
    }
    $images=& $adb -s $Serial shell 'ls /sdcard/Android/data/app.minime.keyboard/files/parity-*.png'
    foreach($remote in $images){
        if($expected -contains ($remote.Trim() -split '/')[-1]){& $adb -s $Serial pull $remote.Trim() $Output | Out-Null}
    }
}
} finally {
    & $adb -s $Serial shell input keyevent KEYCODE_SLEEP
    Start-Sleep -Milliseconds 500
    $studyDisplay=(& $adb -s $Serial shell dumpsys display) -join "`n"
    [IO.File]::WriteAllText((Join-Path $Output 'display-after-collection.txt'),$studyDisplay)
    if($LASTEXITCODE -ne 0 -or !(Test-PhoneDisplayOff $studyDisplay)){throw 'Reference study display OFF could not be verified'}
    Write-Output 'Reference study cleanup verified: display OFF after evidence collection.'
}
}
