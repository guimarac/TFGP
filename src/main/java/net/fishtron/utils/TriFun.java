package net.fishtron.utils;

public interface TriFun<A,B,C,D> {
    D apply(A a, B b, C c);
}
