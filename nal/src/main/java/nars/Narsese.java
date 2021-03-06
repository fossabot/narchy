package nars;

import com.github.fge.grappa.matchers.base.Matcher;
import com.github.fge.grappa.rules.Rule;
import com.github.fge.grappa.run.ParseRunner;
import com.github.fge.grappa.run.ParsingResult;
import com.github.fge.grappa.run.context.MatcherContext;
import com.github.fge.grappa.stack.ValueStack;
import com.github.fge.grappa.transform.ParserTransformer;
import com.github.fge.grappa.transform.base.ParserClassNode;
import jcog.Util;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.obj.QuantityTerm;
import nars.truth.Truth;
import nars.util.SoftException;
import nars.util.time.Tense;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.term.Term.nullIfNull;
import static nars.util.time.Tense.*;

/**
 * NARese, syntax and language for interacting with a NAR in NARS.
 * https://code.google.com/p/open-nars/wiki/InputOutputFormat
 */
public class Narsese {

    private static final Class parser;

    //    static {
////            String narseseProto = "nars.NarseseParser";
//            String narseseClass = "nars.NarseseParser$$grappa";
//            parser = new ClassLoader(Narsese.class.getClassLoader()) {
//                @Override
//                public synchronized Class findClass(String name) {
//
//                    try {
//                        //URL nurl = Narsese.class.getClass().getResource("nars/NarseseParser.class");
//                        URL nurl = Util.locate(this,
//                                "nars/NarseseParser.class");
//                        URLConnection nc = nurl.openConnection();
//                        long lastModified = nc.getLastModified();
//
//                        Class[] fresh = new Class[1];
//                        byte[] classCode = FileCache.fileCache(Narsese.class.getSimpleName() + "_" + lastModified, () -> {
//                            try {
//                                ParserClassNode node = ParserTransformer.extendParserClass(NarseseParser.class);
//                                fresh[0] = node.getExtendedClass();
//                                return node.getClassCode();
//                            } catch (Exception e) {
//                                throw new RuntimeException(e);
//                            }
//                        });
//                        if (fresh[0]!=null)
//                            return fresh[0];
//                        else {
//                            //ClassLoader cl = Narsese.class.getClassLoader();
//                            try (
//                                    final ReflectiveClassLoader loader
//                                            = new ReflectiveClassLoader(this.getParent());
//                            ) {
//                                return loader.loadClass(narseseClass, classCode);
//                            }
//
////                            return defineClass(classCode, 0, classCode.length);
//                        }
////                        Class<?> protoClass = loadClass(narseseProto);
////
////                        //String narseseClass = Narsese.NarseseParser.class.getName() + "$$grappa";
////                        byte[] classCode = new byte[0];
////                        ParserClassNode node = ParserTransformer.extendParserClass(protoClass);
////                        classCode = node.getClassCode();
////                        //final ClassLoader classLoader = node.getParentClass().getClassLoader();
////                        final Class<?> extendedClass;
////
////
////                        //                        classCode = ParserTransformer.//extendParserClass(Narsese.class).getClassCode();
//////                                getByteCode(protoClass);
////                        //byte[] ba = classCode;
////                        //return defineClass(name, ba, 0, ba.length);
////
////                        //return ParserTransformer.extendParserClass(protoClass).getExtendedClass();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//
//                }
//            }.findClass(narseseClass);
//
//
//    }
    static {

        try {

            ParserClassNode node = ParserTransformer.extendParserClass(NarseseParser.class);
            parser = node.getExtendedClass();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static final String NARSESE_TASK_TAG = "Narsese";

    static class MyParseRunner extends ParseRunner {

        /**
         * Constructor
         *
         * @param rule the rule
         */
        MyParseRunner(Rule rule) {
            super(rule);
        }

        @Override
        public boolean match(MatcherContext context) {
            final Matcher matcher = context.getMatcher();

            //final PreMatchEvent<T> preMatchEvent = new PreMatchEvent<>(context);
            //bus.post(preMatchEvent);

//            if (throwable != null)
//                throw new GrappaException("parsing listener error (before match)",
//                        throwable);

            // FIXME: is there any case at all where context.getMatcher() is null?
            @SuppressWarnings("ConstantConditions") final boolean match = matcher.match(context);
//
//            final MatchContextEvent<T> postMatchEvent = match
//                    ? new MatchSuccessEvent<>(context)
//                    : new MatchFailureEvent<>(context);

            //bus.post(postMatchEvent);

//            if (throwable != null)
//                throw new GrappaException("parsing listener error (after match)",
//                        throwable);

            return match;
        }

    }

    //These should be set to something like RecoveringParseRunner for performance
    private final ParseRunner inputParser;
    private final ParseRunner termParser;

    public Narsese(INarseseParser p) {
        this.inputParser = new MyParseRunner(p.Input());
        this.termParser = new MyParseRunner(p.Term());
    }

    //private final ParseRunner singleTaskRuleParser = new ListeningParseRunner3(TaskRule());

    //private final Map<String,Term> termCache = new HashMap();


    static final ThreadLocal<Narsese> parsers = ThreadLocal.withInitial(
            //() -> Grappa.createParser(Narsese.class)
            () -> {
                try {
                    return new Narsese((INarseseParser) (parser.getConstructor().newInstance()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
    );


    public static Narsese the() {
        return parsers.get();
    }

    /**
     * returns number of tasks created
     */
    public static void tasks(String input, Collection<Task> c, NAR m) throws NarseseException {
        tasks(input, c::add, m);
    }

    public static List<Task> tasks(String input, NAR m) throws NarseseException {
        List<Task> result = $.newArrayList(1);
        tasks(input, result, m);

        return result;
    }

    /**
     * gets a stream of raw immutable task-generating objects
     * which can be re-used because a Memory can generate them
     * ondemand
     */
    static void tasks(String input, Consumer<Task> c, NAR m) throws NarseseException {
        Narsese p = the();

        int parsedTasks = 0;

        ParsingResult r = p.inputParser.run(input);

        int size = r.getValueStack().size();

        for (int i = size - 1; i >= 0; i--) {
            Object o = r.getValueStack().peek(i);

            Object[] y;
            if (o instanceof Task) {
                y = (new Object[]{o});
            } else if (o instanceof Object[]) {
                y = ((Object[]) o);
            } else {
                throw new NarseseException("Parse error: " + input);
            }

            Task t = decodeTask(m, y);

            c.accept(t);
            parsedTasks++;

        }

        if (parsedTasks == 0)
            throw new NarseseException("nothing parsed: " + input);


    }


    /**
     * parse one task
     */
    static public Task task(String input, NAR n) throws NarseseException {
        List<Task> tt = tasks(input, n);
        if (tt.size() != 1)
            throw new NarseseException(tt.size() + " tasks parsed in single-task parse: " + input);
        return tt.get(0);
    }


    static Task decodeTask(NAR nar, Object[] x) {
        if (x.length == 1 && x[0] instanceof Task) {
            return (Task) x[0];
        }

        Term content = ((Term) x[1]).normalize();
            /*if (!(content instanceof Compound)) {
                throw new NarseseException("Task term unnormalizable: " + contentRaw);
                //return Command.task($.func("log", content));
            } else */

        Object px = x[2];

        byte punct =
                px instanceof Byte ?
                        (Byte) x[2]
                        :
                        (byte) (((Character) x[2]).charValue());

        Object _t = x[3];
        Truth t;

        if (_t instanceof Truth) {
            t = (Truth)_t;
        } else if (_t instanceof Float)  {
            //frequency, default confidence
            t = $.t((Float)_t, nar.confDefault(punct));
        } else {
            t = null;
        }



        if (t == null && (punct == BELIEF || punct == GOAL)) {
            t = $.t(1, nar.confDefault(punct));
        }

        Task y = Task.tryTask(content, punct, t, (C, tr) -> {
            //TODO construct directly and remove TaskBuilder


            long[] occ = occurrence(nar, x[4]);

            Task yy = new NALTask(C, punct, tr, nar.time(), occ[0], occ[1], nar.evidence());
            yy.pri(x[0] == null ? nar.priDefault(punct) : (Float) x[0]);
            yy.log(NARSESE_TASK_TAG);
            return yy;
        });

        if (y == null) {
            throw new InvalidTaskException(content, "input: "  + Arrays.toString(x));
        }

        return y;

    }

    private static long[] occurrence(NAR nar, Object O) {
        if (O == null)
            return ETERNAL_ETERNAL;
        else if (O instanceof Tense) {
            long o = Tense.getRelativeOccurrence((Tense) O, nar);
            return new long[] { o, o };
        } else if (O instanceof QuantityTerm) {
            long qCycles = nar.time.toCycles(((QuantityTerm) O).quant);
            long o = nar.time() + qCycles;
            return new long[] { o, o };
        } else if (O instanceof Integer) {
            //cycle time, or default time unit
            long o = nar.time() + (Integer) O;
            return new long[] { o, o };
        } else if (O instanceof Object[]) {
            long[] start = occurrence(nar, ((Object[])O)[0]);
            if (start[0]!=start[1] || start[0] == ETERNAL || start[0] == TIMELESS)
                throw new UnsupportedOperationException();
            long[] end = occurrence(nar, ((Object[])O)[1]);
            if (end[0]!=end[1] || end[0] == ETERNAL || end[0] == TIMELESS)
                throw new UnsupportedOperationException();
            if (start[0] <= end[0]) {
                start[1] = end[0];
                return start;
            } else {
                end[1] = start[0];
                return end;
            }
        } else if (O instanceof long[]){
            return (long[])O;
        } else {
            throw new UnsupportedOperationException("unrecognized occurrence: " + O);
        }
    }

    public static Term term(String s, boolean normalize) throws NarseseException {

        Term y = term(s);
        if (normalize) {
            return nullIfNull(y.normalize());
        } else {
            return y;
            //            Termed existing = index.get(y, false);
            //            if (existing == null)
            //                return y;
            //            else
            //                return existing.term();
        }


    }

    public static Term term(String s) throws NarseseException {
        return Narsese.the()._term(s);
    }


    /**
     * parse one term NOT NORMALIZED
     */
    Term _term(String s) throws NarseseException {

        Exception ee = null;
        try {
            //Term x = singleTerms.get(s);

            ParsingResult r = termParser.run(s);

            ValueStack stack = r.getValueStack();

            if (stack.size() == 1) {
                Object x = stack.pop();

                if (x instanceof String)
                    return Atomic.the((String) x);
                else if (x instanceof Term)
                    return (Term) x;
            } else {
                Object[] x = Util.map(0, stack.size(), stack::peek, Object[]::new);
                ee = new SoftException("incomplete parse: " +  Arrays.toString(x) );
            }



        } catch (Exception e) {
            ee = e;
        }
        throw new NarseseException(s, null, ee);
    }


    public interface INarseseParser {
        Rule Input();

        Rule Term();
    }

    /**
     * Describes an error that occurred while parsing Narsese
     */
    public static class NarseseException extends Exception {

        @Nullable
        public final ParsingResult result;

        /**
         * An invalid addInput line.
         *
         * @param message type of error
         */
        public NarseseException(String message) {
            super(message);
            this.result = null;
        }

        public NarseseException(String input, Throwable cause) {
            this(input, null, cause);
        }

        public NarseseException(String input, ParsingResult result, Throwable cause) {
            super(input + '\n' + (result!=null ? result : cause), cause);
            this.result = result;
        }
    }

}
