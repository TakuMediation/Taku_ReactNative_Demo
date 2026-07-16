import { useCallback, useEffect, useRef, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import {
  ATNativeAdView,
  ATNativeIconView,
  ATNativeTitleView,
  ATNativeDescView,
  ATNativeMediaView,
  ATNativeMainImageView,
  ATNativeCtaView,
  ATNativeCloseView,
  NativeCallback,
  type ATNative,
  type NativeAd,
} from '@anythink/react-native-sdk';

/**
 * 列表内的原生广告 cell：从共享的 ATNative 消费一条广告，绑到本 cell 渲染。
 *
 * 整个列表共用一个 ATNative（缓存池），每个广告 cell getNativeAd() 取一条不同的 NativeAd
 * 对象渲染。消费后由用户决定是否补货（这里消费即补一条）。
 * cell 卸载销毁该条；可见性 onPause/onResume 由父列表统一调度（通过 onAdReady 把对象回传）。
 */
export function ListNativeAd({
  native,
  cellId,
  onEvent,
  onAdReady,
  onAdGone,
}: {
  native: ATNative;
  cellId: string;
  /** 初始倾向（父列表用它选 ATNative 缓存池）；实际布局以 offer 的 isExpress 为准。 */
  mode?: 'template' | 'self';
  onEvent?: (callbackName: string) => void;
  /** 拿到广告对象后回传给父列表（用于存活集合 + 可见性调度）。 */
  onAdReady?: (cellId: string, ad: NativeAd) => void;
  /** cell 卸载，通知父列表移出存活集合。 */
  onAdGone?: (cellId: string) => void;
}) {
  const [ad, setAd] = useState<NativeAd | null>(null);
  const [status, setStatus] = useState<'loading' | 'loaded' | 'empty'>(
    'loading'
  );
  // 主图/视频二选一：offer 同时有 mainImage 和 mediaView 时优先展示视频，避免上下画两块。
  const [hasMedia, setHasMedia] = useState(false);
  const adRef = useRef<NativeAd | null>(null);
  const evt = useRef(onEvent);
  evt.current = onEvent;
  const ready = useRef(onAdReady);
  ready.current = onAdReady;
  const gone = useRef(onAdGone);
  gone.current = onAdGone;

  const dismissAd = useCallback(() => {
    const current = adRef.current;
    if (!current) {
      return;
    }
    current.destroy();
    adRef.current = null;
    setAd(null);
    setStatus('empty');
    gone.current?.(cellId);
  }, [cellId]);

  const handleAdEvent = useCallback(
    (callbackName: string) => {
      evt.current?.(callbackName);
      if (callbackName === NativeCallback.close) {
        dismissAd();
      }
    },
    [dismissAd]
  );

  useEffect(() => {
    let mounted = true;
    let timer: ReturnType<typeof setTimeout> | null = null;

    // 从共享缓存池消费一条；取不到（池空）则补货 + 轮询重取（最多 N 次），
    // 补货 + 等待后重取，让等待中的广告位拿到货再显示。补货是用户策略，可调。
    const tryConsume = async (attempt: number) => {
      const got = await native.getNativeAd();
      if (!mounted) {
        got?.destroy();
        return;
      }
      if (got) {
        adRef.current = got;
        // 读素材判断有无视频：有 mediaView 则优先视频、隐藏主图（二选一）。
        try {
          const m = await got.getAdMaterial();
          if (mounted) {
            setHasMedia(m?.isMediaViewAvailable === true);
          }
        } catch {
          // 读素材失败不影响展示，按无视频处理（走主图）。
        }
        if (!mounted) {
          got.destroy();
          adRef.current = null;
          return;
        }
        setAd(got);
        setStatus('loaded');
        ready.current?.(cellId, got);
        native.makeAdRequest(); // 消费即补一条，给后续 cell 备货
        return;
      }
      // 池空：补货后稍等重取
      native.makeAdRequest();
      if (attempt < 5) {
        timer = setTimeout(() => void tryConsume(attempt + 1), 800);
      } else {
        setStatus('empty');
      }
    };

    void tryConsume(0);

    return () => {
      mounted = false;
      if (timer) clearTimeout(timer);
      gone.current?.(cellId);
      if (adRef.current) {
        adRef.current.destroy();
        adRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cellId]);

  // 没货（加载中/池空）时不占空间：返回 null。
  if (status !== 'loaded' || !ad) {
    return null;
  }

  // 据「实际 offer 类型」分流：同一个位会混返模板和自渲染，按 ad.isExpress 决定布局。
  // express 进自渲染布局 = 空壳（素材由 SDK 模板 view 画，不暴露给 AssetView）；必须走模板布局。
  const asTemplate = ad.isExpress;
  const tag = asTemplate ? '模板 Native' : '自渲染 Native';

  if (asTemplate) {
    return (
      <View style={styles.tplRow}>
        <Text style={styles.tag}>{tag}</Text>
        <ATNativeAdView
          ad={ad}
          adWidth={320}
          adHeight={320}
          style={styles.tplView}
          onAdEvent={(p) => handleAdEvent(p.callbackName)}
        />
      </View>
    );
  }

  return (
    <View style={styles.row}>
      <Text style={styles.tag}>{tag}</Text>
      <ATNativeAdView
        ad={ad}
        style={styles.selfView}
        onAdEvent={(p) => handleAdEvent(p.callbackName)}
      >
        <View style={styles.selfTop}>
          <ATNativeIconView style={styles.selfIcon} />
          <View style={styles.selfTextCol}>
            <ATNativeTitleView style={styles.selfTitle} />
            <ATNativeDescView style={styles.selfDesc} />
          </View>
        </View>
        {hasMedia ? (
          <ATNativeMediaView style={styles.selfMedia} />
        ) : (
          <ATNativeMainImageView style={styles.selfMain} resizeMode="cover" />
        )}
        <ATNativeCtaView style={styles.selfCtaText} />
        <ATNativeCloseView style={styles.selfClose} />
      </ATNativeAdView>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    padding: 8,
    backgroundColor: '#f7f7f7',
    borderWidth: 1,
    borderColor: '#d7d7d7',
  },
  // 模板：SDK 创意（含视频）高度不定，overflow:hidden 防溢出盖住相邻 cell；
  // tplView 不锁死高度，让原生 forceLayoutNativeChild 按内容撑（adHeight 仅请求 hint）。
  tplRow: {
    padding: 8,
    backgroundColor: '#f7f7f7',
    borderWidth: 1,
    borderColor: '#d7d7d7',
    overflow: 'hidden',
  },
  tag: { fontSize: 11, color: '#999', marginBottom: 6, marginLeft: 2 },
  tplView: { width: 320, height: 320, alignSelf: 'center' },
  selfView: { padding: 10, backgroundColor: '#fff', borderRadius: 8 },
  selfTop: { flexDirection: 'row', alignItems: 'center' },
  selfIcon: { width: 48, height: 48, borderRadius: 6, backgroundColor: '#eee' },
  selfTextCol: { flex: 1, marginLeft: 10 },
  selfTitle: { fontSize: 15, fontWeight: '600', color: '#222' },
  selfDesc: { fontSize: 12, color: '#666', marginTop: 2 },
  selfMedia: {
    height: 140,
    marginTop: 8,
    borderRadius: 6,
    backgroundColor: '#eee',
    overflow: 'hidden',
  },
  selfMain: {
    height: 140,
    marginTop: 8,
    borderRadius: 6,
    backgroundColor: '#eee',
  },
  // CTA 的背景/尺寸/padding 必须写在 ATNativeCtaView 自身：自渲染重建只 measure 绑了 ref 的
  // AssetView，套在外层包裹 View 上的背景/尺寸量不到，会塌成看不见的白字。
  selfCtaText: {
    marginTop: 8,
    alignSelf: 'flex-start',
    minWidth: 96,
    paddingVertical: 8,
    paddingHorizontal: 14,
    backgroundColor: '#2d6cdf',
    borderRadius: 6,
    color: '#fff',
    fontSize: 13,
    textAlign: 'center',
  },
  selfClose: {
    position: 'absolute',
    top: 0,
    right: 2,
    width: 16,
    height: 16,
    zIndex: 1,
  },
});
