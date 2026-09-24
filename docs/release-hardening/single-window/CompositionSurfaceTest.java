package dev.minime.ime;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.*;
import android.view.inspector.WindowInspector;
import android.view.inputmethod.BaseInputConnection;
import android.widget.FrameLayout;
import java.io.*;
import org.json.JSONObject;

/** Real-window geometry and OS touch routing, independent of decoder labels. */
public final class CompositionSurfaceTest extends KeyboardInteractionTest {
    public void testLandscapeRecreationKeepsBufferGeometryAndTouchRouting()throws Exception {
        android.content.Context context=getInstrumentation().getTargetContext();
        String auto=android.provider.Settings.System.getString(context.getContentResolver(),android.provider.Settings.System.ACCELEROMETER_ROTATION);
        String prior=android.provider.Settings.System.getString(context.getContentResolver(),android.provider.Settings.System.USER_ROTATION);
        JSONObject savedRotation=new JSONObject().put("accelerometer_rotation",auto).put("user_rotation",prior);
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(activity.getExternalFilesDir(null),"composition-rotation-before.json")),java.nio.charset.StandardCharsets.UTF_8)){out.write(savedRotation.toString(2));}
        android.app.Instrumentation.ActivityMonitor monitor=getInstrumentation().addMonitor(EditorTestActivity.class.getName(),null,false);
        try {
            assertTrue(getInstrumentation().getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_FREEZE_90));
            android.app.Activity recreated=monitor.waitForActivityWithTimeout(6000);assertNotNull("Editor recreated for landscape",recreated);
            activity=(EditorTestActivity)recreated;
            long until=SystemClock.uptimeMillis()+6000;
            while(!activity.hasWindowFocus() && SystemClock.uptimeMillis()<until)SystemClock.sleep(50);
            assertTrue("Recreated editor owns window",activity.hasWindowFocus());
            assertEquals(android.content.res.Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation);
            getInstrumentation().runOnMainSync(()-> {
                activity.text.setText("");activity.text.requestFocus();
                android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.restartInput(activity.text);imm.showSoftInput(activity.text,0);
            });
            node("Space").recycle();SystemClock.sleep(500);
            testBufferGeometryRawTapAndBlankTouchThrough();
        } finally {
            getInstrumentation().getUiAutomation().setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);
            getInstrumentation().removeMonitor(monitor);
            restoreRotationSetting("user_rotation",prior);
            restoreRotationSetting("accelerometer_rotation",auto);
            assertEquals("Auto rotation unchanged",auto,android.provider.Settings.System.getString(context.getContentResolver(),android.provider.Settings.System.ACCELEROMETER_ROTATION));
            assertEquals("User rotation unchanged",prior,android.provider.Settings.System.getString(context.getContentResolver(),android.provider.Settings.System.USER_ROTATION));
        }
    }
    private void restoreRotationSetting(String key,String value)throws Exception {
        if(value!=null && !value.matches("[0-3]"))throw new IllegalStateException("Unexpected rotation setting");
        String command=value==null?"settings delete system "+key:"settings put system "+key+" "+value;
        try(android.os.ParcelFileDescriptor fd=getInstrumentation().getUiAutomation().executeShellCommand(command);
            InputStream stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(stream.read()!=-1){}}
    }
    private KeyboardView keyboard(View view) {
        if(view instanceof KeyboardView)return (KeyboardView)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            KeyboardView found=keyboard(((ViewGroup)view).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    private KeyboardView keyboard() {
        KeyboardView[] result={null};getInstrumentation().runOnMainSync(()-> {
            for(View root:WindowInspector.getGlobalWindowViews()) {
                KeyboardView found=keyboard(root);if(found!=null && found.isShown()){result[0]=found;break;}
            }
        });assertNotNull("Attached visible keyboard",result[0]);return result[0];
    }
    private Rect bounds(View view) {
        Rect result=new Rect();getInstrumentation().runOnMainSync(()-> {
            int[] at=new int[2];view.getLocationOnScreen(at);result.set(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight());
        });return result;
    }
    private int editorImeInset() {
        int[] result={-1};getInstrumentation().runOnMainSync(()-> {
            WindowInsets insets=activity.getWindow().getDecorView().getRootWindowInsets();assertNotNull(insets);
            result[0]=insets.getInsets(WindowInsets.Type.ime()).bottom;
        });return result[0];
    }
    private void tap(float x,float y) {
        long down=SystemClock.uptimeMillis();
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}) {
            MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            try{assertTrue(getInstrumentation().getUiAutomation().injectInputEvent(event,true));}finally{event.recycle();}
            SystemClock.sleep(40);
        }
        getInstrumentation().waitForIdleSync();
    }
    public void testBufferGeometryRawTapAndBlankTouchThrough()throws Exception {
        KeyboardView keyboard=keyboard();Rect initial=bounds(keyboard);
        Rect editor=bounds(activity.getWindow().getDecorView());
        int initialInset=editorImeInset();assertTrue("Editor has an IME inset",initialInset>0);
        for(char key:"nihaopronunciation".toCharArray()) {
            click(String.valueOf(key));
            assertEquals("Typing must not move or resize the keyboard",initial,bounds(keyboard));
            assertEquals("Typing must not resize or pan the editor",editor,bounds(activity.getWindow().getDecorView()));
            assertEquals("Reported editor resize boundary stays fixed",initialInset,editorImeInset());
        }
        View annotation=keyboard.compositionAnnotation();Rect chip=bounds(annotation);
        assertEquals("Buffer touches keyboard top",initial.top,chip.bottom);
        assertTrue("Buffer has usable width",chip.width()>0);
        getInstrumentation().runOnMainSync(()->assertSame("No second window for raw spelling",keyboard.getRootView(),annotation.getRootView()));
        Rect raw=new Rect();android.view.accessibility.AccessibilityNodeInfo node=node("Exact input nihaopronunciation");node.getBoundsInScreen(raw);node.recycle();
        tap(raw.exactCenterX(),raw.exactCenterY());
        getInstrumentation().runOnMainSync(()-> {
            assertEquals("Raw chip commits exact spelling","nihaopronunciation",activity.text.getText().toString());
            assertEquals(-1,BaseInputConnection.getComposingSpanStart(activity.text.getText()));
        });
        assertEquals(initial,bounds(keyboard));assertEquals(editor,bounds(activity.getWindow().getDecorView()));
        click("n");chip=bounds(annotation);
        final int x=initial.right-32,y=chip.centerY();assertTrue("Probe is outside visible chip",x>chip.right+10);
        View target=new View(activity);int[] hits={0};
        getInstrumentation().runOnMainSync(()-> {
            View content=activity.findViewById(android.R.id.content);int[] at=new int[2];content.getLocationOnScreen(at);
            FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(32,24);params.leftMargin=x-at[0]-16;params.topMargin=y-at[1]-12;
            target.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_UP)hits[0]++;return true;});
            target.setBackgroundColor(android.graphics.Color.MAGENTA);
            activity.addContentView(target,params);
        });
        getInstrumentation().waitForIdleSync();
        try {
            assertTrue(bounds(target).contains(x,y));SystemClock.sleep(100);
            android.graphics.Bitmap probe=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(probe);
            try{assertEquals("Blank host is transparent to editor content",android.graphics.Color.MAGENTA,probe.getPixel(x,y));}finally{probe.recycle();}
            tap(x,y);assertEquals("Blank annotation pixels route touches to the editor window",1,hits[0]);
        }
        finally{getInstrumentation().runOnMainSync(()->((ViewGroup)target.getParent()).removeView(target));}
        JSONObject result=new JSONObject().put("keyboard",initial.toShortString()).put("editor",editor.toShortString())
            .put("blankTouchHits",hits[0]).put("rawTapExact",true).put("singleWindow",true);
        result.put("editorImeInset",initialInset).put("blankTransparent",true);
        try(Writer out=new OutputStreamWriter(new FileOutputStream(new File(activity.getExternalFilesDir(null),"composition-surface.json")),java.nio.charset.StandardCharsets.UTF_8)){out.write(result.toString(2));}
        android.graphics.Bitmap image=getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        try(FileOutputStream out=new FileOutputStream(new File(activity.getExternalFilesDir(null),"composition-surface.png"))){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
    }
}
