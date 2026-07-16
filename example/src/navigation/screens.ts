/** demo 屏名（手搓导航，零依赖）。 */
export type ScreenName =
  | 'init'
  | 'home'
  | 'reward'
  | 'interstitial'
  | 'splash'
  | 'banner'
  | 'native'
  | 'list'
  | 'feature';

/** 各广告/功能屏统一收到的导航回调。 */
export interface ScreenProps {
  onBack: () => void;
}
