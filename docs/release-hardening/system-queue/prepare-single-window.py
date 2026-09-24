"""Build a production-equivalent source overlay for the single-window trial."""
import difflib
import hashlib
import json
import shutil
from pathlib import Path

root = Path(__file__).resolve().parents[3]
out = root / 'artifacts/single-window'
if out.exists():
    raise ValueError('Refusing to overwrite a trial')
out.mkdir()
shutil.copytree(root / 'app/src/main/java', out / 'main')
shutil.copytree(root / 'app/src/androidTest/java', out / 'test')
shutil.copyfile(root / 'docs/release-hardening/single-window/CompositionSurfaceTest.java',
                out / 'test/dev/minime/ime/CompositionSurfaceTest.java')

def once(text, before, after):
    if text.count(before) != 1:
        raise ValueError('Changed source anchor: ' + before[:80])
    return text.replace(before, after, 1)

path = out / 'main/dev/minime/ime/KeyboardView.java'
before = path.read_text(encoding='utf-8')
text = once(before, '    private final PopupWindow annotationWindow;', '    private final FrameLayout annotationHost;')
text = once(text, '    private final Runnable placeAnnotation=this::placeAnnotation;\n    private int annotationX=-1,annotationY=-1,annotationWidth=-1;\n', '')
start = text.index('        annotationWindow=new PopupWindow(')
end = text.index('        strip=new LinearLayout', start)
text = text[:start] + '''        annotationHost=new FrameLayout(context);
        annotationHost.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(24)));
        annotationHost.addView(annotation,new FrameLayout.LayoutParams(-2,-1,Gravity.TOP|Gravity.LEFT));
        annotation.setVisibility(INVISIBLE);
''' + text[end:]
text = once(text, 'setPadding(bars.left,0,bars.right,bars.bottom); return insets;',
    'setPadding(bars.left,0,bars.right,bars.bottom);\n            annotationHost.setPadding(bars.left,0,bars.right,0);return insets;')
start = text.index('    private void queueAnnotation() {')
end = text.index('    private float[] center(', start)
text = text[:start] + '''    private void queueAnnotation() {
        annotation.setVisibility(inputActive && annotationRequested ? VISIBLE : INVISIBLE);
    }
    View inputSurface() {
        LinearLayout surface=new LinearLayout(getContext());surface.setOrientation(VERTICAL);
        surface.addView(annotationHost);surface.addView(this,new LinearLayout.LayoutParams(-1,-2));
        return surface;
    }
    void computeInputInsets(android.inputmethodservice.InputMethodService.Insets out) {
        if(!isShown())return;
        int[] at=new int[2];getLocationInWindow(at);
        // Exclude the transparent annotation host from editor resize/pan. Its
        // visible raw-text chip participates only in the exact touch region.
        out.contentTopInsets=at[1];out.visibleTopInsets=at[1];
        out.touchableInsets=android.inputmethodservice.InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
        out.touchableRegion.set(0,at[1],getRootView().getWidth(),getRootView().getHeight());
        if(annotation.isShown()) {
            annotation.getLocationInWindow(at);
            out.touchableRegion.op(at[0],at[1],at[0]+annotation.getWidth(),at[1]+annotation.getHeight(),android.graphics.Region.Op.UNION);
        }
    }
''' + text[end:]
text = once(text, '        removeCallbacks(placeAnnotation);annotationWindow.dismiss();\n', '')
path.write_text(text, encoding='utf-8')
(out / 'KeyboardView.patch').write_text(''.join(difflib.unified_diff(before.splitlines(True), text.splitlines(True), fromfile='KeyboardView baseline', tofile='single-window trial')), encoding='utf-8')
service = out / 'main/dev/minime/ime/MiniMeService.java'
original = service.read_text(encoding='utf-8')
text = once(original, '        render(); return keyboard;', '        View surface=keyboard.inputSurface();render();return surface;')
text = once(text, '    @Override public void onStartInput(EditorInfo attribute,boolean restarting) {',
    '''    @Override public void onComputeInsets(Insets outInsets) {
        super.onComputeInsets(outInsets);
        if(keyboard!=null)keyboard.computeInputInsets(outInsets);
    }
    @Override public void onStartInput(EditorInfo attribute,boolean restarting) {''')
service.write_text(text, encoding='utf-8')
(out / 'MiniMeService.patch').write_text(''.join(difflib.unified_diff(original.splitlines(True), text.splitlines(True), fromfile='MiniMeService baseline', tofile='single-window trial')), encoding='utf-8')
init = '''gradle.beforeProject { p ->
    p.plugins.withId('com.android.application') {
        def root=p.rootProject.projectDir
        def android=p.extensions.getByName('android')
        android.sourceSets.main.java.setSrcDirs([new File(root,'artifacts/single-window/main')])
        android.sourceSets.androidTest.java.setSrcDirs([new File(root,'artifacts/single-window/test'),new File(root,'core/src/testSupport/java')])
        android.sourceSets.release.java.srcDir(new File(root,'artifacts/validation-pattern/fixture/java'))
        android.sourceSets.release.manifest.srcFile(new File(root,'artifacts/validation-pattern/fixture/AndroidManifest.xml'))
    }
}
'''
(out / 'fixture.init.gradle').write_text(init, encoding='utf-8')
pins = {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in
        [path, service, out / 'test/dev/minime/ime/CompositionSurfaceTest.java',
         out / 'test/dev/minime/ime/KeyboardInteractionTest.java',
         root / 'docs/release-hardening/SINGLE-WINDOW-PLAN.md']}
(out / 'source-manifest.json').write_text(json.dumps(pins, indent=2) + '\n', encoding='utf-8')
print(json.dumps(pins, indent=2))
