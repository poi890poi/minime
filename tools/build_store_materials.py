"""Build the MinIME store kit from authored copy and the existing vector identity.
Requires Pillow; fonts use Windows Segoe UI and Microsoft JhengHei. PNGs are new
raster exports of original geometry, not alterations to captured UI screenshots.
"""
from pathlib import Path
import html,json,hashlib,zipfile,shutil
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parent.parent
KIT=ROOT/'docs/play-publishing/kit'
G=KIT/'graphics';G.mkdir(parents=True,exist_ok=True)
TEAL='#006765';INK='#153e3c';CREAM='#f3f5ed'
FONT=Path('C:/Windows/Fonts')
def font(size,bold=False,cjk=False):return ImageFont.truetype(str(FONT/('msjhbd.ttc' if bold and cjk else 'msjh.ttc' if cjk else 'segoeuib.ttf' if bold else 'segoeui.ttf')),size)
# Shared approved artwork. Store output stays square; Play supplies its mask.
from icon_art import render as render_icon, svg as icon_svg, svg_paths
render_icon(512,'square').save(G/'icon-512.png')
(G/'icon.svg').write_text(icon_svg('square'),encoding='utf-8')
for locale,tag in [('en-US','Your words. Your keyboard.'),('zh-TW','自己的話，自己的鍵盤。')]:
 im=Image.new('RGB',(1024,500),CREAM);d=ImageDraw.Draw(im)
 d.ellipse((900,-140,1280,240),fill='#d7e7df');d.ellipse((-180,395,160,735),fill='#d7e7df')
 d.text((88,115),'MinIME',font=font(76,True),fill=TEAL)
 d.text((92,229),tag,font=font(30,cjk=locale=='zh-TW'),fill=INK)
 d.text((92,319),'中文 · English · 台語 · 日本語',font=font(24,cjk=True),fill=TEAL)
 svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500" viewBox="0 0 1024 500">',f'<rect width="1024" height="500" fill="{CREAM}"/>','<circle cx="1090" cy="50" r="190" fill="#d7e7df"/><circle cx="-10" cy="565" r="170" fill="#d7e7df"/>',f'<text x="88" y="190" font-family="Segoe UI" font-weight="700" font-size="76" fill="{TEAL}">MinIME</text>',f'<text x="92" y="265" font-family="Segoe UI,Microsoft JhengHei" font-size="30" fill="{INK}">{html.escape(tag)}</text>',f'<text x="92" y="347" font-family="Microsoft JhengHei" font-size="24" fill="{TEAL}">中文 · English · 台語 · 日本語</text>']
 mark=render_icon(310,None);im.paste(mark,(651,86),mark)
 svg.append('<g transform="translate(651 86) scale(6.4583333333)">'+svg_paths()+'</g>')
 im.save(G/('feature-'+locale+'.png'));(G/('feature-'+locale+'.svg')).write_text(''.join(svg)+'</svg>\n',encoding='utf-8')
# Copy-ready listing fields, no Markdown headings or developer placeholders.
source=(ROOT/'docs/play-publishing/STORE-LISTING.md').read_text(encoding='utf-8')
for locale,prefix,end in [('en-US','English','Traditional Chinese short description:'),('zh-TW','Traditional Chinese','## Console fields')]:
 out=KIT/'listing'/locale;out.mkdir(parents=True,exist_ok=True)
 short=source.split(prefix+' short description:\n',1)[1].split('\n\n',1)[0].strip().rstrip('。.')
 full=source.split(prefix+' full description:\n',1)[1].split(end,1)[0].strip()
 full='\n\n'.join(''.join(p.splitlines()) if locale=='zh-TW' else ' '.join(p.splitlines()) for p in full.split('\n\n'))
 fields={'title':'MinIME','short-description':short,'full-description':full,'release-notes':('Offline input with focused language modes, local learning, source notices, and optional settings and dictionary transfer.' if locale=='en-US' else '離線輸入、主要語言模式、本機學習、來源授權聲明，以及自選設定與詞典匯出匯入。')}
 for name,text in fields.items():
  assert len(text)<={'title':30,'short-description':80,'full-description':4000,'release-notes':500}[name]
  (out/(name+'.txt')).write_text(text+'\n',encoding='utf-8')
for name,src in [('privacy/privacy.html','site/privacy.html'),('privacy/privacy.txt','app/src/main/assets/privacy.txt'),('review/STORE-LISTING.md','docs/play-publishing/STORE-LISTING.md'),('review/LICENSING.md','LICENSING.md'),('review/LICENSE','LICENSE'),('review/NOTICE.txt','app/src/main/assets/NOTICE.txt')]:
 dest=KIT/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/src,dest)
print('Generated original graphics and listing fields')

# Validate the complete, allowlisted kit and package only public materials.
for p in G.glob('*.png'):
    with Image.open(p) as image:
        if p.name=='icon-512.png':assert image.size==(512,512) and image.mode=='RGBA' and p.stat().st_size<=1024*1024
        else:assert image.size==(1024,500) and image.mode=='RGB'
shots=sorted((KIT/'screenshots').glob('*.png'))
assert len(shots)==4,'Four reviewed real screenshots are required'
for p in shots:
    with Image.open(p) as image:assert image.size==(1080,1920) and image.mode=='RGB'
alt=json.loads((KIT/'alt-text.json').read_text(encoding='utf-8-sig'))
assert all(len(t)<=140 for row in alt.values() for t in row.values())
expected={'README.md','alt-text.json','privacy/privacy.html','privacy/privacy.txt',
          'review/LICENSE','review/LICENSING.md','review/NOTICE.txt',
          'review/RELEASE-CHECKLIST.md','review/STORE-LISTING.md'}
expected.update('graphics/'+n for n in ['icon-512.png','icon.svg','feature-en-US.png','feature-en-US.svg','feature-zh-TW.png','feature-zh-TW.svg'])
expected.update('listing/'+locale+'/'+name+'.txt' for locale in ['en-US','zh-TW'] for name in ['title','short-description','full-description','release-notes'])
expected.update('screenshots/'+name+'.png' for name in ['01-chinese','02-english','03-taiwanese','04-japanese'])
actual={p.relative_to(KIT).as_posix() for p in KIT.rglob('*') if p.is_file() and p.name!='MANIFEST.json'}
assert actual==expected,'Unexpected or missing kit file: '+repr(actual.symmetric_difference(expected))
entries=[]
for p in sorted(KIT.rglob('*')):
    if not p.is_file() or p.name=='MANIFEST.json':continue
    assert p.suffix.lower() in ('.txt','.md','.html','.json','.svg','.png') or p.name=='LICENSE'
    entries.append({'path':p.relative_to(KIT).as_posix(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(KIT/'MANIFEST.json').write_text(json.dumps({'status':'store materials; app-release gates remain open','files':entries},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
out=ROOT/'artifacts/downloads/MinIME-Google-Play-kit.zip';out.parent.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(KIT.rglob('*')):
        if p.is_file():
            info=zipfile.ZipInfo('MinIME-Google-Play-kit/'+p.relative_to(KIT).as_posix(),date_time=(2026,9,10,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
            z.writestr(info,p.read_bytes())
print('ZIP:',out,'bytes:',out.stat().st_size,'SHA256:',hashlib.sha256(out.read_bytes()).hexdigest())
