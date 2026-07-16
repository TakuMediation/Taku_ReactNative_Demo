package com.anythink.reactnative.nativead;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.anythink.core.api.ATAdInfo;
import com.anythink.core.api.ATAdSourceStatusListener;
import com.anythink.core.api.ATNetworkConfirmInfo;
import com.anythink.core.api.ATShowConfig;
import com.anythink.core.api.AdError;
import com.anythink.nativead.api.ATNative;
import com.anythink.nativead.api.ATNativeAdView;
import com.anythink.nativead.api.ATNativeDislikeListener;
import com.anythink.nativead.api.ATNativeEventExListener;
import com.anythink.nativead.api.ATNativeMaterial;
import com.anythink.nativead.api.ATNativeNetworkListener;
import com.anythink.nativead.api.ATNativePrepareExInfo;
import com.anythink.nativead.api.ATNativePrepareInfo;
import com.anythink.nativead.api.NativeAd;
import com.anythink.reactnative.event.ATReactNativeEventEmitter;
import com.anythink.reactnative.listener.AdListenerFactory;
import com.anythink.reactnative.utils.Const;
import com.anythink.reactnative.utils.MsgTools;
import com.anythink.reactnative.utils.Utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一个 placementId 的 Native Helper（支持列表多广告）。
 * 持 ATNative(加载器) + 多条 NativeAd（按 adId 索引，对齐安卓「单 ATNative + 多 NativeAd 对象」）。
 *
 * adId = 跨桥的 NativeAd 对象引用：fetchNativeAd 从缓存池弹一条时分配（placementId#序号），
 * JS 透传，用户无感。素材/渲染/绑定/事件/生命周期/销毁均按 adId 定位到具体那条。
 * 空 adId 回退到最近一条（mLastAdId），保证单广告/改造半态不崩。
 *
 * 加载期回调走 ATNativeNetworkListener，展示期走 ATNativeEventExListener，统一 emit NativeCall（payload 带 adId）。
 */
public class ATNativeHelper {

    private final Context mAppContext;
    private final String mPlacementId;
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private ATNative mATNative;
    private Map<String, Object> mLocalExtra;
    private Map<String, Object> mTkExtra;
    private String mShowScenarioId;
    private String mShowCustomExt;

    /** 按 adId 索引的多条广告（弹出顺序，LinkedHashMap 便于调试）。 */
    private final Map<String, NativeAd> mNativeAds = new LinkedHashMap<>();
    /** 每条广告对应的容器（视图按 adId 绑定，不再 1:1）。 */
    private final Map<String, ATNativeContainer> mContainers = new LinkedHashMap<>();
    /** 每条广告的模板渲染去重标志。 */
    private final Map<String, Boolean> mTemplateRendered = new HashMap<>();
    /** 自渲染已 renderAdContainer 去重（每 offer 仅一次，重复调会 clear 掉已注册的曝光/点击）。 */
    private final Map<String, Boolean> mSelfRendered = new HashMap<>();
    /** adId 自增序号。 */
    private final AtomicInteger mAdSeq = new AtomicInteger(0);
    /** 最近一条 adId（空 adId 回退用）。 */
    private String mLastAdId;

    /** 空/未知 adId 回退到最近一条；都没有则 null。 */
    private String resolveAdId(String adId) {
        if (adId != null && mNativeAds.containsKey(adId)) {
            return adId;
        }
        return mLastAdId;
    }

    private NativeAd adOf(String adId) {
        String id = resolveAdId(adId);
        return id != null ? mNativeAds.get(id) : null;
    }

    /** Fabric ViewManager 注入 SDK 容器，按 adId 绑定。 */
    public void setContainer(String adId, ATNativeContainer container) {
        String id = resolveAdId(adId);
        if (id != null) {
            mContainers.put(id, container);
        }
    }

    public ATNativeAdView getContainer(String adId) {
        String id = resolveAdId(adId);
        return id != null ? mContainers.get(id) : null;
    }

