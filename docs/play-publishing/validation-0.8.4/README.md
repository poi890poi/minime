# Local phone evidence — September 12, 2026

Build: 0.8.4 debug, production source c0f6a60. Android 13 authorized phone.
Core and pinned desktop Rime checks had passed before this local debug build.
No release binary or production behavior changed for these checks.

26 instrumentation tests passed in two sessions: seven editor integration,
one publishing-settings, eight backup validation, four overlapping-touch checks,
one touch timing, three human-imprecision tests, and two keyboard lifecycle tests
(URL/password/numeric editors and hide/restart composition). The imprecision
matrices each cover 7,480 constructed cases, portrait and landscape. These are
synthetic touch correctness checks, not a measured human hit rate.

Touch timing includes 464 actions (400 letters and 64 spaces), four language modes,
two pacing conditions, OS-injected events and frame-submission callbacks. Raw
samples and summary are in touch/. Typing speed 150 ms and 60 ms between actions.
Observed raw-text submission p95 ranges from 20.96 to 45.22 ms across groups.
Chinese candidate submission p95 is 104.00 ms at the slower pace; at the faster
pace no matching candidate submission was observed before the next action.
Other modes also have missing candidate samples. Missing is not zero latency or
proof of smoothness; see the detailed limitations in touch/summary.json.

This does not pass signed-release performance acceptance. Physical digitizer and
display presentation latency, Chrome timing, large fonts/accessibility and manual
document-provider/cross-package migration remain unverified. Android 16/16 KB
runtime testing remains open. The screenshot session separately passed one capture
test, but visual review exposed the native sentence ranking defect.

All sessions held the shared phone lease after explicit handoff. Prior preferences,
IME and viewport were restored, display OFF verified, and ownership explicitly
released. No personal input or private backup is included here.
