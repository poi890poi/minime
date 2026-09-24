param([Parameter(Mandatory=$true)][DateTimeOffset]$ReservationDeadline)
$ErrorActionPreference='Stop'
$out='artifacts/single-window-language'
$pins=@{
 'artifacts/language-baseline/accepted.apk'='0e073584a32d4b7323a0d1d5d9eae74df26e8f902f641a55b9d66f03d557d344'
 'artifacts/single-window/trial.apk'='db55f153b895558ad34ad0aa19c2703f35b13b2514b9659e9333f6613be95b4d'
 'artifacts/single-window/tests4.apk'='740ed630d4badb9db74d2240644675d6a366fda62682bbb5678145217667ee56'
}
foreach($path in $pins.Keys){if((Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant() -cne $pins[$path]){throw "Changed experiment binary: $path"}}
$runs=@(
 @('chinese-old','Chinese','old'),@('chinese-new','Chinese','new'),
 @('taiwanese-new','Taiwanese','new'),@('taiwanese-old','Taiwanese','old'),
 @('japanese-old','Japanese','old'),@('japanese-new','Japanese','new'),
 @('english-new','English','new'),@('english-old','English','old'))
foreach($run in $runs){if(Test-Path -LiteralPath "$out/$($run[0]).log"){throw 'Refusing to overwrite a session'}}
. ./tools/phone-lease.ps1
Invoke-WithPhoneLease {
 foreach($run in $runs) {
  if([DateTime]::UtcNow.AddMinutes(17) -gt $ReservationDeadline.UtcDateTime){throw 'Insufficient acknowledged headroom for cooling, test and cleanup'}
  $tag=$run[0];$method='dev.minime.ime.TouchLatencyTest#test'+$run[1]+'Shard0'
  $app=if($run[2] -eq 'old'){'artifacts/language-baseline/accepted.apk'}else{'artifacts/single-window/trial.apk'}
  Write-Output "Starting $tag"
  & ./artifacts/validation-pattern/run-timing-session.ps1 -Tag ('single-window-language-'+$tag) -App $app -Tests artifacts/single-window/tests4.apk -Classes $method -Reports @('touch-latency.tsv') -RequireCool > "$out/$tag.log" 2>&1
  if($LASTEXITCODE){throw "Replay failed: $tag"}
  & 'C:/Users/Lee/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 docs/release-hardening/single-window-language/summarize.py $tag
  if($LASTEXITCODE){throw "Workload/frame integrity failed: $tag"}
  Write-Output "Completed $tag"
 }
}
