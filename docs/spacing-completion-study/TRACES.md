# Detailed spacing/completion traces

Notation: `␠` is one literal U+0020 space; `↵` is newline. Empty text is `∅`.

## en-completion-letter

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pronun` | `pronun` |
| candidate-selected | `pronunciation` | `pronunciation` |
| next-word | `pronunciation␠test` | `pronunciation␠test` |
| space | `pronunciation␠test␠` | `pronunciation␠test␠` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-completion-explicit-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `tomorr` | `tomorr` |
| candidate-selected | `tomorrow` | `tomorrow` |
| space | `tomorrow␠` | `tomorrow␠` |
| next-word | `tomorrow␠morning` | `tomorrow␠morning` |
| space | `tomorrow␠morning␠` | `tomorrow␠morning␠` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-completion-comma

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `hell` | `hell` |
| candidate-selected | `hello` | `hello` |
| comma | `hello,` | `hello,` |
| next-word | `hello,world` | `hello,world` |
| space | `hello,world␠` | `hello,world␠` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-completion-period

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pleas` | `pleas` |
| candidate-selected | `please` | `please` |
| period | `please.` | `please.` |
| next-word | `please.help` | `please.help` |
| space | `please.help␠` | `please.help␠` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-completion-delete

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `becau` | `becau` |
| candidate-selected | `because` | `because` |
| after-delete | `because` | `becaus` |
| after-edit | `because␠x` | `becausx` |
| space | `because␠x␠` | `becausx␠` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-partial-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pronun` | `pronun` |
| space-accept | `pronunciation␠` | `pronun␠` |
| next-word | `pronunciation␠test` | `pronun␠test` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-full-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `hello` | `hello` |
| first-space | `hello␠` | `hello␠` |
| next-word | `hello␠world` | `hello␠world` |
| space | `hello␠world␠` | `hello␠world␠` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-double-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `hello` | `hello` |
| first-space | `hello␠` | `hello␠` |
| second-space | `hello.␠` | `hello.␠` |
| undo | `hello.␠` | `hello.` |
| after-undo | `hello.␠world` | `hello.world` |

Sources: `batch-01` (observed), `batch-01` (observed).

## en-literal-comma

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `hello` | `hello` |
| comma | `hello,` | `hello,` |
| next-word | `hello,world` | `hello,world` |
| space | `hello,world␠` | `hello,world␠` |

Sources: `batch-02` (observed), `batch-02` (observed).

## en-typo-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `thos` | `thos` |
| space-accept | `this␠` | `thos␠` |
| undo | `this␠` | `thos` |

Sources: `batch-02` (observed), `batch-02` (observed).

## en-spelling-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `recieve` | `recieve` |
| space-accept | `receive␠` | `recieve␠` |
| undo | `receive␠` | `recieve` |

Sources: `batch-02` (observed), `batch-02` (observed).

## en-contraction-space

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `cant` | `cant` |
| space-accept | `can't␠` | `cant␠` |
| undo | `can't␠` | `cant` |

Sources: `batch-02` (observed), `batch-02` (observed).

## en-next-word-tap

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `thank` | `thank` |
| space | `thank␠` | `thank␠` |
| candidate-selected | `thank␠you` | `thank␠you` |
| next-word | `thank␠you␠again` | `thank␠you␠again` |
| space | `thank␠you␠again␠` | `thank␠you␠again␠` |

Sources: `batch-02` (observed), `batch-02` (observed).

## py-english-space

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `meeting` |
| first-space | `meeting` | `meeting␠` |
| next-word | `meeting` | `meeting␠test` |
| second-accept | `meeting␠test` | `meeting␠test␠` |

Sources: `recheck-pinyin` (observed), `recheck-pinyin` (observed).

## py-english-two-spaces

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `meeting` |
| first-space | `meeting` | `meeting␠` |
| second-space | `meeting␠` | `meeting␠␠` |
| next-word | `meeting␠` | `meeting␠␠test` |

Sources: `recheck-pinyin` (observed), `recheck-pinyin` (observed).

## py-english-completion

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `pronun` |
| candidate-selected | `pronunciation` | `pronunciation` |
| next-word | `pronunciation` | `pronunciationtest` |
| space | `pronunciation␠test` | `pronunciationtest␠` |

Sources: `recheck-pinyin` (observed), `recheck-pinyin` (observed).

## py-chinese-space

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `nihao` |
| phrase-accept | `你好` | `你好` |
| literal-space | `你好␠` | `你好␠` |
| next-syllable | `你好␠` | `你好␠ma` |
| space | `你好␠嗎` | `你好␠ma␠` |

Sources: `batch-03` (observed), `batch-03` (observed).

