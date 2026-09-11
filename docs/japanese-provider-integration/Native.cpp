// Instrumentation-only adapter. Upstream MIT helpers retain their source notice.
#define main upstream_cli_main
#include "cli/kana_kanji/astar_bunsetsu_cli.cpp"
#undef main
#include <jni.h>
#include <chrono>

struct Model {
    LOUDSReaderUtf16 readings;
    LOUDSWithTermIdUtf16 trie;
    LOUDSWithTermIdReaderUtf16 terms;
    LOUDSReaderUtf16 surfaces;
    TokenArray tokens;
    kk::PosTable pos;
    kk::ConnectionMatrix connection;
    jlong elapsed=0;
    explicit Model(const std::string& dir):
        readings(LOUDSReaderUtf16::loadFromFile(dir+"/yomi_termid.louds")),
        trie(LOUDSWithTermIdUtf16::loadFromFile(dir+"/yomi_termid.louds")),terms(trie),
        surfaces(LOUDSReaderUtf16::loadFromFile(dir+"/tango.louds")),
        tokens(TokenArray::loadFromFile(dir+"/token_array.bin")),
        pos(kk::PosTable::loadFromFile(dir+"/pos_table.bin")),
        connection(ConnectionIdBuilder::readShortArrayFromBytesBE(dir+"/connection_single_column.bin")) {}
};
static void fail(JNIEnv* env,const std::exception& error) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),error.what());
}
extern "C" JNIEXPORT jlong JNICALL Java_dev_minime_ime_JapaneseNativeTestProvider_create(JNIEnv* env,jclass,jstring directory) {
    const char* chars=env->GetStringUTFChars(directory,nullptr);
    if(!chars)return 0;
    std::string dir(chars);env->ReleaseStringUTFChars(directory,chars);
    try{return reinterpret_cast<jlong>(new Model(dir));}catch(const std::exception& e){fail(env,e);return 0;}
}
extern "C" JNIEXPORT jobjectArray JNICALL Java_dev_minime_ime_JapaneseNativeTestProvider_query(JNIEnv* env,jclass,jlong handle,jstring input) {
    Model& m=*reinterpret_cast<Model*>(handle);
    const jchar* chars=env->GetStringChars(input,nullptr);if(!chars)return nullptr;
    std::u16string kana(reinterpret_cast<const char16_t*>(chars),env->GetStringLength(input));env->ReleaseStringChars(input,chars);
    try {
        auto start=std::chrono::steady_clock::now();
        auto graph=kk::GraphBuilder::constructGraph(kana,m.readings,m.terms,m.tokens,m.pos,m.surfaces,kk::YomiSearchMode::CommonPrefixOnly,1);
        auto [choices,bounds]=kk::FindPath::backwardAStarWithBunsetsu(graph,static_cast<int>(kana.size()),m.connection,8,50);
        m.elapsed=std::chrono::duration_cast<std::chrono::nanoseconds>(std::chrono::steady_clock::now()-start).count();
        auto result=env->NewObjectArray(static_cast<jsize>(choices.size()),env->FindClass("java/lang/String"),nullptr);
        if(!result)return nullptr;
        for(size_t i=0;i<choices.size();++i){const auto& text=choices[i].string;
            auto value=env->NewString(reinterpret_cast<const jchar*>(text.data()),static_cast<jsize>(text.size()));
            if(!value)return nullptr;env->SetObjectArrayElement(result,static_cast<jsize>(i),value);env->DeleteLocalRef(value);
        }return result;
    }catch(const std::exception& e){fail(env,e);return nullptr;}
}
extern "C" JNIEXPORT jlong JNICALL Java_dev_minime_ime_JapaneseNativeTestProvider_elapsed(JNIEnv*,jclass,jlong handle) {return reinterpret_cast<Model*>(handle)->elapsed;}
extern "C" JNIEXPORT void JNICALL Java_dev_minime_ime_JapaneseNativeTestProvider_destroy(JNIEnv*,jclass,jlong handle) {delete reinterpret_cast<Model*>(handle);}
