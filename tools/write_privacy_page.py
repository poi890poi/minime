"""Generate public HTML from the exact offline policy bundled by Android."""
import argparse
import html
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    sections = (ROOT / 'app/src/main/assets/privacy.txt').read_text(encoding='utf-8').strip().split('\n\n')
    title, date = sections[0].split('\n', 1)
    body = '<h1>' + html.escape(title) + '</h1><p>' + html.escape(date) + '</p>\n'
    for i, section in enumerate(sections[1:]):
        if i and '\n' in section:
            heading, text = section.split('\n', 1)
            body += '<h2>' + html.escape(heading) + '</h2>\n'
        else:
            text = section
        body += '<p>' + html.escape(text).replace('\n', '<br>') + '</p>\n'
    content = ('<!doctype html>\n<html lang="en"><head><meta charset="utf-8">'
               '<meta name="viewport" content="width=device-width,initial-scale=1">'
               '<title>MinIME Privacy Policy</title><style>'
               'body{font:18px/1.65 system-ui,sans-serif;max-width:780px;margin:40px auto;'
               'padding:0 24px;color:#202b33}h1{font-size:2rem}h2{font-size:1.3rem;'
               'margin-top:2em}a{color:#005db3}</style></head><body><main>'
               + body + '</main></body></html>\n').encode('utf-8')
    target = ROOT / 'site/privacy.html'
    if args.check:
        if target.read_bytes() != content:
            raise SystemExit('Privacy HTML differs from the offline policy; regenerate it')
    else:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

if __name__ == '__main__':
    main()
