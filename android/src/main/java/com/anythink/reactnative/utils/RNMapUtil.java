package com.anythink.reactnative.utils;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 下行（JS→Native）数据格式工具。
 *
 * 约定：RN 一律传结构化对象（Codegen → ReadableMap）。
 * - 复杂嵌套参数（filterSpec/atAdRequest/sharedPlacementConfig）：先转 JSON 字符串，
 *   再交 {@link BridgeJsonMapUtil}.*FromJson 复用既有 JSON 解析逻辑。
 * - 简单参数（customMap/extraDic）：ReadableMap 直接转 HashMap，不经 BridgeJsonMapUtil。
 */
public class RNMapUtil {

    /** 复杂嵌套参数：ReadableMap → JSON 字符串（交 BridgeJsonMapUtil 解析）。 */
    public static String readableMapToJsonString(ReadableMap map) {
        if (map == null) {
            return "{}";
        }
        try {
            return new JSONObject(map.toHashMap()).toString();
        } catch (Throwable e) {
            MsgTools.printMsg("readableMapToJsonString error: " + e.getMessage());
            return "{}";
        }
    }

    /** ReadableArray → JSON 字符串（桥接层日志 / 调试）。 */
    public static String readableArrayToJsonString(ReadableArray array) {
        if (array == null) {
            return "[]";
        }
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < array.size(); i++) {
                switch (array.getType(i)) {
                    case Null:
                        jsonArray.put(JSONObject.NULL);
                        break;
                    case Boolean:
                        jsonArray.put(array.getBoolean(i));
                        break;
                    case Number:
                        jsonArray.put(array.getDouble(i));
                        break;
                    case String:
                        jsonArray.put(array.getString(i));
                        break;
                    default:
                        jsonArray.put(String.valueOf(array.getDynamic(i)));
                        break;
                }
            }
            return jsonArray.toString();
        } catch (Throwable e) {
            MsgTools.printMsg("readableArrayToJsonString error: " + e.getMessage());
            return "[]";
        }
    }

    /** 简单参数：ReadableMap → HashMap（直接交原生，不经 BridgeJsonMapUtil）。 */
    public static Map<String, Object> readableMapToHashMap(ReadableMap map) {
        if (map == null) {
            return new HashMap<>();
        }
        return map.toHashMap();
    }

    /**
     * {@code ATSDK.setDeviceInformationData} 专用：RN {@code toHashMap()} 数字为 {@link Double}，
     * 嵌套对象为 {@link Map}，而 Core SDK 要求 {@link Integer}/{@link JSONObject}。
     */
    public static Map<String, Object> deviceInformationDataFromReadableMap(ReadableMap map) {
        Map<String, Object> raw = readableMapToHashMap(map);
        if (raw.isEmpty()) {
            return raw;
        }
        Map<String, Object> out = new HashMap<>();
        putIntegerValue(raw, out, KEY_ANR_COUNT);
        putIntegerValue(raw, out, KEY_DYNAMIC_SCORE);
        putIntegerValue(raw, out, KEY_TOTAL_SCORE);
        putBooleanValue(raw, out, KEY_IS_HIGH_CONSUMPTION);
        putStringValue(raw, out, KEY_ANR_TIMESTAMPS);
        putJsonObjectValue(raw, out, KEY_MMED_M_A_C);
        return out;
    }

    private static final String KEY_ANR_COUNT = "anr_count";
    private static final String KEY_DYNAMIC_SCORE = "dynamic_score";
    private static final String KEY_TOTAL_SCORE = "total_score";
    private static final String KEY_IS_HIGH_CONSUMPTION = "is_high_consumption";
    private static final String KEY_ANR_TIMESTAMPS = "anr_timestamps";
    private static final String KEY_MMED_M_A_C = "mmed_m_a_c";

    private static void putIntegerValue(Map<String, Object> raw, Map<String, Object> out, String key) {
        if (!raw.containsKey(key)) {
            return;
        }
        Object value = raw.get(key);
        if (value instanceof Integer) {
            out.put(key, value);
        } else if (value instanceof Number) {
            out.put(key, ((Number) value).intValue());
        }
    }

    private static void putBooleanValue(Map<String, Object> raw, Map<String, Object> out, String key) {
        if (!raw.containsKey(key)) {
            return;
        }
        Object value = raw.get(key);
        if (value instanceof Boolean) {
            out.put(key, value);
        } else if (value instanceof Number) {
            out.put(key, ((Number) value).intValue() != 0);
        }
    }

    private static void putStringValue(Map<String, Object> raw, Map<String, Object> out, String key) {
        if (!raw.containsKey(key)) {
            return;
        }
        Object value = raw.get(key);
        if (value != null) {
            out.put(key, String.valueOf(value));
        }
    }

    private static void putJsonObjectValue(Map<String, Object> raw, Map<String, Object> out, String key) {
        if (!raw.containsKey(key)) {
            return;
        }
        Object value = raw.get(key);
        if (value instanceof JSONObject) {
            out.put(key, value);
            return;
        }
        if (value instanceof Map) {
            try {
                Object wrapped = Utils.wrapCodecValueForJson(value);
                if (wrapped instanceof JSONObject) {
                    out.put(key, wrapped);
                }
            } catch (Throwable e) {
                MsgTools.printMsg("deviceInformationDataFromReadableMap " + key + " error: " + e.getMessage());
            }
        }
    }
}
