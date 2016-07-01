package nars.irc;

import com.google.common.html.HtmlEscapers;
import edu.cmu.sphinx.util.props.tools.HTMLDumper;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.concept.OperationConcept;
import nars.experiment.Talk;
import nars.gui.BagChart;
import nars.gui.ConceptBagInput;
import nars.index.CaffeineIndex;
import nars.index.TermIndex;
import nars.nal.nal8.Execution;
import nars.nal.nal8.operator.TermFunction;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.time.RealtimeMSClock;
import nars.util.Texts;
import nars.util.Util;
import nars.util.Wiki;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.layout.FastOrganicLayout;

import java.io.IOException;
import java.util.List;
import java.util.Random;

/**
 * Created by me on 6/20/15.
 */
public class NarseseIRCBot extends Talk {

    private static final Logger logger = LoggerFactory.getLogger(NarseseIRCBot.class);



    public String toString(Object t) {
        if (t instanceof Task) {
            Task tt = (Task)t;

            String ss = ((Task)t).toStringWithoutBudget(nar);

            if (tt.log()!=null && tt.lastLogged().toString().startsWith("Answer"))
                ss += " " + tt.lastLogged();

            return ss;
        }
        return t + " " + t.getClass();
    }

    final NAR nar;
    final IRCBot irc;
    float ircMessagePri = 0.85f;

    public NarseseIRCBot(NAR nar) throws Exception {
        super(nar);
        irc = new IRCBot(
                "irc.freenode.net",
                //"localhost",
                "NARchy", "#netention") {
            @Override
            protected void onMessage(String channel, String nick, String msg) {
                NarseseIRCBot.this.onMessage(channel, nick, msg);
            }
        };

        this.nar = nar;

        final int maxNodes = 128;
        final int maxEdges = 8;

//        new SpaceGraph<Termed>(
//                new ConceptBagInput(nar, maxNodes, maxEdges)
//        ).withTransform(
//                //new Spiral()
//                new FastOrganicLayout()
//        ).show(900, 900);
        BagChart.show((Default) nar);

        addOperators();
    }

    private void addOperators() {
        nar.onExec(new IRCBotOperator("help") {

            @Nullable @Override public Object function(Compound arguments) {
                return "hahha you need to RTFM";
            }
        });
        nar.onExec(new IRCBotOperator("clear") {
            @Nullable @Override public Object function(Compound arguments) {
                @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
                cbag.clear();
                return "Conscious cleared";
            }
        });
        nar.onExec(new IRCBotOperator("memstat") {
            @Nullable @Override public Object function(Compound arguments) {
                return nar.index.summary();
            }
        });
        nar.onExec(new IRCBotOperator("top") {
            @Nullable @Override public Object function(Compound arguments) {
                StringBuilder b = new StringBuilder();
                @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
                cbag.topWhile(c -> {
                    b.append(c.toString()).append("  ");
                    return true;
                }, 10);
                b.append(", pri: " + cbag.priMin() + "<" + Texts.n4(cbag.priHistogram(5)) + ">" + cbag.priMax());
                return b.toString();
            }
        });
        nar.onExec(new IRCBotOperator("readWiki") {

            float pri = 0.5f;

            @Override
            protected Object function(Compound arguments) {

                String base = "simple.wikipedia.org";
                                //"en.wikipedia.org";
                Wiki enWiki = new Wiki(base);

                String lookup = arguments.term(0).toString();
                try {
                    //remove quotes
                    String page = enWiki.normalize(lookup.replace("\"",""));
                    //System.out.println(page);

                    enWiki.setMaxLag(-1);

                    String html = enWiki.getRenderedText(page);
                    html = StringEscapeUtils.unescapeHtml4(html);
                    String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                    //System.out.println(strippedText);

                    List<Term> tokens = hear(strippedText, $.the("wiki_" + page), pri);

                    return "Reading " + base + ":\"" + page + "\": " + strippedText.length() + " characters, " + tokens.size() + " tokens";

                } catch (IOException e) {
                    e.printStackTrace();
                    return e.toString();
                }
            }
        });
    }

    abstract class IRCBotOperator extends TermFunction {


        public IRCBotOperator(String id) {
            super(id);
        }

        @Override
        public boolean autoReturnVariable() {
            return true;
        }



        @Nullable
        @Override
        public Object function(Compound arguments, TermIndex i) {
            Object y = function(arguments);

            send("//" + y.toString());

            //loop back as hearing
            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));

            return y;
        }

        protected abstract Object function(Compound arguments);


    }

