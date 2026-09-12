#include <jni.h>
#include <rime_api.h>
#include <rime/service.h>
#include <rime/context.h>
#include <rime/composition.h>
#include <rime/menu.h>
#include <rime/candidate.h>
#include "rime_candidate_origin.h"
#include <mutex>
#include <string>
#include <vector>

namespace {
std::mutex lock;
bool ready=false;
// An empty session owns the shared model cache; query sessions retain no input.
RimeSessionId modelOwner=0;
struct Session {
    RimeApi* api=rime_get_api();
    RimeSessionId id=api->create_session();
    ~Session() {if(id)api->destroy_session(id);}
};
std::string utf8(JNIEnv* env,jstring value) {
    const char* chars=env->GetStringUTFChars(value,nullptr);
    if(!chars)return {};
    std::string result(chars);env->ReleaseStringUTFChars(value,chars);return result;
}
}
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_minime_ime_RimeBackend_initialize(JNIEnv* env,jclass,jstring shared,jstring user) {
    std::lock_guard<std::mutex> guard(lock);
    if(ready)return true;
    try {
    auto s=utf8(env,shared),u=utf8(env,user);
    RIME_STRUCT(RimeTraits,traits);
    traits.shared_data_dir=s.c_str();traits.user_data_dir=u.c_str();
    traits.prebuilt_data_dir=s.c_str();
    traits.app_name="rime.minime";traits.min_log_level=3;traits.log_dir="";
    auto api=rime_get_api();api->setup(&traits);api->initialize(&traits);
    auto id=api->create_session();
    ready=api->select_schema(id,"luna_pinyin");
    if(ready)modelOwner=id;
    else {api->destroy_session(id);api->finalize();}
    return ready;
    } catch(const std::exception&) {ready=false;return false;}
}
extern "C" JNIEXPORT jobjectArray JNICALL
Java_dev_minime_ime_RimeBackend_query(JNIEnv* env,jclass,jstring raw,jboolean includePrefixes) {
    std::lock_guard<std::mutex> guard(lock);
    std::vector<std::string> words,prefixes;
    auto input=utf8(env,raw);
    try {
    if(ready && !input.empty() && input.size()<=96) {
        Session owned;auto api=owned.api;auto id=owned.id;
        if(api->select_schema(id,"luna_pinyin")) {
            api->set_input(id,input.c_str());
            auto session=rime::Service::instance().GetSession(id);
            auto context=session?session->context():nullptr;
            if(context && context->input()==input && !context->composition().empty()) {
                auto& segment=context->composition().back();
                if(segment.menu)for(size_t i=0;i<100 && (includePrefixes || words.size()<24);i++) {
                    auto candidate=segment.menu->GetCandidateAt(i);
                    if(!candidate)break;
                    if(candidate->start()!=0 || candidate->end()==0 || candidate->end()>input.size())continue;
                    if(candidate->end()==input.size() && words.size()<24)
                        words.push_back(minimeCandidateRecord(candidate));
                    else if(includePrefixes && candidate->end()<input.size() && prefixes.size()<12)
                        prefixes.push_back(minimeCandidateRecord(candidate));
                }
            }
        }
    }
    } catch(const std::exception&) {words.clear();prefixes.clear();}
    // Keep full-phrase order and default intact, while exposing a few prefix choices.
    size_t preview=std::min<size_t>(3,prefixes.size());
    words.insert(words.begin()+std::min<size_t>(3,words.size()),prefixes.begin(),prefixes.begin()+preview);
    words.insert(words.end(),prefixes.begin()+preview,prefixes.end());
    auto result=env->NewObjectArray(words.size(),env->FindClass("java/lang/String"),nullptr);
    for(size_t i=0;i<words.size();i++) {
        // UTF-8 can contain supplementary Han glyphs; Java decodes standard UTF-8.
        auto bytes=env->NewByteArray(words[i].size());
        env->SetByteArrayRegion(bytes,0,words[i].size(),reinterpret_cast<const jbyte*>(words[i].data()));
        auto cls=env->FindClass("java/lang/String");
        auto constructor=env->GetMethodID(cls,"<init>","([BLjava/lang/String;)V");
        auto charset=env->NewStringUTF("UTF-8");
        auto text=env->NewObject(cls,constructor,bytes,charset);
        env->SetObjectArrayElement(result,i,text);
        env->DeleteLocalRef(bytes);env->DeleteLocalRef(text);env->DeleteLocalRef(charset);env->DeleteLocalRef(cls);
    }
    return result;
}