## py-chinese-candidate

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `nihao` |
| candidate-selected | `你好` | `你好` |
| next-syllable | `你好` | `你好ma` |
| space | `你好嗎` | `你好ma␠` |

Sources: `batch-03` (observed), `batch-03` (observed).

## py-english-then-chinese

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `meeting` |
| space | `meeting` | `meeting␠` |
| chinese-typed | `meeting` | `meeting␠nihao` |
| chinese-accepted | `meeting你好` | `meeting␠你好` |

Sources: `batch-03` (observed), `batch-03` (observed).

## py-chinese-then-english

Board: pinyin; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `∅` | `nihao` |
| candidate-selected | `你好` | `你好` |
| english-typed | `你好` | `你好meeting` |
| english-accepted | `你好meeting` | `你好meeting␠` |

Sources: `batch-03` (observed), `batch-03` (observed).

## en-candidate-switch-chinese

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pronun` | `pronun` |
| candidate-selected | `pronunciation` | `pronunciation` |
| switch | `pronunciation` | `pronunciation` |
| chinese-typed | unavailable | `pronunciationnihao` |
| space | unavailable | `pronunciation你好` |

Sources: `batch-03` (unavailable), `batch-03` (observed).

## en-nosuggest-completion

Board: english; field: nosuggest; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pronun` | `pronun` |
| candidate-selected | unavailable | `pronunciation` |
| next-word | unavailable | `pronunciation␠test` |
| space | unavailable | `pronunciation␠test␠` |

Sources: `batch-03` (unavailable), `batch-03` (observed).

## en-url-completion

Board: english; field: url; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pronun` | `pronun` |
| candidate-selected | `pronunciation` | unavailable |
| next-word | `pronunciation␠test` | unavailable |

Sources: `batch-03` (observed), `batch-03` (unavailable).

## en-email-literal

Board: english; field: email; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `name` | `name` |
| at-symbol | `name@` | `name@` |
| typed | `name@example` | `name@example` |
| period | `name@example.` | `name@example.` |
| typed | `name@example.com` | `name@example.com` |
| space | `name@example.com␠` | `name@example.com␠` |

Sources: `batch-03` (observed), `batch-03` (observed).

## en-correction-on-pronun

Board: english; field: normal; MinIME spelling correction: True.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `pronun` | `pronun` |
| space-accept | `pronunciation␠` | `pronun␠` |
| delete-1 | `pronunciation␠` | `pronun` |
| delete-2 | `pronun` | `pronu` |

Sources: `followups` (observed), `followups` (observed).

## en-correction-on-thos

Board: english; field: normal; MinIME spelling correction: True.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `thos` | `thos` |
| space-accept | `this␠` | `this␠` |
| delete-1 | `this␠` | `thos` |
| delete-2 | `thos` | `tho` |

Sources: `followups` (observed), `followups` (observed).

## en-correction-on-recieve

Board: english; field: normal; MinIME spelling correction: True.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `recieve` | `recieve` |
| space-accept | `receive␠` | `receive␠` |
| delete-1 | `receive␠` | `recieve` |
| delete-2 | `recieve` | `reciev` |

Sources: `followups` (observed), `followups` (observed).

## en-correction-on-the

Board: english; field: normal; MinIME spelling correction: True.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `the` | `the` |
| space-accept | `the␠` | `the␠` |
| delete-1 | `the␠` | `the` |
| delete-2 | `the` | `th` |

Sources: `followups` (observed), `followups` (observed).

## en-basic-delete-control

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `hello` | `hello` |
| delete-1 | `hell` | `hell` |
| delete-2 | `hel` | `hel` |
| accepted | `hello␠` | `hel␠` |
| delete-3 | `hello␠` | `hel` |
| delete-4 | `hel` | `he` |

Sources: `followups` (observed), `followups` (observed).

## en-completion-delete-repeat

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `becau` | `becau` |
| selected | `because` | `because` |
| delete-1 | `because` | `becaus` |
| delete-2 | `becaus` | `becau` |
| delete-3 | `becau` | `beca` |
| after-edit | `becaux` | `becax` |

Sources: `followups` (observed), `followups` (observed).

## en-double-space-delete-repeat

Board: english; field: normal; MinIME spelling correction: False.

| Action stage | Google Zhuyin editor text | MinIME editor text |
|---|---|---|
| ready | `∅` | `∅` |
| typed | `hello` | `hello` |
| space-1 | `hello␠` | `hello␠` |
| space-2 | `hello.␠` | `hello.␠` |
| delete-1 | `hello.␠` | `hello.` |
| delete-2 | `hello.` | `hello` |
| delete-3 | `hello` | `hell` |

Sources: `followups` (observed), `followups` (observed).
