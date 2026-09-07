# Unicode emoji data

Pinned Unicode Emoji 12.0 `emoji-test.txt`, official source:
https://www.unicode.org/Public/emoji/12.0/emoji-test.txt

Copyright © 2019 Unicode, Inc. Distributed under the Unicode data license;
see LICENSE.txt (retrieved from https://www.unicode.org/license.txt).
`tools/compile_emoji.py` preserves fully-qualified sequences and their descriptive
names, generating `app/src/main/assets/emoji.tsv`. No image assets are bundled;
Android renders the characters. Emoji 12.0 is chosen for the Android 10 minimum
platform; OEM font coverage can still vary.
