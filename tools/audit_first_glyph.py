"""Replay the existing Chinese regression corpus through two native adapters.

Measures candidate access and IPC + decoder latency, not semantic accuracy or
phone latency. All inputs are reused regression data; no fresh holdout claim.
"""
import argparse
import gzip
import hashlib
import json
import pathlib
import subprocess
import time


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("before", type=pathlib.Path)
    parser.add_argument("after", type=pathlib.Path)
    parser.add_argument("--corpus", type=pathlib.Path, default=pathlib.Path("docs/conversation-ranking/corpus/inputs.tsv"))
    parser.add_argument("--output", type=pathlib.Path, default=pathlib.Path("artifacts/first-glyph/audit"))
    args = parser.parse_args()
    root = pathlib.Path.cwd()
    args.output.mkdir(parents=True, exist_ok=True)
    queries = sorted({line.split("\t")[2] for line in args.corpus.read_text(encoding="utf8").splitlines() if line and not line.startswith("en-")})
    results = {}
    for stage, exe in [("before", args.before), ("after", args.after)]:
        user = args.output / (stage + "-user")
        user.mkdir(exist_ok=True)
        process = subprocess.Popen([str(exe.resolve()), str(root / ".tools/rime-evaluation/msvc/dist/lib/rime.dll"), str(root / "app/src/main/rimeAssets/rime"), str(user.resolve())], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, encoding="utf8")
        assert process.stdout.readline().strip() == "READY 1.16.1"
        rows = []
        try:
            for query in queries:
                start = time.perf_counter_ns()
                process.stdin.write(query + "\n")
                process.stdin.flush()
                candidates = []
                while True:
                    line = process.stdout.readline()
                    if not line:
                        raise RuntimeError("Native evaluator closed early")
                    if line.strip() == "END":
                        break
                    end, origin, text = line.rstrip("\r\n").split("\t")
                    candidates.append(dict(end=int(end), origin=origin, text=text))
                rows.append(dict(input=query, candidates=candidates, micros=(time.perf_counter_ns() - start) / 1000))
        finally:
            process.stdin.close()
            process.wait(timeout=10)
        assert process.returncode == 0
        results[stage] = rows
        payload = ("\n".join(json.dumps(r, ensure_ascii=False) for r in rows) + "\n").encode("utf8")
        (args.output / (stage + ".jsonl.gz")).write_bytes(gzip.compress(payload, mtime=0))

    summary = dict(corpus_sha256=digest(args.corpus), before_sha256=digest(args.before), after_sha256=digest(args.after), queries=len(queries), removed_candidates=0, changed_full_order=0, gained_first_character=0, lost_first_character=0, constructed_after=0)
    def glyphs(row):
        return [c for c in row["candidates"] if c["end"] < len(row["input"]) and len(c["text"]) == 1]
    for before, after in zip(results["before"], results["after"]):
        summary["removed_candidates"] += sum(c not in after["candidates"] for c in before["candidates"])
        summary["changed_full_order"] += [c for c in before["candidates"] if c["end"] == len(before["input"])] != [c for c in after["candidates"] if c["end"] == len(after["input"])]
        summary["gained_first_character"] += not glyphs(before) and bool(glyphs(after))
        summary["lost_first_character"] += bool(glyphs(before)) and not glyphs(after)
        summary["constructed_after"] += sum(c["origin"] != "L" for c in after["candidates"])
    for stage, rows in results.items():
        times = sorted(r["micros"] for r in rows)
        summary[stage] = dict(with_first_character=sum(bool(glyphs(r)) for r in rows), total_candidates=sum(len(r["candidates"]) for r in rows), latency_us={str(p): times[int((len(times)-1)*p/100)] for p in (50, 95, 99)})
    (args.output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf8")
    print(json.dumps(summary, indent=2), flush=True)
    assert summary["removed_candidates"] == summary["changed_full_order"] == summary["lost_first_character"] == summary["constructed_after"] == 0


if __name__ == "__main__":
    main()
