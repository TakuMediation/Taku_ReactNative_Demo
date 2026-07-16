const mockListeners: Record<string, (raw: unknown) => void> = {};

jest.mock('react-native', () => ({
  NativeEventEmitter: jest.fn().mockImplementation(() => ({
    addListener: (event: string, cb: (raw: unknown) => void) => {
      mockListeners[event] = cb;
      return { remove: jest.fn() };
    },
  })),
}));

jest.mock('../specs/NativeATBridge', () => ({
  __esModule: true,
  default: {
    addAutoLoadRewardedVideo: jest.fn(),
    removeAutoLoadRewardedVideo: jest.fn(),
    autoLoadRewardedVideoReady: jest.fn(() => true),
    showAutoLoadRewardedVideo: jest.fn(() => Promise.resolve()),
    showAutoLoadRewardedVideoWithConfig: jest.fn(() => Promise.resolve()),
    addAutoLoadInterstitial: jest.fn(),
    removeAutoLoadInterstitial: jest.fn(),
    autoLoadInterstitialReady: jest.fn(() => true),
    showAutoLoadInterstitial: jest.fn(() => Promise.resolve()),
    showAutoLoadInterstitialWithConfig: jest.fn(() => Promise.resolve()),
  },
}));

import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATRewardVideoAutoAd } from '../rewardvideo/ATRewardVideoAutoAd';
import { ATInterstitialAutoAd } from '../interstitial/ATInterstitialAutoAd';
import {
  CallName,
  RewardVideoCallback,
  InterstitialCallback,
} from '../constants/Const';

const bridge = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;

function emit(callName: string, raw: Record<string, unknown>): void {
  mockListeners[callName]?.(raw);
}

beforeEach(() => {
  ATAdEvents.dispose();
  for (const k of Object.keys(mockListeners)) {
    delete mockListeners[k];
  }
  jest.clearAllMocks();
  ATAdEvents.init(); // 把 NativeEventEmitter 监听器接进 mockListeners
});

describe('ATRewardVideoAutoAd', () => {
  it('addPlacement 默认空 settings + 自定义 settings 透传', () => {
    ATRewardVideoAutoAd.addPlacement('p1');
    expect(bridge.addAutoLoadRewardedVideo).toHaveBeenCalledWith('p1', {});
    ATRewardVideoAutoAd.addPlacement('p2', { a: 1 });
    expect(bridge.addAutoLoadRewardedVideo).toHaveBeenCalledWith('p2', {
      a: 1,
    });
  });

  it('removePlacement / isAdReady / show 透传桥', async () => {
    ATRewardVideoAutoAd.removePlacement('p1');
    expect(bridge.removeAutoLoadRewardedVideo).toHaveBeenCalledWith('p1');

    expect(ATRewardVideoAutoAd.isAdReady('p1')).toBe(true);
    expect(bridge.autoLoadRewardedVideoReady).toHaveBeenCalledWith('p1');

    await ATRewardVideoAutoAd.show('p1');
    expect(bridge.showAutoLoadRewardedVideo).toHaveBeenCalledWith('p1', '');
    await ATRewardVideoAutoAd.show('p1', 'sc');
    expect(bridge.showAutoLoadRewardedVideo).toHaveBeenCalledWith('p1', 'sc');
    await ATRewardVideoAutoAd.show('p1', {
      scenarioId: 'sc',
      showCustomExt: 'ext',
    });
    expect(bridge.showAutoLoadRewardedVideoWithConfig).toHaveBeenCalledWith(
      'p1',
      { scenarioId: 'sc', showCustomExt: 'ext' }
    );
  });

  it('show with config 在 WithConfig 不可用时回退 JSON scenario', async () => {
    delete (bridge as { showAutoLoadRewardedVideoWithConfig?: unknown })
      .showAutoLoadRewardedVideoWithConfig;
    await ATRewardVideoAutoAd.show('p1', {
      scenarioId: 'sc',
      showCustomExt: 'ext',
    });
    expect(bridge.showAutoLoadRewardedVideo).toHaveBeenCalledWith(
      'p1',
      JSON.stringify({ scenarioId: 'sc', showCustomExt: 'ext' })
    );
  });

  it('setAdListener 经同一事件通道分发；removeAdListener 取消', () => {
    const onLoaded = jest.fn();
    ATRewardVideoAutoAd.setAdListener('p1', {
      onRewardedVideoAdLoaded: onLoaded,
    });
    emit(CallName.RewardedVideoCall, {
      callbackName: RewardVideoCallback.loaded,
      placementID: 'p1',
    });
    expect(onLoaded).toHaveBeenCalledTimes(1);

    ATRewardVideoAutoAd.removeAdListener('p1');
    emit(CallName.RewardedVideoCall, {
      callbackName: RewardVideoCallback.loaded,
      placementID: 'p1',
    });
    expect(onLoaded).toHaveBeenCalledTimes(1);
  });
});

