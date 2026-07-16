// 捕获 NativeEventEmitter 注册的监听器（变量名 mock 前缀以满足 jest.mock 工厂约束）
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
    loadRewardedVideo: jest.fn(),
    showRewardedVideo: jest.fn(() => Promise.resolve()),
    showRewardedVideoWithConfig: jest.fn(() => Promise.resolve()),
    rewardedVideoReady: jest.fn(() => Promise.resolve(true)),
    checkRewardedVideoLoadStatus: jest.fn(() =>
      Promise.resolve({ isLoading: false, isReady: true })
    ),
    getRewardedVideoValidAds: jest.fn(() => Promise.resolve([])),
    entryRewardedVideoScenario: jest.fn(),
    setRewardedVideoLocalExtra: jest.fn(),
    setRewardedVideoTKExtra: jest.fn(),
  },
}));

import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATRewardVideoAd } from '../rewardvideo/ATRewardVideoAd';
import { CallName, RewardVideoCallback } from '../constants/Const';

const bridge = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;

function emit(raw: Record<string, unknown>): void {
  mockListeners[CallName.RewardedVideoCall]?.(raw);
}

describe('ATRewardVideoAd (phase-2a)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    jest.clearAllMocks();
    ATAdEvents.init();
  });

  it('R-Q01: load(adRequest) 包成 {atAdRequest} 透传到 TurboModule', () => {
    const ad = new ATRewardVideoAd('p-reward');
    ad.load({ channelSource: 1 });
    expect(bridge.loadRewardedVideo).toHaveBeenCalledWith('p-reward', {
      atAdRequest: { channelSource: 1 },
    });
  });

  it('load() 无参 → extraDic 为空对象', () => {
    const ad = new ATRewardVideoAd('p-reward');
    ad.load();
    expect(bridge.loadRewardedVideo).toHaveBeenCalledWith('p-reward', {});
  });

  it('R-Q03/R-Q04(ready): loaded 事件 → onRewardedVideoAdLoaded（驱动 ready）', () => {
    const ad = new ATRewardVideoAd('p1');
    const onLoaded = jest.fn();
    ad.setAdListener({ onRewardedVideoAdLoaded: onLoaded });

    emit({ callbackName: RewardVideoCallback.loaded, placementID: 'p1' });

    expect(onLoaded).toHaveBeenCalledTimes(1);
  });

  it('R-Q02/R-Q04(failed): loadFail fixture → onRewardedVideoAdFailed 携带 fullErrorInfo（驱动 failed）', () => {
    // AdError fixture（requestMessage = AdError.getFullErrorInfo()）
    const loadFail = {
      callbackName: RewardVideoCallback.loadFail,
      placementID: 'p1',
      requestMessage: 'code:9999, msg:no fill',
    };
    const ad = new ATRewardVideoAd('p1');
    const onFailed = jest.fn();
    ad.setAdListener({ onRewardedVideoAdFailed: onFailed });

    emit(loadFail);

    expect(onFailed).toHaveBeenCalledTimes(1);
    expect(onFailed.mock.calls[0]?.[0]?.fullErrorInfo).toBe(
      'code:9999, msg:no fill'
    );
  });

  it('reward/playStart/close 分发到对应回调', () => {
    const ad = new ATRewardVideoAd('p1');
    const cb = {
      onRewardedVideoAdPlayStart: jest.fn(),
      onReward: jest.fn(),
      onRewardedVideoAdClosed: jest.fn(),
    };
    ad.setAdListener(cb);

    emit({ callbackName: RewardVideoCallback.playStart, placementID: 'p1' });
    emit({ callbackName: RewardVideoCallback.reward, placementID: 'p1' });
    emit({ callbackName: RewardVideoCallback.close, placementID: 'p1' });

    expect(cb.onRewardedVideoAdPlayStart).toHaveBeenCalledTimes(1);
    expect(cb.onReward).toHaveBeenCalledTimes(1);
    expect(cb.onRewardedVideoAdClosed).toHaveBeenCalledTimes(1);
  });

  it('多 placement 不串：只命中自己 placementID 的回调', () => {
    const ad1 = new ATRewardVideoAd('p1');
    const onLoaded1 = jest.fn();
    ad1.setAdListener({ onRewardedVideoAdLoaded: onLoaded1 });

    emit({ callbackName: RewardVideoCallback.loaded, placementID: 'p2' });

    expect(onLoaded1).not.toHaveBeenCalled();
  });

  it('isAdReady 返回 Promise<boolean> 并透传 TurboModule', async () => {
    const ad = new ATRewardVideoAd('p1');
    await expect(ad.isAdReady()).resolves.toBe(true);
    expect(bridge.rewardedVideoReady).toHaveBeenCalledWith('p1');
  });

  it('show() 无参 → showRewardedVideo(pid,"")；string → scenario；object → WithConfig', () => {
    const ad = new ATRewardVideoAd('p1');
    ad.show();
    expect(bridge.showRewardedVideo).toHaveBeenCalledWith('p1', '');
    ad.show('sc');
    expect(bridge.showRewardedVideo).toHaveBeenCalledWith('p1', 'sc');
    ad.show({ scenarioId: 'sc' });
    expect(bridge.showRewardedVideoWithConfig).toHaveBeenCalledWith('p1', {
      scenarioId: 'sc',
    });
  });

  it('checkAdStatus / checkValidAdCaches 透传桥', async () => {
    const ad = new ATRewardVideoAd('p1');
    await ad.checkAdStatus();
    expect(bridge.checkRewardedVideoLoadStatus).toHaveBeenCalledWith('p1');
    await ad.checkValidAdCaches();
    expect(bridge.getRewardedVideoValidAds).toHaveBeenCalledWith('p1');
  });

  it('entryAdScenario 默认空 tkExtra + 自定义透传', () => {
    ATRewardVideoAd.entryAdScenario('p1', 'sc1');
    expect(bridge.entryRewardedVideoScenario).toHaveBeenCalledWith(
      'p1',
      'sc1',
      {}
    );
    ATRewardVideoAd.entryAdScenario('p1', 'sc2', { tk: 1 });
    expect(bridge.entryRewardedVideoScenario).toHaveBeenCalledWith(
      'p1',
      'sc2',
      {
        tk: 1,
      }
    );
  });

  it('setLocalExtra / setTKExtra 透传桥', () => {
    const ad = new ATRewardVideoAd('p1');
    ad.setLocalExtra({ a: 1 });
    expect(bridge.setRewardedVideoLocalExtra).toHaveBeenCalledWith('p1', {
      a: 1,
    });
    ad.setTKExtra({ b: 2 });
    expect(bridge.setRewardedVideoTKExtra).toHaveBeenCalledWith('p1', { b: 2 });
  });

  it('AdSource 事件分流到 setAdSourceStatusListener', () => {
    const ad = new ATRewardVideoAd('p1');
    const onSource = jest.fn();
    ad.setAdSourceStatusListener(onSource);
    emit({
      callbackName: 'rewardedVideoAdSourceAttempt',
      placementID: 'p1',
    });
    expect(onSource).toHaveBeenCalledTimes(1);
  });

  it('multipleLoaded 事件分流到 setAdMultipleLoadedListener', () => {
    const ad = new ATRewardVideoAd('p1');
    const onMulti = jest.fn();
    ad.setAdMultipleLoadedListener(onMulti);
    emit({
      callbackName: RewardVideoCallback.multipleLoaded,
      placementID: 'p1',
      extraDic: { x: 1 },
    });
    expect(onMulti).toHaveBeenCalledWith({ x: 1 });
  });

  it('setAdRevenueListener 接收 CommonADCall 收入回调', () => {
    const ad = new ATRewardVideoAd('p1');
    const onRevenue = jest.fn();
    ad.setAdRevenueListener(onRevenue);
    mockListeners[CallName.CommonADCall]?.({
      callbackName: 'adShowRevenueCallbackKey',
      placementID: 'p1',
      extraDic: { networkFirmId: 1 },
    });
    expect(onRevenue).toHaveBeenCalledWith({ networkFirmId: 1 });
  });

  it('removeAdListener 后事件不再分发', () => {
    const ad = new ATRewardVideoAd('p1');
    const onLoaded = jest.fn();
    ad.setAdListener({ onRewardedVideoAdLoaded: onLoaded });
    ad.removeAdListener();
    emit({ callbackName: RewardVideoCallback.loaded, placementID: 'p1' });
    expect(onLoaded).not.toHaveBeenCalled();
  });

  it('dispatchRewardEvent 覆盖剩余 callbackName 分支', () => {
    const ad = new ATRewardVideoAd('p1');
    const cb = {
      onRewardedVideoAdPlayEnd: jest.fn(),
      onRewardedVideoAdPlayFailed: jest.fn(),
      onRewardedVideoAdPlayClicked: jest.fn(),
      onDeeplinkCallback: jest.fn(),
      onRewardedVideoAdAgainPlayStart: jest.fn(),
      onRewardedVideoAdAgainPlayEnd: jest.fn(),
      onRewardedVideoAdAgainPlayFailed: jest.fn(),
      onRewardedVideoAdAgainPlayClicked: jest.fn(),
      onAgainReward: jest.fn(),
    };
    ad.setAdListener(cb);
    const names = [
      RewardVideoCallback.playEnd,
      RewardVideoCallback.playFail,
      RewardVideoCallback.click,
      RewardVideoCallback.deeplink,
      RewardVideoCallback.againPlayStart,
      RewardVideoCallback.againPlayEnd,
      RewardVideoCallback.againPlayFail,
      RewardVideoCallback.againClick,
      RewardVideoCallback.againReward,
      'someUnknownCallback', // default 分支
    ];
    for (const callbackName of names) {
      emit({ callbackName, placementID: 'p1' });
    }
    expect(cb.onRewardedVideoAdPlayEnd).toHaveBeenCalled();
    expect(cb.onRewardedVideoAdPlayFailed).toHaveBeenCalled();
    expect(cb.onRewardedVideoAdAgainPlayFailed).toHaveBeenCalled();
    expect(cb.onAgainReward).toHaveBeenCalled();
  });
});
