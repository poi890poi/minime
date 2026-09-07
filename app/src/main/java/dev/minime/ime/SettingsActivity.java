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
        text("Tap EN for English or 中 to return to your Chinese layout. English mode offers word completions and uses English punctuation. Tap a suggestion to choose it, then Space for a space. Space otherwise keeps your typed spelling. Chinese mode accepts Pinyin or Zhuyin and still supports mixed English. The blue candidate is the Space choice. English + Space commits a real space; Chinese + Space accepts Chinese. The exact input stays at the left; tap it or hold Space to recover the spelling. Tap Shift once for one capital, twice quickly for Caps Lock, and once again to unlock. Holding Shift also locks capitals.",16);
        text("On QWERTY, slide up to commit a capital letter and down for the small symbol. Shift-and-tap also gives capitals. On Zhuyin, slide down for lowercase letters or digits and up for capitals or shifted digits. Slides commit directly. Hold a key to choose its slide alternatives by tapping. Hold Backspace to keep deleting. Space enters first tone on an unfinished Zhuyin syllable; press again to accept. ㄦ is beside Space. In English mode, Enter accepts your spelling and inserts a newline in one press. In Chinese mode, Enter first accepts composition; the next Enter inserts a newline. Search, Go and other field actions still run directly.",16);
        text("Comma and period commit immediately: ，。 in Chinese mode, ,. in English mode and URL/password fields. Slide either punctuation key up for the other width. Hold comma to open emoji; hold period for punctuation and text-face alternatives. In Chinese mode the period choices can also change your punctuation width preference. Tap ☺ for emoji or ?123 for categorized symbols; ABC returns to letters. Emoji groups include skin tones, flags and families. No recent-emoji history is stored.",16);
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
        text("Fully offline. No network permission, keystroke logs, telemetry, or cloud backup. Only explicit candidate choices are learned locally, with recent Chinese context. Password fields use direct input without a composition buffer, suggestions, or learning. Fields requesting no personalized learning do not read or write your learned choices.",16);
        text("About this prototype",21);
        text("Version 0.1.0. This is an independent implementation, not a Google product. Reference gestures and composition were studied on Android 13; ranking and full compatibility still need evaluation. Language data: McBopomofo 3.1 (MIT) and AOSP LatinIME (Apache 2.0).",16);
        button("Open-source notices",()-> {
            try(java.io.InputStream in=getAssets().open("NOTICE.txt")) {
                java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); byte[] b=new byte[4096]; int n;
                while((n=in.read(b))!=-1) out.write(b,0,n);
                new android.app.AlertDialog.Builder(this).setTitle("Notices").setMessage(out.toString("UTF-8")).setPositiveButton("Close",null).show();
            } catch(java.io.IOException e) { Toast.makeText(this,"Notices unavailable",Toast.LENGTH_SHORT).show(); }
        });
    }
    private void text(String text,int size) { TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setPadding(0,14,0,10); body.addView(v); }
    private void button(String label,Runnable action) { Button b=new Button(this); b.setText(label); b.setOnClickListener(v->action.run()); body.addView(b); }
}
