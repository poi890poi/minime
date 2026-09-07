"""Freeze external encyclopedia titles before evaluating dictionary coverage.
This file is evaluation-only; the add-on compiler never consumes its output.
"""
import hashlib,json,urllib.parse,urllib.request
from pathlib import Path
out=Path(__file__).resolve().parent.parent/'docs/addons-learning/wikipedia-holdout.json'
categories=['台灣歷史事件','台灣日治時期人物','台灣男歌手','台灣女歌手','台灣山峰','台灣河流','台灣原住民族','台灣特有種','台灣植物','台灣小吃','台灣飲料','台灣電影作品','台灣小說','台語歌曲']
records=[];errors=[]
for category in categories:
    query=urllib.parse.urlencode(dict(action='query',list='categorymembers',cmtitle='Category:'+category,cmnamespace=0,cmlimit=30,format='json',formatversion=2))
    url='https://zh.wikipedia.org/w/api.php?'+query
    try:
        req=urllib.request.Request(url,headers={'User-Agent':'MinIMEResearch/0.6 (https://github.com/poi890poi/minime) independent coverage evaluation'})
        raw=urllib.request.urlopen(req,timeout=35).read();data=json.loads(raw)
        members=data['query']['categorymembers']
        records.append(dict(category=category,url=url,sha256=hashlib.sha256(raw).hexdigest(),members=members))
        print(category,len(members),flush=True)
    except Exception as error:
        errors.append(dict(category=category,error=str(error)));print(category,str(error),flush=True)
out.write_text(json.dumps(dict(role='fresh evaluation holdout; never imported or used for ranking',records=records,errors=errors),ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
