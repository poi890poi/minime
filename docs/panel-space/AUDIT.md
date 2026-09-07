# Header and palette space

Baseline 0efddc6 / app 0.5.4. User identifies an unused top band and asks to
observe Google's replacement of the keyboard status bar in symbols/emoji.
Classification: layout defect plus intentional palette space reallocation.

Observed Google symbols replace the Chinese/English/voice toolbar with category
controls at the same vertical position. Google's phonetic text is a small chip
above the candidate row, outside the fixed keyboard body. MinIME reserves a
full-width 24 dp blank header and retains a second toolbar above palette controls.
Its portrait IME begins at y=1332, versus Google's y=1407 on this phone; the extra
24 dp explains almost all of that top difference. MinIME's letters are already
at stable vertical positions. Keep those positions while returning the empty
24 dp band to the editor and showing phonetics only as a bounded floating chip.

Use the existing nonfocusable PopupWindow pattern already used by the punctuation
palette. Dismiss the annotation on panel changes, input-view finish, window hide
and detach; exact spelling remains selectable. Keep candidate row height and
touch handling unchanged. The fixed content height becomes 48 + four key rows
(284 dp portrait / 184 dp landscape). System navigation insets remain additive.

On symbols/emoji, hide the candidate/language row and use all 48 dp for the
palette. Add a third grid row at comparable cell height instead of retaining a
second empty toolbar. Bottom-row actions and the overall input window stay fixed.
Preserve composition, candidate ranking, data, privacy/learning and gesture rules.

Verify idle controls begin at the keyboard top, palette controls replace that
row, all grid/chooser children fit, typing never moves keys or the input window,
and raw recovery remains accessible above the candidate row. Recheck popup hide,
editor changes, candidate stability, palettes, and imprecise touches. Test only
RFCR91GWXLX; restore preferences/IME and sleep/verify the display after each run.

The preliminary plan incorrectly requested a direct Emoji action from Google's
symbol layout. It only exposes emoji by returning to letters and holding comma.
Retain those partial observations, not as a passing paired study. Its final
case also hit a stale editor assertion on the main thread and crashed the test
process. Move that ownership assertion to the test thread with a bounded settle
check so failure is recorded as unavailable without bypassing the ownership gate.
