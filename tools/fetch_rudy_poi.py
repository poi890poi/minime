"""Download Rudy Map's published POI bundle and pin dataset-level provenance."""
from pathlib import Path
import hashlib,json,urllib.request,zipfile
root=Path(__file__).resolve().parent.parent/'third_party/rudy'
root.mkdir(exist_ok=True)
urls=['https://map.happyman.idv.tw/rudy/MOI_OSM_Taiwan_TOPO_Rudy.poi.zip','https://moi.kcwu.csie.org/MOI_OSM_Taiwan_TOPO_Rudy.poi.zip']
for url in urls:
    try:
        req=urllib.request.Request(url,headers={'User-Agent':'MinIME-source-import/0.6 (https://github.com/poi890poi/minime)'})
        with urllib.request.urlopen(req,timeout=55) as response:
            raw=response.read();headers=dict(response.headers)
        path=root/'rudy-poi.zip';path.write_bytes(raw)
        with zipfile.ZipFile(path) as archive:members=[dict(name=m.filename,bytes=m.file_size) for m in archive.infolist()]
        (root/'source.json').write_bytes((json.dumps(dict(dataset='Rudy Map MOI.OSM Taiwan TOPO POI',page='https://rudymap.tw/',url=url,retrieved='2026-09-08',bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),headers=headers,members=members,license='ODbL-1.0; OpenStreetMap contributors, packaged by Rudy Map'),ensure_ascii=False,indent=2)+'\n').encode('utf-8'))
        print(len(raw),members,flush=True);break
    except Exception as error:print(url,error,flush=True)
else:raise RuntimeError('No POI mirror available')
