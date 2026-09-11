"""Isolated performance patch to pinned MIT source; no dictionary modifications."""
from pathlib import Path
import shutil, difflib, hashlib, json, zipfile
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1]
WORK=ROOT/'artifacts/japanese-engine-benchmark'
ORIGINAL=WORK/'kazuma/kana-kanji-conversion-c-plus-plus-f8af0c2e6a7538f83e7483e9171ebec04fd963a0'
COPY=WORK/'kazuma-indexed'

def patch():
    manifest=json.loads((HERE.parent/'japanese-engine-benchmark/source-manifest.json').read_text(encoding='utf8'))
    expected=next(x['sha256'] for x in manifest['downloads'] if x['path']=='kazuma.zip')
    assert hashlib.sha256((WORK/'kazuma.zip').read_bytes()).hexdigest()==expected
    with zipfile.ZipFile(WORK/'kazuma.zip') as archive:
        for member in archive.infolist():
            if not member.is_dir():
                path=ORIGINAL/Path(*Path(member.filename).parts[1:])
                assert path.read_bytes()==archive.read(member),('Upstream source modified',path)
    shutil.copytree(ORIGINAL,COPY,dirs_exist_ok=True)
    edits={}
    for name in ['token_array.hpp','token_array.cpp']:
        path=Path('src/dictionary_builder/token_array')/name
        before=(ORIGINAL/path).read_text(encoding='utf8');after=before
        if name.endswith('.hpp'):
            after=after.replace('private:\n','private:\n    std::vector<int> loadedPostingOffsets; // MinIME: index immutable loaded data only.\n',1)
        else:
            after=after.replace('    postingsBits = BitVector{};','    postingsBits = BitVector{};\n    loadedPostingOffsets.clear();',1)
            start=after.index('    const int p0 = postingsBits.select0(termId + 1);')
            end=after.index('\n    std::vector<TokenEntry> out;',start)
            original=after[start:end]
            after=after[:start]+'''    int b, c;
    if (!loadedPostingOffsets.empty()) {
        if (termId < 0 || static_cast<size_t>(termId) + 1 >= loadedPostingOffsets.size())
            return {};
        b = loadedPostingOffsets[static_cast<size_t>(termId)];
        c = loadedPostingOffsets[static_cast<size_t>(termId) + 1];
    } else {
'''+original.replace('    const int b =','    b =').replace('    const int c =','    c =')+'''\n    }
'''+after[end:]
            after=after.replace('    t.postingsBits = readBitVector(ifs);','''    t.postingsBits = readBitVector(ifs);
    int offset = 0;
    for (size_t bit = 0; bit < t.postingsBits.size(); ++bit) {
        if (t.postingsBits.get(bit)) ++offset;
        else t.loadedPostingOffsets.push_back(offset);
    }''',1)
        assert after!=before
        (COPY/path).write_text(after,encoding='utf8',newline='\n')
        edits[str(path).replace('\\','/')]=''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+path.as_posix(),tofile='b/'+path.as_posix()))
    (HERE/'posting-index.patch').write_text(''.join(edits.values()),encoding='utf8',newline='\n')
    print('Patched isolated copy:',COPY)

if __name__=='__main__':patch()
