package cz.tomkren.fishtron.ugen.cache;

import cz.tomkren.fishtron.types.Sub;
import cz.tomkren.fishtron.types.Type;
import cz.tomkren.fishtron.ugen.Gen;
import cz.tomkren.fishtron.ugen.cache.data.SubData;
import cz.tomkren.fishtron.ugen.data.PreSubsRes;
import cz.tomkren.fishtron.ugen.Mover;
import cz.tomkren.fishtron.ugen.data.SubsRes;
import cz.tomkren.fishtron.ugen.data.Ts1Res;
import cz.tomkren.utils.F;
import cz.tomkren.utils.TODO;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.*;

/** Created by user on 13. 9. 2016.*/

public class Cache {

    private Gen gen;

    private Map<String, TypeData> typeDataMap;
    private List<SubData> subsList;
    private Map<String, Integer> sub2id;

    public Cache(Gen gen) {
        this(gen, new HashMap<>(), new ArrayList<>(), new HashMap<>());
    }

    private Cache(Gen gen, Map<String, TypeData> typeDataMap, List<SubData> subsList, Map<String, Integer> sub2id) {
        this.gen = gen;
        this.typeDataMap = typeDataMap;
        this.subsList = subsList;
        this.sub2id = sub2id;
    }

    // -- main public interface ----------------------------------------------------------------------

    public List<Ts1Res> ts1(Type t, int n) {
        List<Ts1Res> ts1results_0 = getTypeData(t).ts1(t, gen, this);
        return Mover.moveTs1Results_0(t, n, ts1results_0);
     }

    public List<SubsRes> subs(int k, Type t, int n) {
        List<SubsRes> results_0 = getSizeTypeData(k, t).getSubsData(gen, this, k, t);
        return Mover.moveSubsResults_0(t, n, results_0);
    }

    public BigInteger getNum(int k, Type t) {
        return getSizeTypeData(k, t).computeNum(gen, this, k, t);
    }

    // -- private methods ---------------------------------------------------------------------------

    private TypeData getTypeData(Type t) {
        return typeDataMap.computeIfAbsent(t.toString(), t_str -> new TypeData());
    }

    private SizeData getSizeTypeData(int k, Type t) {
        return getTypeData(t).getSizeData(k);
    }

    int addSub(Sub sub) {
        String sub_str = sub.toString();
        Integer sub_id = sub2id.get(sub_str);
        if (sub_id == null) {
            sub_id = subsList.size();
            subsList.add(new SubData(sub));
            sub2id.put(sub_str, sub_id);
        } else {
            subsList.get(sub_id).incrementNumUsed();
        }
        return sub_id;
    }

    Sub getSub(int sub_id) {
        return subsList.get(sub_id).getSub();
    }


    // -- Stats ------------------------------------------------------

    public int getNumCachedSubs() {
        return subsList.size();
    }

    public String getCachedSubsStats() {

        double sumNumUsed = 0.0;
        int maxNumUsed = 0;

        for (SubData subData : subsList) {
            int numUsed = subData.getNumUsed();
            sumNumUsed += numUsed;
            if (numUsed > maxNumUsed) {
                maxNumUsed = numUsed;
            }
        }
        int numSubs = subsList.size();
        double meanNumUsed = sumNumUsed / numSubs;

        return "numCachedSubs : "+numSubs+"\n"+
               "sumNumUsed    : "+sumNumUsed+"\n"+
               "meanNumUsed   : "+meanNumUsed+"\n"+
               "maxNumUsed    : "+maxNumUsed;
    }

    // -- toJson method and its utils ---------------------------------------------------------------

    private static final String TYPES_KEY = "types";
    private static final String SUBS_KEY = "subs";


    public JSONObject toJson() {
        return F.obj(
                TYPES_KEY, F.jsonMap(typeDataMap, TypeData::toJson),
                SUBS_KEY,  F.jsonMap(subsList, SubData::toJson)
        );
    }

    public static Cache fromJson(Gen gen, JSONObject data) {
        JSONObject types_json = data.getJSONObject(TYPES_KEY);
        JSONArray subs_json = data.getJSONArray(SUBS_KEY);

        Map<String, TypeData> newTypeDataMap = F.map(types_json, TypeData::fromJson);
        List<SubData> newSubsList = F.map(subs_json, SubData::fromJson);

        Map<String, Integer> newSub2id = new HashMap<>();

        int sub_id = 0;
        for (SubData subInfo : newSubsList) {
            Sub sub = subInfo.getSub();
            String sub_str = sub.toString();
            newSub2id.put(sub_str, sub_id);
            sub_id ++;
        }

        return new Cache(gen, newTypeDataMap, newSubsList, newSub2id);
    }

    /*private static JSONObject subsToJson_debugVersion(List<Sub> subsList) {
        JSONObject ret = new JSONObject();
        for (int i = 0; i < subsList.size(); i++) {
            ret.put(Integer.toString(i), subsList.get(i).toJson());
        }
        return ret;
    }*/


}
