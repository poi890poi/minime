#include "token_array/token_array.hpp"
#include <iostream>
#include <stdexcept>

int main(int argc,char** argv) {
    if(argc!=2)return 2;
    const auto t=TokenArray::loadFromFile(argv[1]);
    // Independent one-pass interpretation of the on-disk bit layout.
    size_t begin=0,end=0,lists=0,checked=0;bool haveBegin=false;
    for(size_t bit=0;bit<t.postingsBits.size();++bit) {
        if(t.postingsBits.get(bit)){++end;continue;}
        if(haveBegin) {
            auto entries=t.getTokensForTermId(static_cast<int>(lists));
            if(entries.size()!=end-begin)throw std::runtime_error("posting length differs");
            for(size_t i=0;i<entries.size();++i) {
                auto p=begin+i;auto e=entries[i];
                if(e.posIndex!=t.posIndex[p]||e.wordCost!=t.wordCost[p]||e.nodeIndex!=t.nodeIndex[p])throw std::runtime_error("posting payload differs");
                checked++;
            }
            lists++;
        }
        begin=end;haveBegin=true;
    }
    if(!t.getTokensForTermId(-1).empty()||!t.getTokensForTermId(static_cast<int>(lists)).empty())throw std::runtime_error("boundary behavior differs");
    std::cout<<"PASS posting lists="<<lists<<" tokens="<<checked<<" final_unterminated_tokens="<<(end-begin)<<std::endl;
}
