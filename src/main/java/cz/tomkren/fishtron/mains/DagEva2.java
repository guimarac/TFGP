package cz.tomkren.fishtron.mains;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import net.fishtron.eva.simple.Logger;
import net.fishtron.eva.simple.SimpleEvolution;
import cz.tomkren.fishtron.sandbox2.JsonEvolutionOpts;
import cz.tomkren.fishtron.terms.PolyTree;
import cz.tomkren.fishtron.workflows.DagEvolutionLogger;
import net.fishtron.utils.Checker;
import net.fishtron.utils.F;
import net.fishtron.utils.Log;
import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/** Created by user on 23. 6. 2016. */

public class DagEva2 {

    public static void main(String[] args) {

        Log.it("dageva2 [v0.6 beta]");

        if (args.length < 2 || args[0].equals("--help")) {
            Log.it("You must provide two program arguments: <json-config-filename> <log-dir-path>");
            return;
        }

        String jsonConfigFilename = args[0];
        String logPath = args[1];

        Log.it("Program arguments:");
        Log.it("  jsonConfigFileName : " + jsonConfigFilename);
        Log.itln("  logPath            : " + logPath);

        try {
            String configStr = Files.toString(new File(jsonConfigFilename), Charsets.UTF_8);
            Log.itln(jsonConfigFilename +" = "+ configStr);
            JSONObject config = new JSONObject(configStr);

            Checker checker = Checker.mk(config);

            JsonEvolutionOpts opts = new JsonEvolutionOpts(config, checker);
            Logger<PolyTree> dagLogger = new DagEvolutionLogger(config, logPath, checker, opts);

            Log.it("Config [OK] ...");
            Log.it("Generating initial population...");

            SimpleEvolution<PolyTree> eva = new SimpleEvolution<>(opts, dagLogger);

            eva.startIterativeEvolution(1); // todo numRuns ??

            if (config.getBoolean("killServer")) {
                String quitMsg = opts.quitServer();
                Log.it("\n\nKilling server, server kill response: "+ quitMsg);
            }

            checker.results();


        } catch (IOException e) {
            Log.itln("Config file error: "+e.getMessage());
        } catch (JSONException e) {
            Log.err("JSON error: " + e.getMessage());
            throw new Error(e);

        } catch (XmlRpcException e) {
            Log.it("Dag-evaluate server error: Server is probably not running (or it is starting right now).");
            // "Start the server and try again, please."

            long sleepTime = 5000;
            Log.it("Sleeping for "+ (sleepTime/1000) +" seconds...");

            F.sleep(sleepTime);
            main(args);

        }

    }


}
