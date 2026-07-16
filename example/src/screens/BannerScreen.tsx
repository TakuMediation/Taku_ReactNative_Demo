import { useEffect, useRef, useState } from 'react';
import { View, StyleSheet } from 'react-native';
import {
  ATBannerView,
  ATBannerViewComponent,
  BannerCallback,
  type ATBannerListener,
} from '@anythink/react-native-sdk';
import { AdCard } from '../components/AdCard';
import { AdButton } from '../components/AdButton';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

const BANNER_W = 320;
const BANNER_H = 50;

/** Banner：命令式 load / checkAdStatus；展示时挂载 Fabric 视图。 */
export function BannerScreen({ onBack }: ScreenProps) {
  const { LogArea, append } = useLogArea();
  const networks = Object.keys(sdkConfig.networks.banner ?? { All: '' });
  const [network, setNetwork] = useState(networks[0] ?? 'All');
  const [visible, setVisible] = useState(false);
  const [showMode, setShowMode] = useState<'plain' | 'scenario' | 'config'>('plain');
  const adRef = useRef<ATBannerView | null>(null);

  const pid = sdkConfig.networks.banner?.[network] ?? '';
  const scenarioId = sdkConfig.bannerScenarioId;
  const showCustomExt = sdkConfig.bannerShowCustomExt;

  const listener: ATBannerListener = {
    onBannerAdLoaded: () => append('onBannerAdLoaded'),
    onBannerAdLoadFail: (e) =>
      append('onBannerAdLoadFail: ' + e.fullErrorInfo),
    onBannerAdShow: () => append('onBannerAdShow'),
    onBannerAdClicked: () => append('onBannerAdClicked'),
    onBannerAdClose: () => {
      append('onBannerAdClose (unmount only)');
      setVisible(false);
    },
    onBannerAdAutoRefreshed: () => append('onBannerAdAutoRefreshed'),
    onBannerAdAutoRefreshFail: (e) =>
      append('onBannerAdAutoRefreshFail: ' + e.fullErrorInfo),
  };

  const bindAd = () => {
    const ad = new ATBannerView();
    ad.setPlacementId(pid);
    ad.setBannerAdListener(listener);
    adRef.current = ad;
    return ad;
  };

  useEffect(() => {
    bindAd();
    setVisible(false);
    return () => {
      adRef.current?.destroy();
      adRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pid]);

  const onLoad = () => {
    append(`loadAd ${BANNER_W}x${BANNER_H}`);
    adRef.current?.loadAd(undefined, { width: BANNER_W, height: BANNER_H });
  };

  const onReady = async () => {
    const status = await adRef.current?.checkAdStatus();
    append(
      'isAdReady = ' +
        status?.isReady +
        ', isLoading = ' +
        status?.isLoading
    );
  };

  const mountBanner = (mode: 'plain' | 'scenario' | 'config') => {
    if (mode === 'plain') {
      append('show (mount banner view)');
      adRef.current?.setShowConfig({});
    } else if (mode === 'scenario') {
      append(`show(scenario=${scenarioId}) pid=${pid}`);
      adRef.current?.setShowConfig({ scenarioId });
      ATBannerView.entryAdScenario(pid, scenarioId);
    } else {
      const config = { scenarioId, showCustomExt };
      append(
        `show(showConfig) scenarioId=${scenarioId} showCustomExt=${showCustomExt}`
      );
      adRef.current?.setShowConfig(config);
    }
    setShowMode(mode);
    setVisible(true);
  };

  const onDestroy = () => {
    append('destroy → destroyBanner');
    setVisible(false);
    adRef.current?.destroy();
    bindAd();
  };

  return (
    <AdCard
      title="Banner"
      onBack={onBack}
      networks={networks}
      network={network}
      onNetworkChange={setNetwork}
      autoLoad={false}
      onAutoLoadChange={() => append('Banner 无 Auto Load')}
      log={<LogArea />}
      actions={
        <>
          <AdButton label="Load Ad" onPress={onLoad} />
          <AdButton label="Is Ad Ready" onPress={onReady} />
          <AdButton
            label="Show Ad"
            color="#1a9c54"
            onPress={() => mountBanner('plain')}
          />
          <AdButton
            label="Show + Scenario"
            color="#15803d"
            onPress={() => mountBanner('scenario')}
          />
          <AdButton
            label="Show + ShowConfig"
            color="#166534"
            onPress={() => mountBanner('config')}
          />
          <AdButton label="Destroy" color="#7f1d1d" onPress={onDestroy} />
          {visible ? (
            <View style={styles.bannerArea}>
              <ATBannerViewComponent
                key={`${pid}-${showMode}`}
                placementID={pid}
                width={BANNER_W}
                height={BANNER_H}
                onAdEvent={(p) => {
                  append(p.callbackName);
                  if (p.callbackName === BannerCallback.close) {
                    setVisible(false);
                  }
                }}
              />
            </View>
          ) : null}
        </>
      }
    />
  );
}

const styles = StyleSheet.create({
  bannerArea: {
    height: 50,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 8,
  },
});
