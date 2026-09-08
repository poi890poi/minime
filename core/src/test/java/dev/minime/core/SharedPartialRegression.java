package dev.minime.core;

import java.io.*;
import java.util.*;
import static dev.minime.core.Regression.*;

final class SharedPartialRegression {
    static void run()throws Exception {
        // Same readings and outputs under every pack name: language identity
        // cannot alter the matching rule. Synthetic fixtures never enter assets.
        for(String pack:Arrays.asList("taiwan","geography","poj","japanese","english")) {
            AddonDictionary dictionary=AddonDictionary.read(new StringReader(pack+"\tka'na'mi\tfixture output\ttest\tfixture\n"));
            for(String raw:Arrays.asList("kanami","kanam","knm","kanm","knami","kanmi","k'n'm","ka-na-m")) {
                List<Candidate> found=dictionary.lookup(raw,Collections.singleton(pack));
                equal("fixture output",found.get(0).text,pack+" complete/prefix/mixed units: "+raw);
                yes(found.get(0).supplemental,"partial scoring preserves learning isolation");
                equal(!raw.equals("kanami"),found.get(0).incomplete,"explicit match completeness");
            }
            yes(dictionary.lookup("knz",Collections.singleton(pack)).isEmpty(),"unmatched source units never invent an output");
            yes(dictionary.lookup("knm",Collections.emptySet()).isEmpty(),"disabled shared matching");
        }
    }
}
