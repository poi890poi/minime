$ErrorActionPreference='Stop'
. "$PSScriptRoot/phone-lease.ps1"
$cases=@(
    @{text="Display Id=0`n  Display State=OFF";expected=$true},
    @{text="Display Id=0`r`n  Display State=OFF`r`n";expected=$true},
    @{text="Display Id=0`n  Display State=ON`nmScreenState=OFF";expected=$false},
    @{text="Display Id=1`n  Display State=OFF`npolicy=DOZE, dozeScreenState=OFF";expected=$false},
    @{text='mActualState=OFF';expected=$true},
    @{text='mScreenState=OFF';expected=$true},
    @{text='mScreenState=OFF_PENDING';expected=$false},
    @{text="Display Id=0`n  Display State=DOZE";expected=$false}
)
foreach($case in $cases) {
    if((Test-PhoneDisplayOff $case.text) -ne $case.expected){throw "Display-state regression: $($case.text)"}
}
Write-Output 'PASS primary display OFF, ON, DOZE, legacy fields and unrelated-display controls; no device commands'
