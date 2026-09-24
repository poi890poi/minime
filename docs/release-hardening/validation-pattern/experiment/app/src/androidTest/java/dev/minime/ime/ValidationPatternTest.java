package dev.minime.ime;

import android.test.InstrumentationTestCase;
import dev.minime.core.ValidationPatternProbe;
import java.io.*;
import java.util.*;

/** Isolated ART predicate cost, including the old per-call compilation. */
@SuppressWarnings("deprecation")
public final class ValidationPatternTest extends InstrumentationTestCase {
    public void testEquivalentValidatorsAndAlternatingCost()throws Exception {
        List<String> inputs=ValidationPatternProbe.inputs();int comparisons=ValidationPatternProbe.verify(inputs);
        for(int round=0;round<3;round++)for(boolean compiled:new boolean[]{false,true})ValidationPatternProbe.measure(inputs,compiled,1);
        File directory=getInstrumentation().getTargetContext().getExternalFilesDir(null);
        try(PrintWriter out=new PrintWriter(new File(directory,"validation-pattern-cost.tsv"),"UTF-8")) {
            out.println("round\tcompiled\tinputs\tpredicates\tnanoseconds");
            for(int round=0;round<10;round++)for(boolean compiled:round%2==0?new boolean[]{false,true}:new boolean[]{true,false})
                out.println(round+"\t"+compiled+"\t"+inputs.size()+"\t"+(inputs.size()*5)+"\t"+ValidationPatternProbe.measure(inputs,compiled,1));
        }
        assertTrue("Independent String.matches oracle exercised",comparisons>0);
    }
}
