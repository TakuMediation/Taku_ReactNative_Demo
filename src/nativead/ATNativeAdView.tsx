import * as React from 'react';
import { useContext, useEffect, useRef, useState } from 'react';
import { findNodeHandle, processColor, StyleSheet, View } from 'react-native';
import type { ViewProps } from 'react-native';
import NativeAdViewComponent, {
  Commands,
} from '../specs/ATNativeAdViewNativeComponent';
import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { NativeAssetType } from './types';
import { NativeAdContext, NativeAdProvider } from './NativeAdProvider';
import type { ATAdEventPayload } from '../types';
import type { ATAdMaterial } from './types';
import type { NativeAd } from './NativeAd';

/**
 * Native Fabric 组件封装。
 *
 * 自渲染：内部放 AssetView 组件（ATNativeTitleView/IconView/MediaView/CloseView...），它们从 Context 取素材自渲染。
 * 开发者用 `ATNative` 加载，`onNativeAdLoaded` 后再渲染本组件 → 本组件 mount 后取素材 setNativeAd
 * → 各 asset `updateAssetView(tag,type)` 注册 → `renderNativeAd()` 一次绑定（native findViewById + prepare 注册点击/曝光）。
 * 模板模式：不放 AssetView 子节点（native attach 时走 renderAdContainer 独立路径）。
 *
 * adWidth/adHeight 可选（模板尺寸）；事件 V1 走 NativeCall 通道按 placementID 过滤。
 */
export interface ATNativeAdViewProps extends ViewProps {
  /** 要展示的广告对象（来自 `await native.getNativeAd()`）。adId 内部用，用户无感。 */
  ad: NativeAd;
  isAdaptiveHeight?: boolean;
  /** 可选：模板模式请求宽(dp)。 */
  adWidth?: number;
  /** 可选：模板模式请求高(dp)。 */
  adHeight?: number;
  onAdEvent?: (payload: ATAdEventPayload) => void;
}

export function ATNativeAdView(props: ATNativeAdViewProps) {
  return (
    <NativeAdProvider>
      <ATNativeAdViewImpl {...props} />
    </NativeAdProvider>
  );
}

function ATNativeAdViewImpl(props: ATNativeAdViewProps) {
  const {
    ad,
    isAdaptiveHeight,
    adWidth,
    adHeight,
    onAdEvent,
    children,
    ...rest
  } = props;
  const placementID = ad.placementId;
  const adId = ad.adId;

  const {
    setNativeAd,
    titleRef,
    descRef,
    ctaRef,
    iconRef,
    mainImageRef,
    mediaRef,
    closeRef,
    textColors,
  } = useContext(NativeAdContext);

  const viewRef = useRef<React.ElementRef<typeof NativeAdViewComponent> | null>(
    null
  );
  const cb = useRef(onAdEvent);
  cb.current = onAdEvent;
  const hasAdEvent = onAdEvent != null;
  // 模板(express) offer：SDK 自渲染模板进容器，隐藏开发者的 AssetView 布局（否则空 AssetView 占位/盖住模板）
  const [isExpress, setIsExpress] = useState(false);

  // 视图级事件按 placementID#adId 分流到本 cell（同位多条不串位）。
  // 依赖只用 placementID/adId/hasAdEvent——cb.current 已动态捕获最新回调，
  // 不把 onAdEvent 函数本身放进依赖（内联箭头每次 render 新引用会反复 remove/add 同一 adId 监听）。
  useEffect(() => {
    if (hasAdEvent) {
      ATAdEvents.setAdInstanceListener(
        placementID,
        adId,
        (p: ATAdEventPayload) => cb.current?.(p)
      );
      return () => ATAdEvents.removeAdInstanceListener(placementID, adId);
    }
    return undefined;
  }, [placementID, adId, hasAdEvent]);

  // 自渲染绑定：本组件在「广告已 loaded」后才被开发者渲染，故 mount 后即可取素材 + 注册 asset。
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const material = (await ATReactNativeBridge.getNativeAdMaterial(
        placementID,
        adId
      )) as ATAdMaterial | null;
      if (cancelled) {
        return;
      }
      if (material) {
        setNativeAd(material);
      }
      const express = material?.isExpress === true;
      setIsExpress(express);
      // setNativeAd 触发 AssetView 重渲染（Image source 填 uri）。注册需等这次 commit 后再做，
      // 否则 ref 指向旧 view、findViewById 取不到（icon/mainImage bind null 的根因）。
      // 用 requestAnimationFrame 等到下一帧（重渲染已 commit）再注册。
      requestAnimationFrame(() => {
        if (cancelled) {
          return;
        }
        const view = viewRef.current;
        if (!view) {
          return;
        }
        // codegen Commands 首参类型在 strict-api 下解析为 never（类型系统怪异），用 cmdView 收口一次性 cast
        const cmdView = view as Parameters<typeof Commands.renderNativeAd>[0];
        // 模板 offer：不注册 AssetView（隐藏了），直接 renderNativeAd（native 转 renderTemplate 画模板）
        if (!express) {
          // 文字类把 JS 侧 style.color 解析成原生 int 附在类型名上（type|c=<int>），
          // native 重建文本用该色。
          const colorTag = (type: string): string => {
            const css = textColors.current?.[type];
            if (!css) {
              return type;
            }
            const int = processColor(css);
            return typeof int === 'number' ? `${type}|c=${int}` : type;
          };
          const register = <T extends React.ElementType>(
            ref: React.RefObject<React.ComponentRef<T> | null>,
            type: string
          ) => {
            if (!ref.current) {
              return;
            }
            const node = findNodeHandle(ref.current);
            if (node != null) {
              Commands.updateAssetView(cmdView, node, type);
            }
          };
          register(titleRef, colorTag(NativeAssetType.title));
          register(descRef, colorTag(NativeAssetType.desc));
          register(ctaRef, colorTag(NativeAssetType.cta));
          register(iconRef, NativeAssetType.appIcon);
          register(mainImageRef, NativeAssetType.mainImage);
          register(mediaRef, NativeAssetType.mediaView);
          register(closeRef, NativeAssetType.dislike);
        }
        Commands.renderNativeAd(cmdView);
      });
    })();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [placementID, adId]);

  // 模板 offer 隐藏了 children，容器会塌（无 RN 内容撑高）。给个 minHeight 让 SDK 模板有空间。
  const { style, ...restProps } = rest as { style?: ViewProps['style'] };
  const mergedStyle = isExpress
    ? [style, { minHeight: adHeight ?? 250 }]
    : style;

  return (
    <NativeAdViewComponent
      ref={viewRef}
      placementID={placementID}
      adId={adId}
      isAdaptiveHeight={isAdaptiveHeight}
      adWidth={adWidth}
      adHeight={adHeight}
      style={mergedStyle}
      {...restProps}
    >
      {/* 模板 offer：隐藏开发者的 AssetView（SDK 把模板画进容器），自渲染 offer 才显示 AssetView。
          自渲染：children 包一层 collapsable=false 根，阻止 Fabric 扁平化纯布局 View
          （否则 asset 平铺成 SDK 容器直接子节点、被 FrameLayout 撑全屏）——开发者无需自己加。 */}
      {isExpress ? null : (
        <View collapsable={false} style={styles.selfRoot}>
          {children}
        </View>
      )}
    </NativeAdViewComponent>
  );
}

const styles = StyleSheet.create({
  // collapsable=false 根：宽度撑满容器，高度由 children 撑（asset 按自身布局排，不被扁平化/撑全屏）
  selfRoot: { width: '100%' },
});