    public ATNativeHelper(Context appContext, String placementId) {
        mAppContext = appContext;
        mPlacementId = placementId;
    }

    /** 加载期事件（无具体 adId，传 null）。 */
    private void emit(String callbackKey, ATAdInfo adInfo, String errMsg, Map<String, Object> extra) {
        emit(callbackKey, null, adInfo, errMsg, extra);
    }

    /** 展示期事件带 adId（按 placementId+adId 分流到对应 cell）。 */
    private void emit(String callbackKey, String adId, ATAdInfo adInfo, String errMsg, Map<String, Object> extra) {
        Map<String, Object> info = null;
        if (adInfo != null) {
            try {
                info = Utils.jsonStrToMap(adInfo.toString());
            } catch (Throwable ignore) {
            }
        }
        Map<String, Object> ex = extra;
        if (adId != null) {
            ex = (extra != null) ? new HashMap<>(extra) : new HashMap<String, Object>();
            ex.put(Const.CallbackKey.adId, adId);
        }
        ATReactNativeEventEmitter.getInstance().sendCallback(
                Const.CallbackMethodCall.NativeCall, callbackKey, mPlacementId, info, errMsg, ex);
    }

    public void setLocalExtra(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        if (mLocalExtra == null) {
            mLocalExtra = new HashMap<>();
        }
        mLocalExtra.putAll(map);
        if (mATNative != null) {
            mATNative.setLocalExtra(mLocalExtra);
        }
    }

