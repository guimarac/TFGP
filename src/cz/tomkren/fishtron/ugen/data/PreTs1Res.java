package cz.tomkren.fishtron.ugen.data;

import cz.tomkren.fishtron.types.Sub;

/** Created by tom on 5. 2. 2017. */

public class PreTs1Res {
    private final String s;
    private final Sub sigma;

    public PreTs1Res(String s, Sub sigma) {
        this.s = s;
        this.sigma = sigma;
    }

    public String getSym() {return s;}
    public Sub getSigma() {return sigma;}

    @Override
    public String toString() {
        return "PreTs1Res{" +
                "s='" + s + '\'' +
                ", sigma=" + sigma +
                '}';
    }
}
