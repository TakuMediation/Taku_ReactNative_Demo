import { useEffect, useRef, useState } from 'react';
import {
  ATRewardVideoAd,
  ATRewardVideoAutoAd,
  type ATRewardVideoListener,
} from '@anythink/react-native-sdk';
import { AdCard } from '../components/AdCard';
import { AdButton } from '../components/AdButton';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

/** 激励视频（卡片风 + 网络选择 + Auto Load）。 */
export function RewardScreen({ onBack }: ScreenProps) {
  const { LogArea, append } = useLogArea();
  const networks = Object.keys(sdkConfig.networks.reward ?? { All: '' });
  const [network, setNetwork] = useState(networks[0] ?? 'All');
  const [autoLoad, setAutoLoad] = useState(false);
  const adRef = useRef<ATRewardVideoAd | null>(null);

  const pid = sdkConfig.networks.reward?.[network] ?? '';
  const scenarioId = sdkConfig.rewardScenarioId;
  const showCustomExt = sdkConfig.rewardShowCustomExt;

  const listener: ATRewardVideoListener = {
    onRewardedVideoAdLoaded: () => append('onRewardedVideoAdLoaded'),
    onRewardedVideoAdFailed: (e) =>
      append('onRewardedVideoAdFailed: ' + e.fullErrorInfo),
    onRewardedVideoAdPlayStart: () => append('onRewardedVideoAdPlayStart'),
    onRewardedVideoAdPlayEnd: () => append('onRewardedVideoAdPlayEnd'),
    onRewardedVideoAdClosed: () => append('onRewardedVideoAdClosed'),
    onRewardedVideoAdPlayClicked: () => append('onRewardedVideoAdPlayClicked'),
    onReward: () => append('onReward'),
  };

  // 手动模式：本实例订阅。Auto 模式：AutoAd 静态订阅（在按钮里）。
  useEffect(() => {
    const ad = new ATRewardVideoAd(pid);
    ad.setAdListener(listener);
    adRef.current = ad;
    return () => ad.removeAdListener();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pid]);

  const onLoad = () => {
    if (autoLoad) {
      append('autoLoad addPlacement');
      ATRewardVideoAutoAd.setAdListener(pid, listener);
      ATRewardVideoAutoAd.addPlacement(pid);
    } else {
      append('load');
      adRef.current?.load();
    }
  };
  const onReady = async () => {
    const ready = autoLoad
      ? ATRewardVideoAutoAd.isAdReady(pid)
      : await adRef.current?.isAdReady();
    append('isAdReady = ' + ready);
  };
  const onShow = () => {
    append('show()');
    if (autoLoad) {
      ATRewardVideoAutoAd.show(pid);
    } else {
      adRef.current?.show();
    }
  };
  const onShowWithScenario = () => {
    append(`show(scenario=${scenarioId}) pid=${pid}`);
    if (autoLoad) {
      ATRewardVideoAutoAd.show(pid, scenarioId);
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
      ATRewardVideoAutoAd.show(pid, config);
    } else {
      adRef.current?.show(config);
    }
  };

  return (
    <AdCard
      title="Rewarded Video"
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
