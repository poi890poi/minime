# Store material preparation impact

Type: documentation, original branding assets and observation tooling. Produce
copy-ready en-US/zh-TW listings, existing-icon exports, feature graphics and real
MinIME screenshots. No production dictionary, candidate ranking or app behavior
changes. Samples are authored demonstration text, not evaluation evidence.

Use a debug-only editor host with the unchanged production IME. Capture actual
candidate output; no fabricated suggestion overlays. Temporarily use a 1080x1920
phone display viewport to meet store aspect ratios, then restore its exact prior
size, preferences and IME and verify display OFF. Never alter AOD.
Validate text limits, image dimensions/color formats, ZIP allowlist and hashes.
The kit is store material; existing licensing/signing/runtime gates remain open.


Shared-device correction: concurrent tasks can change the keyboard or sleep the
phone during captures. The first two attempts failed; one diagnostic showed
Samsung Honeyboard and was excluded. The final capture checks the selected MinIME
service before input/capture and passed, with all four images reviewed locally.
Do not attribute these contention failures to dictionary quality or app behavior.

The user explicitly requested cross-task collaboration. Both MinIME and SHINE
agreed on request/acknowledge/operate+cleanup/release, with SHINE owning the next
window. MinIME added the shared named mutex to test-device and capture-store before
any ADB command. Local checks verified nested ownership, cross-process exclusion,
release/reacquisition and PowerShell syntax. No phone operation was used for guard
validation. A mutex supplements coordination and cannot protect unwrapped commands.


Delivery review: automatic approval rejected the first public-upload attempt.
The internal release report included the test-phone serial. It was replaced with
an original publisher checklist, and the ZIP was rebuilt with an exact 27-file
allowlist. Inspection found no phone identifier, private local paths, preference
backups or signing key blocks. The narrower public-only retry was approved and
the downloaded Cloudflare bytes matched the local SHA-256 hash. No private report
was served by that blocked attempt. Screenshot source pixels were not edited.