    public void setShowConfig(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            mShowScenarioId = "";
            mShowCustomExt = "";
            return;
        }
        Object scenario = map.get("scenarioId");
        Object ext = map.get("showCustomExt");
        mShowScenarioId = scenario != null ? String.valueOf(scenario) : "";
        mShowCustomExt = ext != null ? String.valueOf(ext) : "";
    }

    public void setTKExtra(Map<String, Object> map) {
        mTkExtra = map;
    }

    private ATNative getOrCreate() {
        if (mATNative == null) {
            mATNative = new ATNative(mAppContext, mPlacementId, new ATNativeNetworkListener() {
                @Override
                public void onNativeAdLoaded() {
                    MsgTools.printCallback("Native", "ATNativeHelper", "onNativeAdLoaded",
                            "placementId=" + mPlacementId);
                    emit(Const.NativeCallback.LoadedCallbackKey, null, null, null);
                }

                @Override
                public void onNativeAdLoadFail(AdError adError) {
                    MsgTools.printCallback("Native", "ATNativeHelper", "onNativeAdLoadFail",
                            "placementId=" + mPlacementId + "--error=" + adError.getFullErrorInfo());
                    emit(Const.NativeCallback.LoadFailCallbackKey, null, adError.getFullErrorInfo(), null);
                }
            });
            if (mLocalExtra != null) {
                mATNative.setLocalExtra(mLocalExtra);
            }
            mATNative.setAdMultipleLoadedListener(AdListenerFactory.multipleLoaded(
                    Const.CallbackMethodCall.NativeCall,
                    Const.NativeCallback.MultipleLoadedCallbackKey, mPlacementId));
            mATNative.setAdSourceStatusListener(new ATAdSourceStatusListener() {
                @Override
                public void onAdSourceBiddingAttempt(ATAdInfo adInfo) {
                    MsgTools.printAdSource("ADSource--Bidding--Attempt", "ATNativeHelper", "onAdSourceBiddingAttempt",
                            MsgTools.adSourceParams(mPlacementId, adInfo));
                    emit(Const.NativeCallback.AdSourceBiddingAttemptCallbackKey, adInfo, null, null);
                }

                @Override
                public void onAdSourceBiddingFilled(ATAdInfo adInfo) {
                    MsgTools.printAdSource("ADSource--Bidding--Filled", "ATNativeHelper", "onAdSourceBiddingFilled",
                            MsgTools.adSourceParams(mPlacementId, adInfo));
                    emit(Const.NativeCallback.AdSourceBiddingFilledCallbackKey, adInfo, null, null);
                }

                @Override
                public void onAdSourceBiddingFail(ATAdInfo adInfo, AdError adError) {
                    MsgTools.printAdSource("ADSource--Bidding--Fail", "ATNativeHelper", "onAdSourceBiddingFail",
                            MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                    emit(Const.NativeCallback.AdSourceBiddingFailCallbackKey, adInfo,
                            adError != null ? adError.getFullErrorInfo() : null, null);
                }

                @Override
                public void onAdSourceAttempt(ATAdInfo adInfo) {
                    MsgTools.printAdSource("ADSource--AD--Start", "ATNativeHelper", "onAdSourceAttempt",
                            MsgTools.adSourceParams(mPlacementId, adInfo));
                    emit(Const.NativeCallback.AdSourceAttemptCallbackKey, adInfo, null, null);
                }

                @Override
                public void onAdSourceLoadFilled(ATAdInfo adInfo) {
                    MsgTools.printAdSource("ADSource--AD--Load--Success", "ATNativeHelper", "onAdSourceLoadFilled",
                            MsgTools.adSourceParams(mPlacementId, adInfo));
                    emit(Const.NativeCallback.AdSourceLoadFilledCallbackKey, adInfo, null, null);
                }

                @Override
                public void onAdSourceLoadFail(ATAdInfo adInfo, AdError adError) {
                    MsgTools.printAdSource("ADSource--AD--Load--Fail", "ATNativeHelper", "onAdSourceLoadFail",
                            MsgTools.adSourceErrorParams(mPlacementId, adInfo, adError));
                    emit(Const.NativeCallback.AdSourceLoadFailCallbackKey, adInfo,
                            adError != null ? adError.getFullErrorInfo() : null, null);
                }
            });
        }
        return mATNative;
    }

    public void load(Map<String, Object> settings) {
        // settings.atAdRequest 解析（与三类一致）；当前最小化：直接 makeAdRequest
        getOrCreate().makeAdRequest();
    }

    /**
     * 模板(express) 独立路径（attach 时调）：开发者已 load+getNativeAd 且是 express 时，SDK 自动渲染模板进容器。
     * 自渲染（非 express）不走这里——由 JS 的 updateAssetView/renderNativeAd command → renderSelfRender 驱动。
     */
    public void renderIfReady(final String adId) {
        final String id = resolveAdId(adId);
        NativeAd ad = id != null ? mNativeAds.get(id) : null;
        if (ad != null && mContainers.get(id) != null && ad.isNativeExpress()) {
            mMain.post(new Runnable() {
                @Override
                public void run() {
                    renderTemplate(id);
                }
            });
        }
    }

    /** 从 SDK 缓存池取一条 NativeAd，应用 JS setShowConfig 的 scenarioId / showCustomExt。 */
    private NativeAd pollNativeAdFromCache() {
        ATShowConfig.Builder builder = new ATShowConfig.Builder();
        if (mShowScenarioId != null && mShowScenarioId.length() > 0) {
            builder.scenarioId(mShowScenarioId);
        }
        if (mShowCustomExt != null && mShowCustomExt.length() > 0) {
            builder.showCustomExt(mShowCustomExt);
        }
        return mATNative.getNativeAd(builder.build());
    }

    /** getNativeAd：从缓存池弹一条，分配 adId，挂展示期监听。返回 adId（无广告返回 null）。 */
    public String fetchNativeAd() {
        if (mATNative == null) {
            return null;
        }
        NativeAd nativeAd = pollNativeAdFromCache();
        MsgTools.printMsg("fetchNativeAd: " + mPlacementId + ", hasAd=" + (nativeAd != null));
        if (nativeAd == null) {
            return null;
        }
        final String adId = mPlacementId + "#" + mAdSeq.incrementAndGet();
        mNativeAds.put(adId, nativeAd);
        mLastAdId = adId;
        nativeAd.setAdRevenueListener(AdListenerFactory.revenueListener());
        nativeAd.setAdDownloadListener(AdListenerFactory.downloadListener());
        nativeAd.setDislikeCallbackListener(new ATNativeDislikeListener() {
            @Override
            public void onAdCloseButtonClick(ATNativeAdView view, ATAdInfo atAdInfo) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onAdCloseButtonClick",
                        "placementId=" + mPlacementId + "--adId=" + adId + "--adInfo=" + atAdInfo);
                emit(Const.NativeCallback.CloseCallbackKey, adId, atAdInfo, null, null);
            }
        });
        nativeAd.setNativeEventListener(new ATNativeEventExListener() {
            @Override
            public void onAdImpressed(ATNativeAdView view, ATAdInfo atAdInfo) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onAdImpressed",
                        "placementId=" + mPlacementId + "--adId=" + adId + "--adInfo=" + atAdInfo);
                emit(Const.NativeCallback.ShowCallbackKey, adId, atAdInfo, null, null);
            }

            @Override
            public void onAdClicked(ATNativeAdView view, ATAdInfo atAdInfo) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onAdClicked",
                        "placementId=" + mPlacementId + "--adId=" + adId + "--adInfo=" + atAdInfo);
                emit(Const.NativeCallback.ClickCallbackKey, adId, atAdInfo, null, null);
            }

            @Override
            public void onAdVideoStart(ATNativeAdView view) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onAdVideoStart",
                        "placementId=" + mPlacementId + "--adId=" + adId);
                emit(Const.NativeCallback.VideoStartKey, adId, null, null, null);
            }

            @Override
            public void onAdVideoEnd(ATNativeAdView view) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onAdVideoEnd",
                        "placementId=" + mPlacementId + "--adId=" + adId);
                emit(Const.NativeCallback.VideoEndKey, adId, null, null, null);
            }

            @Override
            public void onAdVideoProgress(ATNativeAdView view, int progress) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onAdVideoProgress",
                        "placementId=" + mPlacementId + "--adId=" + adId + "--progress=" + progress);
                Map<String, Object> extra = new HashMap<>();
                extra.put("progress", progress);
                emit(Const.NativeCallback.VideoProgressKey, adId, null, null, extra);
            }

            @Override
            public void onDeeplinkCallback(ATNativeAdView atNativeAdView, ATAdInfo atAdInfo, boolean isSuccess) {
                MsgTools.printCallback("Native", "ATNativeHelper", "onDeeplinkCallback",
                        "placementId=" + mPlacementId + "--adId=" + adId + "--adInfo=" + atAdInfo
                                + "--isSuccess=" + isSuccess);
                Map<String, Object> extra = new HashMap<>();
                extra.put(Const.CallbackKey.isDeeplinkSuccess, isSuccess);
                emit(Const.NativeCallback.DeeplinkCallbackKey, adId, atAdInfo, null, extra);
            }
        });
        return adId;
    }

    public NativeAd getNativeAd(String adId) {
        return adOf(adId);
    }

    /**
     * 自渲染素材回传：从 NativeAd.getAdMaterial() 取素材字段打包给 JS，
     * 由 RN 的 AssetView 组件（&lt;Text&gt;/&lt;Image&gt;）自渲染填值。key 用 Const.Native.*。
     * mediaView 是 SDK 原生 view（不能序列化），仅回传 isMediaViewAvailable 标志，真 view 在 renderNativeAd 时 addView。
     */
    public Map<String, Object> getAdMaterialMap(String adId) {
        NativeAd ad = adOf(adId);
        if (ad == null) {
            return null;
        }
        ATNativeMaterial m = ad.getAdMaterial();
        if (m == null) {
            return null;
        }
        boolean hasMedia = m.getAdMediaView() != null;
        boolean express = ad.isNativeExpress();
        Map<String, Object> map = new HashMap<>();
        map.put(Const.Native.title, m.getTitle());
        map.put(Const.Native.desc, m.getDescriptionText());
        map.put(Const.Native.cta, m.getCallToActionText());
        map.put(Const.Native.icon, m.getIconImageUrl());
        map.put(Const.Native.mainImage, m.getMainImageUrl());
        Double star = m.getStarRating();
        if (star != null) {
            map.put("starRating", star);
        }
        map.put("advertiser", m.getAdvertiserName());
        map.put("isMediaViewAvailable", hasMedia);
        // offer 类型：express(模板) → JS 隐藏 AssetView 让 SDK 画模板
        map.put("isExpress", express);
        MsgTools.printMsg("getAdMaterial: " + mPlacementId + ", express=" + express + ", hasMedia=" + hasMedia);
        return map;
    }

    public boolean isNativeExpress(String adId) {
        NativeAd ad = adOf(adId);
        return ad != null && ad.isNativeExpress();
    }

    public Map<String, Object> checkAdStatus() {
        Map<String, Object> map = new HashMap<>(5);
        if (mATNative != null) {
            com.anythink.core.api.ATAdStatusInfo info = mATNative.checkAdStatus();
            map.put("isLoading", info.isLoading());
            map.put("isReady", info.isReady());
            ATAdInfo top = info.getATTopAdInfo();
            if (top != null) {
                map.put("adInfo", top.toString());
            }
            return map;
        }
        map.put("isLoading", false);
        map.put("isReady", false);
        return map;
    }

    public List<ATAdInfo> validAdCaches() {
        return mATNative != null ? mATNative.checkValidAdCaches() : null;
    }

    /**
     * 模板(express) 独立路径：SDK 用 renderAdContainer 把平台模板渲染进容器，再 prepare。
     * 仅 express 走（renderIfReady 或 renderSelfRender 转发）；自渲染走 doRenderSelfRender。
     * 模板 view 是 SDK 注入，Fabric 容器不会布局它 → 渲染后强制 measure+layout 铺满容器修正（同模板黑块的修法）。
     */
    public void renderTemplate(String adId) {
        String id = resolveAdId(adId);
        if (id == null) {
            return;
        }
        NativeAd ad = mNativeAds.get(id);
        ATNativeContainer container = mContainers.get(id);
        if (ad == null || container == null || !ad.isNativeExpress()) {
            return;
        }
        if (Boolean.TRUE.equals(mTemplateRendered.get(id))) {
            return; // 已渲染过，避免重复 renderAdContainer
        }
        mTemplateRendered.put(id, true);
        MsgTools.printMsg("renderTemplate: " + mPlacementId + ", adId=" + id);
        try {
            ad.renderAdContainer(container, null);
        } catch (Throwable e) {
            MsgTools.printMsg("renderAdContainer fail: " + e.getMessage());
        }
        ad.prepare(container, new ATNativePrepareExInfo());
        forceLayoutNativeChild(container, /*wrapHeight*/ true);
    }

    /**
     * SDK 注入的 view（模板 view / media surface）在 Fabric 容器内不被 Yoga 布局 → 0 尺寸。
     * 在 host 的 OnPreDraw 里持续 ~30 帧用 host 尺寸 measure+layout 修正（让异步 surface 到位后也被布局）。
     *
     * @param host        监听 + 提供尺寸的容器
     * @param wrapHeight  true（模板）：用 host 宽 EXACTLY + 高 UNSPECIFIED 量 SDK view 自然高，并把 host 撑到该高度；
     *                    false（media）：直接用 host 宽高 EXACTLY 铺满。
     */
    private void forceLayoutNativeChild(final android.view.ViewGroup host, final boolean wrapHeight) {
        final int[] frames = {0};
        host.getViewTreeObserver().addOnPreDrawListener(
                new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        int w = host.getWidth();
                        int h = host.getHeight();
                        if (frames[0]++ > 30 || w <= 0 || (!wrapHeight && h <= 0)) {
                            android.view.ViewTreeObserver o = host.getViewTreeObserver();
                            if (o.isAlive()) {
                                o.removeOnPreDrawListener(this);
                            }
                            return true;
                        }
                        int wSpec = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                        int hSpec = wrapHeight
                                ? View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                                : View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
                        int maxH = 0;
                        for (int i = 0; i < host.getChildCount(); i++) {
                            View child = host.getChildAt(i);
                            // 只布局 SDK 注入的 view，跳过 RN 的 ReactXxx（它们由 Yoga 自己布局）
                            if (child.getClass().getName().startsWith("com.facebook.react")) {
                                continue;
                            }
                            child.measure(wSpec, hSpec);
                            int mh = wrapHeight ? child.getMeasuredHeight() : h;
                            child.layout(0, 0, w, mh > 0 ? mh : h);
                            maxH = Math.max(maxH, mh);
                        }
                        // 模板：把 host 撑到 SDK view 高度（host=ReactViewGroup，否则塌掉看不全）
                        if (wrapHeight && maxH > 0 && host.getHeight() < maxH) {
                            host.setMinimumHeight(maxH);
                            android.view.ViewGroup.LayoutParams lp = host.getLayoutParams();
                            if (lp != null && lp.height != maxH) {
                                lp.height = maxH;
                                host.setLayoutParams(lp);
                            }
                        }
                        return true;
                    }
                });
    }

    /**
     * 自渲染绑定（JS 的 renderNativeAd command 触发，debounce 50ms 防多次抖动）。
     * 流程：findViewById(各 asset tag) → setXxxView + 收 clickViewList；getAdMediaView addView 进 mediaView 占位；
     * mNativeAd.prepare 注册点击/曝光。RN view 由 RN 自己布局，不 re-parent → 无 relayout 需求。
     *
     * @param container ATNativeContainer（=ATNativeAdView 子类）
     * @param assetTags JS 注册的 {react tag → 类型}
     */
    public void renderSelfRender(final String adId,
                                 final ATNativeContainer container,
                                 final Map<Integer, String> assetTags,
                                 final Map<Integer, Integer> assetColors) {
        final String id = resolveAdId(adId);
        if (id == null) {
            return;
        }
        // 按 adId debounce（多 cell 并发渲染各自独立，不互相取消）
        Runnable prev = mRenderRunnables.remove(id);
        if (prev != null) {
            mMain.removeCallbacks(prev);
        }
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mRenderRunnables.remove(id);
                doRenderSelfRender(id, container, assetTags, assetColors);
            }
        };
        mRenderRunnables.put(id, r);
        mMain.postDelayed(r, 50);
    }

    private final Map<String, Runnable> mRenderRunnables = new HashMap<>();

    private void doRenderSelfRender(String adId,
                                    ATNativeContainer container,
                                    Map<Integer, String> assetTags,
                                    Map<Integer, Integer> assetColors) {
        NativeAd ad = adOf(adId);
        if (ad == null || container == null || assetTags == null) {
            return;
        }
        if (ad.isNativeExpress()) {
            // 这个位 auction 返回的是模板 offer → 转模板路径（SDK 画模板），忽略 AssetView。
            // JS 侧也会据 getNativeAd 的 isExpress 隐藏 AssetView。
            MsgTools.printMsg("renderSelfRender: express offer, route to renderTemplate: " + mPlacementId);
            renderTemplate(adId);
            return;
        }
        final NativeAd mNativeAd = ad; // 下方沿用原变量名
        MsgTools.printMsg("renderSelfRender: " + mPlacementId + ", adId=" + adId + ", assets=" + assetTags.size());

        // 模型 A：等 RN AssetView 都布局完（有尺寸）再 native 重建，避免 measure 量到 0 漏素材。
        // OnPreDraw 等到关键素材有尺寸（或超时 ~30 帧）再执行 doRebuildAndRegister。
        scheduleRebuildWhenLaidOut(adId, mNativeAd, container, assetTags, assetColors);
    }

    /** 等 RN AssetView 布局完（关键素材有尺寸）后执行重建+注册；超时也执行（按已有尺寸尽力）。 */
    private void scheduleRebuildWhenLaidOut(final String adId, final NativeAd nativeAd,
                                            final ATNativeContainer container,
                                            final Map<Integer, String> assetTags,
                                            final Map<Integer, Integer> assetColors) {
        final int[] frames = {0};
        container.getViewTreeObserver().addOnPreDrawListener(
                new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // 等「关键素材」有尺寸再重建：标题/icon 必有内容，它们有尺寸即说明 RN 布局完成。
                        // 不检查全部 asset——media/mainImage 据 offer 二选一，空的会塌成 0 高（collapsed），
                        // 若要求它们也>0 则 allSized 永远 false → 必超时强搬 → 那刻别的 asset 没好 → 空白。
                        boolean keySized = false;
                        for (Map.Entry<Integer, String> e : assetTags.entrySet()) {
                            if (e.getKey() == null) {
                                continue;
                            }
                            String type = e.getValue();
                            if (Const.Native.title.equals(type) || Const.Native.icon.equals(type)) {
                                View v = container.findViewById(e.getKey());
                                if (v != null && v.getWidth() > 0 && v.getHeight() > 0) {
                                    keySized = true;
                                    break;
                                }
                            }
                        }
                        if (keySized || frames[0]++ > 60) {
                            android.view.ViewTreeObserver o = container.getViewTreeObserver();
                            if (o.isAlive()) {
                                o.removeOnPreDrawListener(this);
                            }
                            doRebuildAndRegister(adId, nativeAd, container, assetTags, assetColors);
                        }
                        return true;
                    }
                });
    }

    /** native 重建素材（含 media）+ renderAdContainer + prepare + 强制布局。 */
    private void doRebuildAndRegister(String adId, NativeAd nativeAd,
                                      ATNativeContainer container,
                                      Map<Integer, String> assetTags,
                                      Map<Integer, Integer> assetColors) {
        // 守卫：OnPreDraw 异步触发，cell 可能已滚出/onDrop（destroy 清了 mNativeAds、container 解绑）。
        // 此时 adId 不在缓存 或 container 已换绑别的 adId，则放弃重建，避免用废弃对象 renderAdContainer/prepare。
        String id = resolveAdId(adId);
        if (id == null || mNativeAds.get(id) != nativeAd || mContainers.get(id) != container) {
            MsgTools.printMsg("rebuild abort (cell dropped/rebound): " + adId);
            return;
        }
        // media：SDK 原生 view，交给 rebuilder 放进 nativeRoot（不再塞 RN holder——模型 A 全用重建层）。
        View mediaView = null;
        try {
            ATNativeMaterial m = nativeAd.getAdMaterial();
            mediaView = (m != null && m.getAdMediaView() != null) ? m.getAdMediaView(container) : null;
        } catch (Throwable e) {
            MsgTools.printMsg("getAdMediaView fail: " + e.getMessage());
        }

        // 移除上一次的重建层（重渲染/换 offer 时，否则多套素材叠加重影）。
        View old = container.findViewWithTag(NativeSelfRenderRebuilder.REBUILD_ROOT_TAG);
        if (old != null) {
            container.removeView(old);
        }

        ATNativePrepareInfo info = new ATNativePrepareExInfo();
        java.util.List<View> clickViews = new java.util.ArrayList<>();
        ATNativeMaterial material = nativeAd.getAdMaterial();
        android.widget.FrameLayout nativeRoot = NativeSelfRenderRebuilder.build(
                container, material, assetTags, assetColors, mediaView, info, clickViews);

        // renderAdContainer（render→prepare 顺序，每 offer 一次）让 GDT 等独占容器 ADN 注册成功。
        if (!Boolean.TRUE.equals(mSelfRendered.get(adId))) {
            mSelfRendered.put(adId, true);
            try {
                nativeAd.renderAdContainer(container, nativeRoot);
            } catch (Throwable e) {
                MsgTools.printMsg("renderAdContainer(self) fail: " + e.getMessage());
            }
        }
        nativeAd.prepare(container, info);

        // 隐藏 RN 渲染的 AssetView（已被 native 重建层顶替），否则两套并存、布局变动时错位 → 重影。
        // AssetView 嵌套层级不定（孙/曾孙），无法按 tag 精确跳过；改为 selfRenderActive：
        // container.drawChild 只画 native 重建层、跳过所有 RN 子树（setVisibility 会被 Fabric 覆写，不可用）。
        container.selfRenderActive = true;
        container.invalidate();

        // 重建层在 RN 容器内需强制 measure/layout 才可见（跳过 RN 子节点的工具）。
        forceLayoutNativeChild(container, /*wrapHeight*/ false);
    }

    /** 销毁一条（adId 空→最近一条；指定 id 已销毁则 no-op，禁止回退 mLastAdId 误杀其他 cell）。 */
    public void destroy(String adId) {
        final String id;
        if (adId == null || adId.isEmpty()) {
            id = mLastAdId;
        } else if (mNativeAds.containsKey(adId)) {
            id = adId;
        } else {
            MsgTools.printMsg("native destroy skip (already gone): " + mPlacementId + ", adId=" + adId);
            return;
        }
        if (id == null) {
            return;
        }
        MsgTools.printMsg("native destroy: " + mPlacementId + ", adId=" + id);
        NativeAd ad = mNativeAds.remove(id);
        ATNativeContainer container = mContainers.remove(id);
        mTemplateRendered.remove(id);
        mSelfRendered.remove(id);
        Runnable r = mRenderRunnables.remove(id);
        if (r != null) {
            mMain.removeCallbacks(r);
        }
        if (id.equals(mLastAdId)) {
            mLastAdId = null;
        }
        if (container != null) {
            container.selfRenderActive = false;
            container.invalidate();
        }
        if (ad != null) {
            // ad.destory() 会触发 GDT 内部 ExoPlayer.stop —— 必须主线程（media3 线程检查）。
            // JS 点移除经桥在 RN bridge 线程调到这里，故强制 post 主线程，避免 "wrong thread" 崩。
            final NativeAd toDestroy = ad;
            runOnMain(new Runnable() {
                @Override
                public void run() {
                    toDestroy.destory();
                }
            });
        }
    }

    /** 在主线程执行（已在主线程则直接跑）。 */
    private void runOnMain(Runnable r) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            r.run();
        } else {
            mMain.post(r);
        }
    }

    /** 整位销毁（清所有条 + 排空 SDK 缓存池）。 */
    public void destroyAll() {
        MsgTools.printMsg("native destroyAll: " + mPlacementId);
        final java.util.List<NativeAd> ads = new java.util.ArrayList<>(mNativeAds.values());
        runOnMain(new Runnable() {
            @Override
            public void run() {
                for (NativeAd ad : ads) {
                    if (ad != null) {
                        ad.destory();
                    }
                }
            }
        });
        mNativeAds.clear();
        mContainers.clear();
        mTemplateRendered.clear();
        mSelfRendered.clear();
        for (Runnable r : mRenderRunnables.values()) {
            mMain.removeCallbacks(r);
        }
        mRenderRunnables.clear();
        mLastAdId = null;
        if (mATNative != null) {
            NativeAd cached;
            while ((cached = mATNative.getNativeAd()) != null) {
                final NativeAd toDestroy = cached;
                runOnMain(new Runnable() {
                    @Override
                    public void run() {
                        toDestroy.destory();
                    }
                });
            }
            mATNative = null;
        }
    }

    /** 生命周期：恢复一条（adId 空→最近一条）。暂停/恢复视频、曝光计时等由广告源决定。 */
    public void onResume(String adId) {
        NativeAd ad = adOf(adId);
        if (ad != null) {
            ad.onResume();
        }
    }

    /** 生命周期：暂停一条。 */
    public void onPause(String adId) {
        NativeAd ad = adOf(adId);
        if (ad != null) {
            ad.onPause();
        }
    }
}
