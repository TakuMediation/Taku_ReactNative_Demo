package com.anythink.reactnative.banner;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.anythink.banner.api.ATBannerView;
import com.anythink.core.api.ATAdConst;
import com.anythink.reactnative.utils.MsgTools;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Banner ViewManager 的架构无关业务（共享 Impl）：创建容器 / props 暂存 / 延迟 attach / 销毁。
 * newarch（codegen Interface）与 oldarch（@ReactProp）两个壳都转发到此，业务零分叉。
 * 泛型用 FrameLayout（与两壳的 ViewManager 泛型一致；ATBannerContainer 是其子类）。
 */
public final class ATBannerViewManagerImpl {
    private ATBannerViewManagerImpl() {}

    /** props 暂存（沿用原 ViewManager 的 State + WeakHashMap）。 */
    public static final class State {
        public String placementId;
        public int adWidth;
        public int adHeight;
        public boolean adaptive;
        public boolean attached;
    }

    private static final WeakHashMap<FrameLayout, State> STATES = new WeakHashMap<>();

    public static State stateOf(FrameLayout v) {
        State s = STATES.get(v);
        if (s == null) {
            s = new State();
            STATES.put(v, s);
        }
        return s;
    }

    public static FrameLayout createView(Context ctx) {
        return new ATBannerContainer(ctx);
    }

    public static void onAfterUpdateTransaction(FrameLayout view) {
        maybeAttach(view);
    }

    /** 关键 props（placementId + 尺寸）齐 → attach banner view + load。 */
    public static void maybeAttach(final FrameLayout container) {
        State st = stateOf(container);
        if (st.attached || st.placementId == null || st.placementId.isEmpty()
                || st.adWidth <= 0 || st.adHeight <= 0) {
            return;
        }
        st.attached = true;
        MsgTools.printMsg("ATBannerViewManager attach: " + st.placementId);

        ATBannerManager mgr = ATBannerManager.getInstance();
        ATBannerHelper helper = mgr.getOrCreateHelperForView(st.placementId);

        // AnyThink AD_WIDTH/AD_HEIGHT 要 px；JS props 是 dp → 乘 density 转 px（否则创意偏小）
        float density = container.getResources().getDisplayMetrics().density;
        int adWidthPx = (int) (st.adWidth * density + 0.5f);
        int adHeightPx = (int) (st.adHeight * density + 0.5f);
        Map<String, Object> extra = new HashMap<>();
        extra.put(ATAdConst.KEY.AD_WIDTH, adWidthPx);
        extra.put(ATAdConst.KEY.AD_HEIGHT, adHeightPx);
        helper.setLocalExtra(extra);

        ATBannerView bannerView = helper.getOrCreateBannerView(mgr.currentActivity());
        if (bannerView == null) {
            st.attached = false;
            return;
        }
        if (bannerView.getParent() instanceof ViewGroup) {
            ((ViewGroup) bannerView.getParent()).removeView(bannerView);
        }
        container.addView(bannerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        Map<String, Object> status = helper.checkAdStatus();
        boolean isReady = Boolean.TRUE.equals(status.get("isReady"));
        if (isReady) {
            MsgTools.printMsg("ATBannerViewManager already ready, skip reload: " + st.placementId);
        } else {
            helper.load(new HashMap<String, Object>(), mgr.currentActivity());
        }
    }

    public static void onDrop(FrameLayout view) {
        State st = STATES.remove(view);
        view.removeAllViews();
        if (st != null && st.placementId != null) {
            MsgTools.printMsg("ATBannerViewManager onDropViewInstance detach: " + st.placementId);
            ATBannerHelper helper = ATBannerManager.getInstance().getOrCreateHelperForView(st.placementId);
            helper.detachFromContainer();
        } else {
            MsgTools.printMsg("ATBannerViewManager onDropViewInstance (no state/pid)");
        }
    }
}
