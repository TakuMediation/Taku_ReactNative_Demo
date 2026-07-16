import { useContext } from 'react';
import { Text, Image, View, StyleSheet, Platform } from 'react-native';
import type { TextProps, ImageProps, ViewProps, TextStyle } from 'react-native';
import { NativeAdContext } from './NativeAdProvider';

/**
 * 自渲染 AssetView 组件。
 * 每个组件从 NativeAdContext 取素材自渲染 + 注册自己的 ref（供 ATNativeAdView 经 updateAssetView 绑定 viewTag）。
 * 必须放在 <ATNativeAdView> 内（它提供 Provider）。文字/图由 RN 填，mediaView 是 SDK view 的占位。
 */

/** 从 style 解析 color → '#rrggbb'/'#aarrggbb' 字符串（flatten 数组样式取最终 color）。无则 undefined。 */
function colorFromStyle(style: TextProps['style']): string | undefined {
  const flat = StyleSheet.flatten(style) as TextStyle | undefined;
  const c = flat?.color;
  return typeof c === 'string' ? c : undefined;
}

/** 标题。 */
export function ATNativeTitleView(props: TextProps) {
  const { titleRef, nativeAd, textColors } = useContext(NativeAdContext);
  const color = colorFromStyle(props.style);
  if (color && textColors.current) {
    textColors.current.title = color;
  }
  return (
    <Text {...props} ref={titleRef}>
      {(nativeAd.title as string) ?? ''}
    </Text>
  );
}

/** 描述。 */
export function ATNativeDescView(props: TextProps) {
  const { descRef, nativeAd, textColors } = useContext(NativeAdContext);
  const color = colorFromStyle(props.style);
  if (color && textColors.current) {
    textColors.current.desc = color;
  }
  return (
    <Text {...props} ref={descRef}>
      {(nativeAd.desc as string) ?? ''}
    </Text>
  );
}

/** CTA 文案（如“查看详情”）。 */
export function ATNativeCtaView(props: TextProps) {
  const { ctaRef, nativeAd, textColors } = useContext(NativeAdContext);
  const color = colorFromStyle(props.style);
  if (color && textColors.current) {
    textColors.current.cta = color;
  }
  return (
    <Text {...props} ref={ctaRef}>
      {(nativeAd.cta as string) ?? ''}
    </Text>
  );
}

/**
 * 图标（appIcon URL）。始终渲染 &lt;Image&gt;（不按 uri 有无切换元素类型，否则 ref 重挂 → findViewById 取不到）。
 * 无 uri 时 source 给 undefined，Image view 仍稳定存在。
 */
export function ATNativeIconView(props: Omit<ImageProps, 'source'>) {
  const { iconRef, nativeAd } = useContext(NativeAdContext);
  const uri = nativeAd.appIcon as string | undefined;
  return <Image {...props} ref={iconRef} source={uri ? { uri } : undefined} />;
}

/**
 * 主图（mainImage URL）。始终渲染 &lt;Image&gt;（ref 稳定），无 uri 时塌成 0 高度——
 * 视频类 offer（mainImageUrl=null + hasMediaView）不占空灰块，主创意走 ATNativeMediaView。
 */
export function ATNativeMainImageView(props: Omit<ImageProps, 'source'>) {
  const { mainImageRef, nativeAd } = useContext(NativeAdContext);
  const uri = nativeAd.mainImage as string | undefined;
  return (
    <Image
      {...props}
      ref={mainImageRef}
      source={uri ? { uri } : undefined}
      style={[props.style, uri ? null : styles.collapsed]}
    />
  );
}

/**
 * 媒体视图占位：SDK 的 mediaView（视频/大图 surface）会 addView 进它。
 * 仅当 isMediaViewAvailable 时撑开尺寸；否则塌成 0 高度（图文 offer 无 mediaView 时不占位）。
 * ref 始终挂着（保持稳定），靠 style 控制是否占位。
 */
export function ATNativeMediaView(props: ViewProps) {
  const { mediaRef, nativeAd } = useContext(NativeAdContext);
  const available = nativeAd.isMediaViewAvailable === true;
  return (
    <View
      {...props}
      // collapsable=false：占位 View 无 children，Fabric 默认会扁平化它（不建真实 native view），
      // 致 native findViewById(mediaTag) 取不到 → SDK media surface 无处 addView（视频/大图 offer 显示不出）。
      collapsable={false}
      ref={mediaRef}
      style={[props.style, available ? null : styles.collapsed]}
    />
  );
}

/**
 * 关闭/dislike 按钮占位。SDK 经 setCloseView 绑定，用户点击触发 nativeAdDidTapCloseButton。
 * Android/iOS 自渲染由 native 画可见 close；RN 层仅保留布局占位（opacity:0 防闪/重复）。
 */
export function ATNativeCloseView(props: ViewProps) {
  const { closeRef } = useContext(NativeAdContext);
  const { children, style, ...rest } = props;
  const isNativePlaceholder =
    (Platform.OS === 'android' || Platform.OS === 'ios') && children == null;
  return (
    <View
      {...rest}
      collapsable={false}
      ref={closeRef}
      style={[
        styles.closeView,
        isNativePlaceholder ? styles.closeViewNativePlaceholder : null,
        style,
      ]}
      pointerEvents={isNativePlaceholder ? 'none' : undefined}
    >
      {children ?? (isNativePlaceholder ? null : <Text style={styles.closeGlyph}>✕</Text>)}
    </View>
  );
}

const styles = StyleSheet.create({
  collapsed: { height: 0, marginTop: 0 },
  closeView: {
    width: 24,
    height: 24,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(0,0,0,0.4)',
    borderRadius: 12,
  },
  /** Android/iOS：占位不参与绘制/点击，可见 close 由 native 层渲染 */
  closeViewNativePlaceholder: {
    opacity: 0,
    backgroundColor: 'transparent',
  },
  closeGlyph: {
    fontSize: 14,
    lineHeight: 16,
    color: '#fff',
    fontWeight: '600',
    textAlign: 'center',
  },
});
