package com.anythink.reactnative.init;

import android.location.Location;
import android.text.TextUtils;

import com.anythink.core.api.ATSDK;
import com.anythink.core.api.ATUserDeviceInfo;
import com.anythink.network.aqy.AqyATInitManager;
import com.anythink.network.baidu.BaiduATCustomController;
import com.anythink.network.baidu.BaiduATInitManager;
import com.anythink.network.beizi.BzATInitManager;
import com.anythink.network.fanwei.FwATInitManager;
import com.anythink.network.gdt.GDTATCustomController;
import com.anythink.network.gdt.GDTATInitManager;
import com.anythink.network.gtm.GTMATCustomController;
import com.anythink.network.gtm.GTMATInitManager;
import com.anythink.network.jingdong.JADATInitManager;
import com.anythink.network.klevin.KlevinATInitManager;
import com.anythink.network.ks.KSATCustomController;
import com.anythink.network.ks.KSATInitManager;
import com.anythink.network.meishu.MsATInitManager;
import com.anythink.network.mimo.MimoATInitManager;
import com.anythink.network.octopus.ZyATInitManager;
import com.anythink.network.oppo.OppoATInitManager;
import com.anythink.network.qumeng.QMATInitManager;
import com.anythink.network.sigmob.SigmobATInitManager;
import com.anythink.network.tanx.TanxATCustomController;
import com.anythink.network.tanx.TanxATInitManager;
import com.anythink.network.tap.TapATInitManager;
import com.anythink.network.toutiao.TTATInitManager;
import com.anythink.network.vivo.VivoATInitManager;
import com.beizi.ad.model.BeiZiLocation;
import com.beizi.fusion.BeiZiCustomController;
import com.bytedance.sdk.openadsdk.LocationProvider;
import com.bytedance.sdk.openadsdk.TTCustomController;
import com.jd.ad.sdk.bl.initsdk.JADPrivateController;
import com.jd.ad.sdk.dl.baseinfo.JADLocation;
import com.meishu.sdk.core.MSAdConfig;
import com.mcto.sspsdk.QyCustomMade;
import com.octopus.ad.model.OctLocation;
import com.qumeng.advlib.core.QMCustomControl;
import com.sigmob.windad.WindCustomController;
import com.heytap.msp.mobad.api.MobCustomController;
import com.tapsdk.tapad.TapAdCustomController;
import com.tapsdk.tapad.TapAdLocation;
import com.tencent.klevin.KlevinCustomController;
import com.tk.adsdk.lib.interf.PtgCustomController;
import com.tk.adsdk.lib.model.AdLocation;
import com.vivo.mobilead.model.VCustomController;
import com.vivo.mobilead.model.VLocation;

import com.miui.zeus.mimo.sdk.MimoCustomController;
import com.miui.zeus.mimo.sdk.MimoLocation;

import com.anythink.reactnative.utils.MsgTools;

import org.json.JSONArray;
import org.json.JSONObject;

import com.octopus.ad.OctopusAdSdkController;

import java.util.HashSet;

/**
 * Holds the ad-network device / permission policy JSON sent from React Native via
 * {@link ATInitManager#setAdSourcePrivacyPolicy(String)} and applies it to each network's
 * CustomController (independent from the {@code putFilter} waterfall JSON).
 * <p>
 * Optional root key {@code "networkFirmIds": [8, 15, ...]} (same ids as TopOn
 * {@code network_firm_id}) lets you apply only to the listed networks, skipping unused
 * controllers. Missing key, empty array, or no valid id is treated as "no filter" and all
 * networks are tried.
 */
public final class AdSourcePrivacyPolicyStore {

    private static final String KEY_NETWORK_FIRM_IDS = "networkFirmIds";

    /**
     * Centralised JSON keys sent from React Native (avoid scattering string literals across each apply method).
     */
    private static final class Keys {
        private Keys() {
        }

