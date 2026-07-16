package com.anythink.reactnative.utils;

import android.text.TextUtils;

import com.anythink.core.api.ATAdConst;
import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdRequest;
import com.anythink.core.api.ATCustomContentInfo;
import com.anythink.core.api.ATCustomContentResult;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.ATRequestingInfo;
import com.anythink.core.api.ATSharedPlacementConfig;
import com.anythink.core.api.ATWaterfallFilter;
import com.anythink.core.basead.adx.api.ATAdxBidFloorInfo;
import com.anythink.core.mg.api.MgPreLoadAdRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses JSON maps into {@link ATAdRequest}, {@link ATSharedPlacementConfig}, {@link ATWaterfallFilter}.
 */
public final class BridgeJsonMapUtil {
    private BridgeJsonMapUtil() {
    }

    public static Map<String, Object> stringObjectMapFromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            JSONObject o = new JSONObject(json);
            return objectMapFromJsonObject(o);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Convert a JSONObject to a String-Object Map; nested JSONObjects are recursed into Maps.
     */
    public static Map<String, Object> objectMapFromJsonObject(JSONObject o) {
        if (o == null) {
            return null;
        }
        Map<String, Object> m = new HashMap<>();
        Iterator<String> it = o.keys();
        while (it.hasNext()) {
            String k = it.next();
            Object v = o.opt(k);
            if (v == null) {
                m.put(k, null);
            } else if (v instanceof JSONObject) {
                m.put(k, objectMapFromJsonObject((JSONObject) v));
            } else {
                m.put(k, v);
            }
        }
        return m;
    }

