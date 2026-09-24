"""Extend the frozen queue overlay with request-ID-only Android trace markers."""
import hashlib,json,shutil
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'artifacts/system-queue'

def once(text,before,after):
    if text.count(before)!=1:raise ValueError('Changed observer anchor: '+before[:60])
    return text.replace(before,after,1)

def main():
    original=ROOT/'artifacts/queue-stages/overlay'
    overlay=OUT/'overlay'
    if overlay.exists():raise ValueError('Refusing to overwrite an experiment')
    shutil.copytree(original,overlay)
    observer=overlay/'main/dev/minime/ime/QueueTrace.java'
    text=observer.read_text(encoding='utf-8')
    text=once(text,'import java.io.*;','import android.os.Trace;\nimport java.io.*;')
    text=once(text,'rows.clear();dropped=0;enabled=true;',
        'if(!Trace.isEnabled())throw new IllegalStateException("System trace is not enabled");rows.clear();dropped=0;enabled=true;')
    text=once(text,'static void posted(Sample s){if(s!=null)s.posted=System.nanoTime();}',
        'static void posted(Sample s){if(s!=null){s.posted=System.nanoTime();Trace.beginAsyncSection("MinIME.decoder.queue."+s.id,s.id);}}')
    text=once(text,'static void entered(Sample s){if(s!=null)s.entered=System.nanoTime();}',
        'static void entered(Sample s){if(s!=null){s.entered=System.nanoTime();Trace.endAsyncSection("MinIME.decoder.queue."+s.id,s.id);Trace.beginSection("MinIME.decoder.callback."+s.id);}}')
    text=once(text,'static void finished(Sample s,boolean accepted){if(s!=null){s.finished=System.nanoTime();s.terminal=accepted?1:3;}}',
        'static void finished(Sample s,boolean accepted){if(s!=null){Trace.endSection();s.finished=System.nanoTime();s.terminal=accepted?1:3;}}')
    observer.write_text(text,encoding='utf-8')
    fixture=OUT/'fixture';fixture.mkdir()
    manifest=(ROOT/'artifacts/validation-pattern/fixture/AndroidManifest.xml').read_text(encoding='utf-8')
    manifest=once(manifest,'<application>','<application>\n        <profileable android:shell="true" />')
    (fixture/'AndroidManifest.xml').write_text(manifest,encoding='utf-8')
    init=(ROOT/'artifacts/queue-stages/diagnostic.init.gradle').read_text(encoding='utf-8')
    init=init.replace('artifacts/queue-stages/overlay/','artifacts/system-queue/overlay/')
    init=init.replace('artifacts/validation-pattern/fixture/AndroidManifest.xml','artifacts/system-queue/fixture/AndroidManifest.xml')
    (OUT/'diagnostic.init.gradle').write_text(init,encoding='utf-8')
    pins={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in
        [observer,overlay/'main/dev/minime/ime/AsyncDecoder.java',fixture/'AndroidManifest.xml',Path(__file__)]}
    (OUT/'source-manifest.json').write_text(json.dumps(pins,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(pins))

if __name__=='__main__':main()
