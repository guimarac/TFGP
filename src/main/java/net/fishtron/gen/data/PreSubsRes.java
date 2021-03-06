package net.fishtron.gen.data;

import net.fishtron.types.Sub;
import java.math.BigInteger;

/** Created by tom on 2. 2. 2017. */

public class PreSubsRes {

    private final BigInteger num;
    private final Sub sigma;

    public PreSubsRes(BigInteger num, Sub sigma) {
        this.num = num;
        this.sigma = sigma;
    }

    public BigInteger getNum() {return num;}
    public Sub getSigma() {return sigma;}
}
