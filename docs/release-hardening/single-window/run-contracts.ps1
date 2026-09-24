param([string]$Tag,[string]$App,[string]$Tests,[string]$Classes,[string[]]$Reports=@(),[Parameter(Mandatory=$true)][DateTimeOffset]$ReservationDeadline)
$ErrorActionPreference='Stop'
. ./tools/phone-lease.ps1
$out="artifacts/single-window/$Tag"
if(Test-Path -LiteralPath $out){throw 'Fresh session identity required'}
New-Item -ItemType Directory $out | Out-Null
Invoke-WithPhoneLease {
 if([DateTime]::UtcNow.AddMinutes(7) -gt $ReservationDeadline.UtcDateTime){throw 'Insufficient reservation for test and cleanup'}
 $adb='E:/Android/Sdk/platform-tools/adb.exe';$saved=[ordered]@{}
 try {
  foreach($key in @('user_rotation','accelerometer_rotation')) {
   $value=(& $adb -s RFCR91GWXLX shell settings get system $key).Trim()
   if($LASTEXITCODE -or $value -notmatch '^([0-3]|null)$'){throw 'Cannot capture rotation settings'}
   $saved[$key]=$value
  }
  $saved | ConvertTo-Json | Set-Content -Encoding utf8 "$out/rotation-before.json"
  & ./tools/test-release-device.ps1 -Serial RFCR91GWXLX -SdkDir E:/Android/Sdk -AppApk $App -RestoreAppApk artifacts/release-hardening/installed-before.apk -TestApk $Tests -TestClass $Classes -Reports $Reports -TimeoutSeconds 180
  if($LASTEXITCODE){throw 'Contract tests failed'}
 } finally {
  try {
   foreach($key in $saved.Keys) {
    $value=$saved[$key]
    if($value -eq 'null'){& $adb -s RFCR91GWXLX shell settings delete system $key}
    else{& $adb -s RFCR91GWXLX shell settings put system $key $value}
    if($LASTEXITCODE){throw 'Rotation restoration failed'}
    $actual=(& $adb -s RFCR91GWXLX shell settings get system $key).Trim()
    if($actual -cne $value){throw 'Rotation readback mismatch'}
   }
   'Original rotation settings restored and verified' | Set-Content "$out/rotation-cleanup.txt"
  } finally {
   & $adb -s RFCR91GWXLX shell input keyevent 223
   Start-Sleep -Milliseconds 500
   $display=(& $adb -s RFCR91GWXLX shell dumpsys display) -join "`n"
   $display | Set-Content -Encoding utf8 "$out/final-display.txt"
   if(!(Test-PhoneDisplayOff $display)){throw 'Display OFF not verified'}
   'Rotation cleanup complete; display OFF verified'
  }
 }
}
