package dev.minime.ime;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

/** Visible local test editors, excluded from release builds. */
public final class EditorTestActivity extends Activity {
    public EditText text, search, password, url, number;
    public TextView actions;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(20,48,20,12);
        ScrollView scroll=new ScrollView(this); scroll.addView(body); setContentView(scroll);
        TextView title=new TextView(this); title.setText("MinIME · Android editor checks"); title.setTextSize(22); body.addView(title);
        text=field(body,"Mixed text",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,EditorInfo.IME_ACTION_NONE);
        text.setMinLines(2);
        search=field(body,"Search",InputType.TYPE_CLASS_TEXT,EditorInfo.IME_ACTION_SEARCH);
        password=field(body,"Password",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD,EditorInfo.IME_ACTION_DONE);
        url=field(body,"URL",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI,EditorInfo.IME_ACTION_GO);
        number=field(body,"Number",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL,EditorInfo.IME_ACTION_NEXT);
        actions=new TextView(this); actions.setText("No editor action yet"); body.addView(actions);
        text.requestFocus();
    }
    private EditText field(LinearLayout parent,String label,int type,int action) {
        EditText e=new EditText(this); e.setHint(label); e.setContentDescription(label); e.setImeOptions(action);
        if((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE)==0) e.setSingleLine(true);
        e.setInputType(type);
        e.setOnEditorActionListener((v,id,event)-> {
            if(event!=null || id==EditorInfo.IME_ACTION_NONE || id==EditorInfo.IME_ACTION_UNSPECIFIED) return false;
            actions.setText("Editor action: "+id); return true;
        });
        parent.addView(e); return e;
    }
}
