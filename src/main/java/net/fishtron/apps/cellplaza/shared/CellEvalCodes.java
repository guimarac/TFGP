package net.fishtron.apps.cellplaza.shared;

import net.fishtron.eval.EvalCode;
import net.fishtron.trees.AppTree;
import net.fishtron.trees.Leaf;

import java.util.function.Function;

/**Created by tom on 19.03.2017.*/

public class CellEvalCodes {

    public static class SeedImg implements EvalCode {
        @Override
        public Object evalCode(Leaf leaf, Function<AppTree, Object> evalFun, int numArgs) {
            return leaf.getParams().toJson().getString("filename");
        }
    }

    public static class NumSteps implements EvalCode {
        @Override
        public Object evalCode(Leaf leaf, Function<AppTree, Object> evalFun, int numArgs) {
            return leaf.getParams().toJson().getInt("n");
        }
    }

}
