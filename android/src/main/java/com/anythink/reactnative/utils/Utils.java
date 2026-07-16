package com.anythink.reactnative.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * JSON / 数组工具（从通用工具移植，仅保留与桥接无 UI/Core 依赖的部分）。
 * 供 {@link BridgeJsonMapUtil} 与事件层复用。
 */
public class Utils {

    public static boolean checkMethodInArray(String[] methodArray, String methodName) {
        if (methodArray == null || methodName == null) {
            return false;
        }
        for (String method : methodArray) {
            if (methodName.equals(method)) {
                return true;
            }
        }
        return false;
    }

    public static Map<String, Object> jsonStrToMap(String jsonStr) throws JSONException {
        Map<String, Object> data = new HashMap<>();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.opt(key);
                if (value instanceof JSONArray) {
                    data.put(key, value.toString());
                } else if (value instanceof JSONObject) {
                    data.put(key, value.toString());
                } else {
                    data.put(key, value);
                }
            }
        } catch (Throwable e) {
            MsgTools.printMsg("jsonStrToMap error: " + e.getMessage());
        }
        return data;
    }

    public static String mapToJsonString(Map<String, Object> map) {
        try {
            return new JSONObject(map).toString();
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * 把嵌套 {@code HashMap}/{@code ArrayList}（如 RN ReadableMap.toHashMap 的产物）序列化为 JSON，
     * 供 {@link BridgeJsonMapUtil} 等解析。
     */
    public static String flutterMapToJsonString(Map<String, ?> map) throws JSONException {
        if (map == null) {
            return "";
        }
        Object wrapped = wrapCodecValueForJson(map);
        if (wrapped instanceof JSONObject) {
            return ((JSONObject) wrapped).toString();
        }
        return "";
    }

    static Object wrapCodecValueForJson(Object value) throws JSONException {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value;
        }
        if (value instanceof Map) {
            JSONObject o = new JSONObject();
            Map<?, ?> m = (Map<?, ?>) value;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getKey() == null) {
                    continue;
                }
                o.put(String.valueOf(e.getKey()), wrapCodecValueForJson(e.getValue()));
            }
            return o;
        }
        if (value instanceof List) {
            JSONArray a = new JSONArray();
            for (Object item : (List<?>) value) {
                a.put(wrapCodecValueForJson(item));
            }
            return a;
        }
        if (value instanceof Boolean || value instanceof Number || value instanceof String) {
            return value;
        }
        return String.valueOf(value);
    }
}
