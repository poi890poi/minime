"""Oracle-user completion and lookup probes. Providers never see reference text."""
from pathlib import Path
import argparse, base64, collections, ctypes, gzip, json, math, statistics, subprocess, sys, time
from ctypes import wintypes
from make_corpus import HERE, ROOT, WORK, order, dump, write_rows
import jaconv

JAVA = r'C:\Program Files\Microsoft\jdk-17.0.11.9-hotspot\bin\java.exe'

def rows(name):
    return [json.loads(s) for s in gzip.open(HERE / name, 'rt', encoding='utf8')]

def b64(s): return base64.b64encode(s.encode()).decode()
def un64(s): return base64.b64decode(s).decode()

class MemoryCounters(ctypes.Structure):
    _fields_ = [('cb', wintypes.DWORD), ('faults', wintypes.DWORD)] + [(s, ctypes.c_size_t) for s in
        ['peak_rss', 'rss', 'peak_paged', 'paged', 'peak_nonpaged', 'nonpaged', 'pagefile', 'peak_pagefile', 'private']]

def memory(pid):
    kernel = ctypes.WinDLL('kernel32', use_last_error=True)
    kernel.OpenProcess.restype = wintypes.HANDLE
    kernel.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
    kernel.CloseHandle.argtypes = [wintypes.HANDLE]
    psapi = ctypes.WinDLL('psapi', use_last_error=True)
    psapi.GetProcessMemoryInfo.argtypes = [wintypes.HANDLE, ctypes.POINTER(MemoryCounters), wintypes.DWORD]
    h = kernel.OpenProcess(0x410, False, pid)
    if not h: return {'unavailable': ctypes.get_last_error()}
    try:
        v = MemoryCounters(); v.cb = ctypes.sizeof(v)
        if not psapi.GetProcessMemoryInfo(h, ctypes.byref(v), v.cb): return {'unavailable': ctypes.get_last_error()}
        return {k: getattr(v, k) for k in ['rss', 'peak_rss', 'private']}
    finally: kernel.CloseHandle(h)

class Engine:
    def __init__(self, name):
        self.name = name
        executable = 'converter-server'+name.removeprefix('kazuma')+'.exe' if name.startswith('kazuma') else 'converter-server.exe'
        command = [JAVA, '-Dfile.encoding=UTF-8', '-Xmx1g', '-cp', 'core/build/manual', 'dev.minime.core.MinimeServer'] if name == 'minime' else [str(WORK / 'build' / executable), str(WORK / 'build')]
        at = time.perf_counter_ns()
        self.log = (WORK / (name + '-stderr.txt')).open('w', encoding='utf8')
        self.proc = subprocess.Popen(command, cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                     stderr=self.log, text=True, encoding='utf8', bufsize=1)
        line = self.proc.stdout.readline().strip().split('\t')
        assert line[0] == 'READY', line
        self.load = {'engine_ns': int(line[1]), 'process_start_to_ready_ns': time.perf_counter_ns() - at,
                     'loaded_memory': memory(self.proc.pid)}
        self.raw = ''; self.text = ''; self.candidates = []; self.preferred = 0
        self.times = []; self.wall_ns = 0; self.engine_ns = 0

    def close(self):
        self.load['final_memory'] = memory(self.proc.pid)
        self.proc.stdin.close()
        try: self.proc.wait(timeout=20)
        except subprocess.TimeoutExpired: self.proc.kill(); self.proc.wait(); raise
        self.log.close()
        assert self.proc.returncode == 0, self.proc.returncode

    def send(self, command, value=''):
        at = time.perf_counter_ns(); self.times = []
        if self.name == 'minime':
            value = b64(value) if command in ['T', 'L'] else str(value)
            self.proc.stdin.write(command + '\t' + value + '\n'); self.proc.stdin.flush()
            p = self.proc.stdout.readline().rstrip('\r\n').split('\t')
            assert p[0] == 'OK', p
            self.engine_ns = int(p[1]); self.raw = un64(p[2]); self.text = un64(p[3]); self.preferred = int(p[4])
            self.times = [int(x) for x in p[5].split(',') if x]
            self.candidates = [(un64(x.rsplit(':', 1)[0]), int(x.rsplit(':', 1)[1])) for x in p[6:]]
        else:
            self.engine_ns = 0
            if command == 'R': self.raw = ''; self.text = ''; self.candidates = []
            elif command == 'S':
                self.text += self.candidates[int(value)][0]; self.raw = ''; self.candidates = []
            elif command == 'L':
                assert not self.raw; self.text += value; self.candidates = []
            else:
                if command == 'T': self.raw += value
                elif command == 'B': self.raw = self.raw[:-int(value)] if int(value) else self.raw
                elif command != 'V': raise ValueError(command)
                if self.raw:
                    # The reference reading is never substituted for actual typed input.
                    kana = jaconv.alphabet2kana(self.raw)
                    assert '\n' not in kana and '\t' not in kana
                    self.proc.stdin.write(kana + '\n'); self.proc.stdin.flush()
                    p = self.proc.stdout.readline().rstrip('\r\n').split('\t')
                    self.engine_ns = int(p[0]); self.candidates = [(s, 0) for s in p[1:]]
                    self.times = [self.engine_ns]
                else: self.candidates = []
        self.wall_ns = time.perf_counter_ns() - at
        return self

