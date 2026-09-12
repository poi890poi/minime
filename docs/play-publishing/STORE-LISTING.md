# Store submission worksheet — draft

App names: MinIME — Pinyin for Taiwan / MinIME — 台灣多語拼音鍵盤
Application ID: app.minime.keyboard. Version: 0.8.4 (32).

English short description:
Offline keyboard for Chinese, English, Taiwanese POJ and Japanese words.

English full description:

MinIME is an offline Android keyboard for Traditional Chinese Pinyin and Zhuyin,
English, Taiwanese Peh-oe-ji (POJ), and Japanese vocabulary.

Switch between focused language modes while typing. Choose phrase candidates,
use symbols and emoji, and enable local learning or add your own dictionary entries
in Settings. Taiwanese candidates can show a Han example alongside pronunciation;
long-press a paired candidate to enter its alternate form. Japanese vocabulary
suggestions include kana and Kanji; this is not a full Japanese sentence converter.

MinIME has no Internet permission, advertisements or analytics. Suggestions run on
your device. Optional backup/import lets you move settings and learned words using
a document you select. Backups are readable files and may contain personal words.

After installing, open MinIME and enable it in Android's keyboard settings, then
select it as your keyboard. Android shows its standard input-method warning.

MinIME is independently developed and is not a Google product. Language resources
come from separately licensed open sources; full notices are available in the app.

Traditional Chinese short description:
離線輸入法，支援中文拼音、注音、英文、台語白話字及日語詞彙。

Traditional Chinese full description:

MinIME 是離線 Android 輸入法，支援繁體中文拼音與注音、英文、台語白話字
（POJ）及日語詞彙。可切換主要語言模式，選擇候選詞，輸入符號與表情符號，
並在設定中啟用本機學習或加入自訂詞彙。

台語候選詞可同時顯示白話字與漢字範例；長按有對應形式的候選詞，可輸入
另一種形式。日語詞彙候選包含假名與漢字，目前不是完整的日語整句轉換器。

MinIME 不要求網路權限，沒有廣告或分析追蹤。候選詞在裝置上產生。
您可以自行匯出或匯入設定與學習資料；備份是可讀取的檔案，可能含有私人詞彙，
請選擇可信任的儲存位置。

安裝後請開啟 MinIME，在 Android 鍵盤設定中啟用，再選擇 MinIME 作為輸入法。
Android 會顯示標準的輸入法提醒。

MinIME 為獨立開發，並非 Google 產品。語言資料採用各自授權的開放來源，
完整來源與授權聲明可在應用程式內查閱。

## Console fields requiring completion

- Category: Productivity (Google's category guidance lists keyboards here). Price: free. Ads: no. No account/login or in-app purchases.
- Publisher name and support email: awaiting owner; do not submit placeholders.
- Privacy URL: https://github.com/poi890poi/minime/blob/codex/close-ime-gaps/PRIVACY.md
  Anonymous HTTPS access verified September 12, 2026 (HTTP 200).
- Target audience/content rating: owner answers the actual Console questionnaires;
  do not infer an IARC rating from keyboard functionality alone.
- Assets: kit/ contains the approved icon, localized feature graphics and actual
  keyboard screenshots. Provenance is in SCREENSHOT-PROVENANCE.md. The Notes editor
  is a capture host, not an advertised app feature. No Google artwork.
- Release notes: localized copy-ready files in kit/listing/, including new identity
  and old-package migration guidance. Full Console worksheet: CONSOLE-ANSWERS.md.

## Reviewer instructions

No credentials or subscription required. Open MinIME, follow the enable/select
controls, and choose MinIME in the Android keyboard picker. Open a normal text field
in a browser or notes app. Select the desired mode in MinIME Settings and enable its
optional dictionaries if needed. Return to a fresh text field after changing mode.
Try letters, candidate selection, symbols/emoji, and the English toggle. Password
fields intentionally suppress composition, suggestions and learning. Privacy and
source notices are accessible from MinIME Settings. Backup export/import is optional;
use nonpersonal test data and cancel Replace to keep current settings.

## Data safety assessment — proposed, not submitted

The current merged release manifest requests no app permissions; the IME service
requires Android's BIND_INPUT_METHOD permission from its caller. No network,
advertising, analytics or crash-reporting SDK is present. Trace this again on the
final signed release and review any new dependency before submitting declarations.

| Flow | Current handling | Proposed reporting treatment |
| --- | --- | --- |
| Composition, candidates, preferences and learning | On-device only | Not off-device collection |
| Accepted text to the receiving app | Explicit user typing through InputConnection | Assess user-initiated transfer exception; not developer collection |
| Manual JSON backup to chosen document provider | User selects destination; provider may upload it | Assess user-initiated transfer exception; disclose plaintext and provider behavior |
| Import | Reads the user-selected file locally after picker access | No independent upload |
| Support/source website | Browser handles navigation | Separate website practices; do not claim websites collect nothing |

Proposed answer is no developer collection or sharing of reportable user data for
this implementation, subject to the Console's current definitions and exceptions.
Do not claim encryption of exported files: they are unencrypted. No account creation
exists, so account-deletion workflows do not apply. Users can clear learned data,
remove custom entries, clear app storage or uninstall; exported copies require
separate deletion through their document provider. Do not advertise an independent
security review or Play approval that has not occurred.

Reference: [Google's Data safety definitions and exceptions](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
