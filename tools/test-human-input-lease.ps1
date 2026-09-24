# Local mocked failure path: never invokes ADB or reserves the physical phone.
$ErrorActionPreference='Stop'
$runner=Join-Path $PSScriptRoot 'test-human-input.ps1'
$tokens=$null;$parseErrors=$null
$ast=[Management.Automation.Language.Parser]::ParseFile($runner,[ref]$tokens,[ref]$parseErrors)
if($parseErrors.Count){throw 'Human-input runner syntax failed'}
$leases=@($ast.FindAll({param($node) $node -is [Management.Automation.Language.CommandAst] -and $node.GetCommandName() -eq 'Invoke-WithPhoneLease'},$true))
if($leases.Count -ne 1){throw 'Expected one outer lease'}
$body=$leases[0].CommandElements[1].ScriptBlock
$calls=@($ast.FindAll({param($node) $node -is [Management.Automation.Language.CommandAst] -and $node.CommandElements[0].Extent.Text -eq '$adb'},$true))
foreach($call in $calls) {
    if($call.Extent.StartOffset -lt $body.Extent.StartOffset -or $call.Extent.EndOffset -gt $body.Extent.EndOffset){throw 'Device operation escapes outer lease'}
}
# Evaluate the real lease body with a deliberately failed first run-ID push.
# The mock fails if any phone operation runs outside ownership.
$script:owned=$false;$script:events=[Collections.Generic.List[string]]::new()
function Invoke-WithPhoneLease([scriptblock]$Action) {
    if($script:owned){throw 'Unexpected nested test lease'}
    $script:owned=$true
    try {& $Action} finally {$script:owned=$false}
}
function Invoke-FakeAdb {
    if(!$script:owned){throw 'Unowned device operation'}
    $line=$args -join ' ';$script:events.Add($line)
    if($line -match ' push '){$global:LASTEXITCODE=1;return}
    $global:LASTEXITCODE=0
    if($line -match 'dumpsys display'){return "Display Id=0`n  Display State=OFF"}
}
function Test-PhoneDisplayOff([string]$Display){return $Display -match 'Display State=OFF'}
$temp=Join-Path ([IO.Path]::GetTempPath()) ('minime-lease-check-'+[guid]::NewGuid())
New-Item -ItemType Directory -Path $temp | Out-Null
$rootOutput=$temp;$phases=@('Matrix');$Serial='RFCR91GWXLX';$adb='Invoke-FakeAdb'
$failed=$false
try {Invoke-WithPhoneLease $body.GetScriptBlock()} catch {if($_ -notmatch 'Cannot transfer run identity'){throw};$failed=$true}
if(!$failed -or $script:owned){throw 'Failure propagation/lease cleanup failed'}
if($script:events.Count -ne 3 -or $script:events[1] -notmatch 'KEYCODE_SLEEP' -or $script:events[2] -notmatch 'dumpsys display'){throw 'Failed transfer did not sleep and verify before releasing'}
if(!(Test-Path -LiteralPath (Join-Path $temp 'display-final.txt'))){throw 'Missing final display evidence'}
Write-Output 'PASS: all ADB call sites inside outer lease; failed first transfer still sleeps/verifies before release. Mock only, no phone commands.'
