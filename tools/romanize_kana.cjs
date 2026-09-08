// Build-time only. WanaKana's established Hepburn conversion supplies ASCII aliases.
const fs = require('fs');
const {toRomaji} = require('../third_party/wanakana/index.js');
const inputs = JSON.parse(fs.readFileSync(0, 'utf8'));
// The pinned library already tokenizes kana during conversion, but its public
// API discards those boundaries. Expose that existing function in an isolated
// build-time context; never change the vendored library or infer units by regex.
const vm = require('vm');
const context = {exports: {}};
const source = fs.readFileSync(require.resolve('../third_party/wanakana/index.js'), 'utf8');
const marker = 'exports.toRomaji = toRomaji;';
if (source.split(marker).length !== 2) throw new Error('Pinned WanaKana export changed');
vm.runInNewContext(source.replace(marker, marker + '\nexports.units = input => splitIntoRomaji(input, mergeWithDefaultOptions({}));'), context);
process.stdout.write(JSON.stringify(Object.fromEntries(inputs.map(kana => {
    const romaji = toRomaji(kana).toLowerCase();
    const tokens = context.exports.units(kana).map(token => token[2].toLowerCase());
    if (tokens.join('') !== romaji) throw new Error('Romanization unit mismatch: ' + kana);
    const reading = tokens.map(token => token.replace(/[ '\u30fb]/g, '')).filter(Boolean).join("'");
    return [kana, {romaji, reading}];
}))));
