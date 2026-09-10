# MinIME branding integration

The approved 台-inspired farmer icon uses a broad bamboo-leaf hat, an enlarged
face with continuous curves, a rounded V smile and deep teal #006765. The master
512 px PNG is pixel-identical to the final approved prototype. `minime/` contains
the public, self-contained 90-file suite; its README documents all formats.

Type: visual design, generated assets and Android/site/store integration. The
runtime keyboard, dictionaries, privacy text, version and existing screenshots
are unchanged. The old M drawable is replaced with adaptive launcher resources;
the original store generator now shares the same artwork source. The informational
site receives favicons; no PWA or new application behaviour is enabled.

One source: `tools/icon_art.py`. Rebuild all exports, Android resources and public
store materials with `python tools/build_icon_suite.py`. Use Pillow and the fonts
listed in the suite README. Generated files preserve exact bytes in Git so the
SHA-256 manifests remain useful across checkouts.

Validation, September 11, 2026:

- Original approved 512 px master and generated PNG: identical pixels.
- Full square Play icon: opaque RGBA, 512x512, below 1 MB.
- Adaptive geometry: largest radius 30.62 dp inside the 33 dp safe radius on a
  108 dp layer; bounds also fit the central 66 dp area. Circular and rounded
  previews, 16–96 px icons, transparent and light/dark themed marks inspected.
- Monochrome eyes/mouth are actual alpha cutouts, verified at interior pixels.
  Optional XML/SVG even-odd V outline was compared against the raster stroke;
  differences occupy 262 boundary pixels at 1024 px (raster edge rounding).
- Clean output rebuild: identical 90-file manifest and ZIP SHA-256
  `df6ba62d180e0a708e9998cc29c68c70ebde9c44d1dc26f214878baf5bba216f`.
- `:app:assembleDebug :app:lintDebug --offline` passed. Packaged API 33 launcher
  XML has background, foreground and monochrome references. No new icon lint
  issues; 18 warnings remain in other project files.
- The first vector integration produced two long-path performance warnings.
  Final Android resources use density-specific PNG layers to retain the approved
  curves without those warnings. Editable SVG/XML masters remain in the suite.
- Public asset ZIP downloaded through the existing tunnel and matched local SHA.

No phone operations occurred; SHINE retained its exclusive reservation. Physical
launcher and themed-icon behaviour on OEM devices are not claimed as tested.
Local logs: `artifacts/icon-suite/android-final.txt`, `final-verify.txt` and
`verification.json`. This work does not clear unrelated Play-release gates.
