"""Recover only uniquely determined syllable boundaries; never rewrite letters."""
import functools,re

def unique_boundaries(reading,syllables,count):
    """Respect every supplied separator and exactly `count` spoken units.

    Keep at most two solutions: ambiguity must fail closed, not choose a spelling
    by frequency, dictionary name or incidental traversal order.
    """
    pieces=re.split(r"[ '\-]+",reading)
    if not pieces or any(not re.fullmatch('[a-zv]+',p) for p in pieces) or count<1:
        return None
    compact=''.join(pieces);boundaries=set();at=0
    for piece in pieces[:-1]:at+=len(piece);boundaries.add(at)
    longest=max(map(len,syllables),default=0)
    @functools.lru_cache(None)
    def split(start,remaining):
        if not remaining:return ((),) if start==len(compact) else ()
        if len(compact)-start<remaining or len(compact)-start>remaining*longest:return ()
        end_limit=min([len(compact),start+longest]+[b for b in boundaries if b>start])
        found=[]
        for end in range(start+1,end_limit+1):
            unit=compact[start:end]
            if unit not in syllables:continue
            for suffix in split(end,remaining-1):
                found.append((unit,)+suffix)
                if len(found)==2:return tuple(found)
        return tuple(found)
    found=split(0,count)
    return "'".join(found[0]) if len(found)==1 else None
