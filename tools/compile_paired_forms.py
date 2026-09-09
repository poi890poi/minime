"""Build source-attested Taiwanese output pairs; never import phonetic entries."""
from pathlib import Path
from collections import Counter, defaultdict
import argparse, csv, hashlib, json, re, unicodedata
from sources import require_sources

ROOT = Path(__file__).resolve().parent.parent

def build(check=False):
    require_sources('itaigi', 'mcbopomofo', 'taihoa', 'taiwanese-basic')
    paths = ['third_party/itaigi/itaigi.csv', 'third_party/mcbopomofo/phrase.occ', 'app/src/main/assets/addons.tsv', 'third_party/taihoa/taihoa.csv', 'third_party/taiwanese_basic/vocabulary.csv']
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
    inventory = [p for line in (ROOT / paths[2]).read_text(encoding='utf8').splitlines()
                 for p in [line.split('\t')] if len(p) == 5 and p[0] == 'poj']
    shipped = {(p[3], p[2]) for p in inventory if p[3].startswith('itaigi:')}
    outputs = {p[2] for p in inventory}
    basic = {p[2] for p in inventory if p[3].startswith('taiwanese-basic:') and p[4] == 'everyday_vocabulary'}
    eligible = defaultdict(list); excluded = Counter()
    for dataset, path in [('itaigi', paths[0]), ('taihoa', paths[3])]:
        with (ROOT / path).open(encoding='utf-8-sig') as stream:
            for entry in csv.DictReader(stream):
                source = dataset + ':' + entry['DictWordID']
                phonetic = unicodedata.normalize('NFC', entry['PojUnicode'].strip())
                if phonetic not in outputs:
                    continue
                han = unicodedata.normalize('NFC', entry['HanLoTaibunPoj'].strip())
                if not re.fullmatch('[\u3400-\u9fff]{1,12}', han):
                    excluded[dataset + ':not_single_all_Han_form'] += 1
                    continue
                familiar_form = all(c in familiar for c in han)
                if not familiar_form and phonetic not in basic:
                    excluded[dataset + ':outside_familiar_and_basic_vocabulary'] += 1
                    continue
                # Reproduce the previous source policy as a tier, not a pinned word list.
                legacy = dataset == 'itaigi' and (source, phonetic) in shipped and familiar_form
                eligible[phonetic].append((han, source, legacy, familiar_form))
    rows = []; restored_basic = 0; preserved = 0
    for phonetic, choices in sorted(eligible.items()):
        han, source, legacy, familiar_form = min(choices, key=lambda p: (
            not p[2], -min(single.get(c, 0) for c in p[0]), -occurrence.get(p[0], 0),
            p[1].split(':')[0], int(p[1].split(':')[1])))
        rows.append(('poj', phonetic, han, source))
        preserved += legacy
        restored_basic += not familiar_form
    payload = ''.join('\t'.join(row) + '\n' for row in rows).encode('utf8')
    output = ROOT / 'app/src/main/assets/paired-forms.tsv'
    if check:
        assert output.read_bytes() == payload, "Paired forms differ from deterministic extraction"
    else:
        output.write_bytes(payload)
    report = dict(format=2, pairs=len(rows), existing_poj_outputs=len(outputs), beginner_headword_outputs=len(basic),
                  legacy_pairs_preserved=preserved, basic_pairs_outside_mandarin_familiarity=restored_basic,
                  pairs_by_source=dict(Counter(row[3].split(':')[0] for row in rows)), existing_itaigi_outputs=len({p[1] for p in shipped}),
                  familiar_characters=len(familiar), minimum_character_occurrence=boundary,
                  familiar_occurrence_fraction=sum(single[c] for c in familiar)/mass,
                  excluded=dict(excluded), multi_example_readings=sum(len(v)>1 for v in eligible.values()),
                  inputs={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths},
                  sha256=hashlib.sha256(payload).hexdigest(),
                  policy='Source-owned iTaigi/Taihoa pairs for existing POJ outputs. Preserve original iTaigi tier; permit authored beginner headwords despite Mandarin frequency cutoff; otherwise retain 99% single-Han mass policy. One canonical form per complete phonetic output.',
                  limitations='Mandarin occurrence is a familiarity preference, not Taiwanese spelling popularity or consensus. Basic headword membership does not establish spelling agreement. Canonical source example may represent a different sense of the same complete reading; no fragment joins or guessed alternate-reading pairings.',
                  licences={'itaigi':'CC0-1.0','mcbopomofo':'MIT','taihoa':'CC-BY-SA-4.0','taiwanese-basic':'CC-BY-SA-4.0'})
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
