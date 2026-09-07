package dev.minime.ime;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public final class SettingsActivity extends Activity {
    private LinearLayout body;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this); body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        int p=(int)(20*getResources().getDisplayMetrics().density); body.setPadding(p,p,p,p); scroll.addView(body); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view,insets)-> {
            android.graphics.Insets bars=insets.getSystemWindowInsets();
            body.setPadding(p+bars.left,p+bars.top,p+bars.right,p+bars.bottom); return insets;
        });
        text("MinIME 注音",28);
        text("Traditional Chinese and English, in one typing flow.",18);
        button("1 · Enable MinIME",()->startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
        button("2 · Choose keyboard",()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker());
        Switch layout=new Switch(this); layout.setText("Show Zhuyin + English layout");
        layout.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("zhuyin",false));
        layout.setOnCheckedChangeListener((b,value)->getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("zhuyin",value).apply()); body.addView(layout);
        text("Typing",21);
        option("Use Rime for Pinyin phrase prediction", "rime_pinyin",false);
        text("Rime offers stronger sentence prediction in our tests, with different candidate ordering. Turn it off to use the original MinIME decoder. It works offline and does not keep its own typing history.",16);
        option("Correct English spelling on Space", "english_correction",false);
        option("Double Space inserts a period in English", "double_space_period",true);
        option("Keep recent emoji on this device", "emoji_recents",false);
        Switch adaptation=new Switch(this);adaptation.setText("Learn English word pairs on this device");
        adaptation.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_learning",false));
        adaptation.setOnCheckedChangeListener((b,value)->getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("english_learning",value).apply());body.addView(adaptation);
        text("Tap EN for English or 中 for Chinese. English offers completions, spelling alternatives and next words. Choosing a completion adds a space before your next word. Optional spelling correction acts on Space; immediate Backspace restores the spelling. Double Space within one second can insert a period. Chinese supports full, initial and mixed Pinyin syllables. The blue candidate is the Space choice; exact input stays at the left. Tap exact input or hold Space to keep the spelling. The down arrow expands candidates into a grid. Tap Shift once for one capital, twice quickly for Caps Lock, and once again to unlock. Capitals also follow the editor's sentence or word setting.",16);
        text("On QWERTY, slide up for a capital and down for the small symbol. In English mode, trace across letters to compose a word; Space accepts it and candidates offer alternatives. On Zhuyin, slide down for lowercase letters or digits and up for capitals or shifted digits. Hold a key for tappable alternatives, or hold Backspace to keep deleting. Space enters first tone on an unfinished Zhuyin syllable; press again to accept. In English mode, Enter accepts spelling and runs the editor action or inserts a newline. In Chinese mode, Enter first accepts composition; the next Enter runs the action or inserts a newline.",16);
        text("Comma and period commit immediately: ，。 in Chinese mode, ,. in English mode and URL/password fields. Slide up for the other width. Hold comma to open emoji. Hold period, drag to a punctuation choice, and release; you can also tap a popup choice. Chinese mode includes a punctuation-width preference. Tap ?123 for symbols; ABC returns to letters. Browse categories or swipe pages. Optional recent emoji stay on this device and are hidden in private input.",16);
        text("Try it",21);
        EditText test=new EditText(this); test.setHint("這個 pronunciation 不對"); test.setMinLines(2); test.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); body.addView(test);
        text("Local dictionary",21);
        text("Add one reading, a tab, and its output per line. Use Pinyin, Zhuyin, or the same literal word in both columns. Entries remain on this device.",16);
        EditText custom=new EditText(this); custom.setHint("minime\tMinIME"); custom.setMinLines(3);
        custom.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        custom.setText(getSharedPreferences("learning",MODE_PRIVATE).getString("custom","")); body.addView(custom);
        EditText reading=new EditText(this); reading.setHint("Reading, e.g. minime");
        reading.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); body.addView(reading);
        EditText output=new EditText(this); output.setHint("Output, e.g. MinIME"); body.addView(output);
        button("Add entry to list",()-> {
            String r=reading.getText().toString().trim(),o=output.getText().toString().trim();
            if(r.isEmpty() || o.isEmpty() || r.length()>96 || o.length()>96 || r.matches("(?s).*[\\t\\r\\n].*") || o.matches("(?s).*[\\t\\r\\n].*")) {
                reading.setError("Enter a reading and output, each up to 96 characters on one line"); return;
            }
            String previous=custom.getText().toString();
            custom.setText(previous+(previous.isEmpty() || previous.endsWith("\n")?"":"\n")+r+"\t"+o);
            reading.setText(""); output.setText("");
        });
        button("Save local dictionary",()-> {
            String value=custom.getText().toString();
            if(value.length()>16000) { custom.setError("Keep the dictionary under 16,000 characters"); return; }
            for(String line:value.split("\n")) if(!line.isEmpty() && !line.matches("[^\t]{1,96}\t[^\t]{1,96}")) { custom.setError("Each line needs reading, tab, output"); return; }
            getSharedPreferences("learning",MODE_PRIVATE).edit().putString("custom",value).apply(); Toast.makeText(this,"Saved on device",Toast.LENGTH_SHORT).show();
        });
        button("Clear learned choices",()-> {
            SharedPreferences data=getSharedPreferences("learning",MODE_PRIVATE); String entries=data.getString("custom","");
            data.edit().clear().putString("custom",entries).apply(); Toast.makeText(this,"Learned choices cleared",Toast.LENGTH_SHORT).show();
        });
        text("Privacy",21);
        text("Fully offline. No network permission, keystroke logs, telemetry, or cloud backup. Explicit choices are learned locally. English word-pair learning and recent emoji are optional and off by default. Password fields use direct input without composition, suggestions or learning. Private fields do not access personalized history. Restart recovery briefly checks only the keyboard's own composing text, up to 96 characters; it does not collect the rest of the editor.",16);
        text("About this prototype",21);
        text("Version 0.4.0. Independent implementation; not a Google product. Optional Rime Pinyin improves sentence prediction in our tests. Long abbreviated sentences still need work. Language foundations: Rime, McBopomofo, AOSP LatinIME, Universal Dependencies and Unicode. See notices for complete sources, authors and licenses.",16);
        button("Open-source notices",()-> {
            try(java.io.InputStream in=getAssets().open("NOTICE.txt")) {
                java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); byte[] b=new byte[4096]; int n;
                while((n=in.read(b))!=-1) out.write(b,0,n);
                new android.app.AlertDialog.Builder(this).setTitle("Notices").setMessage(out.toString("UTF-8")).setPositiveButton("Close",null).show();
            } catch(java.io.IOException e) { Toast.makeText(this,"Notices unavailable",Toast.LENGTH_SHORT).show(); }
        });
    }
    private void text(String text,int size) { TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setPadding(0,14,0,10); body.addView(v); }
    private void option(String label,String key,boolean initial) {
        Switch toggle=new Switch(this);toggle.setText(label);
        toggle.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean(key,initial));
        toggle.setOnCheckedChangeListener((b,value)->getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean(key,value).apply());body.addView(toggle);
    }
    private void button(String label,Runnable action) { Button b=new Button(this); b.setText(label); b.setOnClickListener(v->action.run()); body.addView(b); }
}
