// Evaluation bridge to the pinned public Rime C API; no Android build or device.
// Prefix endpoints are obtained independently from the highlighted preedit's raw
// suffix, then checked against the Android adapter's recorded candidate lists.
#include <rime_api.h>
#include <windows.h>
#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include <regex>
#include <io.h>
#include <fcntl.h>
struct Word {size_t end;std::string text;};
int main(int argc,char** argv) {
    if(argc!=4 && argc!=5)return 2;
    bool legacyOrder=!(argc==5 && std::string(argv[4])=="--natural-order");
    _setmode(_fileno(stdout),_O_BINARY);
    auto dll=LoadLibraryA(argv[1]);if(!dll)return 3;
    auto entry=reinterpret_cast<RimeApi*(*)()>(GetProcAddress(dll,"rime_get_api"));if(!entry)return 4;
    auto api=entry();if(!RIME_API_AVAILABLE(api,highlight_candidate))return 5;
    RIME_STRUCT(RimeTraits,traits);traits.shared_data_dir=argv[2];traits.prebuilt_data_dir=argv[2];traits.user_data_dir=argv[3];
    traits.app_name="rime.minime-desktop-test";traits.min_log_level=3;traits.log_dir="";
    api->setup(&traits);api->initialize(&traits);
    auto owner=api->create_session();if(!api->select_schema(owner,"luna_pinyin"))return 6;
    std::cout<<"READY "<<api->get_version()<<std::endl;
    std::string input;
    while(std::getline(std::cin,input)) {
        if(!input.empty() && input.back()=='\r')input.pop_back();
        std::vector<Word> full,prefix,ordered;
        if(input.size()<=96 && std::regex_match(input,std::regex("[a-zv]+('[a-zv]+)*"))) {
            auto id=api->create_session();api->select_schema(id,"luna_pinyin");api->set_option(id,"soft_cursor",false);api->set_input(id,input.c_str());
            RIME_STRUCT(RimeContext,initial);
            if(api->get_context(id,&initial)) {
                std::vector<std::string> texts;
                for(int i=0;i<initial.menu.num_candidates && i<100;i++)texts.emplace_back(initial.menu.candidates[i].text);
                api->free_context(&initial);
                for(size_t i=0;i<texts.size();i++) {
                    if(i>0 && !api->highlight_candidate(id,i))break;
                    RIME_STRUCT(RimeContext,ctx);if(!api->get_context(id,&ctx))break;
                    int suffix=ctx.composition.length-ctx.composition.sel_end;
                    // A single fresh ASCII composition has no confirmed prefix.
                    if(ctx.composition.sel_start==0 && suffix>=0 && static_cast<size_t>(suffix)<input.size()) {
                        size_t end=input.size()-suffix;
                        if(end==input.size() && full.size()<24) {full.push_back({end,texts[i]});ordered.push_back(full.back());}
                        else if(end<input.size() && prefix.size()<12) {prefix.push_back({end,texts[i]});ordered.push_back(prefix.back());}
                    }
                    api->free_context(&ctx);
                }
            }
            api->destroy_session(id);
        }
        if(legacyOrder) {
            size_t preview=std::min<size_t>(3,prefix.size());
            full.insert(full.begin()+std::min<size_t>(3,full.size()),prefix.begin(),prefix.begin()+preview);
            full.insert(full.end(),prefix.begin()+preview,prefix.end());
        } else full=std::move(ordered);
        for(auto& word:full)std::cout<<word.end<<'\t'<<word.text<<'\n';
        std::cout<<"END"<<std::endl;
    }
    api->destroy_session(owner);api->finalize();return 0;
}
