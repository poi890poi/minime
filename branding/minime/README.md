# MinIME icon suite

Approved design: a Taiwan farmer's broad bamboo-leaf hat over a friendly face,
inspired by 台. Deep teal #006765; warm cream #FFF7E5; bamboo leaf #D9C28E.
The face has continuous curves and a rounded V smile. Transparent colour marks
include a thin teal face contour for visibility on white and light surfaces. Artwork is original;
no party, flag or third-party logo artwork is included.

- `source/`: editable SVG masters and the Python geometry source (Pillow needed
  for PNG exports). Full icon, transparent colour mark, black and white cutout marks.
- `png/`: rounded icon at 16, 24, 32, 48, 64, 96, 128, 192, 256, 512 and 1024 px.
- `transparent/`: colour, black and white marks at 256, 512 and 1024 px.
- `android/res/`: ready-to-copy launcher resources. Adaptive icons for API 26+;
  dedicated monochrome layer for API 33+; round and standard density fallbacks.
  Density-matched PNG layers preserve the exact curves without parsing long paths.
  Optional vector XML masters are in `source/`, outside packaged Android resources.
- `android/layers/`: separate 108 dp foreground, background and monochrome layers,
  in SVG and all five density PNG sizes. Do not pre-mask these layers.
- `store/`: opaque 512 px Play icon and editable SVG, plus English and Traditional
  Chinese feature graphics. Upload the square PNG: Play applies its corner mask.
- `web/`: SVG favicon, multi-resolution ICO, PNG icons, 180 px Apple touch icon and
  optional manifest example. Icon entries use purpose `any`, not `maskable`.
- `PREVIEW.png`: design and mask/size comparisons. `MANIFEST.json`: payload hashes.

Android manifest integration:

    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"

The 108 dp adaptive layers contain the approved artwork within the central 66 dp
safe area. The OS supplies launcher masks and themed colours. Eyes and mouth in
monochrome marks are transparent cutouts, so they survive arbitrary tinting.
The themed preview colours are examples, not fixed app colours.

Keep proportions and the spacing between hat and face. The solid-background icon
is preferred on busy backgrounds. Transparent colour marks have a teal contour;
adaptive foregrounds omit it because their background is already teal. Use the black/white cutout marks when one ink
is required. The bamboo-leaf colour is part of the design, not a material texture.

Rebuild from the repository with `python tools/build_icon_suite.py` (Pillow,
Windows Segoe UI and Microsoft JhengHei for the preview/feature text). The source
icon geometry needs no font. Font binaries are not distributed. The editable
feature SVGs reference system fonts and may render differently elsewhere.

Artwork and original tooling: Apache-2.0; see LICENSE. This asset-only ZIP contains
no APK, signing keys, device backups or language dictionaries. It does not clear
Google Play app-release gates. Android resource build/lint and offline mask checks
are separate from physical launcher/device validation.

Specifications checked September 11, 2026:
https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
https://developer.android.com/distribute/google-play/resources/icon-design-specifications
