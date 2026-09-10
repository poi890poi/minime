// Enumerate the pinned library's existing mappings, including IME aliases.
const fs = require('fs'), vm = require('vm');
const w = require('../third_party/wanakana/index.js');
const source = fs.readFileSync(require.resolve('../third_party/wanakana/index.js'), 'utf8');
const marker = 'exports.toRomaji = toRomaji;';
if (source.split(marker).length !== 2) throw new Error('Pinned export changed');
const context = {exports: {}};
vm.runInNewContext(source.replace(marker, marker + '\nexports.tree = ime => createRomajiToKanaMap(ime, false);'), context);
const found = new Map();
const grammar = process.argv.includes('--grammar');
function visit(node, key) {
    for (const [letter, value] of Object.entries(node)) {
        if (letter) visit(value, key + letter);
        else if (/^[a-z'-]+$/.test(key) && (grammar ? /^[\u3041-\u3096\u30fc]+$/.test(value) : [...value].length === 1 && /^[\u3041-\u3096]$/.test(value))) {
            found.set(key, {key, hiragana:value, katakana:w.toKatakana(value)});
        }
    }
}
visit(context.exports.tree(false), '');
if (!grammar) visit(context.exports.tree(true), '');
process.stdout.write(JSON.stringify([...found.values()].sort((a,b)=>a.key.localeCompare(b.key, 'en'))));
