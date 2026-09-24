param([Parameter(Mandatory=$true)][DateTimeOffset]$ReservationDeadline)
$ErrorActionPreference='Stop'
$out='artifacts/post-window-stages'
$pins=@{
 'diagnostic.apk'='5e914edb80694c3ef01fa8b95a04d96b284eb7b9ed45e9b1cc64dc574b9be749'
 'tests.apk'='93dfea4dc9dc913916c157f1f72fbcf770b873d448b29feec066ce4652952927'
}
foreach($name in $pins.Keys){
 if((Get-FileHash -LiteralPath "$out/$name").Hash.ToLowerInvariant() -cne $pins[$name]){throw "Changed diagnostic binary: $name"}
}
foreach($mode in @('chinese','taiwanese','japanese')){
 if(Test-Path -LiteralPath "$out/$mode.log"){throw 'Refusing to overwrite a diagnostic session'}
}
. ./tools/phone-lease.ps1
Invoke-WithPhoneLease {
 foreach($mode in @('Chinese','Taiwanese','Japanese')) {
  if([DateTime]::UtcNow.AddMinutes(17) -gt $ReservationDeadline.UtcDateTime){throw 'Insufficient acknowledged headroom for cooling, replay and cleanup'}
  $tag=$mode.ToLowerInvariant()
  Write-Output "Starting $tag"
  & ./artifacts/validation-pattern/run-timing-session.ps1 -Tag ('post-window-stages-'+$tag) -App "$out/diagnostic.apk" -Tests "$out/tests.apk" -Classes ('dev.minime.ime.TouchLatencyTest#test'+$mode+'QueuesShard0') -Reports @('touch-latency.tsv','candidate-queues.tsv','candidate-stages.tsv','candidate-providers.tsv','candidate-work.tsv') -RequireCool > "$out/$tag.log" 2>&1
  if($LASTEXITCODE){throw "Diagnostic replay failed: $tag"}
  & 'C:/Users/Lee/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' -X utf8 docs/release-hardening/post-window-stages/summarize.py $tag
  if($LASTEXITCODE){throw "Diagnostic evidence validation failed: $tag"}
  Write-Output "Completed $tag"
 }
}
