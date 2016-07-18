package cz.tomkren.fishtron.latticegen;

import cz.tomkren.fishtron.types.Sub;
import cz.tomkren.fishtron.types.Type;
import cz.tomkren.fishtron.types.TypeTerm;
import cz.tomkren.fishtron.types.Types;
import cz.tomkren.utils.AB;
import cz.tomkren.utils.Checker;
import cz.tomkren.utils.Log;
import cz.tomkren.utils.TODO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/** Created by tom on 18. 7. 2016.*/

public class LSolver {

    private static List<AB<Sub,BigInteger>> subs_1(List<AB<String,Type>> gamma, Type t) {

        List<AB<String,Sub>> allSubs = ts_1(gamma, t);

        Map<String,AB<Sub,BigInteger>> subsMap = new TreeMap<>();
        for (AB<String,Sub> p : allSubs) {
            Sub sub = p._2();

            String key = sub.toString();
            AB<Sub,BigInteger> val = subsMap.get(key);

            if (val == null) {
                subsMap.put(key, new AB<>(sub, BigInteger.ONE));
            } else {
                val.set_2(val._2().add(BigInteger.ONE));
            }
        }

        return new ArrayList<>(subsMap.values());
    }

    private static List<AB<String,Sub>> ts_1(List<AB<String,Type>> gamma, Type t) {

        List<AB<String,Sub>> ret = new ArrayList<>();

        for (AB<String,Type> p : gamma) {
            String s = p._1();
            Type t_s = p._2();

            Type t_s_fresh = fresh(t_s, t);
            Sub mu = Sub.mgu(t, t_s_fresh);

            if (!mu.isFail()) {
                ret.add(new AB<>(s, mu.restrict(t)));
            }
        }

        return ret;
    }




    private static Type fresh(Type typeToFresh, Type typeToAvoid) {
        int startVarId = typeToAvoid.getNextVarId();
        Sub old2fresh = new Sub();
        AB<Type,Integer> p = typeToFresh.freshenVars(startVarId, old2fresh);
        return p._1();
    }

    private static AB<Type,Sub> normalize(Type t) {
        Sub t2nf = new Sub();
        Type nf = t.freshenVars(0, t2nf)._1();
        Sub nf2t = t2nf.inverse();
        if (nf2t.isFail()) {throw new Error("Unable to construct inverse: "+nf2t.getFailMsg());}
        return new AB<>(nf,nf2t);
    }


    public static void main(String[] args) {
        Checker ch = new Checker();

        testNormalizations(ch);
        testTs1(ch);

        ch.results();
    }


    private static List<AB<String,Type>> mkGamma(String... strs) {
        if (strs.length % 2 != 0) {throw new Error("There must be an even number of gamma strings.");}
        List<AB<String,Type>> ret = new ArrayList<>(strs.length/2);
        for (int i = 0; i < strs.length; i+=2) {
            ret.add(new AB<>(strs[i],Types.parse(strs[i+1])));
        }
        return ret;
    }


    private static void testTs1(Checker ch) {

        Log.it("\n-- ts_1 & subs_1 tests ---------------------------------------------------\n");


        Type t = Types.parse("x1 -> x0");
        List<AB<String,Type>> gamma = mkGamma(
                "s", "(a -> (b -> c)) -> ((a -> b) -> (a -> c))",
                "s2","(x5 -> (x0 -> x1)) -> ((x5 -> x0) -> (x5 -> x1))",
                "s3","(y5 -> (x0 -> x1)) -> ((y5 -> x0) -> (y5 -> x1))",
                "k", "a -> (b -> a)",
                "k2","x1 -> (x0 -> x1)",
                "+", "Int -> (Int -> Int)"
        );

        AB<Type,Sub> nf = normalize(t);
        Type t_nf = nf._1();
        Sub nf2t  = nf._2();


        Log.listLn(gamma);


        ts_1(gamma, t);

        Log.it("t: "+t);
        Log.it("t_nf: "+t_nf);
        Log.itln("nf2t: "+nf2t);

        List<AB<String, Sub>> ts1_t = ts_1(gamma, t_nf);
        Log.listLn(ts1_t);

        List<AB<Sub, BigInteger>> subs1_t = subs_1(gamma, t_nf);
        Log.listLn(subs1_t);


    }


    private static void testNormalizations(Checker ch) {

        Log.it("\n-- normalization tests ---------------------------------------------------\n");

        Type t1 = Types.parse("(x111 -> (x11 -> x1)) -> ((x111 -> x11) -> (x111 -> x1))");
        Type t2 = Types.parse("(x0 -> (x11 -> x1)) -> ((x0 -> x11) -> (x0 -> x1))");
        Type t3 = Types.parse("(x2 -> (x1 -> x0)) -> ((x2 -> x1) -> (x2 -> x0))");
        Type t4 = Types.parse("(x2 -> (x0 -> x1)) -> ((x2 -> x0) -> (x2 -> x1))");

        ch.it(t1);
        ch.it(((TypeTerm)t1).fold(Object::toString, Object::toString) +"\n");

        checkNormalisation(ch, t1);
        checkNormalisation(ch, t2);
        checkNormalisation(ch, t3);
        checkNormalisation(ch, t4);

    }

    private static void checkNormalisation(Checker ch, Type t) {
        AB<Type,Sub> p = normalize(t);
        Type nf  = p._1();
        Sub nf2t = p._2();

        ch.it(nf2t.apply(nf),t.toString());
        ch.it(p);
        ch.it("");
    }
}
