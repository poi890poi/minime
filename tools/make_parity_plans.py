"""Fixed evaluation-only semantic cases; no data from these plans enters the APK."""
import json
from pathlib import Path
root=Path(__file__).resolve().parent.parent
dest=root/'docs/parity/plans';dest.mkdir(parents=True,exist_ok=True)
cases=[]
def typed(s,label='typed'):return {'type':s,'label':label}
def key(s,**kw):return dict(key=s,**kw)
def case(id,mode,actions,**kw):cases.append(dict(id=id,mode=mode,actions=actions,**kw))
for full,initials,mixed,target in [
 ('xiexie','xx','xiex','謝謝'),('bukeqi','bkq','bukq','不客氣'),
 ('zaoshanghao','zsh','zaoshh','早上好'),('womenmingtianjian','wmmtj','womenmtjian','我們明天見'),
 ('woyaohekafei','wyhkf','woyhekafei','我要喝咖啡'),('jintianxiawu','jtxw','jintxiawu','今天下午'),
 ('qingbangwokanyixia','qbwkyx','qingbwkanyixia','請幫我看一下'),('xianzaijidian','xzjd','xianzaijd','現在幾點')]:
 for kind,reading in [('full',full),('initials',initials),('mixed',mixed)]:
  case('py-'+reading,'pinyin',[typed(reading),key('SPACE',label='accepted')],group='pinyin-'+kind,target=target)
for reading in ['schedu','arrang','tomorr','teh','recieve','definately','dont','im','wont','hellp','thsi','adress']:
 case('en-'+reading,'english',[typed(reading),key('SPACE',label='accepted')],group='english-spelling')
for id,text in [('thanks','thank you '),('see','see you '),('good','good morning '),('going','going to ')]:
 case('en-next-'+id,'english',[typed(text)],group='english-prediction')
case('en-double-space','english',[typed('hello'),key('SPACE'),key('SPACE')],group='english-spacing')
case('en-sentence-caps','english',[typed('hello'),key('PERIOD'),typed(' world')],field='sentences',group='capitalization')
case('en-word-caps','english',[typed('hello world')],field='words',group='capitalization')
case('en-completion-space','english',[typed('pronun'),{'choose':'pronunciation'},typed('test ')],group='candidate-acceptance')
case('en-raw-recovery','english',[typed('teh'),{'raw':'teh'},key('SPACE')],group='candidate-acceptance')
case('en-comma-spacing','english',[typed('hello'),key('COMMA'),typed(' world'),key('PERIOD')],group='punctuation')
case('en-caps-lock','english',[key('SHIFT',double=True),typed('api usb '),key('SHIFT'),typed('x ')],group='capitalization')
case('en-shift-once','english',[key('SHIFT'),typed('hello ')],group='capitalization')
for mode in ['pinyin','english']:
 case(mode+'-slide-up',mode,[key('a',slide=-1)],group='gestures')
 case(mode+'-slide-number',mode,[key('q',slide=1)],group='gestures')
 case(mode+'-shift-slide',mode,[key('SHIFT'),key('q',slide=-1)],group='gestures')
 case(mode+'-comma-hold',mode,[key('COMMA',hold=700,capture=True)],group='palettes')
 case(mode+'-period-hold',mode,[key('PERIOD',hold=700,capture=True)],group='palettes')
 case(mode+'-symbols',mode,[key('SYMBOLS',capture=True),key('LETTERS'),typed('x')],group='palettes')
case('py-composition-slide','pinyin',[typed('nihao'),key('a',slide=-1)],group='gestures')
case('py-refine','pinyin',[typed('bukeqi'),key('DELETE'),key('DELETE'),typed('qi'),key('SPACE')],group='editing')
case('en-selected-delete','english',[typed('hello world '),{'cursor':0,'end':5},key('DELETE')],group='editing')
case('en-reedit','english',[typed('hello '),key('DELETE'),typed('x ')],group='editing')
case('en-cursor-in-word','english',[typed('hello '),{'cursor':2},typed('x ')],group='editing')
case('py-hide-show','pinyin',[typed('nihao'),{'hideShow':True},key('SPACE')],group='lifecycle')
case('py-restart','pinyin',[typed('srf'),{'restart':True},key('SPACE')],group='lifecycle')
case('en-restart','english',[typed('hel'),{'restart':True},typed('lo ')],group='lifecycle')
case('py-active-switch','pinyin',[typed('nihao'),key('LANGUAGE'),typed('hello ')],group='switching')
case('en-active-switch','english',[typed('hello'),key('LANGUAGE'),typed('nihao'),key('SPACE')],group='switching')
for field in ['nosuggest','private','password','url','email']:
 case('field-'+field,'english',[typed('pronun'),key('SPACE')],field=field,group='editor-policy')
case('field-number','english',[typed('12.3')],field='number',group='editor-policy')
case('field-search-en','english',[typed('hello'),key('ENTER')],field='search',group='editor-action')
case('field-search-py','pinyin',[typed('nihao'),key('ENTER')],field='search',group='editor-action')
case('en-enter','english',[typed('hello'),key('ENTER')],group='editor-action')
case('py-enter','pinyin',[typed('nihao'),key('ENTER'),key('ENTER')],group='editor-action')
case('url-slides','english',[typed('name'),key('a',slide=1),typed('example'),key('PERIOD'),typed('com')],field='email',group='editor-policy')
case('py-latin-space','pinyin',[typed('meeting'),key('SPACE')],group='intentional-control')
for prefix in ['co','in','re']:
 case('en-cap-'+prefix,'english',[typed(prefix)],group='english-prefix-cap')
case('en-word-trace','english',[dict(trace='hello',capture=True)],group='gestures')
case('py-expanded-candidates','pinyin',[typed('shi'),key('EXPAND',capture=True)],group='candidate-presentation')
case('en-expanded-candidates','english',[typed('co'),key('EXPAND',capture=True)],group='candidate-presentation')
case('landscape-pinyin','pinyin',[typed('nihao'),key('SPACE',capture=True)],landscape=True,group='landscape')
case('landscape-english','english',[typed('hello '),key('PERIOD',capture=True)],landscape=True,group='landscape')
for at in range(0,len(cases),10):
 (dest/f'batch-{at//10+1:02}.json').write_text(json.dumps(cases[at:at+10],ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(dest.parent/'matrix.json').write_text(json.dumps(cases,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'{len(cases)} paired cases, {(len(cases)+9)//10} batches')
