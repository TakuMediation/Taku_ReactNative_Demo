import { useState, useCallback } from 'react';
import type { ScreenName } from './navigation/screens';
import { InitScreen } from './screens/InitScreen';
import { HomeScreen } from './screens/HomeScreen';
import { RewardScreen } from './screens/RewardScreen';
import { InterstitialScreen } from './screens/InterstitialScreen';
import { SplashScreen } from './screens/SplashScreen';
import { BannerScreen } from './screens/BannerScreen';
import { NativeScreen } from './screens/NativeScreen';
import { ListScreen } from './screens/ListScreen';
import { FeatureScreen } from './screens/FeatureScreen';

/**
 * Demo 路由器（手搓 screen state 导航，零依赖）。
 * 起始 = init 入口页：用户同意隐私 → initSdk → 进 Home。
 * 各广告页自包含内日志区。
 */
export default function App() {
  const [screen, setScreen] = useState<ScreenName>('init');

  const onBack = useCallback(() => setScreen('home'), []);
  // 原生列表从原生页进入，返回回原生页（其余页返回回主页）
  const backToNative = useCallback(() => setScreen('native'), []);

  switch (screen) {
    case 'home':
      return <HomeScreen onNavigate={setScreen} />;
    case 'reward':
      return <RewardScreen onBack={onBack} />;
    case 'interstitial':
      return <InterstitialScreen onBack={onBack} />;
    case 'splash':
      return <SplashScreen onBack={onBack} />;
    case 'banner':
      return <BannerScreen onBack={onBack} />;
    case 'native':
      return (
        <NativeScreen onBack={onBack} onOpenList={() => setScreen('list')} />
      );
    case 'list':
      return <ListScreen onBack={backToNative} />;
    case 'feature':
      return <FeatureScreen onBack={onBack} />;
    default:
      return <InitScreen onEnterDemo={() => setScreen('home')} />;
  }
}
