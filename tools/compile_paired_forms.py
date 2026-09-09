"""Build source-attested Taiwanese output pairs; never import phonetic entries."""
from pathlib import Path
from collections import Counter, defaultdict
import argparse, csv, hashlib, json, re, unicodedata
from sources import require_sources

ROOT = Path(__file__).resolve().parent.parent

def build(check=False):
    require_sources('itaigi', 'mcbopomofo')
    paths = ['third_party/itaigi/itaigi.csv', 'third_party/mcbopomofo/phrase.occ', 'app/src/main/assets/addons.tsv']
    occurrence = {}
    for line in (ROOT / paths[1]).read_text(encoding='utf8').splitlines():
        p = line.split()
        if len(p) == 2:
            occurrence[p[0]] = float(p[1])
    single = {c: n for c, n in occurrence.items() if len(c) == 1 and '\u3400' <= c <= '\u9fff' and n > 0}
    mass = sum(single.values()); running = 0; boundary = 0
    for count in sorted(single.values(), reverse=True):
        running += count
        boundary = count
        if running >= mass * .99:
            break
    familiar = {c for c, count in single.items() if count >= boundary}
    shipped = {(p[3], p[2]) for line in (ROOT / paths[2]).read_text(encoding='utf8').splitlines()
               for p in [line.split('\t')] if len(p) == 5 and p[0] == 'poj' and p[3].startswith('itaigi:')}
    eligible = defaultdict(list); excluded = Counter()
    with (ROOT / paths[0]).open(encoding='utf-8-sig') as stream:
        for entry in csv.DictReader(stream):
            source = 'itaigi:' + entry['DictWordID']
            phonetic = unicodedata.normalize('NFC', entry['PojUnicode'])
            if (source, phonetic) not in shipped:
                continue
            han = unicodedata.normalize('NFC', entry['HanLoTaibunPoj'].strip())
            if not re.fullmatch('[\u3400-\u9fff]{1,12}', han):
                excluded['not_single_all_Han_form'] += 1
                continue
            if any(c not in familiar for c in han):
                excluded['outside_familiar_character_set'] += 1
                continue
            eligible[phonetic].append((han, source))
    rows = []
    for phonetic, choices in sorted(eligible.items()):
        han, source = min(choices, key=lambda p: (-min(single[c] for c in p[0]), -occurrence.get(p[0], 0), int(p[1].split(':')[1])))
        rows.append(('poj', phonetic, han, source))
    payload = ''.join('\t'.join(row) + '\n' for row in rows).encode('utf8')
    output = ROOT / 'app/src/main/assets/paired-forms.tsv'
    if check:
        assert output.read_bytes() == payload, "Paired forms differ from deterministic extraction"
    else:
        output.write_bytes(payload)
    report = dict(format=1, pairs=len(rows), existing_itaigi_outputs=len({p[1] for p in shipped}),
                  familiar_characters=len(familiar), minimum_character_occurrence=boundary,
                  familiar_occurrence_fraction=sum(single[c] for c in familiar)/mass,
                  excluded=dict(excluded), multi_example_readings=sum(len(v)>1 for v in eligible.values()),
                  inputs={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths},
                  sha256=hashlib.sha256(payload).hexdigest(),
                  policy='99% single-Han occurrence mass, all boundary ties; valid source-owned POJ/Han pairs only; one form per complete phonetic output.',
                  limitations='Written Taiwan Mandarin familiarity proxy; not Taiwanese spelling popularity, semantic disambiguation or community consensus. No beginner-example homophone joins.',
                  licences={'itaigi':'CC0-1.0','mcbopomofo':'MIT'})
    manifest = ROOT/'docs/candidate-annotations/pairs-manifest.json'
    encoded = json.dumps(report,ensure_ascii=False,indent=2)+'\n'
    if check:
        assert manifest.read_text(encoding='utf8') == encoded, 'Paired manifest differs from extraction'
        print('PASS reproducible paired forms and source manifest:', len(rows), 'pairs')
    else:
        manifest.write_bytes(encoded.encode('utf8'))
        print(encoded)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    build(parser.parse_args().check)
