package cz.tomkren.fishtron.ugen.apps.cellplaza.v2;

import cz.tomkren.fishtron.ugen.apps.cellplaza.PlazaImg;
import cz.tomkren.utils.AA;
import cz.tomkren.utils.Log;
import org.json.JSONArray;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**Created by tom on 11.03.2017.*/

public class CellWorld {

    private final int numStates;

    private Cell[][] cells;
    private int step;
    private Rule rule;
    private String plazaDir;

    private final JSONArray pixelSizes;

    CellWorld(int numStates, String plazaDir, String coreName, Rule rule, JSONArray pixelSizes) {
        this(numStates, plazaDir, coreName, rule, false, pixelSizes);
    }

    CellWorld(int numStates, String plazaDir, String coreName, Rule rule, boolean writeTestImages, JSONArray pixelSizes) {

        this.numStates = numStates;
        this.pixelSizes = pixelSizes;

        this.rule = rule;
        this.step = 0;
        this.plazaDir = plazaDir;

        String filename_seed = plazaDir + "/seed.png";
        //String filename_grad = plazaDir + "/grad.png";
        String filename_sens = plazaDir + "/sens.png";
        String filename_core = coreName == null ? null : plazaDir +"/cores/"+ coreName;

        PlazaImg seed = PlazaImg.mk(filename_seed);
        PlazaImg core = PlazaImg.mk(filename_core);
        //PlazaImg grad = PlazaImg.mk(filename_grad);
        PlazaImg sens = PlazaImg.mk(filename_sens);

        checkSizes(seed, /*grad,*/ sens);

        mkCells(seed, core, /*grad,*/ sens);

        if (writeTestImages) {
            writeCellsImgs();
            writeTests(seed, /*grad,*/ sens);
        }

        Log.it("CellWorld created!\n");
    }

