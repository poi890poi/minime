import urllib.parse
from fetch_addon_sources import save
for name,body in [('taiwan-books','?item wdt:P31/wdt:P279* wd:Q571; wdt:P50 ?author. ?author wdt:P27 wd:Q865.'),('taiwan-songs','?item wdt:P31 wd:Q7366; wdt:P175 ?artist. ?artist wdt:P27 wd:Q865.')]:
 query='SELECT DISTINCT ?item ?itemLabel WHERE { '+body+' SERVICE wikibase:label { bd:serviceParam wikibase:language "zh-tw,zh-hant,zh,en". } } ORDER BY ?item LIMIT 60'
 try:save('wikidata',name+'.json','https://query.wikidata.org/sparql?format=json&query='+urllib.parse.quote(query))
 except Exception as error:print(name,error)