//    public synchronized void flush() {
//        if (output != null) {
//            SortedIndex<BLink<Task>> l = output.buffer.list;
//
//            int n = Math.min(paragraphSize, l.size());
//
//            Iterable<String> ii = Iterables.transform(
//                    l.list().subList(0, n), (x) -> toString(x.get()) );
//
//            String s = Joiner.on("  ").join(ii);
//            if (!s.isEmpty()) {
//                send(s);
//            }
//            l.clear();
//        }
//    }

    public static void main(String[] args) throws Exception {
        Global.DEBUG = false;

        Random rng = new XorShift128PlusRandom(1);
        Default nar = new Default(
                1024, 4, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), true)
                //new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
                //new Indexes.WeakTermIndex(256 * 1024, rng)
                //new Indexes.SoftTermIndex(128 * 1024, rng)
                //new Indexes.DefaultTermIndex(128 *1024, rng)
                //,new FrameClock()
                ,new RealtimeMSClock()
        );
        nar.perfection.setValue(0);

        nar.DEFAULT_BELIEF_PRIORITY = 0.1f;
        nar.DEFAULT_GOAL_PRIORITY = 0.8f;

        nar.DEFAULT_QUEST_PRIORITY = 0.5f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.5f;


        nar.conceptActivation.setValue(0.15f);
        nar.cyclesPerFrame.set(32);

        nar.logSummaryGT(System.out, 0.6f);

        new MySTMClustered(nar, 32, '.');

        NarseseIRCBot bot = new NarseseIRCBot(nar);

        nar.loop(20f);

        Util.pause(1000);

        logger.info("Reading corpus..");

        Term sehToMe = $.the("seh");

        final String[] corpus = new String[] {
                "these words are false.",
                "here is a word and the next word.",
                "i hear words.",

                "are these words true?",

                "true is not false.",

                "i say words.",
                "hear my words!",
                "say my words!",

                "if i hear it maybe i say it.",
                "a solution exists for each problem.", //https://simple.wikipedia.org/wiki/Problem
                "talk in a way that helps and feels good!",
                "language rules word combining to form statements and questions.",
                "i learn meaning.",
                "symbols represent ideas, objects, or quantities.",
                "communication transcends literal meaning.", //https://simple.wikipedia.org/wiki/Pragmatics
                "feelings, beliefs, desires, and emotions seem to originate spontaneously.",
                "what is the origin of mental experience?",
                "i am not you.",
                "you are not me.",

                "who am i?",
                "who are you?",
                "i am me.",
                "you are you.",
                "we are we.",
                "they are they.",

                "where is it?",
                "it is here.",
                "it is there.",

                "why is it?",
                "it is.",
                "it is not.",
                "it is because that.",

                "when is it?",
                "it is now.",
                "it is then.",

                "dunno."
        };

        bot.hear(corpus, sehToMe, 0.9f);

    }

//    public void loop(File corpus, int lineDelay) {
//        new Thread(() -> {
//            try {
//                nar.step();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//           /* List<String> lines = null;
//            try {
//                lines = Files.readAllLines(Paths.get(corpus.toURI()));
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
//            while (true)  {
//
//                for (String s : lines) {
//                    s = s.trim();
//                    if (s.isEmpty())continue;
//
//                    nar.input(s);
//
//                    try {
//                        Thread.sleep(lineDelay);
//                    } catch (InterruptedException e) {
//                    }
//                }
//
//            }*/
//        }).start();
//
//    }
//


    final StringBuilder buffer = new StringBuilder();
    int outputBufferLength = 64;

    @Override
    public void say(OperationConcept o, Term content, Term context) {
        super.say(o, content, context);

//        content.recurseTerms(v -> {
//            String s = null;
//            if (v instanceof Atom) {
//                Atom a = (Atom) v;
//                s = a.toStringUnquoted();
//            } else {
//                s = v.toString();
//            }
//            if (s!=null) {
//                synchronized(buffer) {
//                    buffer.append(' ').append(s);
//                }
//            }
//        });

        String x = content.toString().replace("\""," "); //HACK unquote everything
        String toSend = null;
        synchronized(buffer) {
            buffer.append(x);

            if (buffer.length() > outputBufferLength) {
                toSend = buffer.toString().trim().replace(" .", ". ").replace(" !", "! ").replace(" ?", "? ");
                buffer.setLength(0);
            }
        }

        if (toSend!=null)
            send(toSend);

    }

    protected void send(String buffer) {
        if ((irc.writer!=null) && (irc.outputting)) {
            irc.send(irc.channel, buffer);
        } else {
            System.out.println("(not connected)\t" + buffer);
        }
    }


