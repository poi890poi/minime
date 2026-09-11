#include "path_algorithm/find_path.hpp"
#include <iostream>
int main() {
    kk::Graph graph(3);
    graph[0].emplace_back(0,0,0,0,0,u"BOS",u"",0,0);
    for(int i=0;i<80;i++)graph[1].emplace_back(0,0,0,0,0,std::u16string(1,char16_t(0x400+i)),u"あ",1,0);
    graph[2].emplace_back(0,0,0,0,0,u"EOS",u"",0,1);
    const kk::ConnectionMatrix conn(std::vector<int16_t>{0});
    kk::FindPath::backwardAStarWithBunsetsu(graph,1,conn,8,50);
    if(graph[1].size()!=50)return 2;
    for(int i=0;i<50;i++)if(graph[1][i].tango!=std::u16string(1,char16_t(0x400+i))) {
        std::cout<<"FAIL beam reorders equal-cost dictionary entries\n";return 1;
    }
    std::cout<<"PASS beam preserves dictionary order for equal costs\n";
}
