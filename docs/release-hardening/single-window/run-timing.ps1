param([Parameter(Mandatory=$true)][DateTimeOffset]$ReservationDeadline)
$ErrorActionPreference='Stop'
. ./tools/phone-lease.ps1
Invoke-WithPhoneLease {
 foreach($item in @(@('a1','artifacts/language-baseline/accepted.apk'),@('b1','artifacts/single-window/trial.apk'),@('b2','artifacts/single-window/trial.apk'),@('a2','artifacts/language-baseline/accepted.apk'))) {
  if([DateTime]::UtcNow.AddMinutes(17) -gt $ReservationDeadline.UtcDateTime){throw 'Insufficient acknowledged time for bounded cooling, replay and cleanup'}
  Write-Output "Starting single-window-$($item[0])"
  & ./artifacts/validation-pattern/run-timing-session.ps1 -Tag ('single-window-'+$item[0]) -App $item[1] -Tests artifacts/single-window/tests4.apk -Classes 'dev.minime.ime.TouchLatencyTest#testTouchWithoutStageHooks' -Reports @('touch-latency.tsv') -RequireCool
  if($LASTEXITCODE){throw 'Timing replay failed'}
 }
}
