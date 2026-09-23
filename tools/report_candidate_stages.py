"""Test-only callback decomposition; never equates decoder work with display latency."""
import argparse
import csv
import gzip
import json
import math
import statistics
from collections import Counter
from pathlib import Path


def summary(values):
    values = sorted(values)
    if not values:
        return {"observed": 0}
    return dict(observed=len(values), mean_ms=statistics.mean(values),
                p50_ms=statistics.median(values), p95_ms=values[math.ceil(.95*len(values))-1],
                max_ms=max(values))


def report(source):
    rows = list(csv.DictReader(source.open(encoding="utf-8"), delimiter="\t"))
    groups = {}
    for mode in dict.fromkeys(r["mode"] for r in rows):
        requested = [r for r in rows if r["mode"] == mode]
        delivered = [r for r in requested if int(r["delivered_ns"])]
        values = {key: [] for key in ("delivery", "callback", "glyph", "render", "votes", "residual")}
        for row in delivered:
            callback = int(row["finished_ns"]) - int(row["delivered_ns"])
            glyph, render = int(row["glyph_ns"]), int(row["render_ns"])
            votes = int(row.get("votes_ns", 0))
            sample = dict(delivery=int(row["delivered_ns"])-int(row["requested_ns"]),
                          callback=callback, glyph=glyph, render=render, votes=votes,
                          residual=callback-glyph-render-votes)
            assert min(sample.values()) >= 0, row
            for key, value in sample.items():
                values[key].append(value/1e6)
        groups[mode] = dict(requests=len(requested), delivered=len(delivered),
                            undelivered=len(requested)-len(delivered),
                            stages={k: summary(v) for k, v in values.items()})
    return groups


def observations(touches, stages):
    """Partition every injected letter; do not equate unchanged presentation with loss."""
    groups = {}
    for index, touch in enumerate(touches):
        if touch["action"] != "key":
            continue
        end = int(touches[index+1]["up_ns"]) if index+1 < len(touches) else float("inf")
        group = groups.setdefault(touch["mode"]+"/"+touch["interval_ms"], Counter())
        matching = [r for r in stages if r["mode"] == touch["mode"] and r["query"] == touch["expected"]
                    and int(touch["up_ns"]) <= int(r["requested_ns"]) < end]
        group["letters"] += 1
        if int(touch["candidate_submit_ns"]):
            group["observed_frame"] += 1
            if index+1 < len(touches) and int(touches[index+1]["down_ns"]) <= int(touch.get("candidate_pre_draw_ns", 0)) < end:
                group["observed_during_next_press_subset"] += 1
        elif touch["mode"] == "english":
            group["synchronous_unobserved"] += 1
        elif not matching:
            group["unmatched_request"] += 1
        else:
            row = matching[-1]
            if not int(row["finished_ns"]):
                group["undelivered"] += 1
            elif int(row["finished_ns"]) >= end:
                group["ready_after_observation_window"] += 1
            elif row.get("presentation_changed") == "false":
                group["ready_unchanged_presentation"] += 1
            else:
                group["ready_changed_without_observed_frame"] += 1
    return groups


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--touch", type=Path)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    result = report(args.source)
    if args.touch:
        with args.touch.open(encoding="utf-8") as f:
            touches = list(csv.DictReader(f, delimiter="\t"))
        with args.source.open(encoding="utf-8") as f:
            stages = list(csv.DictReader(f, delimiter="\t"))
        result["frame_observations"] = observations(touches, stages)
    (args.output / "stages.json").write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
    (args.output / "stages.tsv.gz").write_bytes(gzip.compress(args.source.read_bytes(), mtime=0))
    print(json.dumps(result, indent=2))
