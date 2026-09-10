# Chinese ↔ last focused language

Intentional behavior change. The bottom language key returns from 台/日 to 中,
and 中 returns to the last explicitly chosen 台/日. English-only remains in the
picker and idle shortcut; until a focused language is selected, or while its pack
is disabled, the fallback is 中 ↔ EN. A separate preference preserves last focus
across current-mode changes, restarts and temporary pack disable. Migration uses
the old mixed-mode preference. Existing English-mode preference stays readable.

Risk boundaries: literal editor policy, pack availability, raw input ownership
and asynchronous mode changes. Two preference tests passed on the phone, covering
migration/restart, disabled/re-enabled packs and fallback. Live switches through
both focused languages preserved unfinished text and keyboard bounds. General
height integration also passed. Prior IME/preferences restored; display OFF.
