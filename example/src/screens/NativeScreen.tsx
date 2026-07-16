import { useCallback, useEffect, useRef, useState } from 'react';
import { Platform, Text, View, StyleSheet } from 'react-native';
import {
  ATNative,
  ATNativeAdView,
  ATNativeTitleView,
  ATNativeDescView,
  ATNativeCtaView,
  ATNativeIconView,
  ATNativeMainImageView,
  ATNativeMediaView,
  ATNativeCloseView,
  NativeCallback,
  type NativeAd,
} from '@anythink/react-native-sdk';
import { Screen } from '../components/Screen';
import { AdButton } from '../components/AdButton';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

const NATIVE_W = 320;
const NATIVE_H = 250;

const scenarioId = sdkConfig.nativeScenarioId;
const showCustomExt = sdkConfig.nativeShowCustomExt;

type ShowMode = 'plain' | 'scenario' | 'config';

/** 模板 / 自渲染各一套：Load → Is Ready → 三种 Show → Destroy（对齐 BannerScreen）。 */
function NativeAdBlock({
  title,
  placementId,
  express,
  append,
  renderSelf,
}: {
  title: string;
  placementId: string;
  express: boolean;
  append: (msg: string) => void;
  renderSelf?: () => React.ReactNode;
}) {
  const nativeRef = useRef<ATNative | null>(null);
  const [visible, setVisible] = useState(false);
  const [showMode, setShowMode] = useState<ShowMode>('plain');
  const [ad, setAd] = useState<NativeAd | null>(null);
  const adRef = useRef<NativeAd | null>(null);

  const unmountAd = useCallback(
    (reason: string) => {
      append(`${title} ${reason}`);
      setVisible(false);
      adRef.current?.destroy();
      adRef.current = null;
      setAd(null);
    },
    [append, title]
  );

  const bindNative = useCallback(() => {
    nativeRef.current?.dispose();
    const n = new ATNative(placementId);
    n.setAdListener({
      onNativeAdLoaded: () => append(`${title} onNativeAdLoaded`),
      onNativeAdLoadFail: (e) =>
        append(`${title} onNativeAdLoadFail: ${e.fullErrorInfo}`),
    });
    nativeRef.current = n;
    return n;
  }, [append, placementId, title]);

  useEffect(() => {
    bindNative();
    setVisible(false);
    setAd(null);
    return () => {
      nativeRef.current?.destroy();
      nativeRef.current?.dispose();
      nativeRef.current = null;
    };
  }, [bindNative, placementId]);

  const onLoad = () => {
    append(`${title} loadAd`);
    const extra =
      express && Platform.OS === 'ios' ? { isNativeShowType: false } : undefined;
    nativeRef.current?.makeAdRequest(undefined, extra);
  };

  const onReady = async () => {
    const status = await nativeRef.current?.checkAdStatus();
    append(
      `${title} isAdReady = ${status?.isReady}, isLoading = ${status?.isLoading}`
    );
  };

  const mountNative = async (mode: ShowMode) => {
    const native = nativeRef.current;
    if (!native) {
      return;
    }
    if (mode === 'plain') {
      append(`${title} show (mount native view)`);
      native.setShowConfig({});
    } else if (mode === 'scenario') {
      append(`${title} show(scenario=${scenarioId})`);
      native.setShowConfig({ scenarioId });
      ATNative.entryAdScenario(placementId, scenarioId);
    } else {
      const config = { scenarioId, showCustomExt };
      append(
        `${title} show(showConfig) scenarioId=${scenarioId} showCustomExt=${showCustomExt}`
      );
      native.setShowConfig(config);
    }
    const nextAd = await native.getNativeAd();
    if (!nextAd) {
      append(`${title} getNativeAd returned null`);
      return;
    }
    nextAd.setNativeEventListener({
      onNativeAdClose: () => unmountAd('onNativeAdClose (unmount)'),
    });
    adRef.current = nextAd;
    setShowMode(mode);
    setAd(nextAd);
    setVisible(true);
  };

  const onDestroy = () => {
    append(`${title} destroy`);
    adRef.current = null;
    setAd(null);
    setVisible(false);
    nativeRef.current?.destroy();
    bindNative();
  };

  return (
    <View style={styles.block}>
      <Text style={styles.blockTitle}>{title}</Text>
      <AdButton label="Load Ad" onPress={onLoad} />
      <AdButton label="Is Ad Ready" onPress={onReady} />
      <AdButton
        label="Show Ad"
        color="#1a9c54"
        onPress={() => mountNative('plain')}
      />
      <AdButton
        label="Show + Scenario"
        color="#15803d"
        onPress={() => mountNative('scenario')}
      />
      <AdButton
        label="Show + ShowConfig"
        color="#166534"
        onPress={() => mountNative('config')}
      />
      <AdButton label="Destroy" color="#7f1d1d" onPress={onDestroy} />
      {visible && ad ? (
        <View style={styles.adArea}>
          <ATNativeAdView
            key={`${placementId}-${showMode}`}
            ad={ad}
            adWidth={NATIVE_W}
            adHeight={NATIVE_H}
            style={express ? styles.tplView : styles.selfView}
            onAdEvent={(p) => {
              append(`${title} ${p.callbackName}`);
              if (p.callbackName === NativeCallback.close) {
                unmountAd('onNativeAdClose (unmount)');
              }
            }}
          >
            {renderSelf?.()}
          </ATNativeAdView>
        </View>
      ) : null}
    </View>
  );
}

