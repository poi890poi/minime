# MinIME Google Play publishing plan

Historical proposal. The owner approved preparation; see [RELEASE.md](RELEASE.md)
for implemented licensing, build, migration and verification status.

Reviewed 2026-09-10 against 0.7.9 / 8553d40. Planning document only: no licence
has been granted, signing key generated, Play application created or release submitted.
Documentation impact: records evidence, unresolved questions and release gates;
no application, dictionary, privacy behavior or source decisions change.

## Recommendation

Prepare a free, offline beta without advertising or billing as the planning default,
subject to the owner's launch preference. Start with internal testing, proceed to
closed testing, then publish when the compliance and product gates pass. The code
and data licences do not by themselves prohibit commercial distribution, but the
current package has unresolved documentation and corpus-rights review items. This
is a dependency and release review, not patent/trademark clearance or legal advice.

## Licence review

| Component | Recorded terms | Distribution result and action |
| --- | --- | --- |
| Original MinIME code | No root LICENSE found | Choose a deliberate code licence. Recommend Apache-2.0 for original code; preserve all separately licensed components. This is an open-source project decision, not a claim that Play requires open-source licensing. |
| McBopomofo; AOSP English dictionary | MIT; Apache-2.0 | Redistribution permitted subject to retained notices and applicable conditions. Existing attribution is packaged. |
| librime native engine and dependencies | BSD-3-Clause, MIT, BSL-1.0, Apache-2.0; Marisa BSD-2-Clause option; LLVM exceptions | Native code is not the LGPL Rime dictionary. Preserve dependency notices and chosen dual-licence option. Pins and build configuration are recorded in third_party/rime. |
| Rime Luna Pinyin, Essay and Prelude data | LGPL-3.0 in pinned licence files | Retain licence/authors, publish corresponding pinned source, patches and reproduction instructions with each release. Review distribution of the compiled data under those terms; do not relabel it Apache or MIT. |
| iTaigi; Wikidata contributions | CC0 | Retain provenance; no noncommercial restriction in the selected sources. |
| Taihoa and beginner POJ; Japanese JMdict/JMnedict/KANJIDIC2; CC-CEDICT/Wikipedia-derived material | CC BY-SA 4.0 | Attribute, identify modifications, preserve the licence for adapted data, and publish reusable dataset artifacts without additional restrictions. EDRDG additionally specifies app/source acknowledgements and an update procedure. |
| Rudy/OSM geography | ODbL-1.0 | Preserve OSM/Rudy attribution and make the adapted geography database available under ODbL. Keep it separate from the other language data. |
| Unicode emoji data | Unicode data licence | Keep notices. The app ships character sequences, not Google emoji artwork. |
| UD English EWT / Chinese GSD production-derived context and spelling data | CC BY-SA annotations/database rights, with underlying-text qualifications | Needs a focused review of the exact derived outputs and source rights, or replacement with clearly licensed inputs. A corpus annotation licence is not blanket permission for the underlying texts. |
| GUM evaluation and other pilot/reference data | Per-source/document terms, including noncommercial material | Not production inputs in the context/spelling compilers. Keep out of APK/AAB assets and review public evaluation-artifact redistribution separately. |