        // switches / permissions
        private static final String AGREE_PRIVACY_STRATEGY = "agreePrivacyStrategy";
        private static final String CAN_USE_ANDROID_ID = "isCanUseAndroidId";
        private static final String CAN_USE_PHONE_STATE = "isCanUsePhoneState";
        private static final String CAN_USE_MAC_ADDRESS = "isCanUseMacAddress";
        private static final String CAN_USE_WIFI_STATE = "isCanUseWifiState";
        private static final String CAN_USE_LOCATION = "isCanUseLocation";
        private static final String CAN_USE_WRITE_EXTERNAL = "isCanUseWriteExternal";
        private static final String CAN_USE_APP_LIST = "isCanUseAppList";
        private static final String CAN_USE_OAID = "isCanUseOaid";
        private static final String CAN_USE_IP = "isCanUseIp";
        private static final String CAN_USE_GENERAL_DATA = "isCanUseGeneralData";
        private static final String CAN_PERSONAL_RECOMMEND = "isCanPersonalRecommend";
        private static final String CAN_USE_RECORD_AUDIO = "isCanUsePermissionRecordAudio";

        // custom ids
        private static final String CUSTOM_IMEI = "customIMEI";
        private static final String CUSTOM_OAID = "customOaid";
        private static final String CUSTOM_ANDROID_ID = "customAndroidId";
        private static final String CUSTOM_MAC_ADDRESS = "customMacAddress";
        private static final String CUSTOM_IP = "customIp";

        // location object
        private static final String CUSTOM_LOCATION = "customLocation";
        private static final String LATITUDE = "latitude";
        private static final String LONGITUDE = "longitude";

        // Tanx
        private static final String ID_ALL_SWITCH = "idAllSwitch";

        // sensors / shake
        private static final String FORBID_SENSOR = "forbidSensor";

        // fanwei
        private static final String ALLOW_HARD_DISK_SIZE_KBYTES = "isAllowHardDiskSizeKBytes";

        private static final String CAN_SHAKE = "isCanShake";
        private static final String SHAKE_VALUE = "shakeValue";
        private static final String SHAKE_ACCELERATION = "acceleration";
        private static final String SHAKE_ANGLE = "angle";
        private static final String SHAKE_TIME = "time";
    }

    /**
     * Same ids as each adapter's {@code *ATConst#NETWORK_FIRM_ID} / {@code com.anythink.core.common.base.Const.NETWORK_FIRM}.
     */
    private static final int FIRM_GDT = 8;
    private static final int FIRM_BAIDU = 22;
    private static final int FIRM_KS = 28;
    private static final int FIRM_TANX = 82;
    private static final int FIRM_TT = 15;
    private static final int FIRM_SIGMOB = 29;
    private static final int FIRM_OPPO = 80;
    private static final int FIRM_VIVO = 79;
    private static final int FIRM_BEIZI = 95;
    private static final int FIRM_MEISHU = 93;
    private static final int FIRM_TAP = 69;
    private static final int FIRM_KLEVIN = 51;
    private static final int FIRM_OCTOPUS = 96;
    private static final int FIRM_QUMENG = 74;
    private static final int FIRM_JD = 72;
    private static final int FIRM_FANWEI = 102;
    private static final int FIRM_GTM = 6;
    private static final int FIRM_AQY = 94;
    private static final int FIRM_MIMO = 49;

    /**
     * Apply order matches the original "apply all" sequence; {@link #filterAllows(int, HashSet)} skips entries.
     */
    private static final int[] FIRM_ID_APPLY_ORDER = {
            FIRM_GDT, FIRM_BAIDU, FIRM_KS, FIRM_TANX, FIRM_TT, FIRM_SIGMOB, FIRM_OPPO, FIRM_VIVO, FIRM_BEIZI, FIRM_MEISHU,
            FIRM_TAP, FIRM_KLEVIN, FIRM_OCTOPUS, FIRM_QUMENG, FIRM_JD, FIRM_FANWEI, FIRM_GTM, FIRM_AQY, FIRM_MIMO
    };

    private static volatile String sPolicyJson;

    private AdSourcePrivacyPolicyStore() {
    }

    public static synchronized void setPolicyJson(String json) {
        if (TextUtils.isEmpty(json)) {
            sPolicyJson = null;
            return;
        }
        if (TextUtils.equals(json, sPolicyJson)) {
            return;
        }
        // Validate once to keep state consistent; avoid storing invalid JSON.
        final JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (Throwable t) {
            MsgTools.printMsg("setPolicyJson parse error: " + (t == null ? "" : t.getMessage()));
            return;
        }
        MsgTools.printMsg("setPolicyJson : " + json);
        applyToAvailableNetworks(root);
        sPolicyJson = json;
    }

