package com.anythink.reactnative.nativead;

import android.content.Context;

import com.anythink.core.api.ATAdConst;
import com.anythink.reactnative.utils.MsgTools;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Native ViewManager 的架构无关业务（共享 Impl）+ 2 个 command（updateAssetView / renderNativeAd）。
 * newarch（codegen Interface）与 oldarch（@ReactProp + 手写 receiveCommand）两个壳都转发到此。
 */
public final class ATNativeAdViewManagerImpl {
    private ATNativeAdViewManagerImpl() {}

    public static final class State {
        public String placementId;
        public String adId;
        public int adWidth;
        public int adHeight;
        public boolean adaptive;
        public boolean attached;
    }

    private static final WeakHashMap<ATNativeContainer, State> STATES = new WeakHashMap<>();

    public static State stateOf(ATNativeContainer v) {
        State s = STATES.get(v);
        if (s == null) {
            s = new State();
            STATES.put(v, s);
        }
        return s;
    }

    public static ATNativeContainer createView(Context ctx) {
        return new ATNativeContainer(ctx);
    }

    public static void onAfterUpdateTransaction(ATNativeContainer view) {
        maybeAttach(view);
    }

    /** 关键 props（placementId）齐才 attach；adId 可后到（setAdId 后再触发 setContainer/renderIfReady）。 */
    public static void maybeAttach(ATNativeContainer container) {
        State st = stateOf(container);
        if (st.placementId == null || st.placementId.isEmpty()) {
            return;
        }
        ATNativeManager mgr = ATNativeManager.getInstance();
        ATNativeHelper helper = mgr.getOrCreateHelperForView(st.placementId);
        container.helper = helper;

        if (!st.attached) {
            st.attached = true;
            MsgTools.printMsg("ATNativeAdViewManager attach: " + st.placementId);
            // 可选尺寸 dp→px
            if (st.adWidth > 0 && st.adHeight > 0) {
                float density = container.getResources().getDisplayMetrics().density;
                Map<String, Object> extra = new HashMap<>();
                extra.put(ATAdConst.KEY.AD_WIDTH, (int) (st.adWidth * density + 0.5f));
                extra.put(ATAdConst.KEY.AD_HEIGHT, (int) (st.adHeight * density + 0.5f));
                helper.setLocalExtra(extra);
            }
        }

        // adId 就绪后绑定容器到对应那条 + 模板(express) 自动渲染
        if (st.adId != null && !st.adId.isEmpty()) {
            helper.setContainer(st.adId, container);
            helper.renderIfReady(st.adId);
        }
    }

    /** command: updateAssetView(int tag, String name)。
     * name 可能带 JS 侧解析好的文字色后缀 "type|c=<int>"：拆出干净类型存 assetTags（两种绑定路径都按它比对），
     * 颜色另存 assetColors 供自渲染重建用 */
    public static void updateAssetView(ATNativeContainer view, int assetViewTag, String assetViewName) {
        String type = assetViewName;
        if (assetViewName != null) {
            int sep = assetViewName.indexOf("|c=");
            if (sep >= 0) {
                type = assetViewName.substring(0, sep);
                try {
                    view.assetColors.put(assetViewTag, Integer.parseInt(assetViewName.substring(sep + 3)));
                } catch (NumberFormatException ignore) {
                }
            }
        }
        view.assetTags.put(assetViewTag, type);
        MsgTools.printMsg("updateAssetView: tag=" + assetViewTag + ", type=" + type);
    }

    /** command: renderNativeAd()。 */
    public static void renderNativeAd(ATNativeContainer view) {
        if (view.helper != null) {
            view.helper.renderSelfRender(view.adId, view,
                    new LinkedHashMap<>(view.assetTags), new LinkedHashMap<>(view.assetColors));
        }
    }

    public static void onDrop(final ATNativeContainer view) {
        State st = STATES.remove(view);
        // 仅在 adId 已绑定时销毁本视图那一条。空 adId（cell 在 adId prop 到达前就被 drop）不销毁——
        // 否则 helper.destroy("") 会回退 mLastAdId，误杀最近一条正在显示的广告。
        if (st != null && st.placementId != null && st.adId != null && !st.adId.isEmpty()) {
            MsgTools.printMsg("ATNativeAdViewManager onDropViewInstance -> destroy adId=" + st.adId);
            ATNativeManager.getInstance().destroy(st.placementId, st.adId);
        }
        view.assetTags.clear();
        view.assetColors.clear();
        view.selfRenderActive = false;
        view.helper = null;
        view.adId = null;
        // removeAllViews 触发 GDT media 的 TextureView.onSurfaceTextureDestroyed → ExoPlayer 要主线程访问；
        // onDrop 可能在 Fabric mounting 线程，故强制主线程拆子 view，避免 "Player accessed on wrong thread" 崩。
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            view.removeAllViews();
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    view.removeAllViews();
                }
            });
        }
    }
}
