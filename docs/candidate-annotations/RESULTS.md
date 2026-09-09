# MinIME 0.7.1: direct Taiwanese paired output

In **台 mode**, a paired candidate shows its primary output above the alternate.
Tap inserts the primary; long press inserts the alternate directly, without a menu.
The default is tap POJ / hold Han. The optional **Prefer Taiwanese Han output**
setting reverses them. **Show Taiwanese Han alternatives** can disable the feature.
A reading without an eligible pair retains its phonetic output and has no hold action.
Space accepts the displayed preferred primary.

The strip remains 48 dp high. Han-primary cells keep their phonetic width so the
reading remains legible. Expanded candidates use the existing fixed keyboard area.
Very long secondary readings can still be ellipsized at the expanded cell boundary;
the accessibility long-click label exposes the complete alternate.

[Phonetic-primary phone screenshot](phonetic-primary.png) ·
[Han-primary phone screenshot](han-primary.png)

## Source coverage and limits

9,385 complete readings have one source-attested pair, drawn reproducibly from
15,188 existing iTaigi outputs. Every chosen pair resolves exactly to the pinned
source's POJ/HanLo fields. No phonetic entries, aliases or ranking weights were added.
The old language assets, including the compiled model and Rime assets, are byte-identical.

The fixed familiarity policy retains 2,641 Han characters covering 99% of positive
single-character occurrence mass in McBopomofo, including all cutoff ties. Every
character of a selected form must belong to that set. The compiler excluded 849
non-single/all-Han records and 5,812 records containing an outside character; these
are record counts, not unique omitted readings. Canonical example selection follows
source occurrence evidence and stable source ID, with no per-word exceptions.

Written Taiwan Mandarin frequency is a familiarity proxy, not evidence of Taiwanese
spelling popularity or community agreement. A shared reading can have multiple
meanings; its one displayed example does not disambiguate every source entry.
Beginner phrases without their own approved pairing remain phonetic-only. No new
MOE-derived BY-ND data is imported. Japanese annotation remains a separate design
from this approved Taiwanese tap/hold implementation. See [source policy](IMPLEMENTATION.md)
and [research](PROPOSAL.md).

## Verification

- Final standard core suite passes 29,186 mechanical assertions. Exhaustive metadata
  parity passes 179,699 complete/prefix queries; the 3,922 frozen mixed-genre conditions
  also retain candidate ordering. These are regression checks, not new language accuracy.
- Pinned desktop Rime output is identical to the prior release. See
  [core verification and rejected approach](CORE-VERIFICATION.md).
- Final Android build and lint pass: zero errors, 17 warnings.
- Authorized phone: 15 final tests pass across physical hold/release, cancellation,
  drag, stale mode change, tap/hold reversal, disabled action, accessibility, stable
  geometry, delayed candidates, human touch imprecision and Chrome. An earlier
  13-test session also measured the real worker/main-thread prediction path.
- The initial Han-primary drawing squeezed phonetic hints into short Han widths.
  Visual inspection rejected that layout; preserving phonetic width passes the
  added geometry assertion and the final screenshots. The initial draw allocation
  warning was resolved by reusing the hint paint and metrics.
- Original Samsung HoneyBoard IME and both preference files were restored. Settings
  and learning were compared with their backups. Display State, mScreenState and
  mActualState were verified **OFF** after the final session. AOD settings were preserved.

## Performance

Phone last-key dispatch through worker and main-thread callback, 186 measured samples
per mode after warmup, using the same previously exposed input set as 0.7.0:

| Mode | 0.7.0 median / p95 | Paired implementation median / p95 |
|---|---:|---:|
| Chinese | 40.9 / 58.2 ms | 41.2 / 58.2 ms |
| English | 0.47 / 1.71 ms | 0.47 / 1.64 ms |
| Taiwanese | 43.7 / 60.8 ms | 43.3 / 61.0 ms |
| Japanese | 42.2 / 58.9 ms | 41.8 / 59.8 ms |

These are separate phone sessions, not a randomized performance experiment; display
refresh is excluded. No end-to-end speedup is claimed. Desktop A/B lookup runs show
similar medians and a small tail cost (p99 +0.37–0.99 ms). See the complete repeated
measurements and cold parsing cost in the core report. Metadata attaches once during
background loading, and hint drawing performs no dictionary lookup or asynchronous work.

The [raw lookup samples](lookup-samples.tsv.gz), [phone samples](phone-latency.tsv.gz),
[final core log](core-final.log.gz), [first rejected core run](first-core-run.log.gz)
and [final phone log](phone-final.log.gz) retain the evidence. Existing conversation,
essay and retrieval inputs remain separate in telemetry and retain their original
exposure/source roles. This feature does not provide a new independently reviewed
Taiwanese spelling consensus corpus or improve candidate ranking.

## Package

[MinIME 0.7.1 ZIP](https://que-put-miracle-wal.trycloudflare.com/MinIME-0.7.1.zip)
contains the signed debug APK, 37,284,809 bytes compressed. The temporary Cloudflare
download was fetched and compared byte-for-byte by SHA-256. ZIP CRC, embedded APK
bytes and APK v2 signature verification pass. The tunnel must remain running.

ZIP SHA-256: `ace0327dcf00e3c62f36ecceb15dcd0dff2ef568537033598f35f3da4e9b11f6`.
Detailed identities and hashes are in [verification.json](verification.json).
