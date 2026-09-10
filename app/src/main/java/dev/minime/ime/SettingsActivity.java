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
        option("Use Rime for Pinyin phrase prediction", "rime_pinyin",true);
        text("Rime is on by default and offers stronger sentence prediction in our tests, with different candidate ordering. Turn it off to use the original MinIME decoder. It works offline and does not keep its own typing history.",16);
        option("Correct English spelling on Space", "english_correction",false);
        text("Missing apostrophes are restored on Space in ordinary text fields even with spelling correction off. Valid source-dictionary spellings stay unchanged; Backspace immediately restores your original input. Pinyin restores annotated English contractions, avoiding possessive guesses over Chinese initials. Exact custom and enabled-pack matches appear before decoder guesses.",16);
        option("Double Space inserts a period in English", "double_space_period",true);
        option("Keep recent emoji on this device", "emoji_recents",false);
        Switch adaptation=new Switch(this);adaptation.setText("Learn English word pairs on this device");
        adaptation.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("english_learning",false));
        adaptation.setOnCheckedChangeListener((b,value)->getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean("english_learning",value).apply());body.addView(adaptation);
        text("Tap the bottom EN key to switch between English and your last mixed mode. Tap the current-mode down arrow to expand candidates, then tap 中 / EN / 台 / 日 to choose a mode. While idle, the mode badge opens the same choices. Switching keeps unfinished spelling and refreshes suggestions without accepting a word. The spacebar shows the active mode. English offers completions, spelling alternatives and next words. Choosing a completion adds a space before your next word. Optional spelling correction acts on Space; immediate Backspace restores the spelling. Double Space within one second can insert a period. Chinese supports full, initial and mixed Pinyin syllables. The dark candidate is the Space choice. Tap exact input or hold Space to keep the spelling. Swipe the candidate strip or use the down arrow to expand it. Tap the selected mixed-mode tab while idle to switch Pinyin/Zhuyin layout. Tap Shift once for one capital, twice quickly for Caps Lock, and once again to unlock.",16);
        text("On QWERTY, slide up for a capital and down for the small symbol. In English mode, trace across letters to compose a word; Space accepts it and candidates offer alternatives. On Zhuyin, slide down for lowercase letters or digits and up for capitals or shifted digits. Hold a key for tappable alternatives, or hold Backspace to keep deleting. Space enters first tone on an unfinished Zhuyin syllable; press again to accept. In English mode, Enter accepts spelling and runs the editor action or inserts a newline. In Chinese mode, Enter first accepts composition; the next Enter runs the action or inserts a newline.",16);
        text("Comma and period commit immediately: ，。 in Chinese mode, ,. in English mode and URL/password fields. Slide up for the other width. Hold comma to open emoji. Hold period, drag to a punctuation choice, and release; you can also tap a popup choice. Chinese mode includes a punctuation-width preference. Tap ?123 for symbols; ABC returns to letters. Browse categories or swipe pages. Optional recent emoji stay on this device and are hidden in private input.",16);
        text("Try it",21);
        EditText test=new EditText(this); test.setHint("這個 pronunciation 不對"); test.setMinLines(2); test.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE); body.addView(test);
        text("Local dictionary",21);
        option("Remember Taiwanese and Japanese candidate choices", "focused_choice_learning",true);
        text("Explicit candidate taps and holds improve ordering for the same typed reading in that language. Space does not train itself. Han and POJ alternatives share one preference. Turning this off hides these preferences and stops updates; Clear learned choices removes them.",16);
        option("Learn repeated Chinese phrases on this device", "phrase_learning",false);
        text("After three accepted occurrences, a phrase becomes an extra candidate for the same reading. Only accepted Chinese segments are observed, including parts you select separately. Edits and field changes break the chain. No surrounding text is collected. Up to 512 reading/phrase pairs are retained; turning this off stops learning and hides these suggestions.",16);
        button("View learned phrases",()-> {
            String saved=getSharedPreferences("learning",MODE_PRIVATE).getString("phrases_v1","");
            new android.app.AlertDialog.Builder(this).setTitle("Reading · phrase · occurrences")
                .setMessage(saved.isEmpty()?"No repeated phrases recorded.":saved)
                .setPositiveButton("Close",null).setNeutralButton("Clear phrase learning",(d,w)->getSharedPreferences("learning",MODE_PRIVATE).edit().remove("phrases_v1").apply()).show();
        });
        text("Optional dictionaries",21);
        option("Taiwan names, culture and local vocabulary", "addon_taiwan",false);
        option("Taiwan geography and history · Rudy Map / OSM", "addon_geography",false);
        option("Enable 日 mode · Japanese + English", "addon_japanese",false);
        option("Enable 台 mode · Taiwanese (POJ) + English", "addon_poj",false);
        option("Show Taiwanese Han alternatives · hold a candidate to insert its second line", "paired_taiwanese",true);
        option("Prefer Taiwanese Han output · tap Han, hold for POJ", "taiwanese_han_primary",false);
        text("Optional modes and dictionaries work offline and are off by default. 中 uses Chinese and English. 台 uses Taiwanese + English; 日 uses Japanese + English. Tap 中 for Chinese. Focused matches lead, including incomplete readings and collisions with English words. English-focused mode queries neither Chinese nor third-language packs. Taiwan culture and geography options apply only when Chinese is one of the two languages. Disabling an optional mode returns its mixed-mode selection to 中; existing pack settings and manual entries are retained. Full, initial and mixed reading matches follow the shared matching rules. Space accepts the dark highlighted candidate; prefix choices that consume only part of your input require a tap.",16);
        text("Taiwanese uses the complete POJ spelling system, including ch/chh, oe/oa, o͘, ⁿ and tone marks. On the letter board, omit tone numbers, spaces and hyphens; type oo for o͘ and nn for ⁿ. Original numbered POJ keys are retained in the source data. Everyday vocabulary and short examples are selected automatically from source dictionaries, with eligible source variants retained. This is not a full Taiwanese decoder.",16);
        text("Japanese mode prioritizes Japanese matches and offers common vocabulary across parts of speech, single hiragana and katakana, and 500 kanji selected by source frequency rank. The raw candidate keeps your Romanization. Source commonness and newspaper frequency are imperfect guides to everyday use. This does not provide Japanese grammar or sentence conversion.",16);
        text("The geography pack systematically imports Rudy Map's hiking, nature, settlement, waterway and historical-site categories. Dataset version, licence, extraction rules and coverage are recorded together. Available source Pinyin/Zhuyin takes precedence; other readings use existing dictionary units. Missing or ambiguous readings are reported rather than guessed.",16);
        button("Dictionary sources and coverage",()->showAsset("addon-sources.txt","Optional dictionary sources"));
        EditText sourceQuery=new EditText(this);sourceQuery.setHint("Find a word or reading in add-on sources");body.addView(sourceQuery);
        button("Find add-on entry sources",()-> {
            String query=sourceQuery.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
            if(query.isEmpty()) {sourceQuery.setError("Enter a word or reading");return;}
            try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.SequenceInputStream(getAssets().open("addons.tsv"),getAssets().open("geography.tsv")),java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder matches=new StringBuilder();String line;int count=0;
                while((line=reader.readLine())!=null)if(!line.startsWith("#") && line.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                    matches.append(line).append("\n\n");if(++count==40) {matches.append("First 40 matches. Narrow the search for more specific results.");break;}
                }
                new android.app.AlertDialog.Builder(this).setTitle("Pack · reading · output · source · category")
                    .setMessage(count==0?"No matching add-on entries.":matches.toString()).setPositiveButton("Close",null).show();
            } catch(java.io.IOException error) {Toast.makeText(this,"Optional dictionary asset unavailable",Toast.LENGTH_LONG).show();}
        });
        text("Custom entries",21);
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
        text("Independent implementation; not a Google product. Rime Pinyin is on by default and supports choosing part of a phrase. Long abbreviated sentences still need work. Language foundations: Rime, McBopomofo, AOSP LatinIME, Universal Dependencies and Unicode. See notices for complete sources, authors and licenses.",16);
        button("Open-source notices",()-> {
            try(java.io.InputStream in=getAssets().open("NOTICE.txt")) {
                java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); byte[] b=new byte[4096]; int n;
                while((n=in.read(b))!=-1) out.write(b,0,n);
                new android.app.AlertDialog.Builder(this).setTitle("Notices").setMessage(out.toString("UTF-8")).setPositiveButton("Close",null).show();
            } catch(java.io.IOException e) { Toast.makeText(this,"Notices unavailable",Toast.LENGTH_SHORT).show(); }
        });
    }
    private void showAsset(String name,String title) {
        try(java.io.InputStream in=getAssets().open(name)) {
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1)out.write(b,0,n);
            new android.app.AlertDialog.Builder(this).setTitle(title).setMessage(out.toString("UTF-8")).setPositiveButton("Close",null).show();
        } catch(java.io.IOException e) {Toast.makeText(this,"Asset unavailable",Toast.LENGTH_LONG).show();}
    }
    private void text(String text,int size) { TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setPadding(0,14,0,10); body.addView(v); }
    private void option(String label,String key,boolean initial) {
        Switch toggle=new Switch(this);toggle.setText(label);
        toggle.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean(key,initial));
        toggle.setOnCheckedChangeListener((b,value)->getSharedPreferences("settings",MODE_PRIVATE).edit().putBoolean(key,value).apply());body.addView(toggle);
    }
    private void button(String label,Runnable action) { Button b=new Button(this); b.setText(label); b.setOnClickListener(v->action.run()); body.addView(b); }
}
