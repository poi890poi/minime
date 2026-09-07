# Learned dictionary portability and sync proposal

No transfer is implemented or enabled in this release. The IME has no INTERNET
permission and Android cloud backup stays disabled.

Start with user-initiated export/import through Android's document picker (SAF).
The user can select local storage, a USB drive, or an installed document provider.
The IME need not embed a cloud SDK or hold account credentials. Show a preview of
phrase count, sources and conflicts before importing; allow replacing or merging.
Keep built-in source packs separate from personal phrases.

Use a versioned UTF-8 JSON container with explicit language and orthography
(`nan-Latn`, `POJ` for Taiwanese), canonical output, input aliases and provenance.
Do not convert POJ to Tâi-lô in storage or export. Each learned record needs a stable
ID, per-device observation counters, and a deletion tombstone. Merge each device's
counter by maximum, then sum across devices: importing the same file twice must
not multiply counts. Resolve deletion before observations; a deliberate re-add
gets a new generation. Keep tombstones until all known replicas acknowledge them.
Imports must enforce reading/output validation, count caps and total size limits.
Today's local phrase counter format is not itself a safe sync protocol.

Offer passphrase-encrypted exports using an existing audited container/library,
authenticated encryption and a memory-hard password KDF, after reviewing Android
support and recovery UX. Never write plaintext exports without the user's explicit
choice. A lost passphrase cannot be recovered by MinIME. No editor surroundings,
private/secure typing, transient composition or device keystroke logs belong in
the export.

For automatic sync later, prefer a separate optional companion app that transports
the encrypted container via a user-selected provider (WebDAV, Drive, or a local
Syncthing folder). Keep transport/network credentials outside the IME. A separate
sync switch, device list, manual sync, conflict preview and delete-everywhere action
are required. Provider backup retention may outlive a delete; explain that at the
point where the user chooses a provider. This phase needs migration and independent
idempotency/deletion tests before release.

Recommendation: implement reviewed SAF import/export first, then encrypted
companion sync. Android Auto Backup is simpler but provides poor phrase-level
review, conflict control and portable source tracking, so it is not the preferred
solution for this requirement.
