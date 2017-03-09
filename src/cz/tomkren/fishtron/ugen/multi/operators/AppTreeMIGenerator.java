package cz.tomkren.fishtron.ugen.multi.operators;

import cz.tomkren.fishtron.eva.IndivGenerator;
import cz.tomkren.fishtron.types.Type;
import cz.tomkren.fishtron.ugen.AppTree;
import cz.tomkren.fishtron.ugen.Gen;
import cz.tomkren.fishtron.ugen.eva.AppTreeIndivGenerator;
import cz.tomkren.fishtron.ugen.eval.EvalLib;
import cz.tomkren.fishtron.ugen.multi.AppTreeMI;
import cz.tomkren.utils.F;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;
import java.util.Set;


/**Created by tom on 09.03.2017.*/

public class AppTreeMIGenerator implements IndivGenerator<AppTreeMI> {

    private final Type goalType;
    private final int maxTreeSize;
    private final Gen gen;
    private final EvalLib lib;
    private final JSONObject allParamsInfo;
    private final Random rand;

    public AppTreeMIGenerator(Type goalType, int maxTreeSize, Gen gen, EvalLib lib,
                                 JSONObject allParamsInfo) {
        this.goalType = goalType;
        this.maxTreeSize = maxTreeSize;
        this.gen = gen;
        this.lib = lib;
        this.allParamsInfo = allParamsInfo;
        this.rand = gen.getRand();
    }

    @Override
    public List<AppTreeMI> generate(int numToGenerate) {
        Set<AppTree> treeSet = AppTreeIndivGenerator.generate_cleverSizes(numToGenerate, goalType, maxTreeSize,gen, allParamsInfo, rand);
        return F.map(treeSet, tree -> new AppTreeMI(tree, lib));
    }

}