def rank(e, targets):
    return next((i for i, (text, consumed) in enumerate(e.candidates[:8]) if text in targets and consumed == 0), -1)

def accept(e, index):
    before = e.text; target = e.candidates[index][0]
    e.send('S', index)
    return e.text == before + target and e.raw == ''

def attempt(e, raw, targets, counts, events, identity):
    before = e.text
    e.send('T', raw); counts['typed'] += len(raw)
    k = rank(e, targets)
    event = {'id': identity, 'raw': raw, 'rank': k, 'candidates': e.candidates[:8],
             'engine_ns': e.engine_ns, 'wall_ns': e.wall_ns}
    if k >= 0:
        counts['selections'] += 1
        ok = accept(e, k); event['accepted_ok'] = ok
        events.append(event)
        if not ok: counts['acceptance_failure'] += 1
        return ok
    counts['deletes'] += len(e.raw)
    e.send('B', len(e.raw))
    assert e.raw == '' and e.text == before, 'Cancellation changed committed output'
    events.append(event)
    return False

def complete(e, utterance, policy):
    e.send('R'); counts = collections.Counter(); events = []; successful = True
    for i, part in enumerate(utterance['parts']):
        identity = utterance['id'] + ':' + str(i)
        if part['kind'] == 'literal':
            # Literals are source separators/non-Japanese keys, never a Kanji rescue.
            e.send('L', part['text']); counts['literal_keys'] += len(part['text']); continue
        if not part['valid']:
            counts['unreadable_clause'] += 1; successful = False; continue
        if policy == 'clause-first' and attempt(e, part['raw'], [part['text']], counts, events, identity):
            counts['clause_success'] += 1; continue
        if policy == 'clause-first': counts['clause_retry'] += 1
        for j, word in enumerate(part['words']):
            if not attempt(e, word['raw'], [word['text']], counts, events, identity + '/' + str(j)):
                successful = False; counts['missing_word'] += 1
    counts['actions'] = counts['typed'] + counts['literal_keys'] + counts['deletes'] + counts['selections']
    counts['output_chars'] = len(utterance['text'])
    return {'id': utterance['id'], 'source': utterance['source'], 'role': utterance['role'], 'policy': policy,
            'previously_seen_text': utterance['previously_seen_text'], 'cross_role_repeat': utterance['cross_role_repeat'],
            'success': successful and e.text == utterance['text'], 'counts': dict(counts), 'events': events,
            'committed': e.text}

