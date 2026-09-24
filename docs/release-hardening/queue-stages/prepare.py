"""Generate an isolated diagnostic app/test overlay; never edit active app sources."""
import difflib,hashlib,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
OUT=ROOT/'artifacts/queue-stages'


def replace_once(text,before,after):
    if text.count(before)!=1:raise ValueError('Diagnostic anchor changed: '+before[:70])
    return text.replace(before,after,1)


def main():
    app=ROOT/'app/src/main/java';test=ROOT/'app/src/androidTest/java'
    source=app/'dev/minime/ime/AsyncDecoder.java'
    original=source.read_text(encoding='utf-8')
    shadow=OUT/'overlay'
    if shadow.exists():raise ValueError('Refusing to overwrite a diagnostic capture')
    shutil.copytree(app,shadow/'main');shutil.copytree(test,shadow/'test')
    text=replace_once(original,'private ScheduledFuture<?> queued;','private ScheduledFuture<?> queued;\n    private QueueTrace.Sample queuedSample;')
    text=replace_once(text,'stats.requests.incrementAndGet();\n        queued=worker.schedule(()-> {\n            List<Candidate> choices=DecodePipeline.run(current,',
        'stats.requests.incrementAndGet();\n        QueueTrace.Sample sample=QueueTrace.begin();queuedSample=sample;\n        QueueTrace.scheduled(sample);\n        queued=worker.schedule(()-> {\n            QueueTrace.worker(sample);\n            List<Candidate> choices;\n            try { choices=DecodePipeline.run(current,')
    text=replace_once(text,'if(choices!=null)deliver(current,choices,result);',
        '} catch(RuntimeException | Error failure){QueueTrace.failed(sample);throw failure;}\n            QueueTrace.providers(sample,choices!=null);\n            if(choices!=null)deliver(current,choices,result,sample);')
    text=replace_once(text,'public void cancel() {requests.cancel();if(queued!=null)queued.cancel(false);}',
        'public void cancel() {requests.cancel();if(queued!=null)queued.cancel(false);QueueTrace.cancel(queuedSample);}')
    old='''    private void deliver(BooleanSupplier current,List<Candidate> choices,Consumer<List<Candidate>> result) {
        main.post(()-> {
            if(!closed && current.getAsBoolean()) {stats.delivered.incrementAndGet();result.accept(choices);}
            else stats.staleDelivery.incrementAndGet();
        });
    }'''
    new='''    private void deliver(BooleanSupplier current,List<Candidate> choices,Consumer<List<Candidate>> result) {
        deliver(current,choices,result,null);
    }
    private void deliver(BooleanSupplier current,List<Candidate> choices,Consumer<List<Candidate>> result,QueueTrace.Sample sample) {
        QueueTrace.posted(sample);
        main.post(()-> {
            QueueTrace.entered(sample);boolean accepted=false;
            try {
                if(!closed && current.getAsBoolean()) {accepted=true;stats.delivered.incrementAndGet();result.accept(choices);}
                else stats.staleDelivery.incrementAndGet();
            } finally {QueueTrace.finished(sample,accepted);}
        });
    }'''
    text=replace_once(text,old,new)
    target=shadow/'main/dev/minime/ime/AsyncDecoder.java';target.write_text(text,encoding='utf-8')
    shutil.copyfile(HERE/'QueueTrace.java',shadow/'main/dev/minime/ime/QueueTrace.java')
    test_file=shadow/'test/dev/minime/ime/TouchLatencyTest.java'
    test_text=test_file.read_text(encoding='utf-8')
    methods='''    public void testChineseQueuesShard0()throws Exception {runQueueReplay("chinese");}
    public void testTaiwaneseQueuesShard0()throws Exception {runQueueReplay("taiwanese_english");}
    public void testJapaneseQueuesShard0()throws Exception {runQueueReplay("japanese_english");}
    private void runQueueReplay(String mode)throws Exception {
        QueueTrace.start();
        try {runReplay(false,mode,0);}
        finally {QueueTrace.write(new File(getInstrumentation().getTargetContext().getExternalFilesDir(null),"candidate-queues.tsv"));}
    }
'''
    test_text=replace_once(test_text,'    public void testTouchToSubmittedFrames()',methods+'    public void testTouchToSubmittedFrames()')
    test_file.write_text(test_text,encoding='utf-8')
    init='''gradle.beforeProject { p ->
    p.plugins.withId('com.android.application') {
        def root=p.rootProject.projectDir
        def android=p.extensions.getByName('android')
        android.sourceSets.main.java.setSrcDirs([new File(root,'artifacts/queue-stages/overlay/main')])
        android.sourceSets.androidTest.java.setSrcDirs([new File(root,'artifacts/queue-stages/overlay/test'),new File(root,'core/src/testSupport/java')])
        android.sourceSets.release.java.srcDir(new File(root,'artifacts/validation-pattern/fixture/java'))
        android.sourceSets.release.manifest.srcFile(new File(root,'artifacts/validation-pattern/fixture/AndroidManifest.xml'))
    }
}
'''
    (OUT/'diagnostic.init.gradle').write_text(init,encoding='utf-8')
    (OUT/'decoder.patch').write_text(''.join(difflib.unified_diff(original.splitlines(True),text.splitlines(True),fromfile='accepted/AsyncDecoder.java',tofile='diagnostic/AsyncDecoder.java')),encoding='utf-8')
    manifest=dict(active_source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                  diagnostic_source_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),
                  observer_sha256=hashlib.sha256((HERE/'QueueTrace.java').read_bytes()).hexdigest(),
                  producer_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  active_sources_unchanged=source.read_text(encoding='utf-8')==original)
    (OUT/'source-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(manifest))


if __name__=='__main__':main()
