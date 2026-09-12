# MinIME 0.8.4 — Console preparation

Prepared September 12, 2026. These are proposed answers for the reviewed build,
not declarations already submitted to Google. Resolve RELEASE-CHECKLIST.md first.

## Identity and listing

| Field | Value |
| --- | --- |
| Application ID | app.minime.keyboard |
| Version / code | 0.8.4 / 32 |
| English title | MinIME — Pinyin for Taiwan |
| Traditional Chinese title | MinIME — 台灣多語拼音鍵盤 |
| Default listing language | zh-TW recommended; en-US localization supplied |
| App or game / category | App / Productivity |
| Price | Free launch proposed |
| Ads / purchases | No ads; no in-app purchases |
| Website | https://github.com/poi890poi/minime |
| Privacy URL | https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md |
| Public developer name / support email | Owner must supply; no placeholders in listing files |

Use the localized text files in listing/. They are UTF-8 with BOM for Windows
compatibility; paste their visible text, without adding XML release-note tags to
an individual locale field. Five screenshots and two feature graphics accompany
the approved icon. No preview video is required for this phone-keyboard listing.

## App content

| Form | Prepared answer / remaining decision |
| --- | --- |
| App access | All functionality available without login, subscription or credentials; reviewer instructions below |
| Contains ads | No |
| Advertising ID | Not used; merged manifest has no AD_ID permission |
| Data safety: reportable collection/sharing | Proposed No; reasoning below |
| Account creation | No; no account-deletion website is applicable |
| Independent security review | No certification claimed |
| News app | No |
| Government app | No |
| Health / financial features | None; general keyboard and language dictionaries |
| Target audience | Owner must choose intended age groups; do not infer from the cute icon |
| IARC content rating | Complete the actual questionnaire; no rating is invented here |
| Countries / regions | Owner selection; Taiwan-first listing is prepared |

Inspect the actual Console for additional forms. Dictionary vocabulary and emoji
can include words for mature topics; an offline keyboard is not automatically an
all-ages content-rating answer. It has no hosted public chat, social feed or
user-to-user sharing service. User-entered text is delivered to the receiving app.

## Data safety reasoning

The merged release manifest requests no app permissions, including Internet.
There is no analytics, advertising or crash-reporting SDK and no developer server.
Composition, preferences and learning remain local. These are outside Google's
off-device collection definition. InputConnection commits and explicit export to
the chosen document provider are user-directed transfers; our proposed No-sharing
answer applies Google's user-initiated-transfer exception, not an assumption that
all transfers to other apps are exempt. Provider-managed cloud storage follows that
provider's practices. Backups are plaintext, not encrypted by MinIME. Browser links
open external websites rather than a MinIME-controlled webview.

Do not tick a blanket encryption claim for exported files. If the no-collection
path hides encryption/deletion questions, leave the hidden fields alone. The local
clear-learning, custom-entry deletion and Android clear-storage/uninstall controls
are documented in the policy; independent exported copies require separate deletion.

[Google Data safety definitions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en)
and [privacy requirements](https://support.google.com/googleplay/android-developer/answer/10144311?rd=1)
were checked September 12, 2026. Reassess if app behavior or dependencies change.

## Copy-ready reviewer access instructions

No credentials are required. Open MinIME, enable it in Android's keyboard settings,
then select MinIME as the current keyboard. Android displays its standard keyboard
warning. Open a normal text field in a browser or notes app. Use MinIME Settings to
enable optional dictionaries and select Chinese, English, Taiwanese or Japanese
focus, then return to a fresh text field. Try letters, candidates, symbols and emoji.
Japanese offers vocabulary, kana and Kanji suggestions, not full sentence conversion.
Password fields intentionally suppress composition, suggestions and learning.
Privacy and source notices are in Settings. Export/import is optional and uses the
Android document picker. Import requires confirmation before replacing saved data;
cancel the confirmation to keep current preferences. No test account is necessary.

## Signing, package registration and tracks

Upload MinIME-0.8.4-play-signed.aab for the Play release, not the APK or a ZIP.
The APK is for sideload review. Both use the dedicated upload certificate with
SHA-256 0037dcd3ae7a98b77fa5ce8c441600d59c849e15f789318fa460a632af8c6a1b.
The public PEM is included in the preparation package; private keys are excluded.
Back up the key AND a portable recovery credential securely outside this computer.
The current Windows DPAPI credential alone is not a portable password backup.

Confirm Play App Signing configuration before first upload. Google may use a
different app-signing certificate; in that case the sideload APK cannot update
directly to the Play-installed app. Use Play-generated APKs for final update tests.
Previous registration proof for dev.minime.ime does not register app.minime.keyboard.
If Console requests proof for the new ID, use its new token and the certificate
specified in that registration flow; never embed the token in the production app.

After rights clearance, start internal testing, inspect Play processing and device
compatibility, and use closed testing before production. If the account is personal
and was created after November 13, 2023, Google currently requires at least 12 testers
continuously opted into a closed test for 14 days before applying for production
access. Account type/date and production eligibility are still unknown here.

[Signing](https://developer.android.com/studio/publish/app-signing),
[release setup](https://support.google.com/googleplay/android-developer/answer/9859348?hl=en),
[personal-account testing](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en).

## Test-track instructions

Ask testers to use their usual messaging, notes and Chrome fields across focused
modes; exercise partial spellings, mixed-language words, editing and candidate
selection, and record missed touches or visible stalls. Test private/password fields,
rotation, large font/display sizes, process restart and manual backup/import using
nonpersonal data. Record app version, Android version, mode and reproduction steps.
Do not request personal conversation logs, passwords or learned-word backups in public
issues. A full secure-field, accessibility, release-performance and 16 KB runtime
sign-off remains necessary; synthetic assertion counts are not language accuracy.

Keep managed publishing enabled when available to separate review approval from
production availability. Do not press Publish or roll out production until the owner
has reviewed the actual Console result. No hosted CI workflow is part of this process.
