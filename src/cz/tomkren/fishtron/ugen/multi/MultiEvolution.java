package cz.tomkren.fishtron.ugen.multi;

import cz.tomkren.fishtron.eva.Operator;
import cz.tomkren.utils.AB;
import cz.tomkren.utils.F;

import cz.tomkren.utils.Log;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Created by tom on 07.03.2017. */

public class MultiEvolution<Indiv extends MultiIndiv> {

    private MultiEvaOpts<Indiv> opts;
    private MultiLogger<Indiv> logger;

    private MultiPopulation<Indiv> population;
    private int numSentIndividuals;
    private int numEvaluatedIndividuals;

    private long startTime;

    public MultiEvolution(MultiEvaOpts<Indiv> opts, MultiLogger<Indiv> logger) {
        this.opts = opts;
        this.logger = logger;
    }

    public void startIterativeEvolution(int run) {
        startTime = System.nanoTime();
        checkOptions();

        makeEmptyPopulation();
        MultiEvalResult<Indiv> evalResult = null;

        int i = 1;
        while (isEvaluationUnfinished()) {

            Log.it("\n\n -- Evolution Loop #"+i+" -----------------\n");

            if (isGeneratingNeeded()) {
                log("Generating initial population...");
                evalResult = sendToEval(generateIndividuals(evalResult));
            } else if(isPopulationLargeEnoughForOperating() && isSendingNeeded()) {
                Log.it("Making children..");
                List<AB<Indiv,JSONObject>> children = makeChildren(evalResult,population,opts.getNumEvaluations());
                evalResult = children.size() > 0 ? sendToEval(children) : justAskForResults();
                Log.it(children.size() +" children made.");
            } else {
                Log.it("Just asking for results..");
                evalResult = justAskForResults();
            }

            updatePopulation(evalResult);
            logger.log(run, numEvaluatedIndividuals, evalResult);

            if (evalResult.isEmpty()) {
                F.sleep(opts.getSleepTime());
            }

            i++;
        }
    }

    private void checkOptions() {
        if (opts.getNumIndividualsToGenerate() < opts.getMinPopulationSizeToOperate()) {
            throw new Error("Evolver Options Check FAIL: numIndividualsToGenerate < minPopulationSizeToOperate");
        }
    }

    private void makeEmptyPopulation() {
        population = new MultiPopulation<>(opts.getIsMaximizationList());
        numSentIndividuals = 0;
        numEvaluatedIndividuals = 0;
    }

    private boolean isEvaluationUnfinished() {
        double runTimeInSeconds = (System.nanoTime()-startTime)/1E9;
        boolean stillSomeTime = opts.getTimeLimit() - runTimeInSeconds > 0.0;
        return stillSomeTime && numEvaluatedIndividuals < opts.getNumEvaluations();
    }

    private boolean isGeneratingNeeded() {
        return numSentIndividuals < opts.getNumIndividualsToGenerate();
    }


    private void log(Object x) {
        opts.getChecker().it(x);
    }

    private List<AB<Indiv,JSONObject>> generateIndividuals(MultiEvalResult<Indiv> evalResult) {


        int yetToGenerate = opts.getNumIndividualsToGenerate() - numSentIndividuals;
        int evaluatorCapabilities = evalResult == null ? opts.getEvalManager().getEvalPoolSize(yetToGenerate)
                                                       : evalResult.getNumEvaluatedIndividuals();

        int numToGenerate = opts.generateMaxOfCapabilitiesAndNeeds()
                          ? Math.max(evaluatorCapabilities, yetToGenerate)
                          : Math.min(evaluatorCapabilities, yetToGenerate);

        List<Indiv> allGenIndivs = new ArrayList<>(numToGenerate);

        while (allGenIndivs.size() < numToGenerate) {
            List<Indiv> genIndivs = opts.getGenerator().generate(numToGenerate);

            List<Indiv> filtered_genIndivs = population.keepOnlyNew_and_checkout(genIndivs);
            allGenIndivs.addAll(filtered_genIndivs);
        }

        return F.map(allGenIndivs, indiv -> new AB<>(indiv, mkIndivJson_forGenerated()));
    }

    private MultiEvalResult<Indiv> sendToEval(List<AB<Indiv,JSONObject>> indivs) {
        numSentIndividuals += indivs.size();
        return opts.getEvalManager().evalIndividuals(indivs);
    }

    private boolean isPopulationLargeEnoughForOperating() {
        return numEvaluatedIndividuals >= opts.getMinPopulationSizeToOperate();
    }

    private boolean isSendingNeeded() {
        return numSentIndividuals < opts.getNumEvaluations();
    }

    private List<AB<Indiv,JSONObject>> makeChildren(MultiEvalResult<Indiv> evalResult, MultiPopulation<Indiv> parentPop, int maxNumToMake) {

        List<AB<Indiv,JSONObject>> children = new ArrayList<>();

        int requestedByEvaluator = evalResult.getNumEvaluatedIndividuals();
        int yetToBeSent = maxNumToMake - numSentIndividuals;
        int numChildren = Math.min(requestedByEvaluator, yetToBeSent);

        while (children.size() < numChildren) {

            Operator<Indiv> operator = opts.getOperators().get(opts.getRandom());
            List<Indiv> parents = selectParents(operator.getNumInputs(), parentPop);

            List<Indiv> chs = operator.operate(parents);

            Log.it("SelectedGenOp: "+operator.getOperatorInfo());

            int neededToMakeYet = numChildren - children.size();
            chs = F.take(neededToMakeYet, chs);

            List<Indiv> filtered_chs = population.keepOnlyNew_and_checkout(chs);

            List<AB<Indiv,JSONObject>> filtered_chsWithInfo = F.map(filtered_chs, ch -> new AB<>(ch, mkIndivJson(operator, parents)));
            children.addAll(filtered_chsWithInfo);
        }

        assert children.size() == numChildren;
        return children;
    }

    private List<Indiv> selectParents(int numParents, MultiPopulation<Indiv> parentPop) {
        MultiSelection<Indiv> parentSelection = opts.getParentSelection();
        List<Indiv> parents = new ArrayList<>(numParents);
        for (int i = 0; i < numParents; i++) {
            Indiv selectedParent = parentPop.select(parentSelection);
            parents.add(selectedParent);
        }
        return parents;
    }

    private MultiEvalResult<Indiv> justAskForResults() {
        return opts.getEvalManager().justAskForResults();
    }

    private void updatePopulation(MultiEvalResult<Indiv> evalResult) {
        List<Indiv> evaluatedIndividuals = evalResult.getIndividuals();
        numEvaluatedIndividuals += evaluatedIndividuals.size();
        for (Indiv indiv : evaluatedIndividuals) {
            population.addIndividual(indiv);
            if (population.size() > opts.getMaxPopulationSize()) {
                population.removeWorstIndividual();
            }
        }
        Log.it("Population Info: "+ population.getPopulationInfo());
    }









    private static JSONObject mkIndivJson_forGenerated() {
        return F.obj(
                "operator", F.obj("name","generator", "generated",true),
                "parents",  F.arr()
        );
    }

    private JSONObject mkIndivJson(Operator<Indiv> operator, List<Indiv> parents) {
        return F.obj(
                "operator", operator.getOperatorInfo(),
                "parents", F.jsonMap(parents, this::mkParentInfo)
        );
    }

    private JSONObject mkParentInfo(Indiv parent) {
        return F.obj(
            "fitness", F.jsonMap(parent.getFitness()),
            "id", parent.getId()
        );
    }

}
