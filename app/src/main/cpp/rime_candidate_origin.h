#pragma once
#include <rime/candidate.h>
#include <string>

// Preserve native provenance through simplifier/uniquifier wrappers. A duplicate
// with any lexical origin is attested; its first origin need not be that origin.
inline bool minimeConstructed(const rime::an<rime::Candidate>& candidate, int depth=0) {
    if(!candidate || depth>16)return true;
    if(auto unique=std::dynamic_pointer_cast<rime::UniquifiedCandidate>(candidate)) {
        for(const auto& item:unique->items())if(!minimeConstructed(item,depth+1))return false;
        return true;
    }
    if(auto shadow=std::dynamic_pointer_cast<rime::ShadowCandidate>(candidate))
        return minimeConstructed(shadow->item(),depth+1);
    return candidate->type()=="sentence";
}
inline std::string minimeCandidateRecord(const rime::an<rime::Candidate>& candidate) {
    return std::to_string(candidate->end())+"\t"+(minimeConstructed(candidate)?"S\t":"L\t")+candidate->text();
}
