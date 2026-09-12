// Read-only instrumentation of the pinned Rime 1.16.1 C++ candidate metadata.
// Built with MSVC against the pinned MSVC DLL; cross-check every page with C API.
#include <rime_api.h>
#include <rime/service.h>
#include <rime/engine.h>
#include <rime/context.h>
#include <rime/composition.h>
#include <rime/menu.h>
#include <rime/candidate.h>
#include <rime/gear/translator_commons.h>
#include "../app/src/main/cpp/rime_candidate_origin.h"
#include <iostream>
#include <iomanip>
#include <chrono>
#include <regex>
#include <io.h>
#include <fcntl.h>
// The DLL exports Service but not this accessor. Exact implementation from the
// pinned BSD-licensed librime src/rime/service.cc, copyright RIME Developers.
#ifndef MINIME_STATIC_RIME
namespace rime {Context* Session::context() const {return engine_?engine_->active_engine()->context():nullptr;}}
#endif
static rime::an<rime::Candidate> genuine(rime::an<rime::Candidate> c) {
    for(int i=0;i<8;i++) {
        if(auto u=std::dynamic_pointer_cast<rime::UniquifiedCandidate>(c)){c=u->items().front();continue;}
        if(auto s=std::dynamic_pointer_cast<rime::ShadowCandidate>(c)){c=s->item();continue;}
        return c;
    }
    return c;
}
int main(int argc,char** argv) {
    if(argc==2 && std::string(argv[1])=="--self-test") {
        auto assembled=std::make_shared<rime::SimpleCandidate>("sentence",0,4,"test");
        auto lexical=std::make_shared<rime::SimpleCandidate>("phrase",0,4,"test");
        auto shadow=std::make_shared<rime::ShadowCandidate>(assembled,"simplified");
        auto unique=std::make_shared<rime::UniquifiedCandidate>(shadow,"uniquified");
        if(!minimeConstructed(unique) || minimeConstructed(lexical))return 7;
        unique->Append(lexical);
        if(minimeConstructed(unique) || minimeCandidateRecord(unique)!="4\tL\ttest")return 8;
        std::cout<<"PASS wrapped construction and lexical duplicate provenance"<<std::endl;return 0;
    }
    if(argc!=3 && argc!=4)return 2;
    // Existing DesktopEvaluation supplies DLL/model/user; linking is pinned at
    // build time. Two arguments retain the detailed research telemetry protocol.
    bool android=argc==4;
    const char* model=argv[android?2:1];const char* user=argv[android?3:2];
    _setmode(_fileno(stdout),_O_BINARY);
    auto api=rime_get_api();
    RIME_STRUCT(RimeTraits,t);t.shared_data_dir=model;t.prebuilt_data_dir=model;t.user_data_dir=user;t.app_name="rime.minime-metadata";t.min_log_level=3;t.log_dir="";
    api->setup(&t);api->initialize(&t);
    auto owner=api->create_session();if(!api->select_schema(owner,"luna_pinyin"))return 3;
    std::cout<<"READY "<<api->get_version()<<std::endl;
    std::string input;
    const std::regex pinyin("[a-zv]+('[a-zv]+)*");
    while(std::getline(std::cin,input)) {
        if(!input.empty()&&input.back()=='\r')input.pop_back();
        if(android && (input.size()>96 || !std::regex_match(input,pinyin))) {
            std::cout<<"END"<<std::endl;continue;
        }
        auto start=std::chrono::steady_clock::now();
        std::vector<std::string> full,prefix;
        auto id=api->create_session();api->select_schema(id,"luna_pinyin");api->set_input(id,input.c_str());
        RIME_STRUCT(RimeContext,apiContext);
        if(!api->get_context(id,&apiContext))return 4;
        auto session=rime::Service::instance().GetSession(id);auto ctx=session?session->context():nullptr;
        if(ctx && !ctx->composition().empty()) {
            auto& segment=ctx->composition().back();
            if(segment.menu) {
                auto page=segment.menu->CreatePage(100,0);
                if(page) {
                    if(page->candidates.size()!=static_cast<size_t>(apiContext.menu.num_candidates))return 5;
                    for(size_t i=0;i<page->candidates.size();i++) {
                        auto c=page->candidates[i];
                        if(c->text()!=apiContext.menu.candidates[i].text)return 6;
                        if(android) {
                            if(c->start()!=0 || c->end()==0 || c->end()>input.size())continue;
                            if(c->end()==input.size() && full.size()<24)full.push_back(minimeCandidateRecord(c));
                            else if(c->end()<input.size() && prefix.size()<12)prefix.push_back(minimeCandidateRecord(c));
                            continue;
                        }
                        auto g=genuine(c);auto phrase=std::dynamic_pointer_cast<rime::Phrase>(g);auto sentence=std::dynamic_pointer_cast<rime::Sentence>(g);
                        std::cout<<c->end()<<'\t'<<g->type()<<'\t'<<std::setprecision(17)<<c->quality()<<'\t'<<(phrase?phrase->weight():0)<<'\t'<<(sentence?sentence->size():1)<<'\t'<<c->text()<<'\t';
                        if(sentence)for(auto& e:sentence->components())std::cout<<e.text<<"|";
                        std::cout<<'\n';
                    }
                    delete page;
                }
            }
        }
        api->free_context(&apiContext);api->destroy_session(id);
        if(android) {
            size_t preview=std::min<size_t>(3,prefix.size());
            full.insert(full.begin()+std::min<size_t>(3,full.size()),prefix.begin(),prefix.begin()+preview);
            full.insert(full.end(),prefix.begin()+preview,prefix.end());
            for(const auto& row:full)std::cout<<row<<'\n';
            std::cout<<"END"<<std::endl;
        } else std::cout<<"END\t"<<std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now()-start).count()<<std::endl;
    }
    api->destroy_session(owner);api->finalize();
}
