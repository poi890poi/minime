"""Read non-streaming ART v3 sampled traces; diagnostic estimates, not call timers.

Format reference: AOSP tools/dmtracedump/tracedump.cc, revision
002aca7141e7baf488b02f330b307f059c47a84d. This is an independent narrow reader.
"""
import argparse
from collections import Counter
import gzip
import json
from pathlib import Path
import struct


def parse(data):
    header, separator, binary = data.partition(b"*end\n")
    if not separator:
        raise ValueError("Missing ART text header")
    text = header.decode("utf-8")
    if "data-file-overflow=true" in text:
        raise ValueError("Truncated sampling buffer")
    methods, threads, section = {}, {}, None
    for line in text.splitlines():
        if line.startswith("*"):
            section = line
        elif section == "*methods" and line:
            identity, name = line.split("\t", 1)
            methods[int(identity, 16)] = name
        elif section == "*threads" and line:
            identity, name = line.split("\t", 1)
            threads[int(identity)] = name
    magic, version, offset, start, record_size = struct.unpack_from("<4sHHQH", binary)
    if magic != b"SLOW" or version != 3 or record_size not in (10, 14):
        raise ValueError(f"Unsupported trace layout: {magic!r}, {version}, {record_size}")
    if (len(binary)-offset) % record_size:
        raise ValueError("Incomplete record")
    main_ids = [identity for identity, name in threads.items() if name == "main"]
    if len(main_ids) != 1:
        raise ValueError("Expected exactly one main thread")
    stack, previous = [], None
    leaves, children = Counter(), Counter()
    application_us = 0
    records = 0
    for at in range(offset, len(binary), record_size):
        thread, encoded, clock = struct.unpack_from("<HII", binary, at)
        if thread != main_ids[0]:
            continue
        records += 1
        if previous is not None:
            elapsed = clock-previous
            if elapsed < 0:
                raise ValueError("Non-monotonic main-thread clock")
            names = [methods.get(method, f"unknown:{method}") for method in stack]
            roots = [i for i, name in enumerate(names) if name.replace(".", "/").startswith("dev/minime/core/CompositionEngine\tapplyCandidates\t")]
            if roots:
                root = roots[0]
                application_us += elapsed
                leaves[names[-1]] += elapsed
                children[names[min(root+1, len(names)-1)]] += elapsed
        previous = clock
        method, action = encoded & ~3, encoded & 3
        if action == 0:
            stack.append(method)
        elif action in (1, 2):
            if not stack or stack[-1] != method:
                raise ValueError(f"Unbalanced main-thread trace at record {records}")
            stack.pop()
        else:
            raise ValueError("Unknown trace action")
    if not application_us:
        raise ValueError("No candidate-application samples; inspect trace before drawing conclusions")
    return dict(main_records=records, sampled_application_us=application_us,
                direct_children_us=children.most_common(30), exclusive_leaf_us=leaves.most_common(40),
                limitation="Sample-derived thread-clock attribution; profiler overhead prevents latency acceptance claims.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    data = args.source.read_bytes()
    result = parse(data)
    args.output.mkdir(parents=True, exist_ok=True)
    (args.output/"profile.json").write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
    (args.output/"application.trace.gz").write_bytes(gzip.compress(data, mtime=0))
    print(json.dumps(result, indent=2))
