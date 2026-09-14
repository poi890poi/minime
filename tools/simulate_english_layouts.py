"""Frozen, offline layout/noise sensitivity experiment; never imported by the IME."""
import os
os.environ.setdefault('OPENBLAS_NUM_THREADS', '1')
import argparse
import collections
import gzip
import hashlib
import json
from pathlib import Path
import re
import time
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
ROWS = {
    'qwerty': ['qwertyuiop', 'asdfghjkl', 'zxcvbnm'],
    'colemak': ['qwfpgjluy', 'arstdhneio', 'zxcvbkm'],
    'dvorak': ['pyfgcrl', 'aoeuidhtns', 'qjkxbmwvz'],
    'split-qwerty': ['qwertyuiop', 'asdfghjkl', 'zxcvbnm'],
}
PROFILES = ['center', 'scatter-1.5', 'scatter-2.5', 'thumb-bias', 'slips']
SEEDS = [20260914, 20260915, 20260916]
METHODS = ['literal', 'spatial', 'spatial-frequency', 'conservative']
FIELDS = ['words', 'characters', 'raw_key_errors', 'raw_word_errors',
          'raw_valid_word_errors', 'oov_words', 'word_errors',
          'damaged_correct_words', 'recovered_errors', 'recovered_valid_word_errors',
          'unavailable_words']


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def geometry(name, width):
    centers = np.zeros((26, 2))
    assert sorted(''.join(ROWS[name])) == list('abcdefghijklmnopqrstuvwxyz')
    for r, row in enumerate(ROWS[name]):
        for c, letter in enumerate(row):
            x = (c + .5 + (10 - len(row)) / 2) * width / 10
            if name == 'split-qwerty':
                x = x * (width - 8) / width + (8 if x >= width / 2 else 0)
            centers[ord(letter) - 97] = [x, (r + .5) * 10]
    return centers


def contacts(intended_centers, width, profile, seed):
    # Matched independent draws. Bias depends on physical side, not word identity.
    rng = np.random.default_rng(seed)
    z = rng.standard_normal(intended_centers.shape)
    large = rng.random(intended_centers.shape[:-1]) < .1
    sigma = 0 if profile == 'center' else 2.5 if profile == 'scatter-2.5' else 1.5
    if profile == 'slips':
        delta = z * np.where(large, 4., 1.5)[..., None]
    else:
        delta = z * sigma
    if profile == 'thumb-bias':
        delta[..., 0] += np.where(intended_centers[..., 0] < width / 2, 1., -1.)
        delta[..., 1] += .5
    return intended_centers + delta


def word_ids(words):
    return np.array([[ord(c) - 97 for c in w] for w in words], dtype=np.int16)


def decode(points, centers, words, frequencies, ids):
    """No expected strings, prompt, genre, noise-profile or thumb labels here."""
    raw_ids = ((points[:, :, None, :] - centers[None, None, :, :]) ** 2).sum(3).argmin(2)
    raw = np.array([''.join(chr(int(c) + 97) for c in row) for row in raw_ids])
    literal_cost = ((points - centers[raw_ids]) ** 2).sum((1, 2)) / 8
    if not len(words):
        return raw_ids, [raw] * 4, np.ones(len(points), dtype=bool)
    templates = centers[ids].reshape(len(words), -1)
    norms = (templates ** 2).sum(1)
    vocab = set(words)
    spatial, weighted, cautious = [], [], []
    for start in range(0, len(points), 128):
        end = start + 128
        flat = points[start:end].reshape(len(points[start:end]), -1)
        distances = np.maximum(0, (flat ** 2).sum(1)[:, None] + norms - 2 * flat @ templates.T)
        spatial.extend(words[distances.argmin(1)])
        scores = distances / 8 - frequencies[None, :] / 64
        best = scores.argmin(1)
        best_scores = scores[np.arange(len(best)), best]
        second = np.partition(scores, 1, axis=1)[:, 1] if len(words) > 1 else np.full(len(best), np.inf)
        proposed = words[best]
        weighted.extend(proposed)
        take = np.array([w not in vocab for w in raw[start:end]]) & (second - best_scores >= 2) & (literal_cost[start:end] - best_scores >= 1)
        cautious.extend(np.where(take, proposed, raw[start:end]))
    return raw_ids, [raw, np.array(spatial), np.array(weighted), np.array(cautious)], np.zeros(len(points), dtype=bool)


