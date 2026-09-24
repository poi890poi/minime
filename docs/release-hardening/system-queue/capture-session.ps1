param([Parameter(Mandatory=$true)][DateTimeOffset]$ReservationDeadline)
$ErrorActionPreference='Stop'
$adb='E:/Android/Sdk/platform-tools/adb.exe'
$serial='RFCR91GWXLX'
$identity=[guid]::NewGuid().ToString()
$remoteConfig="/data/local/tmp/minime-queue-$identity.pbtxt"
$remoteTrace="/data/misc/perfetto-traces/minime-queue-$identity.pftrace"
$capturePid=$null
$captureState=@{Pid=$null}
$out='artifacts/system-queue/phone-scoped-fixed'
if(Test-Path -LiteralPath $out){throw 'Use a new capture directory'}
New-Item -ItemType Directory $out | Out-Null
. ./tools/phone-lease.ps1
Invoke-WithPhoneLease {
    try {
        & $adb -s $serial shell input keyevent 223
        $space=(& $adb -s $serial shell df -k /data)
        $space | Set-Content -Encoding utf8 "$out/storage.txt"
        $columns=($space[-1].Trim() -split '\s+')
        if($columns.Count -lt 6 -or [long]$columns[3] -lt 2097152){throw 'Need 2 GiB free for bounded trace and storage headroom'}
        $coolDeadline=[DateTime]::UtcNow.AddMinutes(10)
        while($true) {
            if([DateTime]::UtcNow.AddMinutes(8) -gt $ReservationDeadline.UtcDateTime){throw 'Insufficient reservation time for replay and cleanup'}
            $thermal=(& $adb -s $serial shell dumpsys thermalservice) -join "`n"
            $battery=(& $adb -s $serial shell dumpsys battery) -join "`n"
            $status=[regex]::Match($thermal,'Thermal Status: (\d+)')
            $temp=[regex]::Match($battery,'temperature: (\d+)')
            if(!$status.Success -or !$temp.Success){throw 'Cannot verify cooled start'}
            [ordered]@{utc=[DateTime]::UtcNow.ToString('o');thermal=[int]$status.Groups[1].Value;batteryC=[int]$temp.Groups[1].Value/10} | ConvertTo-Json -Compress | Add-Content -Encoding utf8 "$out/cooldown.jsonl"
            if([int]$status.Groups[1].Value -eq 0 -and [int]$temp.Groups[1].Value -lt 340){break}
            if([DateTime]::UtcNow -ge $coolDeadline){throw 'Cooldown deadline exceeded'}
            Start-Sleep -Seconds 30
        }
        & $adb -s $serial push docs/release-hardening/system-queue/capture.pbtxt $remoteConfig
        if($LASTEXITCODE){throw 'Trace config push failed'}
        $traceContext=@{DeadlineUtc=$ReservationDeadline.UtcDateTime;Adb=$adb;Serial=$serial;Config=$remoteConfig;Trace=$remoteTrace;Out=$out;Identity=$identity;State=$captureState}
        $startTrace={
            if($null -eq $traceContext -or $traceContext.DeadlineUtc -isnot [DateTime] -or !$traceContext.Adb -or !$traceContext.Out){throw 'Missing captured trace context'}
            if([DateTime]::UtcNow.AddMinutes(6) -gt $traceContext.DeadlineUtc){throw 'No room for bounded trace, test and restoration'}
            [DateTime]::UtcNow.ToString('o') | Set-Content "$($traceContext.Out)/trace-start-utc.txt"
            $traceAdb=$traceContext.Adb;$traceSerial=$traceContext.Serial
            $started=(& $traceAdb -s $traceSerial shell "cat $($traceContext.Config) | perfetto --background-wait --txt --config - --out $($traceContext.Trace)") -join "`n"
            $traceExit=$LASTEXITCODE
            $started | Set-Content -Encoding utf8 "$($traceContext.Out)/start.txt"
            if($traceExit){throw 'System trace did not start'}
            $match=[regex]::Match($started,'(?m)^\s*(\d+)\s*$')
            if(!$match.Success){throw 'Cannot identify trace owner; trace has a hard 180-second duration'}
            $traceContext.State.Pid=$match.Groups[1].Value
            [ordered]@{pid=$traceContext.State.Pid;identity=$traceContext.Identity;config=$traceContext.Config;trace=$traceContext.Trace} | ConvertTo-Json | Set-Content -Encoding utf8 "$($traceContext.Out)/ownership.json"
        }.GetNewClosure()
        & ./tools/test-release-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk -AppApk artifacts/system-queue/diagnostic.apk -RestoreAppApk artifacts/release-hardening/installed-before.apk -TestApk artifacts/system-queue/tests.apk -TestClass 'dev.minime.ime.TouchLatencyTest#testChineseQueuesShard0' -Reports @('touch-latency.tsv','candidate-queues.tsv','candidate-work.tsv') -TimeoutSeconds 300 -BeforeInstrumentation $startTrace
        if($LASTEXITCODE){throw 'System trace replay failed'}
    } finally {
        try {
            $capturePid=$captureState.Pid
            if($capturePid) {
                $commandLine=(& $adb -s $serial shell cat "/proc/$capturePid/cmdline" 2>$null) -join ''
                if($LASTEXITCODE -eq 0) {
                    if(!$commandLine.Contains($remoteTrace)){throw 'Trace process identity changed; refusing to signal it'}
                    & $adb -s $serial shell kill -INT $capturePid
                    for($attempt=0;$attempt -lt 20;$attempt++) {
                        & $adb -s $serial shell test -d "/proc/$capturePid"
                        if($LASTEXITCODE -ne 0){break}
                        Start-Sleep -Milliseconds 500
                    }
                    & $adb -s $serial shell test -d "/proc/$capturePid"
                    if($LASTEXITCODE -eq 0){throw 'Owned trace process did not exit'}
                }
                & $adb -s $serial pull $remoteTrace "$out/trace.pftrace"
                if($LASTEXITCODE){throw 'Trace pull failed; retained remote trace'}
                if((Get-Item "$out/trace.pftrace").Length -eq 0){throw 'Empty trace; retained remote file'}
                Get-FileHash "$out/trace.pftrace" | Format-List | Out-String | Set-Content -Encoding utf8 "$out/trace-hash.txt"
                & $adb -s $serial shell rm -f $remoteTrace $remoteConfig
                if($LASTEXITCODE){throw 'Owned trace files not cleaned up'}
                'Owned trace stopped, pulled and remote files removed' | Set-Content "$out/trace-cleanup.txt"
            }
        } finally {
            & $adb -s $serial shell rm -f $remoteConfig
            & $adb -s $serial shell input keyevent 223
            Start-Sleep -Milliseconds 500
            $display=(& $adb -s $serial shell dumpsys display) -join "`n"
            $display | Set-Content -Encoding utf8 "$out/final-display.txt"
            if(!(Test-PhoneDisplayOff $display)){throw 'Display OFF not confirmed after trace cleanup'}
            'Final display OFF verified after trace cleanup'
        }
    }
}
