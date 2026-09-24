# Cover the complete touch-test wrapper with the phone lease

September 24, 2026. Tooling bug fix. Inspection found test-human-input.ps1 pushing
a run ID before its nested guarded runner and reading state/pulling reports after
that runner returned. Those operations escaped the shared-device mutex. The
wrapper also accepted Dozing as asleep, which does not prove display OFF.

The wrapper now retains one outer lease across every phone operation, including
all nested sessions, reports and final cleanup. Windows mutex ownership is
reentrant on the same thread. Final cleanup explicitly sleeps and verifies the
primary display OFF, even if the first transfer fails. Existing preference/IME
restoration stays in the nested device runner; handoff acknowledgement remains
required before invoking this wrapper.

tools/test-human-input-lease.ps1 parses every ADB call site and executes the real
outer body with a failing transfer mock: sleep and display verification must
occur before ownership is released. tools/test-phone-display.ps1 retains OFF,
ON, DOZE and unrelated-display controls. Neither check touches the phone.

This wrapper still generates synthetic imprecision tests. Its name is historical;
passing it cannot certify physical human hit rates or actual panel presentation.
No keyboard behavior, test expectations, data or release payload changes.
