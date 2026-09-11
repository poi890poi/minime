// Evaluation only. The pinned lexical ranking is unchanged; aborted output is discarded.
#define main upstream_cli_main
#include "cli/kana_kanji/astar_bunsetsu_cli.cpp"
#undef main
#include <sstream>

int main(int argc, char** argv) {
    if (argc != 3) return 2;
    const std::string dir = argv[1], mode = argv[2];
    if (mode != "bounded" && mode != "uncapped" && mode != "zero") return 3;
    using Clock = std::chrono::steady_clock;
    auto start = Clock::now();
    const auto yc = LOUDSReaderUtf16::loadFromFile(dir + "/yomi_termid.louds");
    const auto yt = LOUDSWithTermIdUtf16::loadFromFile(dir + "/yomi_termid.louds");
    const LOUDSWithTermIdReaderUtf16 yr(yt);
    const auto tango = LOUDSReaderUtf16::loadFromFile(dir + "/tango.louds");
    const auto tokens = TokenArray::loadFromFile(dir + "/token_array.bin");
    const auto pos = kk::PosTable::loadFromFile(dir + "/pos_table.bin");
    std::cout << "READY\t" << std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now()-start).count() << std::endl;
    std::string line;
    while (std::getline(std::cin, line)) {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        std::u16string q;
        if (!utf8_to_u16(line,q)) return 4;
        bool available = true;
        std::ostringstream capture;
        start = Clock::now();
        PredictionBudget::begin(mode != "uncapped", mode == "zero" ? 0 : 8192);
        auto old = std::cout.rdbuf(capture.rdbuf());
        try {
            PredictionBudget::check();
            print_prediction(yc,yr,tokens,pos,tango,q,static_cast<int>(q.size()),8);
            PredictionBudget::check();
        } catch (const PredictionBudgetExceeded&) { available = false; }
        std::cout.rdbuf(old);
        const auto elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(Clock::now()-start).count();
        std::cout << elapsed << '\t' << available << '\t' << PredictionBudget::used;
        if (available) {
            std::istringstream lines(capture.str()); std::string row;
            while (std::getline(lines,row)) {
                auto a=row.find('\t'); if(a==std::string::npos) continue;
                auto b=row.find('\t',a+1); if(b==std::string::npos) return 5;
                std::cout << '\t' << row.substr(a+1,b-a-1);
            }
        }
        std::cout << std::endl;
    }
}
