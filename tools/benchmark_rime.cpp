// Evaluation-only bridge to the public librime C API. No probe labels feed Rime.
#include <rime_api.h>
#include <chrono>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>
#ifdef _WIN32
#include <windows.h>
#endif

int main(int argc, char** argv) {
    if(argc!=5) {std::cerr << "shared user inputs.tsv output.tsv\n";return 2;}
#ifdef _WIN32
    auto library=LoadLibraryA("rime.dll");
    if(!library){std::cerr<<"Cannot load rime.dll: "<<GetLastError()<<'\n';return 6;}
    auto getApi=reinterpret_cast<RimeApi*(*)()>(GetProcAddress(library,"rime_get_api"));
    if(!getApi)return 7;
    RimeApi* api=getApi();
#else
    RimeApi* api=rime_get_api();
#endif
    RIME_STRUCT(RimeTraits, traits);
    traits.shared_data_dir=argv[1]; traits.user_data_dir=argv[2];
    traits.app_name="rime.minime-evaluation";traits.min_log_level=2;traits.log_dir="";
    api->setup(&traits);api->initialize(&traits);
    if(api->start_maintenance(true)) api->join_maintenance_thread();
    std::ifstream input(argv[3]);std::ofstream output(argv[4]);
    output << "case\tcontext\tinput\texpected\trank\tmicroseconds\ttop5\n";
    int count=0,top1=0,top5=0;std::string line;
    while(std::getline(input,line)) {
        if(line.empty() || line[0]=='#')continue;
        if(line.back()=='\r')line.pop_back();
        std::vector<std::string> p;std::stringstream row(line);std::string value;
        while(std::getline(row,value,'\t'))p.push_back(value);
        if(p.size()!=4)return 3;
        auto session=api->create_session();
        if(!api->select_schema(session,"luna_pinyin"))return 4;
        auto start=std::chrono::steady_clock::now();
        for(char c:p[2])api->process_key(session,c,0);
        RIME_STRUCT(RimeContext, context);
        if(!api->get_context(session,&context))return 5;
        auto us=std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now()-start).count();
        int rank=0;std::string first;
        for(int i=0;i<context.menu.num_candidates;i++) {
            std::string text=context.menu.candidates[i].text;
            if(text==p[3] && !rank)rank=i+1;
            if(i<5){if(i)first+=" | ";first+=text;}
        }
        output<<p[0]<<'\t'<<p[1]<<'\t'<<p[2]<<'\t'<<p[3]<<'\t'<<rank<<'\t'<<us<<'\t'<<first<<'\n';
        count++;if(rank==1)top1++;if(rank && rank<=5)top5++;
        api->free_context(&context);api->destroy_session(session);
    }
    output<<"# top1="<<top1<<'/'<<count<<" top5="<<top5<<'/'<<count<<'\n';
    std::cout<<"top1="<<top1<<'/'<<count<<" top5="<<top5<<'/'<<count<<'\n';
    api->finalize();return 0;
}
