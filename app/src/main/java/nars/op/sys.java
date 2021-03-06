//package nars.op;
//
//import nars.$;
//import nars.NAR;
//import nars.index.term.TermIndex;
//import nars.nal.nal8.Operator;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.atom.Atom;
//import nars.time.Tense;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.util.Scanner;
//
///**
// * Execute a sequence of commands in system shell. DANGEROUS
// */
//public class sys implements Operator.CommandOperator {
//
//    final static Logger logger = LoggerFactory.getLogger(sys.class);
//
//    @Override
//    public void run(@NotNull Atom op, Term[] args, @NotNull NAR nar) {
//        for (Term x : args.terms()) {
//            if (x instanceof Atom) {
//
//                String cmd = $.unquote(x);
//
//                long pid = -1;
//                try {
//                    Process p = Runtime.getRuntime().exec(cmd);
////                    final long pidCopy = pid =
////                            p.hashCode(); //HACK temporary
////                            //p.getPid();
//                    @NotNull Runnable t = ()-> {
//                        try {
//
//                            Scanner s = new Scanner(p.getInputStream());
//                            while (s.hasNextLine()) {
//                                String l = s.nextLine();
//                                //logger.info("{}: {}", cmd, l);
//                                onInput(cmd, l);
//                            }
//                        /*while (s.hasNext()) {
//                            String l = s.next();
//                        }*/
//
//                            int result = p.waitFor();
//                        } catch (Exception e) {
//                            error(cmd, e);
//                        }
//                    };
//                    nar.runLater(t);
//
////        logger.trace("runAsyncs run {}", t);
////
////        try {
////            return asyncs.submit(t);
////        } catch (RejectedExecutionException e) {
////            logger.error("runAsync error {} in {}", t, e);
////            return null;
////        }
//                } catch (IOException e) {
//                    error(cmd, e);
//                }
//
//                if (pid!=-1)
//                    return pid;
//                else
//                    return false;
//            }
//        }
//
//    }
//
//    @Override
//    public Object function(Compound args, TermIndex i) {
//    }
//
//    public void error(String cmd, Exception e) {
//        logger.error("exec \"{}\": {}", cmd, e);
//    }
//
//
//    protected void onInput(String cmd, String line) {
//        nar.believe(
//            $.inst($.quote(line), $.quote(cmd)), Tense.Present,
//                1f, 0.9f
//                );
//    }
//
//
//}
