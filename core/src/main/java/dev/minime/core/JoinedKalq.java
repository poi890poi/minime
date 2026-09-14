package dev.minime.core;

/** Joined KALQ letter positions and row-ordered slide symbols, independent of language. */
public final class JoinedKalq {
    private JoinedKalq() {}
    private static final String[] ROWS={"mbwhgtoj","p xcie u","ryszkalq","dnfv"};
    private static final String ASCII="1234567890@*+-=/#()':\"?!~…";
    private static final String CHINESE="1234567890@*+-=/#（）、「」？！～.";
    public static int rows() {return ROWS.length;}
    public static String row(int row) {return ROWS[row];}
    public static String symbols(boolean ascii) {return ascii?ASCII:CHINESE;}
    public static String symbol(int row,int column,boolean ascii) {
        if(ROWS[row].charAt(column)==' ')return "";
        int index=0;
        for(int r=0;r<=row;r++)for(int c=0;c<(r==row?column:ROWS[r].length());c++)
            if(ROWS[r].charAt(c)!=' ')index++;
        return symbols(ascii).substring(index,index+1);
    }
}
