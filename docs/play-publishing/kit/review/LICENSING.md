# Licensing

Original MinIME code and original project documentation are licensed under
[Apache-2.0](LICENSE), unless a file says otherwise. This licence does not replace
third-party licences or confer ownership of imported material.

- `third_party/`: original upstream licences, copyright, pins and source notices.
- `app/src/main/assets/` and generated models: source-derived data with separate
  MIT, Apache-2.0, CC0, CC BY-SA 4.0, ODbL and Unicode terms. The source catalog,
  compilation reports and packaged notices identify the applicable components.
- `app/src/main/rimeAssets/`: compiled Rime data; LGPL-3.0 source archives,
  patches and licences are in `third_party/rime/`. Native librime code is BSD.
- `docs/` and test resources containing imported corpus excerpts, screenshots,
  measurements or source records retain their original terms. These are not
  automatically Apache-licensed and are excluded from the runtime source bundle.
- `geography.tsv`: the OSM/Rudy-derived database is offered under ODbL-1.0,
  independently of other language dictionaries.

App code, CC BY-SA adaptations, ODbL geography and LGPL Rime data are provided as
separate components. No proprietary Google Zhuyin APK, code, artwork or model is
licensed or distributed by this project. The reference APKs are user-provided
research files excluded from Git and release archives.

The production context/spelling data derived from Universal Dependencies retains
an unresolved underlying-text rights review before Play distribution. Its existing
CC BY-SA annotation/database notice does not establish rights to every source text.
See `docs/play-publishing/PLAN.md` and `sources/release-policy.json`.

Maintain dictionary sources through the pinned source framework. Review EDRDG
source updates at least once per release and quarterly between releases; record
pins, licences and regression results before importing an update. A version check
is not automatic admission or a reason to weaken a source licence.
