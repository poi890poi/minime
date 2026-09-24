# Candidate queue diagnosis

September 25, 2026. Follows the frozen [queue plan](QUEUE-STAGES-PLAN.md).
Accepted production remains `6bffd17` / publication `e89a337`.

The common unexplained interval is between posting a completed result and entering
its main-thread callback. Worker deadline overshoot is small in all three modes.
This justifies a delivery experiment; it does not yet establish synchronization
barriers as the cause or establish a production speedup.

| Mode | Requests / accepted | Deadline overshoot mean / p95 | Provider work mean / p95 | Main queue mean / p95 | Callback mean / p95 |
|---|---:|---:|---:|---:|---:|
| Chinese | 665 / 665 | 0.16 / 0.40 ms | 1.86 / 6.49 ms | 8.94 / 16.35 ms | 5.24 / 11.08 ms |
| Taiwanese | 894 / 891 | 0.16 / 0.40 ms | 4.93 / 18.55 ms | 8.17 / 17.12 ms | 2.60 / 5.98 ms |
| Japanese | 508 / 508 | 0.16 / 0.38 ms | 0.83 / 2.13 ms | 10.57 / 21.47 ms | 3.41 / 6.52 ms |

Each p95 is the duration within which 95% of that stage's observed episodes
completed. These are separate distributions and must not be added. Taiwanese has
three cancelled pipelines and also a substantive provider tail (46.53 ms p99);
delivery alone cannot remove that work. No failed or unresolved queue episodes.

All 2,354 prescribed injected actions match frozen shard 0 at 150/60 ms cadences.
All raw and Space submissions are observed. Queue capture records 2,067 requests;
one Chinese letter window has two requests and is kept ambiguous in correlation,
not assigned selectively. Aggregates retain missing candidate frames and strata.
This is one instrumented session per mode, not the prescribed large release
sample, a physical touch test, or a cross-app latency claim. Test editor and IME
share a process. Instrumentation overhead prevents comparisons against unhooked
baselines from being called speedups.

The isolated overlay changes timestamps only. All 24 packaged assets match the
accepted fixture, only classes.dex differs apart from signatures, and active
production AsyncDecoder is unchanged. Six reporter fixtures and the Java observer
contract pass. Shared core passes 1,770,165 assertions (contract checks, not
language accuracy). The pinned desktop replay completes 13,014 inputs and its
output hash remains `1d2aa3693ae0ddbaf1c259252fc4d9689fcd458dfb23a214f9ed29c476fcde5d`.

Aggregate observations and observer sources are in [queue-stages](queue-stages).
Raw device reports and private restoration backups remain local. Each phone
session restores original APK, preferences/learning and Samsung IME, verifies
display OFF, and the outer lease was explicitly released after all three runs.

Next: test asynchronous decoder-result messages only. Android documents that
[asynchronous messages](https://developer.android.com/reference/android/os/Message#setAsynchronous(boolean))
can bypass synchronization barriers associated with display updates, but this can
change ordering relative to ordinary messages. Keep generation/closed guards and
test cancellation and lifecycle behavior. Repeated unhooked comparisons must
protect raw/Space response and candidate availability before admission.
