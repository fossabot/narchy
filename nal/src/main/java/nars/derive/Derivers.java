package nars.derive;

import nars.NAR;
import nars.derive.premise.PremiseDeriverRuleSet;

import java.util.*;

/**
 * utility class for working witih Deriver's
 */
public class Derivers {

    /** HACK range is inclusive */
    public static Set<String> standard(int minLevel, int maxLevel, String... otherFiles) {
        Set<String> files = new TreeSet();
        for (int level = minLevel; level <= maxLevel; level++) {
            switch (level) {
                case 8:
                    //files.add("motivation.nal");
                    //files.add("list.nal");  //experimental
                case 7:
                    //TODO move temporal induction to a separate file
                    //fallthru
                case 6:
                    files.add("nal6.nal");
                    files.add("nal6.guess.nal");
                    files.add("nal6.layer2.nal");

                    files.add("induction.nal");  //TODO nal6 only needs general induction, not the temporal parts

                    files.add("misc.nal"); //TODO split this up
                    break;
                case 5:
                case 4:
                    files.add("nal4.nal");
                    break;
                case 3:
                case 2:
                    files.add("nal3.nal");
                    files.add("nal3.guess.nal");
                    files.add("nal2.nal");
                    files.add("nal2.guess.nal");
                    break;
                case 1:
                    files.add("analogy.nal");
                    files.add("nal1.nal");
                    files.add("nal1.guess.nal");
                    break;
            }
        }

        Collections.addAll(files, otherFiles);

        return files;
    }

//    /**
//     * loads default deriver rules, specified by a range (inclusive) of levels. this allows creation
//     * of multiple deriver layers each targetting a specific range of the NAL spectrum
//     */
//    public static Function<NAR, Deriver> deriver(int minLevel, int maxLevel, String... extraFiles) {
//        assert ((minLevel <= maxLevel && maxLevel > 0) || extraFiles.length > 0);
//
//        return (nar)->new Deriver(files(minLevel, maxLevel, nar, extraFiles), nar);
//    }

//    public static Function<NAR, Deriver> deriver(String... extraFiles) {
//        return nar -> new Deriver(DeriveRuleSet.load(nar,
//                List.of(extraFiles)
//        ), nar)
//        ;
//    }

    /** standard ruleset */
    public static PremiseDeriverRuleSet nal(int minLevel, int maxLevel, NAR nar, String... extraFiles) {
        return files(nar, standard(minLevel, maxLevel, extraFiles)        );
    }

    public static PremiseDeriverRuleSet files(NAR nar, String... filename) {
        return files(nar, List.of(filename));
    }

    public static PremiseDeriverRuleSet files(NAR nar, Collection<String> filename) {
        return PremiseDeriverRuleSet.files(nar, filename);
    }
    public static PremiseDeriverRuleSet parse(NAR nar, String... rules) {
        return new PremiseDeriverRuleSet(nar, rules);
    }
}
