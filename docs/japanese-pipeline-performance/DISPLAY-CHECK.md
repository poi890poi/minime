# Cleanup verifier correction

The first focused APK session passed its instrumentation test but the wrapper
failed cleanup verification. The saved actual report contains:

```text
Display States: size=1
  Display Id=0
  Display State=OFF
```

The previous checker accepted only `mScreenState=OFF` or `mActualState=OFF`.
Neither field appeared in this report, so it falsely rejected the explicit
primary-display OFF state. This does not establish that Android was upgraded.

The new shared helper first reads the actual state immediately following
`Display Id=0`. ON and DOZE fail even if another legacy field says OFF. When that
primary block is absent, exact legacy OFF fields remain supported. Requested
policies, secondary-display OFF and `OFF_PENDING` do not count as OFF.

`tools/test-phone-display.ps1` checks these cases without accessing a device.
Under the still-acknowledged phone reservation and mutex, a separate sleep/readback
confirmed primary display OFF. Prior IME/preferences had already been restored
and checked before the old parser failed. A local orchestration retry was needed
because checking `$LASTEXITCODE` after a pure PowerShell test incorrectly treated
an unset/stale native exit code as failure; that attempt issued no device command.

Failed-wrapper evidence folder:
`artifacts/device-tests/f79e2d10-c16a-440a-bfda-06337bda4393`.
Recovery report: `artifacts/japanese-pipeline/display-recovery.txt`.
The instrumentation success is retained separately from that wrapper failure.
No always-on-display setting was changed.
