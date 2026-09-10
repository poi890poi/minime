"""Build the approved MinIME icon suite and wire its Android/site resources."""
from pathlib import Path
import hashlib,json,shutil,zipfile,subprocess,sys,math
from PIL import Image,ImageDraw,ImageFont
from icon_art import TEAL,CREAM,LEAF,geometry,points,svg,render,vector,smile_outline,draw_path
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'branding/minime'
RES=ROOT/'app/src/main/res'
FILES=set()

def save(name,content):
    path=OUT/name;path.parent.mkdir(parents=True,exist_ok=True)
    if isinstance(content,str):path.write_bytes(content.encode('utf8'))
    else:content.save(path)
    FILES.add(name)
    return path

def resource(name,content):
    path=save('android/res/'+name,content);target=RES/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,target)

def main():
    FILES.clear()
    OUT.mkdir(parents=True,exist_ok=True)
    for name,background,mono in [('minime.svg','rounded',None),('minime-mark.svg',None,None),('minime-black.svg',None,'#000000'),('minime-white.svg',None,'#FFFFFF')]:save('source/'+name,svg(background,mono))
    save('source/icon_art.py',(ROOT/'tools/icon_art.py').read_text(encoding='utf-8-sig'))
    for size in (16,24,32,48,64,96,128,192,256,512,1024):save(f'png/minime-{size}.png',render(size))
    for name,mono in [('color',None),('black','#000000'),('white','#FFFFFF')]:
        for size in (256,512,1024):save(f'transparent/minime-{name}-{size}.png',render(size,None,mono))
    save('store/icon-512.png',render(512,'square'));save('store/icon.svg',svg('square'))
    for density,size,layer in [('mdpi',48,108),('hdpi',72,162),('xhdpi',96,216),('xxhdpi',144,324),('xxxhdpi',192,432)]:
        resource(f'mipmap-{density}/ic_launcher.png',render(size))
        resource(f'mipmap-{density}/ic_launcher_round.png',render(size,'circle'))
        save(f'android/layers/foreground-{density}.png',render(layer,None,adaptive=True))
        resource(f'drawable-{density}/ic_launcher_foreground.png',render(layer,None,adaptive=True))
        save(f'android/layers/monochrome-{density}.png',render(layer,None,'#FFFFFF',adaptive=True))
        resource(f'drawable-{density}/ic_launcher_monochrome.png',render(layer,None,'#FFFFFF',adaptive=True))
        save(f'android/layers/background-{density}.png',Image.new('RGBA',(layer,layer),TEAL))
    save('source/android-foreground.xml',vector())
    save('source/android-monochrome.xml',vector(mono=True))
    resource('values/launcher_colors.xml',f'<resources><color name="launcher_background">{TEAL}</color></resources>\n')
    for version in (26,33):
        content='<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@color/launcher_background"/>\n    <foreground android:drawable="@drawable/ic_launcher_foreground"/>\n'
        if version==33:content+='    <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>\n'
        content+='</adaptive-icon>\n'
        for name in ('ic_launcher','ic_launcher_round'):resource(f'mipmap-anydpi-v{version}/{name}.xml',content)
    save('android/layers/foreground.svg',svg(None,adaptive=True));save('android/layers/monochrome.svg',svg(None,'#FFFFFF',True))
    save('android/layers/background.svg',f'<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108"><path fill="{TEAL}" d="M0 0h108v108H0z"/></svg>\n')
    save('web/favicon.svg',svg());save('web/apple-touch-icon.png',render(180,'square'))
    for size in (16,32,48,192,512):save(f'web/icon-{size}.png',render(size,'square' if size>=192 else 'rounded'))
    ico=OUT/'web/favicon.ico';render(256).save(ico,format='ICO',sizes=[(n,n) for n in (16,24,32,48,64,128,256)]);FILES.add('web/favicon.ico')
    save('web/site.webmanifest',json.dumps({'name':'MinIME','short_name':'MinIME','icons':[{'src':'icon-192.png','sizes':'192x192','type':'image/png','purpose':'any'},{'src':'icon-512.png','sizes':'512x512','type':'image/png','purpose':'any'}],'theme_color':TEAL,'background_color':TEAL},indent=2)+'\n')
    # Only the favicon is wired into the informational website; this does not add a PWA.
    for name in ('favicon.svg','favicon.ico','apple-touch-icon.png'):
        dest=ROOT/'site'/name;shutil.copyfile(OUT/'web'/name,dest)
    subprocess.run([sys.executable,str(ROOT/'tools/build_store_materials.py')],cwd=ROOT,check=True)
    for locale in ('en-US','zh-TW'):
        for suffix in ('png','svg'):
            name=f'feature-{locale}.{suffix}';source=ROOT/'docs/play-publishing/kit/graphics'/name
            path=OUT/'store'/name;shutil.copyfile(source,path);FILES.add('store/'+name)
    save('LICENSE',(ROOT/'LICENSE').read_text(encoding='utf8'))
    save('palette.json',json.dumps({'deep_teal':TEAL,'warm_cream':CREAM,'bamboo_leaf':LEAF},indent=2)+'\n')
    save('README.md',README)
    preview()
    validate()
    actual={p.relative_to(OUT).as_posix() for p in OUT.rglob('*') if p.is_file() and p.name!='MANIFEST.json'}
    assert actual==FILES,'Unexpected or missing public suite files: '+repr(actual.symmetric_difference(FILES))
    entries=[{'path':n,'bytes':(OUT/n).stat().st_size,'sha256':hashlib.sha256((OUT/n).read_bytes()).hexdigest()} for n in sorted(FILES)]
    save('MANIFEST.json',json.dumps({'name':'MinIME approved icon suite','files':entries},ensure_ascii=False,indent=2)+'\n')
    target=ROOT/'artifacts/downloads/MinIME-icon-suite.zip';target.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as z:
        for name in sorted(FILES):
            info=zipfile.ZipInfo('MinIME-icon-suite/'+name,date_time=(2026,9,11,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;z.writestr(info,(OUT/name).read_bytes())
    print('Icon suite:',len(FILES),'files;',target.stat().st_size,'bytes; SHA256',hashlib.sha256(target.read_bytes()).hexdigest())

def validate():
    # Guard the Android 66 dp safe area, including representative curved extrema.
    samples=[(18+1.5*x,18+1.5*y) for path,_,_,_ in geometry() for x,y in points(path)]
    bounds=[min(x for x,y in samples),min(y for x,y in samples),max(x for x,y in samples),max(y for x,y in samples)]
    radius=max(math.hypot(x-54,y-54) for x,y in samples)
    assert min(bounds[:2])>=21 and max(bounds[2:])<=87 and radius<33,(bounds,radius)
    store=Image.open(OUT/'store/icon-512.png');assert store.size==(512,512) and store.mode=='RGBA' and store.getextrema()[3]==(255,255)
    assert (OUT/'store/icon-512.png').stat().st_size<1024*1024
    mono=Image.open(OUT/'transparent/minime-white-512.png')
    for x,y in ((19,31),(29,31),(24,38.8),(0,0)):assert mono.getpixel((round(x*512/48),round(y*512/48)))[3]==0,(x,y)
    # Independently compare the closed SVG/Android smile cutout to the raster stroke.
    g=geometry();stroke=Image.new('L',(1024,1024));outline=Image.new('L',stroke.size)
    draw_path(ImageDraw.Draw(stroke),g[4][0],None,255,g[4][3],1024/48)
    draw_path(ImageDraw.Draw(outline),smile_outline(),255,None,0,1024/48)
    differing=sum(abs(a-b)>128 for a,b in zip(stroke.tobytes(),outline.tobytes()))
    assert differing<400,'Monochrome stroke outline mismatch: '+str(differing)
    print('Validated Play RGBA, alpha cutouts, mask radius',round(radius,2),'dp; outline edge pixels',differing)

def preview():
    im=Image.new('RGB',(1100,720),'#F3F4EE');d=ImageDraw.Draw(im)
    def font(n,bold=False):return ImageFont.truetype('C:/Windows/Fonts/'+('segoeuib.ttf' if bold else 'segoeui.ttf'),n)
    def text(x,y,t,n=18):d.text((x,y),t,font=font(n,n>=24),fill='#244743')
    def tile(icon,x,y,size):icon=icon.resize((size,size),Image.Resampling.LANCZOS);im.paste(icon,(x,y),icon)
    text(38,25,'MinIME / complete icon suite',30)
    text(38,72,'Approved bamboo-leaf hat, soft face and rounded V smile.',18)
    tile(render(512),40,136,280);text(40,440,'Master icon',22)
    tile(render(512,'circle'),370,136,160);text(370,313,'Round launcher')
    tile(render(512),580,136,160);text(580,313,'Rounded launcher')
    for x,bg,ink,label in [(800,'#DAEDE1',TEAL,'Light theme'),(800,'#173D39','#BDEBDC','Dark theme')]:
        y=136 if label=='Light theme' else 367
        card=Image.new('RGBA',(160,160));cd=ImageDraw.Draw(card);cd.rounded_rectangle((0,0,159,159),radius=43,fill=bg)
        card.alpha_composite(render(160,None,ink));tile(card,x,y,160);text(x,y+177,label)
    text(370,381,'Transparent mark',18);tile(render(256,None),386,420,125)
    text(560,381,'One colour',18);tile(render(256,None,TEAL),582,420,125)
    text(40,528,'Native-size exports',18)
    x=40
    for size in (16,24,32,48,64,96):tile(render(size),x,572,size);text(x,680,str(size)+' px',13);x+=size+32
    text(825,618,TEAL,18);text(825,648,'SVG · PNG · ICO · XML',15)
    save('PREVIEW.png',im)

README='''# MinIME icon suite

Approved design: a Taiwan farmer's broad bamboo-leaf hat over a friendly face,
inspired by 台. Deep teal #006765; warm cream #FFF7E5; bamboo leaf #D9C28E.
The face has continuous curves and a rounded V smile. Artwork is original;
no party, flag or third-party logo artwork is included.

- `source/`: editable SVG masters and the Python geometry source (Pillow needed
  for PNG exports). Full icon, transparent colour mark, black and white cutout marks.
- `png/`: rounded icon at 16, 24, 32, 48, 64, 96, 128, 192, 256, 512 and 1024 px.
- `transparent/`: colour, black and white marks at 256, 512 and 1024 px.
- `android/res/`: ready-to-copy launcher resources. Adaptive icons for API 26+;
  dedicated monochrome layer for API 33+; round and standard density fallbacks.
  Density-matched PNG layers preserve the exact curves without parsing long paths.
  Optional vector XML masters are in `source/`, outside packaged Android resources.
- `android/layers/`: separate 108 dp foreground, background and monochrome layers,
  in SVG and all five density PNG sizes. Do not pre-mask these layers.
- `store/`: opaque 512 px Play icon and editable SVG, plus English and Traditional
  Chinese feature graphics. Upload the square PNG: Play applies its corner mask.
- `web/`: SVG favicon, multi-resolution ICO, PNG icons, 180 px Apple touch icon and
  optional manifest example. Icon entries use purpose `any`, not `maskable`.
- `PREVIEW.png`: design and mask/size comparisons. `MANIFEST.json`: payload hashes.

Android manifest integration:

    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"

The 108 dp adaptive layers contain the approved artwork within the central 66 dp
safe area. The OS supplies launcher masks and themed colours. Eyes and mouth in
monochrome marks are transparent cutouts, so they survive arbitrary tinting.
The themed preview colours are examples, not fixed app colours.

Keep proportions and the spacing between hat and face. The solid-background icon
is preferred on busy backgrounds. Use the black/white cutout marks when one ink
is required. The bamboo-leaf colour is part of the design, not a material texture.

Rebuild from the repository with `python tools/build_icon_suite.py` (Pillow,
Windows Segoe UI and Microsoft JhengHei for the preview/feature text). The source
icon geometry needs no font. Font binaries are not distributed. The editable
feature SVGs reference system fonts and may render differently elsewhere.

Artwork and original tooling: Apache-2.0; see LICENSE. This asset-only ZIP contains
no APK, signing keys, device backups or language dictionaries. It does not clear
Google Play app-release gates. Android resource build/lint and offline mask checks
are separate from physical launcher/device validation.

Specifications checked September 11, 2026:
https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
https://developer.android.com/distribute/google-play/resources/icon-design-specifications
'''
if __name__=='__main__':main()
