// Evaluation adapter. Upstream MIT CLI supplies UTF conversion helpers only;
// the graph and A* are the unmodified pinned implementation. No reference labels.
#define main upstream_cli_main
#include "cli/kana_kanji/astar_bunsetsu_cli.cpp"
#undef main
#include <chrono>

int main(int argc, char** argv) {
    if(argc!=2)return 2;
    using Clock=std::chrono::steady_clock;
    auto begin=Clock::now(); std::string dir=argv[1];
    const auto yomiCps=LOUDSReaderUtf16::loadFromFile(dir+"/yomi_termid.louds");
    const auto yomiTrie=LOUDSWithTermIdUtf16::loadFromFile(dir+"/yomi_termid.louds");
    const LOUDSWithTermIdReaderUtf16 yomiTerm(yomiTrie);
    const auto tango=LOUDSReaderUtf16::loadFromFile(dir+"/tango.louds");
    const auto tokens=TokenArray::loadFromFile(dir+"/token_array.bin");
    const auto pos=kk::PosTable::loadFromFile(dir+"/pos_table.bin");
    const auto connVec=ConnectionIdBuilder::readShortArrayFromBytesBE(dir+"/connection_single_column.bin");
    const kk::ConnectionMatrix conn(std::vector<int16_t>(connVec.begin(),connVec.end()));
    std::cout<<"READY\t"<<std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now()-begin).count()<<std::endl;
    std::string line;
    while(std::getline(std::cin,line)) {
        if(!line.empty()&&line.back()=='\r')line.pop_back();
        std::u16string q; if(!utf8_to_u16(line,q))return 3;
        begin=Clock::now();
        auto graph=kk::GraphBuilder::constructGraph(q,yomiCps,yomiTerm,tokens,pos,tango,kk::YomiSearchMode::CommonPrefixOnly,1);
        auto [candidates,boundaries]=kk::FindPath::backwardAStarWithBunsetsu(graph,static_cast<int>(q.size()),conn,8,50);
        auto elapsed=std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now()-begin).count();
        std::cout<<elapsed;
        for(const auto& c:candidates) {std::string s;if(!u16_to_utf8(c.string,s))return 4;std::cout<<'\t'<<s;}
        std::cout<<std::endl;
    }
}
