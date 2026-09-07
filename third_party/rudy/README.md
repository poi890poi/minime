# Rudy Map / OpenStreetMap geography names

Upstream: https://rudymap.tw/ (MOI.OSM Taiwan TOPO, POIv3).
Snapshot: 2026.09.03; fetched 2026-09-08. Exact URL, response metadata,
ZIP SHA-256 and archive member identity are in `source.json`.

© OpenStreetMap contributors; POI packaging by Rudy Map. Database data and this
derived geography database are distributed under ODbL 1.0:
https://www.openstreetmap.org/copyright
https://opendatacommons.org/licenses/odbl/1-0/

The importer takes all descendants of Rudy's hiking, nature, settlement, waterway
and historic-site categories. It excludes the separate NPA shelter and giant-tree
datasets and accepts only records carrying OSM P/W/R IDs. It does not copy map
styles, contours, tiles, hiking articles or pictures.

`extracted-names.json.gz` is the complete machine-extracted name/reading snapshot
after the documented category, script and length rules: no name allowlist, manual
spellings, per-name web citations, or test-driven entry selection. It includes
names whose readings cannot yet be resolved, so later dictionary improvements can
increase coverage without another download. Provenance is dataset-level.

`app/src/main/assets/geography.tsv` is the entire derived runtime database, kept
separate from CC-BY-SA language packs. Readings prefer upstream tags, then exact or
unambiguous units from the MIT-licensed McBopomofo dataset already in this repo.
Derived readings are not independently verified local pronunciations. Candidate
ties use source unigram frequency; POI way-segment counts are not popularity.

Offline rebuild: `python tools/compile_rudy_names.py`.
Refresh: `python tools/fetch_rudy_poi.py`, then
`python tools/compile_rudy_names.py --extract`.

The full ZIP and temporary SQLite database are ignored; runtime has no network
dependency. The normalized source snapshot, extraction/compilation code, complete
derived database, hashes, counts and unavailable readings are provided. Review
source-version changes and evaluation deltas before shipping an update.
