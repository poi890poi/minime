#pragma once
#include <rime_api.h>
#include <rime/service.h>
#include <rime/context.h>
#include <rime/composition.h>
#include <rime/menu.h>
#include <rime/gear/translator_commons.h>
#include "rime_candidate_origin.h"
#include <algorithm>
#include <vector>

inline bool minimeSingleCharacter(const std::string& text) {
    size_t count=0;
    for(unsigned char c:text)if((c&0xc0)!=0x80)++count;
    return count==1;
}

// A separate bounded character reserve prevents long prefixes consuming the
// entire recovery quota. Shared by Android and the pinned desktop adapter.
class MinimePrefixChoices {
    std::vector<std::string> characters_;
    size_t firstEnd_=0;
public:
    void observe(const rime::an<rime::Candidate>& candidate,size_t inputSize) {
        if(candidate->start()!=0 || candidate->end()==0 || candidate->end()>inputSize)return;
        if(!firstEnd_) {
            auto genuine=rime::Candidate::GetGenuineCandidate(candidate);
            if(auto phrase=std::dynamic_pointer_cast<rime::Phrase>(genuine))
                firstEnd_=phrase->spans().NextStop(0);
        }
        if(candidate->end()<inputSize && minimeSingleCharacter(candidate->text()) && characters_.size()<12)
            characters_.push_back(minimeCandidateRecord(candidate));
    }
    void append(RimeApi* api,const std::string& input,std::vector<std::string>& prefixes) {
        // The first page may contain only full phrases. Ask the same decoder for
        // its first syllable, using its actual raw span (also valid for initials
        // and explicit apostrophes). Never guess segmentation from text length.
        if(characters_.size()<12 && firstEnd_>0 && firstEnd_<input.size()) {
            struct Query {
                RimeApi* api;RimeSessionId id;
                ~Query(){if(id)api->destroy_session(id);}
            } query{api,api->create_session()};
            if(query.id && api->select_schema(query.id,"luna_pinyin")) {
                auto first=input.substr(0,firstEnd_);
                api->set_input(query.id,first.c_str());
                auto session=rime::Service::instance().GetSession(query.id);
                auto context=session?session->context():nullptr;
                if(context && context->input()==first && !context->composition().empty()) {
                    auto menu=context->composition().back().menu;
                    if(menu)for(size_t i=0;i<100 && characters_.size()<12;i++) {
                        auto c=menu->GetCandidateAt(i);if(!c)break;
                        if(c->start()!=0 || c->end()!=firstEnd_ || !minimeSingleCharacter(c->text()))continue;
                        auto record=minimeCandidateRecord(c);
                        if(std::find(characters_.begin(),characters_.end(),record)==characters_.end())characters_.push_back(record);
                    }
                }
            }
        }
        for(const auto& record:characters_)
            if(std::find(prefixes.begin(),prefixes.end(),record)==prefixes.end())prefixes.push_back(record);
    }
};
