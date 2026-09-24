# A lexical contraction category is broader, but not complete

September 25, 2026. **Reject the category-only replacement at the reference
screen.** No production metadata, vocabulary, counts or runtime changed.

The bounded API collector traversed all five categories, including pagination,
and repeated the traversal. Both captures have identical 1,421 memberships and
1,319 unique pages. The membership snapshot SHA-256 is
`612f26f10ba6967c746168e94b7d29e07c6f1956d95ada0e7682240da213760b`.
Mainspace/internal-apostrophe ASCII filtering yields 501 normalized forms; 70
meet the unchanged accepted vocabulary/frequency eligibility rule. Old flags
number 52. The resulting metadata SHA-256 is
`80dfebcb74623c8a65380fc37c8e3e5abd59f9921146fb7c210fa56c3b0b7737`.

| AUX-bearing reference | Eligible occurrences | Old flags include | Category flags include |
|---|---:|---:|---:|
| EWT dev | 207 | 204 | 204 |
| EWT test | 233 | 232 | 231 |
| GUM test | 206 | 197 | 198 |

One EWT loss fails the frozen per-source gate despite the GUM gain. This is
grammar-flag inclusion, not runtime restoration or typing accuracy. Reused GUM
genres and verbal/nonverbal ambiguity are retained in
`wiktionary-grammar-source/reference.json`. No full-pipeline or phone pass is
claimed for this version. Four collector fixtures pass, covering continued empty
pages, graph cycles, pagination cycles and malformed/duplicate memberships.

Category membership can include dialectal, historical and non-AUX lexical
contractions. Neither inclusion nor omission is conclusive evidence of safe
mixed-language intent. The subsequent general productive-pronoun proposal has
its own frozen plan and [separate outcome](PRODUCTIVE-GRAMMAR-RESULTS.md).

The contributor notice states CC BY-SA 4.0/GFDL for original material. This audit
collects titles/membership only, not entry prose or quotations. Captures, URLs,
timestamps and response hashes remain local. Source review remains open; this
experiment does not grant production or redistribution clearance.