    private void mkCells(PlazaImg seed, PlazaImg core, /*PlazaImg grad,*/ PlazaImg sens) {

        int width = seed.getWidth();
        int height = seed.getHeight();

        cells = new Cell[height][width];

        AA<Integer> coreMarker1 = null;
        AA<Integer> coreMarker2 = null;

        // make cells
        for (int y = 0; y < height; y++) {
            Cell[] cellRow = new Cell[width];
            for (int x = 0; x < width; x++) {

                Color seedColor = seed.getColor(x, y);

                if (seedColor.equals(Color.red)) {
                    seedColor = Cell.stateToColor(Cell.DEAD, numStates);  //Cell.DEAD_COLOR;
                    AA<Integer> coreMarker = AA.mk(x,y);
                    if (coreMarker1 == null) {
                        coreMarker1 = coreMarker;
                    } else if (coreMarker2 == null) {
                        coreMarker2 = coreMarker;
                    } else {
                        throw new Error("More then two core markers, third : "+coreMarker);
                    }
                }

                int state = Cell.colorToState(seedColor, x, y, numStates);
                cellRow[x] = new Cell(state);
            }
            cells[y] = cellRow;
        }

        /*if (grad != null) {
            // set gradients
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    Color gradColor = grad.getColor(x, y);
                    double pRule = Cell.colorToPRule(gradColor, x, y);
                    getCell(x, y).setPRule(pRule);
                }
            }
        }*/

        if (core != null) {

            if (coreMarker1 == null || coreMarker2 == null) {
                throw new Error("Less then two core markers.");
            }

            int coreDx = coreMarker1._1();
            int coreDy = coreMarker1._2();
            int coreWidth = coreMarker2._1() - coreDx + 1;
            int coreHeight = coreMarker2._2() - coreDy + 1;
            Log.it("Core markers: " + coreMarker1 + " & " + coreMarker2 + " ... " + coreWidth + " × " + coreHeight);
            if (coreWidth != core.getWidth() || coreHeight != core.getHeight()) {
                throw new Error("Core size does not match core markers: " +
                        core.getWidth() + " × " + core.getHeight() + " -vs- " + coreWidth + " × " + coreHeight);
            }

            // load core
            for (int y = 0; y < coreHeight; y++) {
                for (int x = 0; x < coreWidth; x++) {
                    int xCell = x + coreDx;
                    int yCell = y + coreDy;
                    Color seedColor = core.getColor(x, y);
                    if (!seedColor.equals(Color.white)) {
                        int state = Cell.colorToState(seedColor, xCell, yCell, numStates);
                        getCell(xCell, yCell).setState(state);
                    }
                }
            }

        }

        // assign neighbours
        int x_max = width - 1;
        int y_max = height - 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = getCell(x, y);

                Color sensorColor = sens == null ? Color.white : sens.getColor(x,y);

                List<AA<Integer>> diffs = Cell.colorToSensorDiffs(sensorColor, x, y, x_max, y_max);
                Cell[] neighbours = new Cell[diffs.size()];

                int n = 0;
                for (AA<Integer> diff : diffs) {
                    neighbours[n] = getCell(x + diff._1(), y + diff._2());
                    n++;
                }

                cell.setNeighbours(neighbours);
            }
        }
    }

    void step() {
        Log.it_noln("Computing next step ("+(step+1)+") ... ");
        eachCell(this::computeNextState);
        eachCell(Cell::setStateToNextState);
        Log.it("done");
        step++;
    }

    void step(int numSteps) {
        for (int s = 0; s < numSteps; s++) {
            step();
        }
    }

    private void computeNextState(Cell cell) {
        cell.computeNextState(rule);
    }

    private void eachCell(Consumer<Cell> processCell) {
        int width  = getWidth();
        int height = getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                processCell.accept(getCell(x,y));
            }
        }
    }


    public int getWidth() {
        return cells[0].length;
    }

    public int getHeight() {
        return cells.length;
    }

    private Cell getCell(int x, int y) {
        return cells[y][x];
    }

    private PlazaImg toImg(Function<Cell,Color> showFun) {

        Log.it_noln("Converting cells to image ... ");

        int width  = getWidth();
        int height = getHeight();

        Color[][] pixels = new Color[height][width];

        for (int y = 0; y<height; y++) {
            Color[] pixelRow = new Color[width];
            for (int x = 0; x<width; x++) {
                pixelRow[x] = showFun.apply(getCell(x,y));
            }
            pixels[y] = pixelRow;
        }

        Log.it("done");
        return new PlazaImg(pixels, pixelSizes);
    }

    private PlazaImg toStateImg() {
        return toImg(cell -> Cell.stateToColor(cell.getState(), numStates));
    }

    //private PlazaImg toGradImg() {return toImg(Cell::getPRuleColor);}

    private PlazaImg toNumNeighbourImg() {
        return toImg(Cell::getNumNeighbourColor);
    }


    void writeState() {
        PlazaImg stateImg = toStateImg();
        stateImg.writeImage(plazaDir+"/run", "state"+getStepXXX()+".png");
    }

    String writeState(int indivId) {
        PlazaImg stateImg = toStateImg();
        String locDir = "indivs";
        String filename = "indiv"+indivId+".png";
        stateImg.writeImage(plazaDir+"/"+locDir, filename);
        return locDir+"/"+filename;
    }


    private String getStepXXX() {
        return (step<100?"0":"") + (step<10?"0":"") + step;
    }



    private void writeCellsImgs() {
        //toGradImg().writeImage(plazaDir+"/out/cells_grad.png");
        toNumNeighbourImg().writeImage(plazaDir+"/out","cells_sens.png");
    }

    private void checkSizes(PlazaImg seed, /*PlazaImg grad,*/ PlazaImg sens) {
        Log.it_noln("Checking sizes ... ");
        //if (grad != null) {grad.checkSize(seed);}
        if (sens != null) {sens.checkSize(seed);}
        Log.it("OK");
    }

    private void writeTests(PlazaImg seed, /*PlazaImg grad,*/ PlazaImg sens) {
        if (seed != null) {seed.writeImage(plazaDir+"/out","test_seed.png");}
        //if (grad != null) {grad.writeImage(plazaDir+"/out","test_grad.png");}
        if (sens != null) {sens.writeImage(plazaDir+"/out","test_sens.png");}
    }
}
