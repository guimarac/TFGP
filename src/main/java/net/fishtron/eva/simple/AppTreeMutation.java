package net.fishtron.eva.simple;

import net.fishtron.eva.Operator;
import net.fishtron.trees.AppTree;
import net.fishtron.utils.F;

import java.util.List;

/**Created by tom on 16. 2. 2017.*/



public abstract class AppTreeMutation implements Operator<AppTreeIndiv> {

    public abstract AppTree mutate(AppTree tree);

    private double operatorProbability;

    public AppTreeMutation(double operatorProbability) {
        this.operatorProbability = operatorProbability;
    }

    @Override public double getWeight() {return operatorProbability;}
    @Override public int getNumInputs() {return 1;}

    @Override
    public List<AppTreeIndiv> operate(List<AppTreeIndiv> parents) {
        AppTreeIndiv parent = parents.get(0);
        AppTree child = mutate(parent.getTree());
        return F.singleton(new AppTreeIndiv(child, parent.getLib()));
    }

}