    public static String getPolicyJson() {
        return sPolicyJson;
    }

    /**
     * Map the unified JSON to each adapter's InitManager (mirrors the setters in network_china_adapter).
     * All network deps are compileOnly: missing classes from a non-integrated network are caught here
     * so they cannot affect other networks.
     */
    private static void applyToAvailableNetworks(final JSONObject root) {
        if (root == null) {
            return;
        }
        try {
            HashSet<Integer> firmFilter = parseNetworkFirmIdFilter(root);
            for (int firmId : FIRM_ID_APPLY_ORDER) {
                if (!filterAllows(firmId, firmFilter)) {
                    continue;
                }
                applyForFirmId(firmId, root);
            }
            ATUserDeviceInfo deviceInfo = new ATUserDeviceInfo();
            deviceInfo.setDevImei(optString(root, Keys.CUSTOM_IMEI));
            deviceInfo.setDevOaid(optString(root, Keys.CUSTOM_OAID));
            ATSDK.setATUserDeviceInfo(deviceInfo);
        } catch (Throwable t) {
            MsgTools.printMsg("applyToAvailableNetworks apply error: " + (t == null ? "" : t.getMessage()));
        }
    }

    private static void logApplyThrowable(String tag, Throwable t) {
        // keep message short; do not spam stack in production
        MsgTools.printMsg(tag + " error: " + (t == null ? "" : t.getMessage()));
    }

    /**
     * @return {@code null} means "no filter": apply to every entry in {@link #FIRM_ID_APPLY_ORDER}.
     *         Returns a non-null set only when {@code networkFirmIds} exists on the root AND at least
     *         one valid firm id parsed; only those ids are then applied.
     *         Missing key, {@code []}, zero length, or no valid id all yield {@code null}
     *         (same as omitting the key – apply to all).
     */
    private static HashSet<Integer> parseNetworkFirmIdFilter(JSONObject root) {
        if (root == null || !root.has(KEY_NETWORK_FIRM_IDS)) {
            return null;
        }
        JSONArray a = root.optJSONArray(KEY_NETWORK_FIRM_IDS);
        if (a == null || a.length() == 0) {
            return null;
        }
        HashSet<Integer> set = new HashSet<>();
        for (int i = 0; i < a.length(); i++) {
            int v = a.optInt(i, Integer.MIN_VALUE);
            if (v != Integer.MIN_VALUE) {
                set.add(v);
            }
        }
        return set.isEmpty() ? null : set;
    }

    private static boolean filterAllows(int firmId, HashSet<Integer> filter) {
        if (filter == null) {
            return true;
        }
        return filter.contains(firmId);
    }

    private static void applyForFirmId(int firmId, JSONObject o) {
        switch (firmId) {
            case FIRM_GDT:
                applyGdt(o);
                break;
            case FIRM_BAIDU:
                applyBaidu(o);
                break;
            case FIRM_KS:
                applyKs(o);
                break;
            case FIRM_TANX:
                applyTanx(o);
                break;
            case FIRM_TT:
                applyTt(o);
                break;
            case FIRM_SIGMOB:
                applySigmob(o);
                break;
            case FIRM_OPPO:
                applyOppo(o);
                break;
            case FIRM_VIVO:
                applyVivo(o);
                break;
            case FIRM_BEIZI:
                applyBeizi(o);
                break;
            case FIRM_MEISHU:
                applyMeishu(o);
                break;
            case FIRM_TAP:
                applyTap(o);
                break;
            case FIRM_KLEVIN:
                applyKlevin(o);
                break;
            case FIRM_OCTOPUS:
                applyOctopus(o);
                break;
            case FIRM_QUMENG:
                applyQumeng(o);
                break;
            case FIRM_JD:
                applyJd(o);
                break;
            case FIRM_FANWEI:
                applyFanwei(o);
                break;
            case FIRM_GTM:
                applyGtm(o);
                break;
            case FIRM_AQY:
                applyAqy(o);
                break;
            case FIRM_MIMO:
                applyMimo(o);
                break;
            default:
                break;
        }
    }

