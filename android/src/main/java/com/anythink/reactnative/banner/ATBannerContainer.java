package com.anythink.reactnative.banner;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Banner 容器（共享，新旧架构通用）：RN 不会给"原生侧 addView 的子 view"做 measure/layout，
 * 故 requestLayout 时强制 measure + layout 自己与子 view（banner 加载内容会触发 requestLayout）。
 */
public class ATBannerContainer extends FrameLayout {
    public ATBannerContainer(Context context) {
        super(context);
    }

    private final Runnable mMeasureAndLayout = new Runnable() {
        @Override
        public void run() {
            measure(
                    View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.EXACTLY));
            layout(getLeft(), getTop(), getRight(), getBottom());
        }
    };

    @Override
    public void requestLayout() {
        super.requestLayout();
        post(mMeasureAndLayout);
    }
}
