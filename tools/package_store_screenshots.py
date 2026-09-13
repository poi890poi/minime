"""Package reviewed, unmodified phone screenshots; never redraw candidate pixels."""
from pathlib import Path
import hashlib, html, json, struct, zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'docs/play-publishing/screenshots-20260913'
SCENES = [
    ('01-chinese', '中文、英文，自然接話', '週末邀約與英文回覆；mingtian 的實際中文候選。'),
    ('02-english', '把日常寫成英文', '感謝、邀約與週末祝福；英文模式的 thank 補全候選。'),
    ('03-taiwanese', '台語白話字，也看得懂', '你好、多謝、再會、食飽未；lí hó 搭配漢字提示。'),
    ('04-japanese', '日本語，從日常開始', '早安、午安、謝謝與明天見；arigatou 的日語候選。'),
    ('05-geography', '記住山林裡的名字', '保留 jianianduan 原始示範，查看加年端社等 Rudy 地名。'),
    ('06-trails', '一段古道，更多地名', '展開 batongguan 候選，認識八通關周邊地名。'),
    ('07-taiwanese-choices', '同一個候選，兩種輸出', '點選輸出 lí hó，長按輸出你好；再輸入 tosia 查看多謝。'),
    ('08-japanese-choices', '漢字、平假名、片假名', '展開 kimochi 日語候選，查看實際可選的寫法。'),
]

records = json.loads((OUT / 'store-candidates.json').read_text(encoding='utf-8-sig'))
assert [r['screenshot'] for r in records] == [s[0] for s in SCENES]
for name, _, _ in SCENES:
    data = (OUT / (name + '.png')).read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    assert struct.unpack('>II', data[16:24]) == (1080, 1920)
    assert data[25] == 2, 'Screenshots must be opaque RGB PNG'

alt = {name + '.png': {'title': title, 'zh-TW': description} for name, title, description in SCENES}
(OUT / 'alt-text.json').write_text(json.dumps(alt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
cards = ''.join(f'<article><a href="{name}.png"><img src="{name}.png" alt="{html.escape(description)}" loading="lazy"></a><h2>{title}</h2><p>{description}</p></article>' for name, title, description in SCENES)
page = '''<!doctype html><html lang="zh-Hant"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>MinIME · 多語日常</title>
<style>*{box-sizing:border-box}body{margin:0;background:#f3f5ed;color:#163e3b;font-family:system-ui,"Microsoft JhengHei",sans-serif}header,main,footer{max-width:1440px;margin:auto;padding:28px}h1{font-size:36px;margin:12px 0}header p{max-width:850px;line-height:1.7}small{color:#006765;letter-spacing:.12em}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:24px;padding-top:0}article{background:white;border-radius:18px;padding:14px;box-shadow:0 8px 24px #153e3c0c}img{display:block;width:100%;height:auto;border-radius:8px}h2{font-size:19px;margin:18px 4px 8px}article p{font-size:14px;line-height:1.65;margin:4px;color:#51665f}a{color:#006765}footer{font-size:13px}</style>
<header><small>MinIME · 0.8.5-review1 · 2026.09.13</small><h1>多語日常，有更多話想說。</h1><p>八張手機實拍，從聊天、問候，到山林地名。點選圖片可查看完整 1080 × 1920 原圖。筆記區是預填的範例情境；鍵盤與候選詞均由實際 MinIME 產生，未修圖或重排。</p><p><a href="MinIME-Screenshots-20260913.zip">下載完整截圖 ZIP</a> · <a href="store-candidates.json">輸入與候選紀錄</a></p></header>
<main>''' + cards + '''</main><footer>這組圖片來自 0.8.5-review1，並非舊版 0.8.4 的發布核准。範例情境不代表詞庫覆蓋率，也不是 MinIME 內建筆記功能。</footer></html>'''
(OUT / 'index.html').write_text(page, encoding='utf-8', newline='\n')
readme = '''# MinIME — richer screenshot set

Eight unmodified 1080x1920 RGB phone screenshots, captured September 13, 2026
from the verified 0.8.5-review1 debug APK. Open index.html for the gallery.

The Notes area contains authored, prefilled example context. Only the recorded
typedInput was entered during each candidate demonstration. The keyboard and
candidate text are real app output; scene 07 also asserts real tap/hold outputs
and records those completed interactions. No candidate pixels were fabricated,
retouched or rearranged. The context is not a coverage benchmark or a claim
that MinIME contains a Notes application. store-candidates.json separates
prefilled text, input, mode, expansion state and observed labels.

The original jianianduan / 加年端社 check is retained. The capture harness requires
the requested place and checks every visible geography choice against matching
Rudy entries. Dictionary and ranking data were not changed for these examples.

These screenshots belong to 0.8.5-review1. The existing 0.8.4 store kit and signed
release binaries remain separate; this screenshot refresh does not approve a
Google Play release. Phone cleanup and capture evidence are in CAPTURE.md.

Order: bilingual conversation, English writing, Taiwanese with Han hints,
Japanese everyday phrases, 加年端社, 八通關 places, Taiwanese tap/hold output,
expanded Japanese choices. Each original PNG can be used independently.
'''
(OUT / 'README.md').write_text(readme, encoding='utf-8', newline='\n')
with (OUT / 'README.md').open('a', encoding='utf-8', newline='\n') as out:
    out.write('\n## Preview\n\n[Download all eight PNGs and the offline gallery](MinIME-Screenshots-20260913.zip)\n\n')
    for first in range(0, len(SCENES), 2):
        out.write('<p>\n')
        for name, title, description in SCENES[first:first+2]:
            out.write(f'<a href="{name}.png"><img src="{name}.png" width="280" alt="{html.escape(title + ": " + description)}"></a>\n')
        out.write('</p>\n\n')
names = [s[0] + '.png' for s in SCENES] + ['README.md', 'CAPTURE.md', 'alt-text.json', 'index.html', 'store-candidates.json']
manifest = {'app_version': '0.8.5-review1', 'app_sha256': '9a8e5fccbfe2306b9209b79241c734bc6f2dda7b8efec5aa26824121e2c20d54',
    'images': 'Unmodified phone captures; prefilled context disclosed separately',
    'files': {name: {'bytes': (OUT / name).stat().st_size, 'sha256': hashlib.sha256((OUT / name).read_bytes()).hexdigest()} for name in names}}
(OUT / 'MANIFEST.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
archive = OUT / 'MinIME-Screenshots-20260913.zip'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED) as z:
    for name in names + ['MANIFEST.json']:
        entry = zipfile.ZipInfo(name, date_time=(2026, 9, 13, 0, 0, 0))
        entry.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(entry, (OUT / name).read_bytes())
with zipfile.ZipFile(archive) as z: assert z.testzip() is None
print(archive, archive.stat().st_size, hashlib.sha256(archive.read_bytes()).hexdigest())
