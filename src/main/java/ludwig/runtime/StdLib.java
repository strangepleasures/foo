package ludwig.runtime;

import ludwig.interpreter.*;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Name("system")
public class StdLib {
    @Name("true")
    public static final boolean TRUE = true;
    @Name("false")
    public static final boolean FALSE = false;
    @Name("null")
    public static final Object NULL = null;

    public static String str(Object x) {
        return String.valueOf(x);
    }

    @Description("Exponent")
    public static double exp(double x) {
        return Math.exp(x);
    }

    public static double sin(double x) {
        return Math.sin(x);
    }

    public static double cos(double x) {
        return Math.cos(x);
    }

    public static double tan(double x) {
        return Math.tan(x);
    }


    @Name("+")
    public static Number plus(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            return x.doubleValue() + y.doubleValue();
        }
        return x.longValue() + y.longValue();
    }

    @Name("-")
    public static Number minus(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            return x.doubleValue() - y.doubleValue();
        }
        return x.longValue() - y.longValue();
    }

    @Name("*")
    public static Number multiply(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            return x.doubleValue() * y.doubleValue();
        }
        return x.longValue() * y.longValue();
    }

    @Name("/")
    public static double divide(Number x, Number y) {
        return x.doubleValue() / y.doubleValue();
    }

    public static long div(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            return Math.round(x.doubleValue() / y.doubleValue());
        }
        return x.longValue() / y.longValue();
    }

    public static long mod(Number x, Number y) {
        if (x instanceof Double || y instanceof Double) {
            return Math.round(x.doubleValue() % y.doubleValue());
        }
        return x.longValue() % y.longValue();
    }

    public static Iterable map(Iterable source, Function f) {
        return () -> new Iterator() {
            Iterator it = source.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return f.apply(it.next());
            }
        };
    }

    @Name("==")
    public static boolean equals(Object x, Object y) {
        return Objects.equals(x, y);
    }

    public static int compare(Comparable x, Comparable y) {
        return x.compareTo(y);
    }

    @Name("<")
    public static boolean less(Comparable x, Comparable y) {
        return x.compareTo(y) < 0;
    }

    @Name(">")
    public static boolean greater(Comparable x, Comparable y) {
        return x.compareTo(y) > 0;
    }

    @Name("<=")
    public static boolean le(Comparable x, Comparable y) {
        return x.compareTo(y) <= 0;
    }

    @Name(">=")
    public static boolean ge(Comparable x, Comparable y) {
        return x.compareTo(y) >= 0;
    }

    @Delayed
    public static boolean and(Supplier<Boolean> x, Supplier<Boolean> y) {
        return x.get() && y.get();
    }

    @Delayed
    public static boolean or(Supplier<Boolean> x, Supplier<Boolean> y) {
        return x.get() || y.get();
    }

    @Delayed
    public static Object cond(Supplier<Boolean> condition, Supplier<?> option1, Supplier<?> option2) {
        return condition.get() ? option1.get() : option2.get();
    }

    public static Iterable tail(Iterable seq) {
        if (seq instanceof List) {
            List list = (List) seq;
            return list.subList(1, list.size());
        } else {
            return () -> {
                Iterator i = seq.iterator();
                if (i.hasNext()) {
                    i.next();
                }
                return i;
            };
        }
    }

    public static PVector prepend(PVector list, Object x) {
        return list.plus(0, x);
    }

    public static PVector append(PVector list, Object x) {
        return list.plus(x);
    }

    public static void print(Object o) {
        System.out.print(o);
    }

    @Name("to-list")
    public static PVector tolist(Iterable source) {
        if (source instanceof PVector) {
            return (PVector) source;
        }
        PVector v = TreePVector.empty();
        for (Object x : source) {
            v = v.plus(x);
        }
        return v;
    }

    public static Iterator iterator(Iterable it) {
        return it.iterator();
    }

    @Name("has-next")
    public static boolean hasNext(Iterator it) {
        return it.hasNext();
    }

    public static Object next(Iterator it) {
        return it.next();
    }

    public static Object get(Iterable seq, int n) {
        if (seq instanceof List) {
            return ((List)seq).get(n);
        }
        return StreamSupport.stream(seq.spliterator(), false).skip(n - 1).findFirst().get();
    }

    public static String join(Iterable<?> it, String prefix, String separator, String suffix) {
        return StreamSupport.stream(it.spliterator(), false).map(String::valueOf).collect(Collectors.joining(prefix, separator, suffix));
    }

    public static long size(Iterable seq) {
        if (seq instanceof Collection) {
            return ((Collection) seq).size();
        }
        long result = 0;
        for (Object x: seq) {
            result++;
        }
        return result;
    }

    public static Var var(Object value) {
        return new Var(value);
    }

    public static Object set(Var var, Object value) {
        var.setValue(value);
        return value;
    }
}