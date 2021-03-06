package net.fishtron.types;

import com.google.common.base.Joiner;
import net.fishtron.utils.AB;
import net.fishtron.utils.F;

import java.util.*;
import java.util.function.Function;

/** Created by tom on 7.11.2015.*/

public class TypeTerm implements Type {

    private final List<Type> args;

    public TypeTerm(List<Type> ts) {
        this.args = ts;
    }

    public TypeTerm(Type... types) {
        this.args = Arrays.asList(types);
    }

    // TODO následující dvě by šli úsporněji, gerujic novej jen když třeba
    @Override
    public Type applyMiniSub(int varId, Type type) {
        return new TypeTerm( F.map(args, t -> t.applyMiniSub(varId, type) ) );
    }

    public List<Type> getArgs() {
        return args;
    }

    @Override
    public Type applySub(Sub sub) {
        return new TypeTerm( F.map(args, t -> t.applySub(sub)) );
    }

    @Override
    public AB<Type, Integer> freshenVars(int startVarId, Sub newVars) {
        List<Type> args2 = new ArrayList<>(args.size());
        int nextId = startVarId;
        for (Type t : args) {
            AB<Type,Integer> p = t.freshenVars(nextId, newVars);
            args2.add(p._1());
            nextId = p._2();
        }
        return new AB<>(new TypeTerm(args2), nextId);
    }

    @Override
    public Type skolemize(Set<Integer> idsAcc) {
        return new TypeTerm(F.map(args, arg -> arg.skolemize(idsAcc)));
    }

    @Override
    public Type deskolemize(Set<Integer> ids) {
        return new TypeTerm(F.map(args, arg -> arg.deskolemize(ids)));
    }

    public <T> T fold(Function<List<T>, T> fNode, Function<Type,T> fLeaf) {
        return fNode.apply(F.map(t -> t instanceof TypeTerm ? ((TypeTerm)t).fold(fNode,fLeaf) : fLeaf.apply(t) , args));
    }

    @Override
    public int getNextVarId(int acc) {
        for (Type arg : args) {
            acc = arg.getNextVarId(acc);
        }
        return acc;
    }

    @Override
    public int getNextVarId_onlySkolemVars(int acc) {
        for (Type arg : args) {
            acc = arg.getNextVarId_onlySkolemVars(acc);
        }
        return acc;
    }

    @Override
    public void getVarIds(TreeSet<Integer> ret) {
        args.forEach(t -> t.getVarIds(ret));
    }

    @Override
    public void getSkolemIds(TreeSet<Integer> acc) {
        args.forEach(t -> t.getSkolemIds(acc));
    }

    @Override
    public String toString() {
        List<Type> sugaredArgs = Types.toSyntaxSugar(args);
        return "("+ Joiner.on(' ').join(sugaredArgs) +")";
    }

    @Override
    public Object toJson() {
        return F.jsonMap(Types.toSyntaxSugar(args), Type::toJson);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeTerm typeTerm = (TypeTerm) o;

        return !(args != null ? !args.equals(typeTerm.args) : typeTerm.args != null);
    }

    @Override
    public int hashCode() {
        return args != null ? args.hashCode() : 0;
    }


}