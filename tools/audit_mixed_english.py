"""Measure Latin alternatives on frozen Han-target tasks, not semantic nonsense."""
import argparse, collections, csv, hashlib, json, re
from pathlib import Path
from audit_candidate_usefulness import inventory, relevance


def latin_kind(text, raw):
    if not re.fullmatch(r"[A-Za-z]+(?:[ '\u2019-][A-Za-z]+)*", text):
        return None
    text, raw = text.lower(), raw.lower()
    if text == raw:
        return 'raw'
    if text.startswith(raw):
        return 'prefix_extension'
    if text.replace("'", '').replace('\u2019', '') == raw.replace("'", ''):
        return 'apostrophe_restoration'
    return 'other_latin'


def summarize(rows):
    groups = collections.defaultdict(collections.Counter)
    examples = []
    for row in rows:
        if not row['target']:
            continue
        values = inventory(row)
        first = next((i for i, (text, end) in enumerate(values)
                      if relevance(text, end, row['raw'], row['target']) != 'off_target'), None)
        kinds = [latin_kind(text, row['raw']) for text, _ in values]
        for group in ('all', row['genre'], row['genre'] + '/' + row['condition']):
            c = groups[group]
            c['episodes'] += 1
            c['latin_top1'] += bool(kinds and kinds[0] and kinds[0] != 'raw')
            for n in (5, 8):
                shown = [k for k in kinds[:n] if k and k != 'raw']
                c[f'with_latin_first{n}'] += bool(shown)
                c[f'latin_slots_first{n}'] += len(shown)
                for kind in shown:
                    c[f'first{n}_{kind}'] += 1
            if first is not None:
                c['with_useful_choice'] += 1
                c['latin_before_first_useful'] += sum(bool(k and k != 'raw') for k in kinds[:first])
        if any(k and k != 'raw' for k in kinds[:8]):
            examples.append({k: row[k] for k in ('genre', 'id', 'condition', 'raw', 'target')} |
                            dict(first8=values[:8], kinds=kinds[:8]))
    # Archive every affected task; presentation examples must not become dictionary exceptions.
    return dict(groups={k: dict(v) for k, v in groups.items()}, affected=examples)


def main():
    p = argparse.ArgumentParser(); p.add_argument('input', type=Path); p.add_argument('output', type=Path)
    a = p.parse_args()
    with a.input.open(encoding='utf-8') as f:
        report = summarize(csv.DictReader(f, delimiter='\t'))
    report['scope'] = 'Latin alternatives on Han-target tasks; prefix extension is spelling evidence, not confidence or semantic invalidity. Raw recovery excluded.'
    report['input_sha256'] = hashlib.sha256(a.input.read_bytes()).hexdigest()
    a.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report['groups']['all']))


if __name__ == '__main__':
    main()
