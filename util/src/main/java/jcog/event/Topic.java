
package jcog.event;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * notifies subscribers when a value is emitted
 */
public interface Topic<V> {

    void enable(Consumer<V> o);

    void disable(Consumer<V> o);

    void delete();

    //List<Consumer<V>> all();


    static Ons all(Object obj, BiConsumer<String /* fieldName*/, Object /* value */> f) {
        return all(obj, f, (key)->true);
    }

    static void each(Object obj, Consumer<Field /* fieldName*/> f) {
        /** TODO cache the fields because reflection may be slow */
        for (Field field : obj.getClass().getFields()) {
            Class<?> returnType = field.getType();
            if (returnType.equals(Topic.class)) {
                f.accept(field);
            }

            //System.out.println(obj + "  " + f + " " + returnType);
        }

    }


    /** registers to all public Topic fields in an object
     * BiConsumer<String  fieldName, Object  value >
     * */
    static Ons all(Object obj, BiConsumer<String, Object> f, Predicate<String> includeKey) {

        Ons s = new Ons();

        each(obj, (field) -> {
            String fieldName = field.getName();
            if (includeKey!=null && !includeKey.test(fieldName))
                return;

            try {
                Topic<?> t = ((Topic) field.get(obj));

                // could send start message: f.accept(f.getName(),  );

                s.add(
                        t.on((nextValue) -> f.accept(
                                fieldName /* could also be the Topic itself */,
                                nextValue
                        )));

            } catch (IllegalAccessException e) {
                f.accept( fieldName, e);
            }

        });


        return s;
    }



    /** TODO rename to 'out' to match Streams api */
    void emit(V arg);

    default On on(Consumer<V> o) {
        return new On.Strong<>(this, o);
    }
    default On onWeak(Consumer<V> o) {
        return new On.Weak<>(this, o);
    }


//    @SafeVarargs
//    static <V> Active onAll(@NotNull Consumer<V> o, Topic... w) {
//        Active r = new Active(w.length);
//
//        for (Topic<V> c : w)
//            r.add( c.on(o) );
//
//        return r;
//    }

    int size();

    boolean isEmpty();

    void emitAsync(V inputted, ExecutorService e);

//    String name();


//
//    @Override
//    @Deprecated public void emit(Class channel, Object arg) {
//
//        if (!(arg instanceof Object[]))
//            super.emit(channel, new Object[] { arg });
//        else
//            super.emit(channel, arg);
//    }

}