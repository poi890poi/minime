"""Source-defined everyday vocabulary. No phrase/name lists or evaluation inputs."""
from pathlib import Path
import csv, json, re, subprocess, tarfile, unicodedata

ROOT = Path(__file__).resolve().parent.parent

def poj_letters(value):
    # A tone mark can sit between o and its combining dot in NFD order.
    value = unicodedata.normalize('NFD', value.lower()).replace('\u0358', 'o').replace('ⁿ', 'nn')
    value = ''.join(c for c in value if not unicodedata.combining(c))
    return value if re.fullmatch('[a-z -]+', value) else None

def short_poj(value):
    return bool(value) and len(re.split('[- ]+', value)) <= 6 and len(value) <= 64

def append_everyday(add, skipped):
    counts = {}
    path = ROOT/'third_party/taiwanese_basic/vocabulary.csv'
    entries = list(csv.DictReader(path.open(encoding='utf-8-sig')))
    counts['taiwanese_source_records'] = len(entries)
    accepted = set()
    for item in entries:
        source = 'taiwanese-basic:' + item['DictWordID']
        for suffix in ('', 'Others'):
            keys = item['PojInput' + suffix].split('/')
            outputs = item['PojUnicode' + suffix].split('/')
            if len(keys) != len(outputs):
                skipped.append([source, suffix, 'unaligned source variants']); continue
            for key, output in zip(keys, outputs):
                key = key.strip().lower(); output = unicodedata.normalize('NFC', output.strip())
                if not key: continue
                if not re.fullmatch('[a-z0-9 -]+', key) or not short_poj(key):
                    skipped.append([source, output, 'unsupported or long beginner headword']); continue
                for alias in (key, re.sub('[1-9]', '', key)):
                    add('poj', alias, output, source, 'everyday_vocabulary')
                accepted.add(output)
        # Complete short examples only. No splitting sentences into guessed phrases.
        for example in item['LekuPoj'].split('/'):
            output = unicodedata.normalize('NFC', example.strip().rstrip('.!?'))
            key = poj_letters(output)
            if not short_poj(key): continue
            add('poj', key, output, source + ':example', 'everyday_expressions')
            accepted.add(output)
    counts['taiwanese_beginner_outputs'] = len(accepted)

    with tarfile.open(ROOT/'third_party/jmdict/jmdict-eng-common.json.tgz') as archive:
        data = json.load(archive.extractfile(next(m for m in archive.getmembers() if m.name.endswith('.json'))))
    assert data['commonOnly'] is True
    selected = [item for item in data['words'] if any(set(s['partOfSpeech']) & {'exp', 'int'} for s in item['sense'])]
    counts['japanese_common_records'] = len(data['words'])
    counts['japanese_expression_records'] = len(selected)
    kana_values = sorted({k['text'] for item in selected for k in item['kana'] if k['common']})
    aliases = json.loads(subprocess.check_output(['node', str(ROOT/'tools/romanize_kana.cjs')], input=json.dumps(kana_values, ensure_ascii=False).encode('utf-8')).decode('utf-8'))
    accepted = set()
    for item in selected:
        source = 'jmdict:' + item['id']
        for kana in item['kana']:
            if not kana['common']: continue
            senses = [s for s in item['sense'] if set(s['partOfSpeech']) & {'exp', 'int'} and ('*' in s['appliesToKana'] or kana['text'] in s['appliesToKana'])]
            if not senses: continue
            roman = aliases[kana['text']]['romaji'].replace(' ', '').replace('・', '')
            key = aliases[kana['text']]['reading']
            if not re.fullmatch("[a-z']{2,64}", roman):
                skipped.append([source, kana['text'], 'unsupported everyday kana alias']); continue
            for output in [kana['text'], roman] + [k['text'] for k in item['kanji'] if k['common'] and ('*' in kana['appliesToKanji'] or k['text'] in kana['appliesToKanji']) and any('*' in s['appliesToKanji'] or k['text'] in s['appliesToKanji'] for s in senses)]:
                add('japanese', key, output, source, 'everyday_expressions'); accepted.add(output)
    counts['japanese_everyday_outputs'] = len(accepted)
    return counts
