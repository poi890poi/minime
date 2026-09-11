"""Register evaluation sources without permitting any production extraction."""
from pathlib import Path
import json,shutil
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'docs/language-contract-benchmark/conversations'
catalog=json.loads((ROOT/'sources/catalog.json').read_text(encoding='utf8'))
specs=[
 ('real-persona-chat','RealPersonaChat','japan-authored','CC-BY-SA-4.0','https://github.com/nu-dialogue/real-persona-chat',
  'Public anonymized human casual dialogue collected by the Nagoya University dialogue group.',
  '192 Japanese conversations total across this source and ASDC; exact source splits in split-plan.json.',
  'Not spontaneous speech; speakers recur across conversations. Kanji readings are independently generated silver annotations.',
  'reference',['docs/language-contract-benchmark/conversations/real-persona-chat-README.md','docs/language-contract-benchmark/conversations/real-persona-chat-LICENSE']),
 ('asdc','Accommodation Search Dialog Corpus','japan-authored','CC-BY-4.0','https://github.com/megagonlabs/asdc',
  'Human customer/operator role-play collected and documented by Megagon Labs.',
  '210 main source dialogues; 96 systematically selected, separate casual/task reporting.',
  'Accommodation-only role-play; formal style and repeated templates. Not broad spontaneous conversation.',
  'reference',['docs/language-contract-benchmark/conversations/asdc-README.md','docs/language-contract-benchmark/conversations/asdc-LICENSE.txt']),
 ('suisiann-thousand','媠聲一千：阮家己的造句','taiwan-authored','CC-BY-SA-4.0','https://github.com/SuiSiann/SuiSiann-TsitTshing',
  'Locally authored everyday examples with parallel Han and supplied Tâi-lô; named contributors in upstream README.',
  '18 aligned source files; 1,887 word/clause references selected by general rules before query evaluation.',
  'Authored situations, not recorded multi-turn conversation; loanword lists acknowledge iTaigi/MOE. Supplied readings converted to POJ with pinned independent tools.',
  'reference',['docs/language-contract-benchmark/conversations/suisiann-thousand-README.md']),
 ('tsay-conversation-evaluation','Tsay / TAICORP spontaneous Taiwanese conversations','taiwan-authored','CC-BY-NC-SA-3.0','https://talkbank.org/phon/access/Chinese/Taiwanese/Tsay.html',
  'Documented family conversations collected in Chiayi by Jane Tsay and collaborators.',
  'No transcript data acquired: official download returned an authentication page.',
  'Access and restricted use unresolved; external-processing restrictions. No corpus text may be added to evaluation or production from this entry.',
  'hold',[])]
for key,name,origin,lic,url,editorial,coverage,limits,decision,audit in specs:
    item={'id':key,'name':name,'origin':origin,'decision':decision,'uses':['evaluation','discovery'],'inputs':[],
      'license':{'id':lic,'status':'verified' if decision=='reference' else 'restricted','evidence':url},'evidence':[url],
      'editorial':editorial,'coverage':coverage,'limitations':limits,'reason':'Evaluation only; source/reading independence and rights audited in docs/language-contract-benchmark/CONVERSATION_DATA.md.',
      'addon_prefixes':[],'audit_files':audit}
    previous=next((s for s in catalog['sources'] if s['id']==key),None)
    if previous is None:catalog['sources'].append(item)
    else:assert previous==item,'Do not overwrite a concurrent source decision'
for item in catalog['sources']:
    if item['id']=='taiwanese-basic' and 'evaluation' not in item['uses']:item['uses'].append('evaluation')
(ROOT/'sources/catalog.json').write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
registry=json.loads((ROOT/'sources/evaluation.json').read_text(encoding='utf8'))
item={'id':'language-contract-conversations-20260911','source_ids':['real-persona-chat','asdc','suisiann-thousand','taiwanese-basic'],
  'genre':['human-casual-chat','human-task-dialogue','authored-everyday-utterance','historical-teaching-example'],
  'role':'development','exposure':'inspected',
  'selection':'Frozen source-conversation/file SHA256 splits; source-bound word/two-token/clause sampling and deterministic error conditions. Separate originally fresh holdout partition in references. No production queries used for reference selection.',
  'files':['docs/language-contract-benchmark/conversations/'+n for n in ['manifest.json','split-plan.json','inputs.tsv','references.jsonl.gz','turns.jsonl.gz','orthography-audit.json']],
  'report_by':['source','genre','role','reference-status','input-condition','unit','production-overlap'],
  'leakage':'Japanese conversations and modern Taiwanese source files disjoint between development and initial holdout, with recurring speakers/phrases explicitly allowed. Historical beginner material shares production lineage. No new corpus text is used as production vocabulary or training.',
  'limitations':'Japanese Kanji reading annotations are silver. Modern Taiwanese examples are authored situations rather than spontaneous multi-turn dialogue. No physical touch observations or human community-usage labels.'}
previous=next((s for s in registry['corpora'] if s['id']==item['id']),None)
if previous is None:registry['corpora'].append(item)
else:assert previous==item
(ROOT/'sources/evaluation.json').write_text(json.dumps(registry,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
shutil.copyfile(ROOT/'third_party/taiwanese_basic/README.md',OUT/'taiwanese-basic-README.md')
print('Registered evaluation-only sources; production inputs unchanged')
