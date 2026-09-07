package dev.minime.ime;

import android.test.InstrumentationTestCase;
import android.view.*;
import java.util.*;

@SuppressWarnings("deprecation")
public final class SymbolOrderingTest extends InstrumentationTestCase {
    private SymbolPanel panel(Set<String> main) {return new SymbolPanel(getInstrumentation().getTargetContext(),false,true,key->{},main);}
    private static void click(View root,String description) {View v=SymbolNavigationTest.find(root,description);assertNotNull(description,v);v.performClick();}
    private static List<String> pages(SymbolPanel panel,String category) {
        click(panel,"Symbol category");
        while(SymbolNavigationTest.find(panel,category)==null && SymbolNavigationTest.find(panel,"Next palette page").isEnabled())click(panel,"Next palette page");
        click(panel,category);List<String> result=new ArrayList<>();
        for(int page=0;page<100;page++) {
            result.addAll(SymbolNavigationTest.entries(panel));
            if(!SymbolNavigationTest.find(panel,"Next palette page").isEnabled())return result;
            click(panel,"Next palette page");
        }
        throw new AssertionError("Palette did not terminate");
    }
    public void testEveryCategoryRetainsInventoryAndPrioritizesAdditionalSymbols() throws Throwable {
        runTestOnUiThread(()-> {
            String[] categories={"常用 Common","括號 Brackets","箭頭 Arrows","數學 Math","貨幣 Currency","數字 Numbers","圖形 Shapes","文字表情 Faces","單位 Units","希臘 Greek"};
            for(boolean[] board:new boolean[][]{{false,false,false},{false,true,false},{true,false,false},{false,true,true}}) {
                Set<String> main=KeyboardView.mainBoardSymbols(board[0],board[1],board[2]);
                SymbolPanel original=panel(Collections.emptySet()),ordered=panel(main);
                for(String category:categories) {
                    List<String> baseline=pages(original,category),actual=pages(ordered,category);
                    List<String> baselineAdditional=new ArrayList<>(),baselineDirect=new ArrayList<>(),actualAdditional=new ArrayList<>(),actualDirect=new ArrayList<>();
                    for(String s:baseline)(main.contains(s)?baselineDirect:baselineAdditional).add(s);
                    boolean reachedDirect=false;
                    for(String s:actual) {
                        if(main.contains(s)) {reachedDirect=true;actualDirect.add(s);}
                        else {assertFalse("Additional symbol must precede direct symbols in "+category,reachedDirect);actualAdditional.add(s);}
                    }
                    assertEquals("Additional inventory/order preserved",baselineAdditional,actualAdditional);
                    assertEquals("Direct inventory/order preserved",baselineDirect,actualDirect);
                }
            }
        });
    }
    public void testAvailabilityUsesCurrentBoardAndNumbersRemainAccessible() throws Throwable {
        runTestOnUiThread(()-> {
            Set<String> chinese=KeyboardView.mainBoardSymbols(false,false,false),english=KeyboardView.mainBoardSymbols(false,true,false),zhuyin=KeyboardView.mainBoardSymbols(true,false,false);
            assertTrue(chinese.contains("（"));assertFalse(english.contains("（"));
            assertTrue(english.contains("("));assertFalse(chinese.contains("("));
            assertTrue(zhuyin.contains("!"));assertFalse(chinese.contains("!"));
            for(Set<String> main:Arrays.asList(chinese,english,zhuyin)) {
                SymbolPanel view=panel(main);List<String> first=SymbolNavigationTest.entries(view);
                assertFalse("Default page no longer begins with a main-board digit",Character.isDigit(first.get(0).codePointAt(0)));
                List<String> all=pages(view,"數字 Numbers");for(char digit='0';digit<='9';digit++)assertTrue(all.contains(String.valueOf(digit)));
            }
        });
    }
}
