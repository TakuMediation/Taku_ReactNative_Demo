/**
 * 自渲染 prepareInfo。各标准元素的值 = `findNodeHandle(ref)` 得到的 **viewTag**（number）。
 * 桥内 `resolveView(viewTag)` 取 RN 渲染好的原生 view，绑定给 SDK 做点击/曝光。
 */
export interface ATNativePrepareInfo {
  /** 应用图标 view 的 viewTag */
  appIcon?: number;
  /** 主图/媒体 view 的 viewTag */
  mainImage?: number;
  /** 标题 view 的 viewTag */
  title?: number;
  /** 描述 view 的 viewTag */
  desc?: number;
  /** 广告标识 view 的 viewTag */
  adLogo?: number;
  /** CTA 按钮 view 的 viewTag */
  cta?: number;
  /** 关闭/dislike view 的 viewTag */
  dislike?: number;
  /** 容器 view 的 viewTag（renderAdContainer 自动填入 containerRef） */
  parent?: number;
  /** 其他可点击元素 view 的 viewTag 数组 */
  elementsView?: number[];
  /** 自定义 view 的 viewTag 数组 */
  customView?: number[];
  /** 自适应高度 */
  isAdaptiveHeight?: boolean;
}

/**
 * 广告素材（getAdMaterial 返回）。自渲染时，AssetView 组件从这里取值在 JS 侧渲染。
 * 字段随广告源可能为空，保持开放可扩。
 */
export interface ATAdMaterial {
  /** 标题 */
  title?: string;
  /** 描述 */
  desc?: string;
  /** CTA 文案（如"查看详情"） */
  cta?: string;
  /** 图标 URL */
  appIcon?: string;
  /** 主图 URL */
  mainImage?: string;
  /** 星级评分 */
  starRating?: number;
  /** 广告主名 */
  advertiser?: string;
  /** 是否有 mediaView（视频/大图 surface 由 SDK 提供，addView 进 ATNativeMediaView 占位） */
  isMediaViewAvailable?: boolean;
  /** offer 是否为模板(express)。true → SDK 自渲染模板，隐藏开发者的 AssetView 布局 */
  isExpress?: boolean;
  [key: string]: unknown;
}

/**
 * 自渲染 asset 类型名（与原生 Const.Native.* 的值一一对应，JS↔native command 协议契约）。
 * ATNativeAdView 经 updateAssetView(tag, type) 把各 AssetView 的 react tag 注册给 native。
 */
export const NativeAssetType = {
  title: 'title',
  desc: 'desc',
  cta: 'cta',
  appIcon: 'appIcon',
  mainImage: 'mainImage',
  mediaView: 'mediaView',
  /** 关闭/dislike 按钮（native setCloseView） */
  dislike: 'dislike',
} as const;