    public static ATAdRequest atAdRequestFromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            ATAdRequest.Builder b = new ATAdRequest.Builder();
            if (o.has("channelSource")) {
                b.setChannelSource(o.optInt("channelSource"));
            }
            if (o.has("adxBidFloorInfo")) {
                ATAdxBidFloorInfo floor = atAdxBidFloorInfoFromJson(o.optJSONObject("adxBidFloorInfo"));
                if (floor != null) {
                    b.setATAdxBidFloorInfo(floor);
                }
            }
            if (o.has("preLoadInfo")) {
                MgPreLoadAdRequest pre = mgPreLoadAdRequestFromJson(o.optJSONObject("preLoadInfo"));
                if (pre != null) {
                    b.setPreLoadInfo(pre);
                }
            }
            return b.build();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Load hooks pass {@code extraDic} as a {@code Map}. When it contains a nested map under key
     * {@code "atAdRequest"}, serializes that map to JSON and delegates to {@link #atAdRequestFromJson(JSONObject)}.
     *
     * @param extra root extra map (same keys as native load local-extra payload)
     * @return built request or {@code null} if absent, empty, or invalid
     */
    @SuppressWarnings("unchecked")
    public static ATAdRequest atAdRequestFromLoadExtraDic(Map<String, Object> extra) {
        if (extra == null) {
            return null;
        }
        Object reqObj = extra.get("atAdRequest");
        if (!(reqObj instanceof Map)) {
            return null;
        }
        Map<String, Object> reqMap = (Map<String, Object>) reqObj;
        if (reqMap.isEmpty()) {
            return null;
        }
        try {
            String json = Utils.flutterMapToJsonString(reqMap);
            if (TextUtils.isEmpty(json)) {
                return null;
            }
            return atAdRequestFromJson(new JSONObject(json));
        } catch (Throwable t) {
            return null;
        }
    }

    private static ATAdxBidFloorInfo atAdxBidFloorInfoFromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            double bidFloor = o.optDouble("bidFloor", 0d);
            ATAdConst.CURRENCY c = parseCurrency(o.optString("currency", "USD"));
            if (o.has("extraMap") && o.opt("extraMap") instanceof JSONObject) {
                Map<String, Object> extra = objectMapFromJsonObject(o.optJSONObject("extraMap"));
                return new ATAdxBidFloorInfo(bidFloor, c, extra);
            }
            return new ATAdxBidFloorInfo(bidFloor, c);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static ATAdConst.CURRENCY parseCurrency(String s) {
        if (s == null) {
            return ATAdConst.CURRENCY.USD;
        }
        switch (s.toUpperCase()) {
            case "RMB":
                return ATAdConst.CURRENCY.RMB;
            case "RMB_CENT":
                return ATAdConst.CURRENCY.RMB_CENT;
            case "USD":
            default:
                return ATAdConst.CURRENCY.USD;
        }
    }

    private static MgPreLoadAdRequest mgPreLoadAdRequestFromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            MgPreLoadAdRequest.Builder b = new MgPreLoadAdRequest.Builder();
            if (o.has("requestId")) {
                b.setRequestId(o.optString("requestId", null));
            }
            if (o.has("psId")) {
                b.setPsId(o.optString("psId", null));
            }
            if (o.has("placementId")) {
                b.setPlacementId(o.optString("placementId", null));
            }
            if (o.has("cpEcpmSwitch")) {
                b.setCpEcpmSwitch(o.optInt("cpEcpmSwitch"));
            }
            if (o.has("cpEcpmTimeout")) {
                b.setCpEcpmTimeout(o.optLong("cpEcpmTimeout", 1000L));
            }
            return b.build();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public static ATSharedPlacementConfig sharedPlacementConfigFromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return sharedPlacementConfigFromJson(new JSONObject(json));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public static ATSharedPlacementConfig sharedPlacementConfigFromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            ATSharedPlacementConfig.Builder b = new ATSharedPlacementConfig.Builder();
            if (o.has("rewardVideoLocalExtra")) {
                b.rewardVideoLocalExtra(objectMapFromJsonObject(o.optJSONObject("rewardVideoLocalExtra")));
            }
            if (o.has("interstitialLocalExtra")) {
                b.interstitialLocalExtra(objectMapFromJsonObject(o.optJSONObject("interstitialLocalExtra")));
            }
            if (o.has("splashLocalExtra")) {
                b.splashLocalExtra(objectMapFromJsonObject(o.optJSONObject("splashLocalExtra")));
            }
            if (o.has("bannerLocalExtra")) {
                b.bannerLocalExtra(objectMapFromJsonObject(o.optJSONObject("bannerLocalExtra")));
            }
            if (o.has("nativeLocalExtra")) {
                b.nativeLocalExtra(objectMapFromJsonObject(o.optJSONObject("nativeLocalExtra")));
            }
            return b.build();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private static JSONArray adInfoListToJsonArray(List<ATAdInfo> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (ATAdInfo ad : list) {
            if (ad == null) {
                arr.put(JSONObject.NULL);
            } else {
                try {
                    arr.put(new JSONObject(ad.toString()));
                } catch (Exception e) {
                    arr.put(ad.toString());
                }
            }
        }
        return arr;
    }

    public static Map<String, Object> requestingInfoToExtraDic(ATRequestingInfo info) {
        Map<String, Object> extra = new HashMap<>();
        if (info == null) {
            return extra;
        }
        try {
            extra.put(Const.MultipleLoadedKeys.BiddingAttempt,
                    adInfoListToJsonArray(info.getBiddingAttemptAdInfoList()).toString());
            extra.put(Const.MultipleLoadedKeys.Loading,
                    adInfoListToJsonArray(info.getLoadingAdInfoList()).toString());
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return extra;
    }

    public static String requestingInfoToJson(ATRequestingInfo info) {
        if (info == null) {
            return "";
        }
        try {
            JSONObject o = new JSONObject();
            o.put("biddingAttemptAdInfoList", adInfoListToJsonArray(info.getBiddingAttemptAdInfoList()));
            o.put("loadingAdInfoList", adInfoListToJsonArray(info.getLoadingAdInfoList()));
            return o.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Build an {@link ATWaterfallFilter} from a root object <code>{"groups":[...]}</code>.
     * Conditions inside a group are AND-ed; groups are OR-ed (equivalent to chained <code>.orFilter()</code>).
     */
    public static ATWaterfallFilter waterfallFilterFromGroupsJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        try {
            return waterfallFilterFromGroupsJson(new JSONObject(json));
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public static ATWaterfallFilter waterfallFilterFromGroupsJson(JSONObject root) {
        if (root == null) {
            return null;
        }
        JSONArray groups = root.optJSONArray("groups");
        if (groups == null || groups.length() == 0) {
            return null;
        }
        ATWaterfallFilter wf = new ATWaterfallFilter();
        boolean anyGroup = false;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject g = groups.optJSONObject(i);
            if (g == null) {
                continue;
            }
            if (!groupHasWaterfallConditions(g)) {
                continue;
            }
            if (anyGroup) {
                wf.orFilter();
            }
            applyWaterfallGroup(wf, g);
            anyGroup = true;
        }
        if (!anyGroup) {
            return null;
        }
        return wf;
    }

    private static boolean groupHasWaterfallConditions(JSONObject g) {
        if (g == null) {
            return false;
        }
        if (readStringListFromGroup(g, "networkId") != null) {
            return true;
        }
        if (readBiddingTypeValues(g) != null) {
            return true;
        }
        if (readStringListFromGroup(g, "networkPlacementId") != null) {
            return true;
        }
        JSONObject eCpm = g.optJSONObject("e_cpm");
        return eCpm != null && (eCpm.has("currency") || eCpm.has("moreThanPrice") || eCpm.has("lessThanPrice"));
    }

    private static void applyWaterfallGroup(ATWaterfallFilter wf, JSONObject g) {
        List<String> netIds = readStringListFromGroup(g, "networkId");
        if (netIds != null && !netIds.isEmpty()) {
            wf.filterNetworkIds(netIds);
        }
        List<String> bidTypes = readBiddingTypeValues(g);
        if (bidTypes != null && !bidTypes.isEmpty()) {
            wf.filterBidTypes(bidTypes);
        }
        List<String> npIds = readStringListFromGroup(g, "networkPlacementId");
        if (npIds != null && !npIds.isEmpty()) {
            wf.filterNetworkPlacementIds(npIds);
        }
        JSONObject eCpm = g.optJSONObject("e_cpm");
        if (eCpm != null) {
            ATWaterfallFilter.PriceInterval pi = priceIntervalFromJson(eCpm);
            if (pi != null) {
                wf.filterAdPrice(pi);
            }
        }
    }

    private static List<String> readStringListFromGroup(JSONObject g, String key) {
        if (g == null || !g.has(key)) {
            return null;
        }
        Object raw = g.opt(key);
        if (raw == null || raw == JSONObject.NULL) {
            return null;
        }
        List<String> out = new ArrayList<>();
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            for (int i = 0; i < arr.length(); i++) {
                Object v = arr.opt(i);
                if (v != null && v != JSONObject.NULL) {
                    out.add(String.valueOf(v));
                }
            }
        } else if (raw instanceof String) {
            String s = (String) raw;
            if (!TextUtils.isEmpty(s)) {
                out.add(s);
            }
        } else {
            out.add(String.valueOf(raw));
        }
        return out.isEmpty() ? null : out;
    }

    private static List<String> readBiddingTypeValues(JSONObject g) {
        if (g == null || !g.has("biddingType")) {
            return null;
        }
        Object raw = g.opt("biddingType");
        List<String> tokens = new ArrayList<>();
        if (raw instanceof JSONArray) {
            JSONArray arr = (JSONArray) raw;
            for (int i = 0; i < arr.length(); i++) {
                String name = arr.optString(i, null);
                String mapped = mapBiddingTypeNameToFilterValue(name);
                if (mapped != null) {
                    tokens.add(mapped);
                }
            }
        } else if (raw instanceof String) {
            String mapped = mapBiddingTypeNameToFilterValue((String) raw);
            if (mapped != null) {
                tokens.add(mapped);
            }
        }
        return tokens.isEmpty() ? null : tokens;
    }

    /**
     * Map JSON literals (e.g. NORMAL / C2S / S2S) to the bidding-type strings expected by {@link ATWaterfallFilter}.
     */
    private static String mapBiddingTypeNameToFilterValue(String name) {
        if (name == null) {
            return null;
        }
        switch (name.trim().toUpperCase()) {
            case "NORMAL":
                return ATWaterfallFilter.NORMAL;
            case "S2S":
                return ATWaterfallFilter.S2S;
            case "C2S":
                return ATWaterfallFilter.C2S;
            default:
                return name.trim();
        }
    }

    private static ATWaterfallFilter.PriceInterval priceIntervalFromJson(JSONObject o) {
        if (o == null) {
            return null;
        }
        try {
            ATAdConst.CURRENCY c = parseCurrency(o.optString("currency", "USD"));
            ATWaterfallFilter.PriceInterval pi = new ATWaterfallFilter.PriceInterval(c);
            if (o.has("moreThanPrice")) {
                pi.moreThanPrice(o.optDouble("moreThanPrice"));
            }
            if (o.has("lessThanPrice")) {
                pi.lessThanPrice(o.optDouble("lessThanPrice"));
            }
            return pi;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Builds {@link ATCustomContentResult} from a map/list (ignores {@code ATAdInfo} on items).
     * <p>
     * Shape: {@code { "customContentInfoList": [ { "customContentString": "...", "customContentObject": { } }, {
     * "customContentDouble": 1.0, "customContentObject": null } ] }} or a root {@link List} of item maps.
     */
    @SuppressWarnings("unchecked")
    public static ATCustomContentResult atCustomContentResultFromArgument(Object arg) {
        if (arg == null) {
            return null;
        }
        List<?> listRaw = null;
        if (arg instanceof Map) {
            Object listObj = ((Map<?, ?>) arg).get("customContentInfoList");
            if (listObj instanceof List) {
                listRaw = (List<?>) listObj;
            }
        } else if (arg instanceof List) {
            listRaw = (List<?>) arg;
        }
        if (listRaw == null || listRaw.isEmpty()) {
            return null;
        }
        List<ATCustomContentInfo> out = new ArrayList<>();
        for (Object o : listRaw) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> m = (Map<String, Object>) o;
            Object obj = m.get("customContentObject");
            Object num = m.get("customContentDouble");
            if (num instanceof Number) {
                out.add(new ATCustomContentInfo(((Number) num).doubleValue(), obj));
            } else {
                String s = m.get("customContentString") != null ? m.get("customContentString").toString() : "";
                out.add(new ATCustomContentInfo(s, obj));
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        return new ATCustomContentResult(out);
    }

    public static void applyAtCustomContentToShowConfigBuilder(ATShowConfig.Builder builder, Object arg) {
        if (builder == null) {
            return;
        }
        ATCustomContentResult r = atCustomContentResultFromArgument(arg);
        if (r != null) {
            builder.customContentResult(r);
        }
    }
}
