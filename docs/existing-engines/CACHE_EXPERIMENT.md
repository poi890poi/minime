# Native model lifetime

Type: performance change. First verified phone run: 84 probe queries, median
119.8 ms, p95 204.3 ms, maximum 301.8 ms; initialization 143 ms. Native query
time includes new-session creation, schema/model lookup, incremental decoding,
candidate consumption filtering and JNI output, excluding MinIME/rendering.

Cause hypothesis: destroying the last Rime session releases weakly held model
objects, causing subsequent queries to reload them. Candidate fix: keep the
initial empty session alive solely as a model-cache owner. Continue using and
destroying a fresh session for every actual query. The owner never receives
input, so no query spelling, committed context or personalized history crosses
requests. Leave models, rankings, per-query algorithm and candidate limits fixed.

Compare all 84 candidate lists and query timings on the same phone, plus visible
commits. Reject output changes or a non-reproducing speedup. Previous measurements
are retained in rime-phone-before-cache.json. This is an engine-integration
lifetime improvement, not a new Pinyin algorithm.

Result: all 84 candidate lists were identical. Median fell to 34.1 ms, p95 to
120.4 ms, maximum to 247.7 ms; initialization was 193 ms. Visible commits passed.
Retain the empty owner. A second isolated experiment uses Rime's existing
`set_input` API to decode the complete query once, instead of replaying every
prefix into a fresh session. MinIME already supplies whole-spelling requests;
no key processing or candidate policy belongs in this adapter. Require all
84 candidate lists and phone interactions to remain identical before accepting.

The whole-input API preserved all 84 candidate lists and passed visible commits.
Median query time was 14.4 ms, p95 36.3 ms, maximum 46.2 ms; initialization was
133 ms. Retain both changes. These are debug ARM64 phone measurements; query
times include JNI/session/candidate work but not dispatch, MinIME fallback lookup
or rendering. Final full-suite validation provides an independent repeated run.

Model-build reproducibility initially differed in the prism's four-byte schema
checksum. Rime includes source mtimes in compiled YAML, and Windows clock
conversion can round an exact second down by one. Fixing source mtimes at a
half-second boundary produced identical bundles in two isolated rebuilds. No
binary metadata, readings, frequencies or expected outputs were patched.

Independent full-suite repeat: 84 candidate lists again identical; median 9.87 ms,
p95 35.40 ms, maximum 40.77 ms. The model was warm from preceding tests, so its
reported loadMs=0 is not a startup measurement. Native timings remain separate
from overall keyboard latency. See rime-phone-final.json. The three long/noisy
inputs in rime-stress.json stayed below the predeclared one-second bound and did
not contaminate the following known query.
