# Language switch, Caps Lock, punctuation and palettes

MinIME 0.2.0 adds the user's requested visible EN / 中 button. One tap accepts
pending composition and switches between literal English and the saved Chinese
layout. The Chinese layout can remain Pinyin. English uses ASCII punctuation;
returning to Chinese restores Chinese punctuation. Mixed input remains available
in Chinese mode. URL, password and numeric field restrictions retain priority.

Shift now supports a timed double tap for Caps Lock and one tap to unlock, with a
distinct locked label. A slow second tap cancels one-shot Shift. Intervening keys
cancel the double-tap interval. Upward letter slides still commit capitals.

Bottom comma and period accept pending composition and commit immediately.
Upward punctuation slides choose the other width. Hold comma opens emoji; hold
period opens punctuation/text-face choices and a Chinese-mode width preference.
English letter slides use ASCII brackets, quotes and punctuation.

The emoji panel includes all 3,010 fully qualified sequences from the pinned
official Unicode Emoji 12.0 data: skin tones, ZWJ families, flags and other groups.
It uses the phone's fonts and stores no recent-use history. Symbols cover common
punctuation, brackets, arrows, mathematics, currencies, numbers, shapes, text faces,
units and Greek. Category and subgroup choices stay inside the keyboard so they
do not steal editor focus. ABC returns to letters; Backspace delegates complete
grapheme deletion to the host editor. Other editors and older fonts need broader
validation. This does not claim the newest Unicode emoji inventory or Google's
exact arrangement, recents or ranking behavior.

## Reference observations

The user-installed Google Zhuyin 2.4.5 was exercised in the local synthetic editor.
Preserved evidence in [caps-punctuation](evidence/caps-punctuation/) includes:

- Double Shift kept A and B uppercase; unlocking produced c.
- Pinyin nihao followed by comma immediately produced 你好，; period added 。.
- Literal hi in Chinese mode still used ，。.
- A single English-keyboard tap exposed English letters and ASCII punctuation;
  typing hi then comma/period produced `hi,.`.
- Holding comma opened the categorized emoji panel. Ordinary symbols included
  digits, punctuation, brackets, arrows, mathematical symbols and shapes.

Failed globe/return-to-Chinese trials and period-hold captures taken only after
release are excluded from these claims. APK resources informed functional key
mappings; no proprietary code, fonts, images or language models are included.
Emoji data and its license are attributed in third_party/unicode and app notices.

## Verification and limitations

Current gates and APK identity are recorded in [VERIFICATION.md](VERIFICATION.md).
New visible phone tests cover double-tap/slow-tap/unlock, language switching during
composition and return to Zhuyin, immediate punctuation and width changes, emoji
group paging, whole skin-tone/family/flag insertion and deletion, symbol categories
and return to typing. Initial failures are retained in ignored diagnostic artifacts.

The runner protects original app preferences outside the instrumented process and
restores them on success, failure or its 180-second timeout. It restores the previous
keyboard and sleeps the display. These UI changes do not improve the Pinyin model's
remaining ranking limitations; see PINYIN_PREDICTION_RESULTS.md.
