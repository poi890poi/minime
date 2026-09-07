package dev.minime.ime;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/** Unicode palettes with optional local recents; private fields never access history. */
final class SymbolPanel extends LinearLayout {
    private static final class Entry {
        final String text,name;
        Entry(String text,String name) {this.text=text;this.name=name;}
    }
    private final Map<String,Map<String,List<Entry>>> catalog=new LinkedHashMap<>();
    private final Button groups,sections;
    private final LinearLayout grid;
    private final TextView counter;
    private final Button previous,next;
    private final Consumer<String> press;
    private final boolean emoji;
    private final boolean remember;
    private float touchX,touchY;
    private static List<String[]> emojiRows;
    private List<Entry> entries=Collections.emptyList();
    private int page;
    private int chooser;
    private String groupName,sectionName;
    private final boolean landscape;
    SymbolPanel(Context context,boolean emoji,boolean privateField,Consumer<String> press) {
        super(context);this.emoji=emoji;this.press=press;setOrientation(VERTICAL);
        remember=emoji && !privateField && context.getSharedPreferences("settings",Context.MODE_PRIVATE).getBoolean("emoji_recents",false);
        landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        int selectorHeight=landscape?28:40,navHeight=landscape?22:34;
        if(emoji) {
            loadEmoji();
            for(Map<String,List<Entry>> sections:catalog.values()) {List<Entry> all=new ArrayList<>();for(List<Entry> list:sections.values())all.addAll(list);
                Map<String,List<Entry>> ordered=new LinkedHashMap<>();ordered.put("All",all);ordered.putAll(sections);sections.clear();sections.putAll(ordered);
            }
            if(remember) {
                Map<String,Entry> byText=new HashMap<>();for(Map<String,List<Entry>> sections:catalog.values())for(Entry e:sections.get("All"))byText.put(e.text,e);
                List<Entry> recent=new ArrayList<>();for(String text:context.getSharedPreferences("learning",Context.MODE_PRIVATE).getString("recent_emoji","").split("\n"))if(byText.containsKey(text))recent.add(byText.get(text));
                if(!recent.isEmpty()) {Map<String,Map<String,List<Entry>>> ordered=new LinkedHashMap<>();ordered.put("Recent",Collections.singletonMap("All",recent));ordered.putAll(catalog);catalog.clear();catalog.putAll(ordered);}
            }
        } else loadSymbols();
        LinearLayout selectors=new LinearLayout(context);selectors.setBaselineAligned(false);
        Button alternate=navButton(emoji?"#+":"☺",emoji?"Symbols":"Emoji",()->press.accept(emoji?"SYMBOLS":"EMOJI"));
        selectors.addView(alternate,new LayoutParams(dp(42),dp(selectorHeight)));
        groups=new Button(context); groups.setContentDescription(emoji?"Emoji category":"Symbol category");
        sections=new Button(context); sections.setContentDescription("Emoji group");
        groups.setTextSize(12);sections.setTextSize(12);groups.setPadding(0,0,0,0);sections.setPadding(0,0,0,0);
        selectors.addView(groups,new LayoutParams(0,dp(selectorHeight),1));
        if(emoji) selectors.addView(sections,new LayoutParams(0,dp(selectorHeight),1));
        addView(selectors,new LayoutParams(-1,dp(selectorHeight)));
        grid=new LinearLayout(context);grid.setOrientation(VERTICAL);addView(grid,new LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(context);nav.setBaselineAligned(false);
        previous=navButton("‹","Previous palette page",()->{page--;render();});
        next=navButton("›","Next palette page",()->{page++;render();});
        counter=new TextView(context);counter.setTextColor(0xff263238);counter.setGravity(Gravity.CENTER);
        nav.addView(previous,new LayoutParams(dp(55),dp(navHeight)));
        nav.addView(counter,new LayoutParams(0,dp(navHeight),1));nav.addView(next,new LayoutParams(dp(55),dp(navHeight)));addView(nav,new LayoutParams(-1,dp(navHeight)));
        groupName=catalog.keySet().iterator().next();
        selectGroup(groupName);
        groups.setOnClickListener(v->{chooser=chooser==1?0:1;page=0;render();});
        sections.setOnClickListener(v->{chooser=chooser==2?0:2;page=0;render();});
        render();
    }
    private int dp(int n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private void selectGroup(String name) {
        groupName=name;sectionName=catalog.get(name).keySet().iterator().next();
        entries=catalog.get(name).get(sectionName);
    }
    private Button navButton(String label,String description,Runnable action) {
        Button b=new Button(getContext());b.setText(label);b.setContentDescription(description);b.setPadding(0,0,0,0);
        b.setOnClickListener(v->action.run());return b;
    }
    private void add(String group,String section,String text,String name) {
        catalog.computeIfAbsent(group,k->new LinkedHashMap<>()).computeIfAbsent(section,k->new ArrayList<>()).add(new Entry(text,name));
    }
    private void symbols(String name,String values) {
        for(String value:values.split(" ")) add(name,name,value,value);
    }
    private void range(String group,int first,int last) {
        Set<String> seen=new HashSet<>();for(Entry e:catalog.get(group).get(group)) seen.add(e.text);
        for(int cp=first;cp<=last;cp++) if(Character.isDefined(cp)) {
            String text=new String(Character.toChars(cp));if(seen.add(text)) add(group,group,text,text);
        }
    }
    private void loadSymbols() {
        symbols("常用 Common","1 2 3 4 5 6 7 8 9 0 ! @ # $ % ^ & * ( ) ， 。 、 ： ； ？ ！ … — ～ · ＿ ． ＋ － × ÷ = / \\ _ + - ' \" : ; ? . , ` |");
        symbols("括號 Brackets","( ) [ ] { } < > （ ） ［ ］ ｛ ｝ 「 」 『 』 【 】 〔 〕 〈 〉 《 》 “ ” ‘ ’ ‹ › « »");
        symbols("箭頭 Arrows","← ↑ → ↓ ↔ ↕ ↖ ↗ ↘ ↙ ⇐ ⇑ ⇒ ⇓ ⇔ ⇕ ↩ ↪ ↰ ↱ ↴ ↵ ↶ ↷ ➜ ➤ ➔ ➡");
        symbols("數學 Math","+ − × ÷ = ≠ ≈ ≡ < > ≤ ≥ ± ∓ ∞ √ ∛ ∑ ∏ ∫ ∂ ∆ ∇ ∈ ∉ ∩ ∪ ⊂ ⊃ ∅ ∀ ∃ ∧ ∨ ¬ ∴ ∵ ° ′ ″ % ‰ π μ Ω");
        symbols("貨幣 Currency","$ ＄ ¢ £ ¤ ¥ ￥ € ₩ ₹ ₽ ₺ ₫ ฿ ₱ ₪ ₴ ₦ ₡ ₲ ₵ ₸ ₮ NT$ US$");
        symbols("數字 Numbers","0 1 2 3 4 5 6 7 8 9 ０ １ ２ ３ ４ ５ ６ ７ ８ ９ ① ② ③ ④ ⑤ ⑥ ⑦ ⑧ ⑨ ⑩ Ⅰ Ⅱ Ⅲ Ⅳ Ⅴ Ⅵ Ⅶ Ⅷ Ⅸ Ⅹ ½ ¼ ¾ ¹ ² ³ ₀ ₁ ₂ ₃");
        symbols("圖形 Shapes","★ ☆ ● ○ ◎ ◉ ◌ ■ □ ▪ ▫ ▲ △ ▼ ▽ ◆ ◇ ◈ ♥ ♡ ♠ ♤ ♣ ♧ ♦ ♢ ✓ ✔ ✕ ✖ ✗ ✘ ※ ＊ † ‡ § ¶ © ® ™ ℠ ℡");
        symbols("文字表情 Faces",":) :-) :D :-D ;) ;-) :P :-P :( :-( :/ :| ^_^ T_T >_< o_O O_O XD");
        symbols("單位 Units","° ℃ ℉ ㎜ ㎝ ㎞ ㎎ ㎏ ㎡ ㎥ ㏄ ℓ ㏎ ㏑ ㏒ ㏕ Ω μ");
        symbols("希臘 Greek","Α Β Γ Δ Ε Ζ Η Θ Ι Κ Λ Μ Ν Ξ Ο Π Ρ Σ Τ Υ Φ Χ Ψ Ω α β γ δ ε ζ η θ ι κ λ μ ν ξ ο π ρ σ ς τ υ φ χ ψ ω");
        range("箭頭 Arrows",0x2190,0x21ff);range("數學 Math",0x2200,0x22ff);
        range("圖形 Shapes",0x25a0,0x25ff);range("貨幣 Currency",0x20a0,0x20bf);
        range("括號 Brackets",0x2768,0x2775);
    }
    private void loadEmoji() {
        if(emojiRows==null) {
            List<String[]> rows=new ArrayList<>();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(getContext().getAssets().open("emoji.tsv"),StandardCharsets.UTF_8))) {
                String line;while((line=reader.readLine())!=null) {String[] p=line.split("\t",-1);if(p.length!=4) throw new IOException("Invalid emoji row");rows.add(p);}
            } catch(IOException e) {throw new IllegalStateException("Bundled emoji catalog unavailable",e);}
            emojiRows=Collections.unmodifiableList(rows);
        }
        for(String[] p:emojiRows)add(p[0],p[1],p[2],p[3]);
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN) {touchX=e.getX();touchY=e.getY();}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && Math.abs(e.getX()-touchX)>dp(48) && Math.abs(e.getX()-touchX)>1.5*Math.abs(e.getY()-touchY))return true;
        return super.onInterceptTouchEvent(e);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getActionMasked()==MotionEvent.ACTION_UP) {if(Math.abs(e.getX()-touchX)>dp(48)) {page+=e.getX()<touchX?1:-1;render();}return true;}
        return true;
    }
    private void insert(Entry e) {
        if(remember) {
            android.content.SharedPreferences prefs=getContext().getSharedPreferences("learning",Context.MODE_PRIVATE);
            LinkedHashSet<String> recent=new LinkedHashSet<>();recent.add(e.text);recent.addAll(Arrays.asList(prefs.getString("recent_emoji","").split("\n")));recent.remove("");
            List<String> list=new ArrayList<>(recent);prefs.edit().putString("recent_emoji",String.join("\n",list.subList(0,Math.min(24,list.size())))).apply();
        }
        press.accept("INSERT:"+e.text);
    }
    private void render() {
        groups.setText(groupName+" ▾");sections.setText(sectionName+" ▾");
        List<String> choices=chooser==1?new ArrayList<>(catalog.keySet()):chooser==2?new ArrayList<>(catalog.get(groupName).keySet()):Collections.emptyList();
        int columns=chooser==0?6:3,perPage=columns*3,size=chooser==0?entries.size():choices.size();
        int count=Math.max(1,(size+perPage-1)/perPage);page=Math.max(0,Math.min(page,count-1));
        grid.removeAllViews();
        for(int row=0;row<3;row++) {
            LinearLayout line=new LinearLayout(getContext());grid.addView(line,new LayoutParams(-1,0,1));
            for(int col=0;col<columns;col++) {
                int index=page*perPage+row*columns+col;
                if(index>=size) {line.addView(new View(getContext()),new LayoutParams(0,-1,1));continue;}
                Entry e=chooser==0?entries.get(index):new Entry(choices.get(index),choices.get(index));
                TextView key=new TextView(getContext());key.setText(e.text);key.setTextColor(Color.BLACK);key.setTextSize(chooser!=0?12:landscape?18:emoji?24:21);
                key.setGravity(Gravity.CENTER);key.setMaxLines(chooser==0?1:3);key.setBackgroundColor(0xfff9fafb);key.setFocusable(true);key.setClickable(true);
                key.setContentDescription(chooser==0?(emoji?"Emoji ":"Symbol ")+e.name:e.name);
                key.setOnClickListener(v->{
                    if(chooser==0) insert(e);
                    else {if(chooser==1) selectGroup(e.text);else {sectionName=e.text;entries=catalog.get(groupName).get(sectionName);}chooser=0;page=0;render();}
                });
                LayoutParams lp=new LayoutParams(0,-1,1);lp.setMargins(dp(1),dp(1),dp(1),dp(1));line.addView(key,lp);
            }
        }
        counter.setText(String.format(Locale.getDefault(),"%d / %d",page+1,count));previous.setEnabled(page>0);next.setEnabled(page+1<count);
    }
}
