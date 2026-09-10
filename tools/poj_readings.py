"""Read aligned, attested POJ variants; never synthesize tone permutations."""
import re
import unicodedata


def letters(value):
    value = unicodedata.normalize('NFD', value.lower()).replace('\u0358', 'o').replace('ⁿ', 'nn').replace('ı', 'i')
    value = ''.join(c for c in value if not unicodedata.combining(c))
    return value if re.fullmatch('[a-z -]+', value) else None


def variants(item, source, skipped):
    for suffix in ('', 'Others'):
        keys = item.get('PojInput' + suffix, '').split('/')
        outputs = item.get('PojUnicode' + suffix, '').split('/')
        if len(keys) != len(outputs):
            skipped.append([source, suffix, 'unaligned source variants'])
            continue
        for key, output in zip(keys, outputs):
            key = key.strip().lower()
            output = unicodedata.normalize('NFC', output.strip())
            if not key and not output:
                continue
            plain = letters(output)
            numeric = bool(re.fullmatch('[a-z0-9 -]+', key)) and len(key) <= 96
            if not output or len(output) > 96 or not (plain or numeric):
                skipped.append([source, output, 'unsupported POJ spelling or exceeds composition limit'])
                continue
            # Preserve existing numeric input aliases. A malformed numeric field
            # cannot veto a supported Unicode spelling; its tone-free alias comes
            # from that spelling, with every output tone mark retained verbatim.
            aliases = {plain} if plain else set()
            if numeric:
                aliases.update((key, re.sub('[1-9]', '', key)))
            yield sorted(aliases), output
