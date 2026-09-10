# Cross-project Windows exclusion for the shared authorized phone. The owner must
# also obtain the explicit task handoff described in AGENTS.md before entering.
function Invoke-WithPhoneLease {
    param([Parameter(Mandatory=$true)][scriptblock]$Action)
    $lease=[Threading.Mutex]::new($false,'Local\Codex.Android.RFCR91GWXLX')
    $owned=$false
    try {
        try { $owned=$lease.WaitOne(0) }
        catch [Threading.AbandonedMutexException] {
            $owned=$true
            throw 'The prior phone session ended unexpectedly. Coordinate recovery before operating the phone.'
        }
        if(!$owned){throw 'Phone RFCR91GWXLX is reserved by another process. No device command was issued; obtain its explicit release.'}
        & $Action
    } finally {
        if($owned){$lease.ReleaseMutex()}
        $lease.Dispose()
    }
}
