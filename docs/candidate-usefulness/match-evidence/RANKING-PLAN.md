# Follow-up loop: stronger evidence for the Chinese preview

Baseline: `9875177`. Behavior experiment confined to the existing Chinese preview
when Space still preserves raw input and no accepted English context/case/custom
choice overrides it. Currently the first Han alternative can be a forward
completion even when a later candidate matches every source reading unit.

Try preferring the first existing, attested whole-input Han candidate whose
reading is fully typed or matched through every source unit. If the existing
preview is a forward completion, exchange only those two Chinese positions before
the existing preview move. Keep all English positions, inventory, input spans,
Space choice and other modes unchanged. Do not add candidates, weights or words.

Acceptance: no per-case loss of English first-five/eight coverage or Space change;
no worse aggregate first-eight whole-target or useful-choice coverage in any
declared genre of the frozen Chinese corpus. Report individual gains/losses and
reject if tradeoffs do not support landing. These are reused labeled tasks, not
fresh holdout accuracy or a semantic nonsense classifier. Local core first; run
the pinned desktop gate if the experiment survives. No device claim.
