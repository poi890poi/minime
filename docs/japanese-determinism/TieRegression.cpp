// Exercises the actual upstream comparator, not an imitation.
#include "path_algorithm/find_path.cpp"
#include <iostream>

int main() {
    kk::Node node;node.sPos=0;node.len=1;
#ifdef ORIGINAL
    auto a=std::make_shared<kk::State>(&node,0,0,nullptr);
    auto b=std::make_shared<kk::State>(&node,0,0,nullptr);
#else
    auto a=std::make_shared<kk::State>(&node,0,0,nullptr,0);
    auto b=std::make_shared<kk::State>(&node,0,0,nullptr,0);
#endif
    // Give the higher-address state earlier logical insertion priority.
    auto first=a.get()>b.get()?a:b;auto second=a.get()>b.get()?b:a;
#ifndef ORIGINAL
    first->ordinal=0;second->ordinal=1;
#endif
    std::priority_queue<std::shared_ptr<kk::State>,std::vector<std::shared_ptr<kk::State>>,kk::StateLess> q;
    q.push(second);q.push(first);
    if(q.top()!=first){std::cout<<"FAIL allocation affects equal-cost priority\n";return 1;}
    second->total=-1;q={};q.push(first);q.push(second);
    if(q.top()!=second)return 2;
    std::cout<<"PASS insertion order is stable; lower cost retains priority\n";
}