describe('ATInterstitialAutoAd', () => {
  it('addPlacement 默认空 settings + 自定义 settings 透传', () => {
    ATInterstitialAutoAd.addPlacement('p1');
    expect(bridge.addAutoLoadInterstitial).toHaveBeenCalledWith('p1', {});
    ATInterstitialAutoAd.addPlacement('p2', { a: 1 });
    expect(bridge.addAutoLoadInterstitial).toHaveBeenCalledWith('p2', { a: 1 });
  });

  it('removePlacement / isAdReady / show 透传桥', async () => {
    ATInterstitialAutoAd.removePlacement('p1');
    expect(bridge.removeAutoLoadInterstitial).toHaveBeenCalledWith('p1');

    expect(ATInterstitialAutoAd.isAdReady('p1')).toBe(true);
    expect(bridge.autoLoadInterstitialReady).toHaveBeenCalledWith('p1');

    await ATInterstitialAutoAd.show('p1');
    expect(bridge.showAutoLoadInterstitial).toHaveBeenCalledWith('p1', '');
    await ATInterstitialAutoAd.show('p1', 'sc');
    expect(bridge.showAutoLoadInterstitial).toHaveBeenCalledWith('p1', 'sc');
    await ATInterstitialAutoAd.show('p1', {
      scenarioId: 'sc',
      showCustomExt: 'ext',
    });
    expect(bridge.showAutoLoadInterstitialWithConfig).toHaveBeenCalledWith(
      'p1',
      { scenarioId: 'sc', showCustomExt: 'ext' }
    );
  });

  it('show with config 在 WithConfig 不可用时回退 JSON scenario', async () => {
    delete (bridge as { showAutoLoadInterstitialWithConfig?: unknown })
      .showAutoLoadInterstitialWithConfig;
    await ATInterstitialAutoAd.show('p1', {
      scenarioId: 'sc',
      showCustomExt: 'ext',
    });
    expect(bridge.showAutoLoadInterstitial).toHaveBeenCalledWith(
      'p1',
      JSON.stringify({ scenarioId: 'sc', showCustomExt: 'ext' })
    );
  });

  it('setAdListener 经同一事件通道分发；removeAdListener 取消', () => {
    const onLoaded = jest.fn();
    ATInterstitialAutoAd.setAdListener('p1', {
      onInterstitialAdLoaded: onLoaded,
    });
    emit(CallName.InterstitialCall, {
      callbackName: InterstitialCallback.loaded,
      placementID: 'p1',
    });
    expect(onLoaded).toHaveBeenCalledTimes(1);

    ATInterstitialAutoAd.removeAdListener('p1');
    emit(CallName.InterstitialCall, {
      callbackName: InterstitialCallback.loaded,
      placementID: 'p1',
    });
    expect(onLoaded).toHaveBeenCalledTimes(1);
  });
});
