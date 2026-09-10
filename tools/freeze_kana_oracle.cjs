// Differential mechanics corpus from existing readings, plus deterministic joins.
// Not a natural-language corpus or a vocabulary quality score.
const fs=require('fs'),vm=require('vm'),w=require('../third_party/wanakana/index.js');
const src=fs.readFileSync(require.resolve('../third_party/wanakana/index.js'),'utf8');
const ctx={exports:{}};const marker='exports.toRomaji = toRomaji;';
vm.runInNewContext(src.replace(marker,marker+'\nexports.tokens = s => splitIntoConvertedKana(s, {}, createRomajiToKanaMap(false, false));'),ctx);
const readings=fs.readFileSync('docs/japanese-coverage/inputs.tsv','utf8').trim().split('\n').map(r=>r.trimEnd().split('\t')).filter(p=>p[0]==='full').map(p=>p[3]);
const inputs=new Set(readings);
for(let i=0;i<readings.length;i+=23) {
    const joined=readings[i]+readings[(i+7919)%readings.length];if(joined.length<=96)inputs.add(joined);
    for(let n=1;n<readings[i].length;n++)inputs.add(readings[i].slice(0,n));
}
// All source grammar keys, including non-Hepburn aliases, and their joins.
const grammar=JSON.parse(require('child_process').execFileSync(process.execPath,['tools/extract_kana.cjs','--grammar']));
for(let i=0;i<grammar.length;i++) {inputs.add(grammar[i].key);inputs.add(grammar[i].key+grammar[(i+317)%grammar.length].key);}
const rows=['input\thiragana\tkatakana\tconsumed'];
for(const raw of [...inputs].sort()) {
    let hira='',end=0;
    for(const [start,stop,text] of ctx.exports.tokens(raw)) {if(!/^[\u3041-\u3096\u30fc]+$/.test(text))break;hira+=text;end=stop;}
    if(end===raw.length && hira!==w.toHiragana(raw))throw Error('Public API oracle disagrees');
    rows.push([raw,hira,w.toKatakana(hira),end===raw.length?0:end].join('\t'));
}
fs.mkdirSync('docs/japanese-continuity',{recursive:true});
fs.writeFileSync('docs/japanese-continuity/kana-oracle.tsv',rows.join('\n')+'\n');
console.log('Frozen kana mechanics probes:',rows.length-1);
