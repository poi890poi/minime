# App-release checklist

Store materials are prepared. No Play release has been submitted.

Before uploading an application bundle:

- Resolve the underlying-text rights question for the production data derived
  from Universal Dependencies, or evaluate and adopt a suitable replacement.
  Retain all other third-party attribution and source-distribution obligations.
- Confirm the publisher name, support email, Play account status/type and permanent
  application ID. Free, offline and no ads are the planned launch settings.
- Configure the upload key and Play App Signing. Build a non-debuggable, correctly
  signed AAB and verify its certificate. The existing preparation AAB is unsigned.
- Host the privacy policy at a stable public HTTPS address, reachable without login.
  Confirm the policy and Data safety responses against the final app's behavior.
- Run Android 16 and 16 KB runtime tests. Build-time ELF/ZIP alignment checks have
  passed; these checks alone do not certify runtime compatibility.
- Complete release-build typing-latency and touch-hit-rate checks, accessibility,
  rotation, normal/private/password editor flows, and full manual-backup migration.
- Review the supplied screenshots against the final release. They show the 0.8.0
  production keyboard code running in a debug-only example editor. Store assets
  may need recapture if the final keyboard changes.
- Complete the Console's app access, Data safety, ads, target-audience and content-
  rating forms. Follow the testing requirements shown for the owner's account.
- Begin with internal testing and then closed testing before production submission.

Never enter signing passwords or private dictionary backups into a public issue,
store description or screenshot. Keep production keys in secure storage.

Full project release evidence and source notices are maintained in the repository:
https://github.com/poi890poi/minime/tree/codex/close-ime-gaps
