"""Evaluation-only AOSP decoder bridge; OpenCC conversion is recorded separately."""
import ctypes as c
import json
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / '.tools/opencc-python'))
from opencc import OpenCC

lib = c.CDLL(str(ROOT / '.tools/aosp-pinyin/pinyin.dll'))
lib.im_open_decoder.argtypes = [c.c_char_p, c.c_char_p]
lib.im_open_decoder.restype = c.c_bool
lib.im_search.argtypes = [c.c_char_p, c.c_size_t]
lib.im_search.restype = c.c_size_t
lib.im_get_candidate.argtypes = [c.c_size_t, c.POINTER(c.c_uint16), c.c_size_t]
lib.im_get_candidate.restype = c.POINTER(c.c_uint16)
lib.im_get_sps_str.argtypes = [c.POINTER(c.c_size_t)]
lib.im_get_sps_str.restype = c.c_char_p
lib.im_enable_shm_as_szm.argtypes = [c.c_bool]
lib.im_enable_ym_as_szm.argtypes = [c.c_bool]
convert = OpenCC('s2twp')
if not lib.im_open_decoder(str(ROOT / '.tools/aosp-pinyin/res/raw/dict_pinyin.dat').encode(),
                           str(ROOT / '.tools/aosp-pinyin/evaluation-user.dat').encode()):
    raise RuntimeError('AOSP dictionary failed to load')
lib.im_enable_shm_as_szm(True)
lib.im_enable_ym_as_szm(True)
results = []
try:
    for line in Path(sys.argv[1]).read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        case, context, raw, expected = line.split('\t')
        lib.im_reset_search()
        start = time.perf_counter()
        count = lib.im_search(raw.encode(), len(raw))
        us = int((time.perf_counter() - start) * 1e6)
        decoded = c.c_size_t()
        lib.im_get_sps_str(c.byref(decoded))
        words = []
        for i in range(min(count, 100)):
            buffer = (c.c_uint16 * 128)()
            if lib.im_get_candidate(i, buffer, 128):
                units = list(buffer)
                text = bytes(buffer)[:units.index(0)*2].decode('utf-16le')
                words.append(text)
        traditional = [convert.convert(w) for w in words]
        rank = traditional.index(expected)+1 if expected in traditional else 0
        results.append(dict(case=case, input=raw, expected=expected, decoded=decoded.value,
                            rank=rank, microseconds=us, simplified=words[:5], traditional=traditional[:5]))
finally:
    lib.im_close_decoder()
report = dict(top1=sum(r['rank']==1 for r in results),
              top5=sum(0<r['rank']<=5 for r in results), count=len(results), results=results)
Path(sys.argv[2]).write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print({k:v for k,v in report.items() if k!='results'})
