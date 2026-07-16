import { useEffect, useRef, useState } from 'react';
import {
  ATInterstitialAd,
  ATInterstitialAutoAd,
  type ATInterstitialListener,
} from '@anythink/react-native-sdk';
import { AdCard } from '../components/AdCard';
import { AdButton } from '../components/AdButton';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

/** 插屏（卡片风 + 网络选择 + Auto Load）。 */
export function InterstitialScreen({ onBack }: ScreenProps) {
  const { LogArea, append } = useLogArea();
  const networks = Object.keys(sdkConfig.networks.interstitial ?? { All: '' });
  const [network, setNetwork] = useState(networks[0] ?? 'All');
  const [autoLoad, setAutoLoad] = useState(false);
  const adRef = useRef<ATInterstitialAd | null>(null);

  const pid = sdkConfig.networks.interstitial?.[network] ?? '';
  const scenarioId = sdkConfig.interstitialScenarioId;
  const showCustomExt = sdkConfig.interstitialShowCustomExt;

  const listener: ATInterstitialListener = {
    onInterstitialAdLoaded: () => append('onInterstitialAdLoaded'),
    onInterstitialAdLoadFail: (e) =>
      append('onInterstitialAdLoadFail: ' + e.fullErrorInfo),
    onInterstitialAdShow: () => append('onInterstitialAdShow'),
    onInterstitialAdShowFailed: (e) =>
      append('onInterstitialAdShowFailed: ' + e.fullErrorInfo),
    onInterstitialAdClose: () => append('onInterstitialAdClose'),
    onInterstitialAdClicked: () => append('onInterstitialAdClicked'),
    onInterstitialAdVideoStart: () => append('onInterstitialAdVideoStart'),
    onInterstitialAdVideoEnd: () => append('onInterstitialAdVideoEnd'),
    onInterstitialAdVideoError: (e) =>
      append('onInterstitialAdVideoError: ' + e.fullErrorInfo),
  };

  useEffect(() => {
    const ad = new ATInterstitialAd(pid);
    ad.setAdListener(listener);
    adRef.current = ad;
    return () => ad.removeAdListener();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pid]);

  const onLoad = () => {
    if (autoLoad) {
      append('autoLoad addPlacement');
      ATInterstitialAutoAd.setAdListener(pid, listener);
      ATInterstitialAutoAd.addPlacement(pid);
    } else {
      append('load');
      adRef.current?.load();
    }
  };
  const onReady = async () => {
    const ready = autoLoad
      ? ATInterstitialAutoAd.isAdReady(pid)
      : await adRef.current?.isAdReady();
    append('isAdReady = ' + ready);
  };
  const onShow = () => {
    append('show()');
    if (autoLoad) {
      ATInterstitialAutoAd.show(pid);
    } else {
      adRef.current?.show();
    }
  };
  const onShowWithScenario = () => {
    append(`show(scenario=${scenarioId}) pid=${pid}`);
    if (autoLoad) {
      ATInterstitialAutoAd.show(pid, scenarioId);
    } else {
      adRef.current?.show(scenarioId);
    }
  };
  const onShowWithConfig = () => {
    const config = { scenarioId, showCustomExt };
    append(
      `show(showConfig) scenarioId=${scenarioId} showCustomExt=${showCustomExt}`
    );
    if (autoLoad) {
      ATInterstitialAutoAd.show(pid, config);
    } else {
      adRef.current?.show(config);
    }
  };

  return (
    <AdCard
      title="Interstitial"
      onBack={onBack}
      networks={networks}
      network={network}
      onNetworkChange={setNetwork}
      autoLoad={autoLoad}
      onAutoLoadChange={setAutoLoad}
      log={<LogArea />}
      actions={
        <>
          <AdButton label="Load Ad" onPress={onLoad} />
          <AdButton label="Is Ad Ready" onPress={onReady} />
          <AdButton label="Show Ad" color="#1a9c54" onPress={onShow} />
          <AdButton
            label="Show + Scenario"
            color="#15803d"
            onPress={onShowWithScenario}
          />
          <AdButton
            label="Show + ShowConfig"
            color="#166534"
            onPress={onShowWithConfig}
          />
        </>
      }
    />
  );
}