def self_test():
    centers = geometry('qwerty', 60)
    words = np.array(['cat', 'cot', 'cut', 'dog'])
    ids = word_ids(words)
    pts = centers[ids]
    raw, result, missing = decode(pts, centers, words, np.zeros(4), ids)
    assert np.array_equal(result[0], words) and np.array_equal(result[1], words)
    assert not missing.any()
    noisy = contacts(pts, 60, 'slips', 123)
    assert np.array_equal(noisy, contacts(pts, 60, 'slips', 123))
    _, result, _ = decode(noisy, centers, words, np.zeros(4), ids)
    brute = [min(words, key=lambda w: sum(float(np.dot(p-centers[ord(c)-97], p-centers[ord(c)-97])) for p, c in zip(taps, w))) for taps in noisy]
    assert list(result[1]) == brute
    unknown = np.array(['zzz'])
    _, result, _ = decode(centers[word_ids(unknown)], centers, words, np.zeros(4), ids)
    assert result[0][0] == 'zzz' and result[1][0] in words and result[3][0] == 'zzz'
    for name in ROWS:
        g = geometry(name, 60)
        assert len(np.unique(g, axis=0)) == 26
        assert np.all((g[:, 0] > 0) & (g[:, 0] < 60))
    print('PASS clean identity, matched RNG, exhaustive brute-force parity, OOV literal retention, layout identity', flush=True)


def load_inputs():
    vocab = {}
    lex = ROOT / 'app/src/main/assets/en_us.tsv'
    corpus = ROOT / 'docs/conversation-ranking/corpus/gum-test.tsv'
    for line in lex.read_text(encoding='utf-8').splitlines():
        w, score = line.split('\t')[:2]
        w = w.lower()
        if re.fullmatch('[a-z]{1,20}', w):
            vocab[w] = max(vocab.get(w, 0), float(score))
    documents, items, excluded = {}, [], collections.Counter()
    for line in corpus.read_text(encoding='utf-8').splitlines():
        genre, sentence, text, _ = line.split('\t')
        doc = sentence.rsplit('-', 1)[0]
        if doc not in documents:
            documents[doc] = {'id': doc, 'genre': genre, 'index': len(documents)}
        assert documents[doc]['genre'] == genre
        for token in text.split():
            if re.fullmatch('[a-z]{1,20}', token):
                items.append((token, documents[doc]['index']))
            else:
                excluded[genre] += 1
    return vocab, list(documents.values()), items, dict(excluded), {str(p.relative_to(ROOT)): sha(p) for p in (lex, corpus)}


