# Reproducible audit evidence

Collected 2026-09-08 for source assessment, outside the Android asset tree.
These source snapshots retain their upstream attribution and terms; the repository's
code license does not replace them. Stored text is audit material, not instructions
to execute. No upstream generators were run.

| Files | Source, revision and terms |
|---|---|
| `taiwan-md-inventory.json.gz`, `taiwan-md-sample-plan.json.gz`; Taiwan.md records inside `sample-records.json.gz` | [Taiwan.md contributors](https://github.com/frank890417/taiwan-md/tree/1056f879181bc12a0b85ecde9e51d847d692179d), commit `1056f879181bc12a0b85ecde9e51d847d692179d`; content CC BY-SA 4.0. Each sample retains its original path and pinned raw URL for attribution. |
| `kemdict-inventory.json.gz`; Kemdict records inside `sample-records.json.gz` | [KisaragiHiu / 如月飛羽 and Kemdict contributors](https://github.com/kemdict/kemdict/tree/5e32a5f0679a59b36668a86ec7fe5bb4ae554ab0), commit `5e32a5f0679a59b36668a86ec7fe5bb4ae554ab0`; Kisaragi's own dictionary CC0. Source LICENSE/README records are included. Imported MOE text retains separate terms; it was not counted as Kisaragi entries. |
| `taicol.zip` | [TaiCOL / Taiwan Biodiversity Information Facility](https://ipt.taibif.tw/resource?r=taibnet_com_all&v=1.13), published archive v1.13, CC BY 4.0. Exact archive includes its EML metadata and attribution. |
| `cip.json.gz` | [Indigenous Peoples Council](https://data.gov.tw/dataset/156632), dataset A53000000A-111027-001, downloaded snapshot; Taiwan Government Open Data License 1.0. Original field values preserved. |
| `music-catalog.json.gz`, `music-records.json.gz` | [Bureau of Audiovisual and Music Industry Development](https://opendata.culture.tw/frontsite/openData/detail?datasetId=581), all linked CSVs in fetched catalog; Taiwan Government Open Data License 1.0. Each CSV record retains its URL, original byte hash and decoded text. |
| `measurements.json`, `artifacts.json`, `fetch-errors.json.gz` | MinIME-generated audit measurements, fingerprints and retrieval status. These do not confer new rights on upstream data. |

Compressed JSON represents the fetched/assembled audit snapshot. Sample text is
stored without editorial changes; CSV text removes its encoding BOM when decoded.
The inventories and sample plan are generated metadata, not copies of entire Git
repositories. Raw-byte and uncompressed-content fingerprints are in `artifacts.json`.

Run `python tools/audit_source_inventory.py` without a network connection to use
these archives when the ignored retrieval cache is absent. The comparison asset
hashes in `measurements.json` identify the MinIME baseline: later asset changes
will legitimately change the overlap measurements and require a new audit review.