    /**
     * Xiaomi Mimo: {@link MimoATInitManager#setMimoCustomController(MimoCustomController)}.
     * Switches mirror the official MimoCustomController docs (location / WiFi state / app list).
     */
    private static void applyMimo(final JSONObject o) {
        try {
            MimoATInitManager.getInstance().setMimoCustomController(new MimoCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public MimoLocation getMimoLocation() {
                    // Per docs: when isCanUseLocation=false the developer can pass coordinates; when true the SDK collects them, so return null.
                    if (optBoolean(o, Keys.CAN_USE_LOCATION, false)) {
                        return null;
                    }
                    double[] ll = new double[2];
                    if (readCustomLocation(o, ll)) {
                        return new MimoLocation(ll[0], ll[1]);
                    }
                    return null;
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                /**
                 * Whether the SDK is allowed to read the device's installed-app list.
                 * @return true = allowed, false = forbidden.
                 */
                @Override
                public boolean alist() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyMimo", t);
        }
    }

    private static void applyGdt(final JSONObject o) {
        try {
            GDTATInitManager.getInstance().setGDTATCustomController(new GDTATCustomController() {
                /*
                 * Whether the user agrees to the privacy policy. Default: true.
                 */
                @Override
                public boolean getAgreePrivacyStrategy() {
                    return optBoolean(o, Keys.AGREE_PRIVACY_STRATEGY, true);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public boolean isCanUseDeviceId() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCanUseMacAddress() {
                    return isCanUseMacAddressEffective(o);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyGdt", t);
        }
    }

    private static void applyKs(final JSONObject o) {
        try {
            KSATInitManager.getInstance().setKSATCustomController(new KSATCustomController() {
                @Override
                public boolean getCanReadICCID() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean getCanReadMacAddress() {
                    return isCanUseMacAddressEffective(o);
                }

                @Override
                public boolean getCanReadNearbyWifiList() {
                    return shouldAllowNearbyWifiList(o);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyKs", t);
        }
    }

    private static void applyBaidu(final JSONObject o) {
        try {
            BaiduATInitManager.getInstance().setBaiduATCustomController(new BaiduATCustomController() {
                @Override
                public boolean getPermissionReadDeviceID() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean getPermissionLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public boolean getPermissionStorage() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public boolean getPermissionAppList() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean getPermissionOAID() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public boolean getPermissionDeviceInfo() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean getPermissionAppUpdate() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean getPermissionRunningApp() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyBaidu", t);
        }
    }

    private static void applyTanx(final JSONObject o) {
        try {
            TanxATInitManager.getInstance().setCustomController(new TanxATCustomController() {
                @Override
                public String getImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean oaidSwitch() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public boolean imeiSwitch() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean idAllSwitch() {
                    // Tanx 2.5.0+ master switch: keep off by default; integrators can override via this key.
                    return optBoolean(o, Keys.ID_ALL_SWITCH, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyTanx", t);
        }
    }

    private static void applyTt(final JSONObject o) {
        try {
            TTATInitManager.getInstance().setTtCustomController(new TTCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public String getMacAddress() {
                    return optString(o, Keys.CUSTOM_MAC_ADDRESS);
                }

                @Override
                public boolean isCanUseWriteExternal() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public String getDevOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public boolean isCanUsePermissionRecordAudio() {
                    return optBoolean(o, Keys.CAN_USE_RECORD_AUDIO, false);
                }

                @Override
                public boolean alist() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyTt", t);
        }
    }

    private static void applySigmob(final JSONObject o) {
        try {
            SigmobATInitManager.getInstance().setWindCustomController(new WindCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public Location getLocation() {
                    double[] ll = new double[2];
                    if (readCustomLocation(o, ll)) {
                        Location loc = new Location("policy");
                        loc.setLatitude(ll[0]);
                        loc.setLongitude(ll[1]);
                        return loc;
                    }
                    return null;
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseOaid() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public String getDevOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public boolean isCanUseAppList() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applySigmob", t);
        }
    }

    private static void applyOppo(final JSONObject o) {
        try {
            OppoATInitManager.getInstance().setCustomController(new MobCustomController() {
                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public MobCustomController.LocationProvider getLocation() {
                    final double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    return new MobCustomController.LocationProvider() {
                        @Override
                        public double getLatitude() {
                            return ll[0];
                        }

                        @Override
                        public double getLongitude() {
                            return ll[1];
                        }
                    };
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public String getMacAddress() {
                    return optString(o, Keys.CUSTOM_MAC_ADDRESS);
                }

                @Override
                public boolean isCanUseWriteExternal() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public boolean alist() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyOppo", t);
        }
    }

    private static void applyVivo(final JSONObject o) {
        try {
            VivoATInitManager.getInstance().setCustomController(new VCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public VLocation getLocation() {
                    double[] ll = new double[2];
                    if (readCustomLocation(o, ll)) {
                        return new VLocation(ll[0], ll[1]);
                    }
                    return null;
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseWriteExternal() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public boolean isCanPersonalRecommend() {
                    return optBoolean(o, Keys.CAN_PERSONAL_RECOMMEND, false);
                }

                @Override
                public boolean isCanUseImsi() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCanUseApplist() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public boolean isCanUseMac() {
                    return isCanUseMacAddressEffective(o);
                }

                @Override
                public boolean isCanUseIp() {
                    return optBoolean(o, Keys.CAN_USE_IP, false);
                }

                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyVivo", t);
        }
    }

    private static void applyBeizi(final JSONObject o) {
        try {
            BzATInitManager.getInstance().setBeiZiCustomController(new BeiZiCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public BeiZiLocation getLocation() {
                    double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    BeiZiLocation l = new BeiZiLocation();
                    l.setLatitude(String.valueOf(ll[0]));
                    l.setLongitude(String.valueOf(ll[1]));
                    return l;
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCanUseOaid() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public String getDevOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseAppList() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean forbidSensor() {
                    // Beizi: forbid sensor APIs (e.g. shake-to-skip). Default false keeps SDK behavior unchanged.
                    return optBoolean(o, Keys.FORBID_SENSOR, false);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyBeizi", t);
        }
    }

    private static void applyMeishu(final JSONObject o) {
        try {
            MsATInitManager.getInstance().setCustomController(new MSAdConfig.CustomController() {
                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public LocationProvider getTTLocation() {
                    final double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    return new LocationProvider() {
                        @Override
                        public double getLatitude() {
                            return ll[0];
                        }

                        @Override
                        public double getLongitude() {
                            return ll[1];
                        }
                    };
                }

                @Override
                public Location getLocation() {
                    double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    Location loc = new Location("policy");
                    loc.setLatitude(ll[0]);
                    loc.setLongitude(ll[1]);
                    return loc;
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCsjUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseImsi() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public String getMacAddress() {
                    return optString(o, Keys.CUSTOM_MAC_ADDRESS);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public boolean canUseMacAddress() {
                    return isCanUseMacAddressEffective(o);
                }

                @Override
                public boolean canUseNetworkState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public boolean canUseStoragePermission() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public boolean canReadInstalledPackages() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean isCanUsePermissionRecordAudio() {
                    return optBoolean(o, Keys.CAN_USE_RECORD_AUDIO, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyMeishu", t);
        }
    }

    private static void applyTap(final JSONObject o) {
        try {
            TapATInitManager.getInstance().setTapAdCustomController(new TapAdCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public TapAdLocation getTapAdLocation() {
                    double[] ll = new double[2];
                    if (readCustomLocation(o, ll)) {
                        return new TapAdLocation(ll[0], ll[1], 1.0);
                    }
                    return null;
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public boolean isCanUseWriteExternal() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public String getDevOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean alist() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyTap", t);
        }
    }

    private static void applyKlevin(final JSONObject o) {
        try {
            KlevinATInitManager.getInstance().setKlevinCustomController(new KlevinCustomController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public Location getLocation() {
                    double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    Location loc = new Location("policy");
                    loc.setLatitude(ll[0]);
                    loc.setLongitude(ll[1]);
                    return loc;
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public String getDevOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyKlevin", t);
        }
    }

    private static void applyOctopus(final JSONObject o) {
        try {
            ZyATInitManager.getInstance().setOctopusAdSdkController(new OctopusAdSdkController() {
                @Override
                public boolean isCanUseAppList() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public OctLocation getLocation() {
                    double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    OctLocation loc = new OctLocation();
                    loc.setLatitude(ll[0]);
                    loc.setLongitude(ll[1]);
                    return loc;
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseOaid() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public boolean isCanShake() {
                    return optBoolean(o, Keys.CAN_SHAKE, true);
                }

                @Override
                public com.octopus.ad.model.ShakeValue getShakeValue() {
                    // Optional JSON:
                    // "shakeValue": { "acceleration": 0.0, "angle": 0.0, "time": 0 }
                    try {
                        JSONObject sv = o != null ? o.optJSONObject(Keys.SHAKE_VALUE) : null;
                        if (sv == null) {
                            return null;
                        }
                        double acceleration = sv.optDouble(Keys.SHAKE_ACCELERATION, 0.0);
                        double angle = sv.optDouble(Keys.SHAKE_ANGLE, 0.0);
                        int time = sv.optInt(Keys.SHAKE_TIME, 0);
                        return new com.octopus.ad.model.ShakeValue(acceleration, angle, time);
                    } catch (Throwable ignored) {
                        return null;
                    }
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyOctopus", t);
        }
    }

    private static void applyQumeng(final JSONObject o) {
        try {
            QMATInitManager.getInstance().setQMCustomControl(new QMCustomControl() {
                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseOaid() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public boolean isCanUseAppList() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean isCanUsePhoneState() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isCanUseAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public String getMacAddress() {
                    return optString(o, Keys.CUSTOM_MAC_ADDRESS);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyQumeng", t);
        }
    }

    private static void applyJd(final JSONObject o) {
        try {
            JADATInitManager.getInstance().setJADPrivateController(new JADPrivateController() {
                @Override
                public boolean isCanUseLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public JADLocation getLocation() {
                    double[] ll = new double[2];
                    if (readCustomLocation(o, ll)) {
                        return new JADLocation(ll[0], ll[1], 0);
                    }
                    return new JADLocation();
                }

                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUseIP() {
                    return optBoolean(o, Keys.CAN_USE_IP, false);
                }

                @Override
                public String getIP() {
                    // When isCanUseIP=false the developer may pass a custom IP; returns "" if not provided.
                    if (optBoolean(o, Keys.CAN_USE_IP, false)) {
                        return "";
                    }
                    return optString(o, Keys.CUSTOM_IP);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyJd", t);
        }
    }

    private static void applyFanwei(final JSONObject o) {
        try {
            FwATInitManager.getInstance().setFanWeiCustomController(new PtgCustomController() {
                @Override
                public String getMediaDeviceImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public boolean isAllowSDKObtainAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public String getMediaAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public boolean isAllowSDKObtainOaId() {
                    return isCanUseOaidEffective(o);
                }

                @Override
                public String getMediaDeviceOaId() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public String getMediaMacAddress() {
                    return optString(o, Keys.CUSTOM_MAC_ADDRESS);
                }

                @Override
                public boolean isAllowSDKObtainNetworkState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public boolean isAllowSDKObtainStoragePermission() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public boolean isAllowSDKObtainWriteExternal() {
                    return optBoolean(o, Keys.CAN_USE_WRITE_EXTERNAL, false);
                }

                @Override
                public boolean isAllowSDKObtainLocation() {
                    return optBoolean(o, Keys.CAN_USE_LOCATION, false);
                }

                @Override
                public AdLocation getMediaLocation() {
                    double[] ll = new double[2];
                    if (!readCustomLocation(o, ll)) {
                        return null;
                    }
                    AdLocation ad = new AdLocation();
                    ad.setLatitude(ll[0]);
                    ad.setLongitude(ll[1]);
                    return ad;
                }

                @Override
                public boolean isAllowHardDiskSizeKBytes() {
                    return optBoolean(o, Keys.ALLOW_HARD_DISK_SIZE_KBYTES, false);
                }

                @Override
                public boolean isAllowSDKInstallList() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }

                @Override
                public boolean isAllowSDKObtainWifiState() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyFanwei", t);
        }
    }

    private static void applyGtm(final JSONObject o) {
        try {
            GTMATInitManager.getInstance().setGTMATCustomerController(new GTMATCustomController() {
                // General device info: model, brand, cache, battery, screen size, network,
                // user-agent, IP address, OS version, language, timezone, etc.
                @Override
                public boolean getAuthorityGeneralData() {
                    return isCanUseGeneralDataEffective(o);
                }

                // oaid
                @Override
                public boolean getAuthorityDeviceID() {
                    return isCanUseOaidEffective(o);
                }

                // device serial number
                @Override
                public boolean getAuthoritySerialID() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyGtm", t);
        }
    }

    private static void applyAqy(final JSONObject o) {
        try {
            AqyATInitManager.getInstance().setCustomMade(new QyCustomMade() {
                @Override
                public String getOaid() {
                    return optString(o, Keys.CUSTOM_OAID);
                }

                @Override
                public boolean isCanUsePhoneIMEI() {
                    return optBoolean(o, Keys.CAN_USE_PHONE_STATE, false);
                }

                @Override
                public boolean isCanUsePhoneAndroidId() {
                    return optBoolean(o, Keys.CAN_USE_ANDROID_ID, false);
                }

                @Override
                public boolean isCanUsePhoneMacAddress() {
                    return isCanUseMacAddressEffective(o);
                }

                @Override
                public boolean isCanUsePhoneWifiSSID() {
                    return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
                }

                @Override
                public String getDevImei() {
                    return optString(o, Keys.CUSTOM_IMEI);
                }

                @Override
                public String getDevMac() {
                    return optString(o, Keys.CUSTOM_MAC_ADDRESS);
                }

                @Override
                public String getDevAndroidId() {
                    return optString(o, Keys.CUSTOM_ANDROID_ID);
                }

                @Override
                public boolean alist() {
                    return optBoolean(o, Keys.CAN_USE_APP_LIST, false);
                }
            });
        } catch (Throwable t) {
            // missing mediation AAR or runtime error; do not log stack in production
            logApplyThrowable("applyAqy", t);
        }
    }

    /**
     * OAID collection switch.
     */
    private static boolean isCanUseOaidEffective(JSONObject o) {
        return optBoolean(o, Keys.CAN_USE_OAID, false);
    }

    /**
     * Whether networks are allowed to read "general device info"
     * (e.g. GTM {@code getAuthorityGeneralData}: model, brand, battery, screen, network, UA, IP, OS, language, timezone).
     */
    private static boolean isCanUseGeneralDataEffective(JSONObject o) {
        return optBoolean(o, Keys.CAN_USE_GENERAL_DATA, false);
    }

    /**
     * Returns {@code ""} when {@code o == null}, the key is missing, or the value is empty,
     * to avoid NPE from calling {@link JSONObject#optString} on a null reference.
     */
    private static String optString(JSONObject o, String key) {
        if (o == null || key == null) {
            return "";
        }
        return o.optString(key, "");
    }

    private static boolean optBoolean(JSONObject o, String key, boolean def) {
        try {
            if (o == null || !o.has(key)) {
                return def;
            }
            Object v = o.opt(key);
            if (v instanceof Boolean) {
                return (Boolean) v;
            }
            if (v instanceof Number) {
                return ((Number) v).intValue() != 0;
            }
            if (v instanceof String) {
                String s = (String) v;
                if ("1".equals(s)) return true;
                if ("0".equals(s)) return false;
                return Boolean.parseBoolean(s);
            }
        } catch (Throwable ignored) {
        }
        return def;
    }

    private static boolean readCustomLocation(JSONObject o, double[] outLatLon) {
        if (o == null || outLatLon == null || outLatLon.length < 2) {
            return false;
        }
        try {
            JSONObject loc = o.optJSONObject(Keys.CUSTOM_LOCATION);
            if (loc == null) {
                return false;
            }
            if (!loc.has(Keys.LATITUDE) || !loc.has(Keys.LONGITUDE)) {
                return false;
            }
            double lat = loc.getDouble(Keys.LATITUDE);
            double lng = loc.getDouble(Keys.LONGITUDE);
            outLatLon[0] = lat;
            outLatLon[1] = lng;
            return true;
        } catch (Throwable t) {
        }
        return false;
    }

    /**
     * "Nearby WiFi list" only depends on the WiFi-state switch.
     */
    private static boolean shouldAllowNearbyWifiList(JSONObject o) {
        if (o == null) {
            return false;
        }
        return optBoolean(o, Keys.CAN_USE_WIFI_STATE, false);
    }

    /**
     * Whether MAC address may be read.
     */
    private static boolean isCanUseMacAddressEffective(JSONObject o) {
        if (o == null) {
            return false;
        }
        return optBoolean(o, Keys.CAN_USE_MAC_ADDRESS, false);
    }
}
