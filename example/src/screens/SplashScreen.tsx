import { useEffect, useRef, useState } from 'react';
import { ATSplashAd } from '@anythink/react-native-sdk';
import { AdCard } from '../components/AdCard';
import { AdButton } from '../components/AdButton';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

/** 开屏（卡片风 + 网络选择）。开屏无 AutoAd，Auto Load 复选不生效。 */
export function SplashScreen({ onBack }: ScreenProps) {
  const { LogArea, append } = useLogArea();
  const networks = Object.keys(sdkConfig.networks.splash ?? { All: '' });
  const [network, setNetwork] = useState(networks[0] ?? 'All');
  const adRef = useRef<ATSplashAd | null>(null);

  const pid = sdkConfig.networks.splash?.[network] ?? '';
  const scenarioId = sdkConfig.splashScenarioId;
  const showCustomExt = sdkConfig.splashShowCustomExt;

  useEffect(() => {
    const ad = new ATSplashAd(pid);
    ad.setAdListener({
      onSplashAdLoaded: (isTimeout) =>
        append('onSplashAdLoaded isTimeout=' + isTimeout),
      onSplashAdLoadFail: (e) =>
        append('onSplashAdLoadFail: ' + e.fullErrorInfo),
      onSplashAdTimeout: () => append('onSplashAdTimeout'),
      onSplashAdShow: () => append('onSplashAdShow'),
      onSplashAdClick: () => append('onSplashAdClick'),
      onSplashAdClose: () => append('onSplashAdClose'),
    });
    adRef.current = ad;
    return () => ad.removeAdListener();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pid]);

  const onShow = () => {
    append('show()');
    adRef.current?.show();
  };
  const onShowWithScenario = () => {
    append(`show(scenario=${scenarioId}) pid=${pid}`);
    adRef.current?.show(scenarioId);
  };
  const onShowWithConfig = () => {
    const config = { scenarioId, showCustomExt };
    append(
      `show(showConfig) scenarioId=${scenarioId} showCustomExt=${showCustomExt}`
    );
    adRef.current?.show(config);
  };

  return (
    <AdCard
      title="Splash"
      onBack={onBack}
      networks={networks}
      network={network}
      onNetworkChange={setNetwork}
      autoLoad={false}
      onAutoLoadChange={() => append('开屏无 Auto Load')}
      log={<LogArea />}
      actions={
        <>
          <AdButton
            label="Load Ad"
            onPress={() => {
              append('load');
              adRef.current?.load();
            }}
          />
          <AdButton
            label="Is Ad Ready"
            onPress={async () =>
              append('isAdReady = ' + (await adRef.current?.isAdReady()))
            }
          />
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
