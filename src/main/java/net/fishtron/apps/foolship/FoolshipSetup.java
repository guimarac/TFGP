package net.fishtron.apps.foolship;

import net.fishtron.eva.IndivGenerator;
import net.fishtron.eva.Operator;
import net.fishtron.eva.multi.*;
import net.fishtron.eva.multi.operators.AppTreeMIGenerator;
import net.fishtron.eva.multi.operators.MultiGenOpFactory;
import net.fishtron.eval.LibPackage;
import net.fishtron.gen.Gen;
import net.fishtron.server.api.Configs;
import net.fishtron.utils.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by tom on 09.10.2017.
 */
public class FoolshipSetup implements MultiEvaSetup {

    public static final String SETUP_NAME = "foolship";


    private final MultiEvaOpts<AppTreeMI> opts;
    private final MultiLogger<AppTreeMI> logger;


    public FoolshipSetup(JSONObject jobConfigOpts, Checker checker) {

        // Basic settings
        int numEvaluations = 1000;
        int numToGen = 40;
        int minPopToOperate = numToGen/2;
        int maxPopSize = numToGen*4;

        // Time settings
        int timeLimit = Integer.MAX_VALUE;
        int sleepTime = 1000;

        // Building parts
        LibPackage libPack = FoolshipLib.mkLibPack(jobConfigOpts);
        //EvalLib evalLib          = libPack.getEvalLib();
        //Gamma gamma              = libPack.getGamma();
        //Type goal                = libPack.getGoal();
        //JSONObject allParamsInfo = libPack.getAllParamsInfo();

        // Generating
        Gen gen = new Gen(libPack.getGamma(), checker); //TODO replace with: Gen.fromJson(generatorDumpPath, gamma, checker)
        int generatingMaxTreeSize = 32;
        IndivGenerator<AppTreeMI> generator = new AppTreeMIGenerator(libPack.getGoal(), generatingMaxTreeSize, gen, libPack.getAllParamsInfo());

        // Operators
        JSONArray operatorsConfig = F.arr(
                F.obj("name","basicTypedXover", "probability",0.4, "maxTreeSize",128),
                F.obj("name","sameSizeSubtreeMutation", "probability",0.3, "maxSubtreeSize",32),
                F.obj("name","oneParamMutation", "probability",0.3, "shiftsWithProbabilities",F.arr(F.arr(-2,0.1), F.arr(-1, 0.4), F.arr(1, 0.4), F.arr(2, 0.1)))
        );
        Distribution<Operator<AppTreeMI>> operators = MultiGenOpFactory.mkOperators(operatorsConfig, checker.getRandom(), gen, libPack.getAllParamsInfo());

        // Evaluation
        double evalTime = Configs.get_double(jobConfigOpts, "evalTime", 30);
        int preferredBufferSize = Configs.get_int(jobConfigOpts, "preferredBufferSize", 32);
        FitnessSignature fitnessSignature = new FitnessSignature(F.arr(F.arr("max","performance")));
        FoolshipEvalManager evalManager = new FoolshipEvalManager(libPack.getEvalLib(), checker, evalTime, preferredBufferSize);

        // Selection
        double tournamentBetterWinsProbability = 0.8;
        MultiSelection<AppTreeMI> parentSelection = new MultiSelection.Tournament<>(tournamentBetterWinsProbability, checker.getRandom());


        opts = new BasicMultiEvaOpts<>(
                numEvaluations, numToGen,
                minPopToOperate, maxPopSize,
                timeLimit, sleepTime,
                generator, fitnessSignature,
                evalManager, parentSelection,
                operators,
                evalManager,
                checker
        );

        logger = FoolshipLogger.mk(jobConfigOpts, checker);
    }

    @Override public MultiEvaOpts<AppTreeMI> getEvaOpts() { return opts; }
    @Override public MultiLogger<AppTreeMI> getLogger() { return logger; }
}