def variants(raw, identity):
    at = int(order(identity)[:8], 16) % max(1, len(raw) - 1)
    keyboard = ['qwertyuiop', 'asdfghjkl', 'zxcvbnm']
    neighbors = {c: r[i + 1] if i + 1 < len(r) else r[i - 1] for r in keyboard for i, c in enumerate(r)}
    return [('full', raw), ('half', raw[:max(1, len(raw)//2)]), ('three-quarter', raw[:max(1, len(raw)*3//4)]),
            ('omission', raw[:at] + raw[at+1:]),
            ('neighbor', raw[:at] + neighbors.get(raw[at], raw[at]) + raw[at+1:]),
            ('transpose', raw[:at] + raw[at:at+2][::-1] + raw[at+2:])]

def probe(e, u, i, part, condition, raw):
    e.send('R'); counts = collections.Counter(); events = []
    ok = attempt(e, raw, [part['text']], counts, events, u['id'] + ':' + str(i)) if raw else False
    initial_hit = ok
    if not ok and condition in ['omission', 'neighbor', 'transpose']:
        ok = attempt(e, part['raw'], [part['text']], counts, events, 'corrected-retry')
    return {'id': u['id'] + ':' + str(i), 'source': u['source'], 'role': u['role'],
            'reference': part['reference'], 'condition': condition, 'changed': raw != part['raw'],
            'initial_hit': initial_hit, 'recovered': ok, 'counts': dict(counts), 'events': events}

def distance(a, b):
    prev = list(range(len(b) + 1))
    for i, x in enumerate(a, 1):
        cur = [i]
        for j, y in enumerate(b, 1): cur.append(min(cur[-1]+1, prev[j]+1, prev[j-1]+(x != y)))
        prev = cur
    return prev[-1]

def main():
    p = argparse.ArgumentParser(); p.add_argument('engine', choices=['minime', 'kazuma', 'kazuma-indexed', 'kazuma-stable', 'kazuma-indexed-stable', 'kazuma-portable'])
    p.add_argument('command', choices=['completion', 'probes', 'ajimee', 'perf'])
    p.add_argument('--role', default='development', choices=['development', 'holdout']); p.add_argument('--pass-number', type=int, default=1)
    p.add_argument('--output-dir',type=Path,default=HERE)
    args = p.parse_args(); output = []; corpus = rows('utterances.jsonl.gz')
    e = Engine(args.engine)
    try:
        if args.command == 'completion':
            for u in corpus:
                if u['role'] != args.role: continue
                for policy in ['clause-first', 'word-first']: output.append(complete(e, u, policy))
        elif args.command == 'probes':
            for u in corpus:
                if u['role'] != args.role: continue
                for i, part in enumerate(u['parts']):
                    if part['kind'] != 'japanese' or not part['valid']: continue
                    for condition, raw in variants(part['raw'], u['id']+':'+str(i)):
                        output.append(probe(e, u, i, part, condition, raw))
        elif args.command == 'ajimee':
            for item in rows('ajimee.jsonl.gz'):
                # Same no-left-context condition for both engines. Never use gold context.
                kana = jaconv.kata2hira(item['input']); raw = jaconv.kana2alphabet(kana)
                e.send('R'); e.send('T', raw)
                choices = e.candidates[:8]; k = rank(e, item['expected_output'])
                top = choices[e.preferred][0] if choices and e.preferred < len(choices) else ''
                output.append({'id': item['index'], 'raw': raw, 'context_supplied': False, 'reference_has_context': bool(item['context_text']),
                               'rank': k, 'preferred': top, 'candidates': choices,
                               'min_cer': min(distance(x, top)/len(x) for x in item['expected_output']),
                               'default_hit': top in item['expected_output'], 'raw_over_96': len(raw) > 96})
        else:
            selected = [(u['id']+':'+str(i), part) for u in corpus if u['role'] == 'development'
                        for i, part in enumerate(u['parts']) if part['kind']=='japanese' and part['valid'] and len(part['raw']) <= 48]
            selected = sorted(selected, key=lambda x: order(x[0]))[:64]
            for cycle in ['first', 'repeat']:
                for identity, part in selected:
                    e.send('R')
                    for key, char in enumerate(part['raw']):
                        e.send('T', char)
                        output.append({'id': identity, 'cycle': cycle, 'key': key, 'raw': e.raw,
                                       'engine_ns': e.times[0], 'wall_ns': e.wall_ns, 'count': len(e.candidates)})
    finally: e.close()
    suffix = str(args.pass_number) if args.command == 'perf' else args.role
    name = args.engine + '-' + args.command + '-' + suffix
    args.output_dir.mkdir(parents=True,exist_ok=True)
    write_rows(args.output_dir / (name+'.jsonl.gz'), output); dump(args.output_dir / (name+'-load.json'), e.load)
    print(name, len(output), flush=True)

if __name__ == '__main__': main()
