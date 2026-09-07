param([Parameter(Mandatory=$true)][string]$Serial,[Parameter(Mandatory=$true)][string]$Plan,
    [Parameter(Mandatory=$true)][string]$Output,[string]$SdkDir='E:\Android\Sdk')
$ErrorActionPreference='Stop'
$adb=Join-Path $SdkDir 'platform-tools/adb.exe'
New-Item -ItemType Directory -Force $Output | Out-Null
& $adb -s $Serial push $Plan /sdcard/Android/data/dev.minime.ime/files/parity-plan.json
if($LASTEXITCODE -ne 0){throw 'Plan transfer failed'}
try {
    & "$PSScriptRoot/test-device.ps1" -Serial $Serial -SdkDir $SdkDir -TestClass dev.minime.ime.ParityStudyTest
} finally {
    & $adb -s $Serial pull /sdcard/Android/data/dev.minime.ime/files/parity-observations.json (Join-Path $Output 'observations.json')
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
    $images=& $adb -s $Serial shell 'ls /sdcard/Android/data/dev.minime.ime/files/parity-*.png'
    foreach($remote in $images){
        if($expected -contains ($remote.Trim() -split '/')[-1]){& $adb -s $Serial pull $remote.Trim() $Output | Out-Null}
    }
}
