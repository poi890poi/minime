# Local closure contract: no ADB or device operations.
$contractState=@{Pid=$null}
function New-TraceContextContract {
    $traceContext=@{DeadlineUtc=[DateTime]::UtcNow.AddHours(1);Adb='test-adb';Out='test-out';State=$contractState}
    return {
        if($traceContext.DeadlineUtc -isnot [DateTime] -or $traceContext.Adb -ne 'test-adb'){throw 'Context lost'}
        $traceContext.State.Pid='123'
        $traceContext.DeadlineUtc
    }.GetNewClosure()
}
$callback=New-TraceContextContract
$actual=& {param($hook) $ReservationDeadline=$null; & $hook} $callback
if($actual -isnot [DateTime] -or $contractState.Pid -ne '123'){throw 'Callback context contract failed'}
'PASS captured deadline and shared ownership state across invocation scope'