def run(out):
    if out.exists():
        raise SystemExit('Use a new output directory; preserve previous results.')
    out.mkdir(parents=True)
    self_test()
    vocab, docs, items, excluded, hashes = load_inputs()
    inputs = collections.defaultdict(list)
    for w, doc in items:
        inputs[len(w)].append((w, doc))
    lex = {}
    for length in inputs:
        words = np.array(sorted(w for w in vocab if len(w) == length))
        lex[length] = (words, np.array([vocab[w] for w in words]), word_ids(words))
    manifest = {'code_sha256': sha(Path(__file__)), 'plan_sha256': sha(ROOT/'docs/two-thumb-english/simulation/PLAN.md'),
                'input_sha256': hashes, 'numpy': np.__version__, 'rows': ROWS, 'widths_mm': [60, 70],
                'profiles': PROFILES, 'seeds': SEEDS, 'documents': docs, 'eligible_tokens': len(items),
                'lexicon_words': len(vocab), 'oov_tokens': sum(w not in vocab for w, _ in items),
                'excluded_tokens_by_genre': excluded, 'role': 'previously evaluated regression; synthetic imprecision',
                'fields': FIELDS, 'runtime': 'desktop Python experiment; not Android latency'}
    (out/'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8')
    started = time.perf_counter()
    aggregate, movement = [], []
    with gzip.open(out/'documents.jsonl.gz', 'wt', encoding='utf-8') as rawout:
        for width in (60, 70):
            for name in ROWS:
                centers = geometry(name, width)
                # Movement of each thumb between its own consecutive letters, within a word.
                move = collections.defaultdict(lambda: [0., 0, 0, 0])
                for word, docid in items:
                    genre = docs[docid]['genre']; last = {}; previous = None
                    for c in word:
                        at = centers[ord(c)-97]; side = int(at[0] >= width/2)
                        if side in last:
                            move[genre][0] += float(np.linalg.norm(at-last[side])); move[genre][1] += 1
                        if previous is not None:
                            move[genre][2] += int(side != previous); move[genre][3] += 1
                        last[side] = at; previous = side
                for genre, values in move.items():
                    movement.append({'layout': name, 'width_mm': width, 'genre': genre, 'same_thumb_travel_mm': values[0],
                                     'same_thumb_moves': values[1], 'alternations': values[2], 'transitions': values[3]})
                for profile in PROFILES:
                    for seed in SEEDS:
                        counts = np.zeros((4, len(docs), len(FIELDS)), dtype=np.int64)
                        digest = hashlib.sha256()
                        for length, records in sorted(inputs.items()):
                            expected = np.array([r[0] for r in records]); docids = np.array([r[1] for r in records])
                            ids = word_ids(expected)
                            points = contacts(centers[ids], width, profile, seed + length * 1000)
                            rawids, predictions, missing = decode(points, centers, *lex[length])
                            rawwrong = predictions[0] != expected
                            rawvalidwrong = rawwrong & np.array([w in vocab for w in predictions[0]])
                            oov = np.array([w not in vocab for w in expected])
                            for m, prediction in enumerate(predictions):
                                digest.update(('\n'.join(prediction)+'\n').encode())
                                wrong = prediction != expected
                                values = [np.ones(len(records)), np.full(len(records), length), (rawids != ids).sum(1),
                                          rawwrong, rawvalidwrong, oov, wrong, ~rawwrong & wrong, rawwrong & ~wrong,
                                          rawvalidwrong & ~wrong, missing]
                                for f, value in enumerate(values):
                                    counts[m, :, f] += np.bincount(docids, weights=value, minlength=len(docs)).astype(np.int64)
                        for m, method in enumerate(METHODS):
                            groups = collections.defaultdict(lambda: np.zeros(len(FIELDS), dtype=np.int64))
                            for d, doc in enumerate(docs):
                                row = {'layout': name, 'width_mm': width, 'profile': profile, 'seed': seed, 'method': method,
                                       'document': doc['id'], 'genre': doc['genre'], 'counts': counts[m, d].tolist()}
                                rawout.write(json.dumps(row)+'\n')
                                groups[doc['genre']] += counts[m, d]
                            for genre, count in groups.items():
                                aggregate.append({'layout': name, 'width_mm': width, 'profile': profile, 'seed': seed,
                                                  'method': method, 'genre': genre, **dict(zip(FIELDS, map(int, count)))})
                        if profile == 'center':
                            assert counts[0, :, FIELDS.index('raw_key_errors')].sum() == 0
                            assert counts[0, :, FIELDS.index('word_errors')].sum() == 0
                        print(f'{name} {width}mm {profile} seed={seed} literal={counts[0,:,6].sum()}/{len(items)} spatial-frequency={counts[2,:,6].sum()}/{len(items)} digest={digest.hexdigest()[:12]}', flush=True)
    (out/'summary.json').write_text(json.dumps({'results': aggregate, 'movement': movement,
                                              'elapsed_seconds': time.perf_counter()-started}, indent=2)+'\n', encoding='utf-8')
    print('DONE', out, 'seconds', round(time.perf_counter()-started, 2), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', type=Path)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        self_test()
    elif args.out:
        run(args.out)
    else:
        parser.error('Choose --self-test or --out.')