Evidence: [source catalog](../../sources/catalog.json), [native/data distinction](../../third_party/rime/README.md),
[ChhoeTaigi's per-dictionary terms](../../third_party/itaigi/README.md),
[EWT underlying-text qualification](../../third_party/ud/UD_English-EWT/README.md),
[context compiler](../../tools/compile_context.py), and [packaged source notices](../../app/src/main/assets/addon-sources.txt).

[CC BY-SA permits commercial reuse under its conditions](https://creativecommons.org/licenses/by-sa/4.0/).
[EDRDG states that compliant dictionary use does not require open-source application code](https://www.edrdg.org/edrdg/licence.html).
[ODbL requires attribution and sharing the adapted database](https://opendatacommons.org/licenses/odbl/summary/).
These are separate data obligations; do not apply a blanket code licence to the whole repository.
Turning a bundled pack off does not remove its distribution obligations.

ChhoeTaigi contains other dictionaries with NC or ND terms. MinIME's selected POJ
sources are iTaigi, Taihoa and the beginner vocabulary; those other dictionaries
must not be admitted by treating the collection as one uniformly licensed source.
The source catalog's `replace` status is often editorial migration debt, not a
finding of licence invalidity. Preserve that distinction in release decisions.

The reference-study record says no proprietary Google APK implementation, models
or assets were incorporated. Keep those APKs out of releases and Git, use MinIME
branding/screenshots, and do not imply Google endorsement. Functional behavior and
protected expression are different questions; see the [reference record](../LEGACY_REFERENCE.md)
and the [Copyright Office explanation](https://www.copyright.gov/circs/circ31.pdf).

## Work before closed testing

1. **Licence and source distribution.** Confirm the original-code licence; create
   a release licence matrix/SBOM from pins and actual shipped files. Refresh stale
   source descriptions (currently including removed Japanese romanized outputs
   and obsolete POJ counts/filters). Audit generated binary metadata, provide
   corresponding Rime source/patches, independently downloadable adapted datasets,
   and complete notices from a stable release tag. Close the UD-derived-data review
   with a documented decision; any replacement needs core coverage/performance
   comparisons using frozen corpora. Check that NC/ND/evaluation sources cannot enter
   the final AAB. Review exported research files separately.

2. **Android release build.** Upgrade compile/target SDK from 35 to 36 and review
   behavior changes, especially input lifecycle and floating composition UI.
   [Google currently requires API 36 for new phone apps and updates from August 31, 2026](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-mt).
   Produce a non-debuggable release AAB. Existing NDK r27 and 16 KB linker settings
   are a foundation, not a final bundle certification: check every native ELF,
   bundle-generated APK ZIP alignment and execution in a 16 KB environment using
   [Android's verification procedure](https://developer.android.com/guide/practices/page-sizes).
   Check ABI/device delivery sizes and native symbol upload.

3. **Signing and migration.** Confirm production applicationId `dev.minime.ime`
   before first upload; select and securely back up the production/upload keys.
   Use [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)
   and the [AAB publishing workflow](https://support.google.com/googleplay/android-developer/answer/9859152?rd=1).
   Existing debug-signed installs generally cannot be updated with a different
   signing certificate. Plan learned-data/settings export and import before any
   uninstall, and consider `.debug` for future development builds. Do not use a
   development debug key as the public production identity.

4. **Privacy and listing.** Publish a stable public HTML privacy policy and link it
   from the app. It should explain local composition/learning, password/private
   behavior, history/custom-entry deletion and retention, no network/telemetry,
   and the developer's contact. Existing settings text is useful evidence but is
   not the complete release policy. [Google requires a privacy policy even for apps that collect no data](https://support.google.com/googleplay/android-developer/answer/10144311?hl=en).
   Prepare the Data safety declaration from the final merged manifest, dependencies
   and data flows; local-only processing is outside collection, and ordinary
   user-initiated text delivery must be assessed under the sharing exceptions.
   Do not submit a declaration based only on absence of INTERNET permission.
   [Google's Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
   Prepare Traditional Chinese and English descriptions, original icon/screenshots,
   feature graphic, support contact, target audience, content rating, ads declaration
   and reviewer instructions for enabling/selecting the IME. No login exists to supply.

5. **Beta and quality gates.** Run the shared core and pinned desktop evaluator
   before Android builds. Distribute the signed bundle internally first; verify
   clean install, update/migration, enable/select flow, Chrome/editor actions,
   passwords/private fields, TalkBack, larger text, rotation and candidate identity.
   Measure typing latency and touch hit rate against [product requirements](../PRODUCT_REQUIREMENTS.md)
   using release builds. Current tests do not certify physical touch latency, and
   prior candidate latency still exceeds targets. Report conversation and essay
   coverage separately in each focused mode, with complete/partial/imprecise input.
   Only RFCR91GWXLX is authorized for agent-operated physical tests; restore its
   settings/IME and verify the display is asleep after every session.

## Play account and rollout

The owner's account type/creation date and pricing remain open questions. If this
is a personal account created after November 13, 2023, the published minimum is
12 testers continuously opted into a closed test for 14 days before applying for
production access. Keep genuine usage/feedback and fixes, not just opt-in counts;
Google reviews the application for access. Existing personal/organization accounts
must follow the requirements shown in their own Console. [Official testing requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).

For a new personal account, budget at least that closed-test period after the beta
is ready, plus account verification, engineering work and Google's review. Do not
promise a calendar launch date yet. After readiness and approval, start with a
limited-market first production release; expand distribution after reviewing
feedback and Android vitals. Use staged rollout for subsequent updates where available.
No account creation, tester invitations or Play submission is authorized by this
planning document alone.

## Proposed independently reviewable changes

- Licensing/source notices and release compliance checks, after the owner chooses a code licence.
- Any UD source replacement, if needed, with separate data/coverage evidence.
- API 36 compatibility and 16 KB release-bundle verification.
- Release signing configuration and safe debug-to-production data migration.
- Privacy policy, in-app access, listing assets and release documentation.
- Remaining measured latency/touch defects, each with its own regression and commit.

The next concrete milestone is a compliant, signed internal-test AAB with a source
bundle, privacy draft, Data safety worksheet and device test report ready to review.
