package dev.minime.core;

import java.util.*;

final class JoinedKalqRegression {
    static void run() {
        Set<Character> letters=new HashSet<>();int spaces=0;
        for(int r=0;r<JoinedKalq.rows();r++)for(int c=0;c<JoinedKalq.row(r).length();c++) {
            char key=JoinedKalq.row(r).charAt(c);
            if(key==' ') {spaces++;Regression.equal("",JoinedKalq.symbol(r,c,true),"Space has no symbol slide");}
            else Regression.yes(letters.add(key),"KALQ letter appears once");
        }
        Regression.equal(26,letters.size(),"Complete alphabet");Regression.equal(2,spaces,"Two internal Spaces");
        for(char c='a';c<='z';c++)Regression.yes(letters.contains(c),"Every Latin letter is available");
        for(boolean ascii:new boolean[]{false,true}) {
            String symbols=JoinedKalq.symbols(ascii);
            Regression.equal(26,symbols.length(),"One symbol per letter");
            Regression.equal(26L,symbols.chars().distinct().count(),"No duplicate symbol slots");
            StringBuilder actual=new StringBuilder();
            for(int r=0;r<JoinedKalq.rows();r++)for(int c=0;c<JoinedKalq.row(r).length();c++)actual.append(JoinedKalq.symbol(r,c,ascii));
            Regression.equal(symbols,actual.toString(),"Visual order preserves complete symbol inventory");
            for(int c=0;c<8;c++)Regression.equal(Integer.toString(c+1),JoinedKalq.symbol(0,c,ascii),"Top row digits");
            Regression.equal("9",JoinedKalq.symbol(1,0,ascii),"Next digit skips Space");
            Regression.equal("0",JoinedKalq.symbol(1,2,ascii),"Zero follows nine");
        }
        Regression.equal("'",JoinedKalq.symbol(2,5,true),"English apostrophe is available directly");
        Regression.equal("、",JoinedKalq.symbol(2,5,false),"Same slot uses Chinese punctuation");
    }
}