/**
 * Native：模板（express）+ 自渲染（素材回传后用 AssetView 在 JS 侧渲染）。
 */
export function NativeScreen({
  onBack,
  onOpenList,
}: ScreenProps & { onOpenList: () => void }) {
  const { LogArea, append } = useLogArea();

  const tplPid = sdkConfig.placements.nativeExpress ?? '';
  const selfPid = sdkConfig.placements.nativeSelfRender ?? '';

  return (
    <Screen title="Native" onBack={onBack} log={<LogArea />}>
      <NativeAdBlock
        title="模板"
        placementId={tplPid}
        express
        append={append}
      />

      <NativeAdBlock
        title="自渲染"
        placementId={selfPid}
        express={false}
        append={append}
        renderSelf={() => (
          <>
            <View style={styles.selfRow}>
              <ATNativeIconView style={styles.selfIcon} />
              <View style={styles.selfTextCol}>
                <ATNativeTitleView style={styles.selfTitle} />
                <ATNativeDescView style={styles.selfDesc} />
              </View>
            </View>
            <View style={styles.selfImageArea}>
              <ATNativeMediaView style={styles.selfImageFill} />
              <ATNativeMainImageView
                style={styles.selfImageFill}
                resizeMode="cover"
              />
            </View>
            <ATNativeCtaView style={styles.selfCtaText} />
            <ATNativeCloseView style={styles.selfClose} />
          </>
        )}
      />

      <AdButton
        label="原生列表（多广告）"
        color="#6741d9"
        onPress={onOpenList}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  block: { marginBottom: 16 },
  blockTitle: {
    fontSize: 15,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
  adArea: { minHeight: 50, alignItems: 'center', marginTop: 8 },
  tplView: { width: NATIVE_W, height: NATIVE_H },
  selfView: {
    width: NATIVE_W,
    padding: 10,
    backgroundColor: '#f5f5f5',
    borderRadius: 8,
  },
  selfRow: { flexDirection: 'row', alignItems: 'center' },
  selfIcon: {
    width: 48,
    height: 48,
    borderRadius: 6,
    backgroundColor: '#eee',
  },
  selfTextCol: { flex: 1, marginLeft: 10 },
  selfTitle: { fontSize: 15, fontWeight: '600', color: '#222' },
  selfDesc: { fontSize: 12, color: '#666', marginTop: 2 },
  selfImageArea: {
    height: 160,
    marginTop: 8,
    borderRadius: 6,
    backgroundColor: '#eee',
    overflow: 'hidden',
  },
  selfImageFill: StyleSheet.absoluteFill,
  selfCtaText: {
    marginTop: 8,
    alignSelf: 'flex-start',
    minWidth: 96,
    paddingVertical: 8,
    paddingHorizontal: 14,
    backgroundColor: '#2d6cdf',
    borderRadius: 6,
    color: '#fff',
    fontSize: 12,
    textAlign: 'center',
  },
  selfClose: {
    position: 'absolute',
    top: 0,
    right: 2,
    width: 18,
    height: 18,
    zIndex: 1,
  },
});
