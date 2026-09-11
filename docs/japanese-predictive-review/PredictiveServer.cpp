// Evaluation only: use pinned upstream implementations without editing them.
#define main upstream_cli_main
#include "cli/kana_kanji/astar_bunsetsu_cli.cpp"
#undef main
#include <chrono>
#include <sstream>

int main(int argc,char** argv) {
    if(argc!=3)return 2;
    const std::string dir=argv[1], mode=argv[2];
    if(mode!="cps"&&mode!="graph"&&mode!="lexical"&&mode!="lexical-fullprefix")return 3;
    using Clock=std::chrono::steady_clock;auto start=Clock::now();
    const auto yc=LOUDSReaderUtf16::loadFromFile(dir+"/yomi_termid.louds");
    const auto yt=LOUDSWithTermIdUtf16::loadFromFile(dir+"/yomi_termid.louds");
    const LOUDSWithTermIdReaderUtf16 yr(yt);
    const auto tango=LOUDSReaderUtf16::loadFromFile(dir+"/tango.louds");
    const auto tokens=TokenArray::loadFromFile(dir+"/token_array.bin");
    const auto pos=kk::PosTable::loadFromFile(dir+"/pos_table.bin");
    const auto cv=ConnectionIdBuilder::readShortArrayFromBytesBE(dir+"/connection_single_column.bin");
    const kk::ConnectionMatrix conn(std::vector<int16_t>(cv.begin(),cv.end()));
    std::cout<<"READY\t"<<std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now()-start).count()<<std::endl;
    std::string line;
    while(std::getline(std::cin,line)) {
        if(!line.empty()&&line.back()=='\r')line.pop_back();
        std::u16string q;if(!utf8_to_u16(line,q))return 4;
        std::vector<std::string> outputs;long nodes=0,mismatch=0;start=Clock::now();
        if(mode=="lexical"||mode=="lexical-fullprefix") {
            // Capture the existing, independent lexical API's top-eight output.
            std::ostringstream capture;auto old=std::cout.rdbuf(capture.rdbuf());
            print_prediction(yc,yr,tokens,pos,tango,q,mode=="lexical"?1:static_cast<int>(q.size()),8);std::cout.rdbuf(old);
            std::istringstream lines(capture.str());std::string row;
            while(std::getline(lines,row)) {
                auto a=row.find('\t');if(a==std::string::npos)continue;
                auto b=row.find('\t',a+1);if(b==std::string::npos)return 5;
                outputs.push_back(row.substr(a+1,b-a-1));
            }
        } else {
            auto graph=kk::GraphBuilder::constructGraph(q,yc,yr,tokens,pos,tango,
                mode=="graph"?kk::YomiSearchMode::CommonPrefixPlusPredictive:kk::YomiSearchMode::CommonPrefixOnly,1);
            auto [candidates,bounds]=kk::FindPath::backwardAStarWithBunsetsu(graph,static_cast<int>(q.size()),conn,8,50);
            for(const auto& c:candidates){std::string s;if(!u16_to_utf8(c.string,s))return 6;outputs.push_back(s);}
            // Auditing occurs after the timed search and is separately excluded below.
            auto end=Clock::now();
            for(const auto& row:graph)for(const auto& n:row)if(!n.yomi.empty()) {
                nodes++;if(n.sPos<0||static_cast<size_t>(n.sPos)+n.yomi.size()>q.size()||q.compare(n.sPos,n.yomi.size(),n.yomi)!=0)mismatch++;
            }
            std::cout<<std::chrono::duration_cast<std::chrono::nanoseconds>(end-start).count()<<'\t'<<nodes<<'\t'<<mismatch;
        }
        if(mode=="lexical"||mode=="lexical-fullprefix")std::cout<<std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now()-start).count()<<"\t0\t0";
        for(const auto& s:outputs)std::cout<<'\t'<<s;
        std::cout<<std::endl;
    }
}
