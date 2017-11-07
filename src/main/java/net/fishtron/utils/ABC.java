package net.fishtron.utils;

// modifikovatelná trojice
public class ABC<A,B,C> {

    public static <A,B,C> ABC<A,B,C> mk(A a, B b, C c) {
        return new ABC<>(a,b,c);
    }

    private A a;
    private B b;
    private C c;

    public ABC(A a, B b, C c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public A _1() {return a;}
    public B _2() {return b;}
    public C _3() {return c;}

    public void set_1(A a) {this.a = a;}
    public void set_2(B b) {this.b = b;}
    public void set_3(C c) {this.c = c;}


    @Override
    public String toString() {
        return "<" + a +","+ b +","+ c + ">";
    }
}
