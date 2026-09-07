// Build-time only. WanaKana's established Hepburn conversion supplies ASCII aliases.
const fs = require('fs');
const {toRomaji} = require('../third_party/wanakana/index.js');
const inputs = JSON.parse(fs.readFileSync(0, 'utf8'));
process.stdout.write(JSON.stringify(Object.fromEntries(inputs.map(kana => [kana, toRomaji(kana).toLowerCase()]))));
