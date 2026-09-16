"""Summarize frozen completion probes; percentages always retain their denominator."""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path


def summarize(source):
    rows = list(csv.DictReader(source.open(encoding="utf-8"), delimiter="\t"))
    groups = collections.defaultdict(list)
    for row in rows:
        groups[("all", row["condition"])].append(row)
        groups[(row["genre"], row["condition"])].append(row)
    result = {}
    for (genre, condition), samples in sorted(groups.items()):
        stats = {}
        for method in ("baseline", "top", "margin"):
            changes = [r for r in samples if r[method] != r["baseline"]]
            stats[method] = {
                "correct": sum(r[method] == r["expected"] for r in samples),
                "count": len(samples),
                "new_changes": len(changes),
                "new_correct": sum(r[method] == r["expected"] for r in changes),
                "new_wrong": sum(r[method] != r["expected"] for r in changes),
                "broke_baseline": sum(r["baseline"] == r["expected"] and r[method] != r["expected"] for r in samples),
            }
        result[genre + "/" + condition] = stats
    return {"rows": len(rows), "unique_raw": len({r["raw"] for r in rows}),
            "sha256": hashlib.sha256(source.read_bytes()).hexdigest(), "groups": result}


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    args.output.write_text(json.dumps(summarize(args.source), indent=2) + "\n", encoding="utf-8")