//    public void restart() {
//        if(running !=null) {
//            try {
//                running.stop();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        nar = narBuilder.get();
//
//        send("Ready: " + nar.toString());
//
//
////        nar.eventTaskProcess.on(c -> {
////            if (!c.isInput())
////                //output.buffer.accept(c);
////                say(c.toString());
////        });
////
////        nar.memory.eventAnswer.on(c -> {
////            if (c.getOne().isInput())
////                send(c.toString());
////        });
//
//        running = nar.loop(100f);
//    }



    protected void onMessage(String channel, String nick, String msg) {
        if (channel.equals("unknown")) return;
        if (msg.startsWith("//")) return; //comment or previous output

//        if (msg.equals("RESET")) {
//            restart();
//        }
//        else if (msg.equals("WTF")) {
//            flush();
//        }
//        else {

        @NotNull List<Task> parsed = nar.tasks(msg.replace("http://","") /* HACK */, o -> {
            //logger.error("unparsed narsese {} in: {}", o, msg);
        });

        int narsese = parsed.size();
        if (narsese > 0) {
            for (Task t : parsed) {
                logger.info("narsese({},{}): {}", channel, nick, t);
            }
            parsed.forEach(nar::input);
        } else {
            logger.info("hear({},{}): {}", channel, nick, msg);
            hear(msg, context(channel, nick), ircMessagePri);
        }

    }

    private Term context(String channel, String nick) {
        return $.quote(nick);
        //return $.p($.quote(nick), nar.self); //ignore channel for now
    }

//    protected void hear(String msg, String context) {
//        try {
//            new Twenglish().parse(nick, nar, msg).forEach(t -> {
//                //t.setPriority(INPUT_SENTENCE_PRIORITY);
//                if (t!=null)
//                    nar.input(t);
//            });
//        } catch (Exception f) {
//            System.err.println(msg + ' ' + f);
//            //send(e.toString());
//        }
//    }

}


   /* public void read(String[] sentences, int delayMS, float priority) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                for (String s : sentences) {

                    s = s.trim();
                    if (s.length() < 2) continue;

                    if (!s.endsWith(".")  && !s.endsWith("?") && !s.endsWith("!")) s=s+'.';
                    if (hear("book", s, priority, delayMS) == 0) continue;

                }
            }
        }).start();
    }*/


   /* public int hear(String channel, String m, float priority, long wordDelay) {
        final int delay = 25, endDelay = 1000, tokenMax = 16, tokenMin = 1;
        List<Twokenize.Span> tokens = Twokenize.twokenize(m);
        int nonPunc = Iterables.size(Iterables.filter(tokens, new Predicate<Twokenize.Span>() {

            @Override
            public boolean apply(Twokenize.Span input) {
                return !input.pattern.equals("punct");
            }
        }));

        if (nonPunc > tokenMax) return 0;
        if (nonPunc < tokenMin) return 0;



        String i = "<language --> hear>. :|: \n " + delay + "\n";

        Iterable<String> s = Iterables.transform(tokens, new Function<Twokenize.Span, String>() {

            @Override
            public String apply(Twokenize.Span input) {
                String a = "";
                String pattern = "";
                Term wordTerm;
                if (input.pattern.equals("word")) {
                    a = input.content.toLowerCase().toString();
                    wordTerm = Atom.the(a);
                    pattern = "word";
                }
                TODO apostrophe words
                else if (input.pattern.equals("punct")) {
                    String b = input.content;
                    wordTerm = Atom.quote(b);

                    a = input.content;
                    pattern = "word";
                }
                else {
                    return "";
                }
                else
                a = "\"" + input.content.toLowerCase() + "\"";
                String r = "<" + a + " --> " + pattern + ">. :|:\n";

                Term tt = Inheritance.make(wordTerm, Term.get(pattern));
                char punc = '.';

                Term tt = Operation.make(nar.memory.operate("^say"), new Term[] {wordTerm});
                char punc = '!';

                nar.input(new Sentence(tt, punc, new TruthValue(1.0f, 0.9f), new Stamp(nar.memory, Tense.Present)));
                nar.think(delay);
                r += "say(" + a + ")!\n";
                r += delay + "\n";
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (a.isEmpty()) return "";
                return "<{\"" + a + "\"}-->WORD>.";
                return "(say, \"" + a + "\", " + channel + "). :|:";
            }
        });
        String xs = "say()!\n" + delay + "\n"; clear the buffer before
        for (String w : s) {
            String xs = "$" + Texts.n2(priority) + "$ " + w + "\n";

            System.err.println(xs);
            nar.input(xs);

            try {
                Thread.sleep(wordDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        System.out.println(nar.time() + " HEAR: " + tokens);
        System.out.println("HEAR: " + i);

        String i = "<(*," + c + ") --> PHRASE>.";
        nar.input(i);
        String j = "<(&/," + c + ") --> PHRASE>. :|:";
        nar.input(j);

        try {
            Thread.sleep(endDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return tokens.size();
    }*/
