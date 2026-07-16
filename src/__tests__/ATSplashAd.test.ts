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
    loadSplash: jest.fn(),
    showSplash: jest.fn(() => Promise.resolve()),
    showSplashWithConfig: jest.fn(() => Promise.resolve()),
    splashReady: jest.fn(() => Promise.resolve(true)),
    checkSplashLoadStatus: jest.fn(() =>
      Promise.resolve({ isLoading: false, isReady: true })
    ),
    getSplashValidAds: jest.fn(() => Promise.resolve([])),
    entrySplashScenario: jest.fn(),
    setSplashLocalExtra: jest.fn(),
    setSplashTKExtra: jest.fn(),
  },
}));

import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATSplashAd } from '../splash/ATSplashAd';
import { CallName, SplashCallback } from '../constants/Const';

const bridge = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;

function emit(raw: Record<string, unknown>): void {
  mockListeners[CallName.SplashCall]?.(raw);
}

describe('ATSplashAd (phase-2c)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    jest.clearAllMocks();
    ATAdEvents.init();
  });

  it('S-Q01: load(adRequest, fetchAdTimeout) 透传到 TurboModule', () => {
    const ad = new ATSplashAd('p-splash');
    ad.load(undefined, 5000);
    expect(bridge.loadSplash).toHaveBeenCalledWith('p-splash', {
      fetchAdTimeout: 5000,
    });
  });

  it('loaded 事件透传 isTimeout 到 onSplashAdLoaded', () => {
    const ad = new ATSplashAd('p1');
    const onLoaded = jest.fn();
    ad.setAdListener({ onSplashAdLoaded: onLoaded });
    emit({
      callbackName: SplashCallback.loaded,
      placementID: 'p1',
      isTimeout: true,
    });
    expect(onLoaded).toHaveBeenCalledWith(true);
  });

  it('loadFail → onSplashAdLoadFail 携带 fullErrorInfo', () => {
    const ad = new ATSplashAd('p1');
    const onFail = jest.fn();
    ad.setAdListener({ onSplashAdLoadFail: onFail });
    emit({
      callbackName: SplashCallback.loadFail,
      placementID: 'p1',
      requestMessage: 'no fill',
    });
    expect(onFail.mock.calls[0]?.[0]?.fullErrorInfo).toBe('no fill');
  });

  it('timeout / show / close 分发到对应回调', () => {
    const ad = new ATSplashAd('p1');
    const cb = {
      onSplashAdTimeout: jest.fn(),
      onSplashAdShow: jest.fn(),
      onSplashAdClose: jest.fn(),
    };
    ad.setAdListener(cb);
    emit({ callbackName: SplashCallback.timeout, placementID: 'p1' });
    emit({ callbackName: SplashCallback.show, placementID: 'p1' });
    emit({ callbackName: SplashCallback.close, placementID: 'p1' });
    expect(cb.onSplashAdTimeout).toHaveBeenCalledTimes(1);
    expect(cb.onSplashAdShow).toHaveBeenCalledTimes(1);
    expect(cb.onSplashAdClose).toHaveBeenCalledTimes(1);
  });

  it('多 placement 不串', () => {
    const ad1 = new ATSplashAd('p1');
    const onLoaded1 = jest.fn();
    ad1.setAdListener({ onSplashAdLoaded: onLoaded1 });
    emit({ callbackName: SplashCallback.loaded, placementID: 'p2' });
    expect(onLoaded1).not.toHaveBeenCalled();
  });

  it('load(adRequest) 包成 {atAdRequest} 透传到 TurboModule', () => {
    const ad = new ATSplashAd('p1');
    ad.load({ channelSource: 1 });
    expect(bridge.loadSplash).toHaveBeenCalledWith('p1', {
      atAdRequest: { channelSource: 1 },
    });
  });

  it('load() 无参 → extraDic 为空对象', () => {
    const ad = new ATSplashAd('p1');
    ad.load();
    expect(bridge.loadSplash).toHaveBeenCalledWith('p1', {});
  });

  it('show() 无 config → showSplash(pid,"")；string → scenario；object → WithConfig', () => {
    const ad = new ATSplashAd('p1');
    ad.show();
    expect(bridge.showSplash).toHaveBeenCalledWith('p1', '');
    ad.show('sc');
    expect(bridge.showSplash).toHaveBeenCalledWith('p1', 'sc');
    ad.show({ scenarioId: 'sc2', heightRatio: 0.3 } as never);
    expect(bridge.showSplashWithConfig).toHaveBeenCalledWith('p1', {
      scenarioId: 'sc2',
      heightRatio: 0.3,
    });
  });

  it('isAdReady 返回 Promise<boolean> 并透传 TurboModule', async () => {
    const ad = new ATSplashAd('p1');
    await expect(ad.isAdReady()).resolves.toBe(true);
    expect(bridge.splashReady).toHaveBeenCalledWith('p1');
  });

  it('checkAdStatus / checkValidAdCaches 透传桥', async () => {
    const ad = new ATSplashAd('p1');
    await ad.checkAdStatus();
    expect(bridge.checkSplashLoadStatus).toHaveBeenCalledWith('p1');
    await ad.checkValidAdCaches();
    expect(bridge.getSplashValidAds).toHaveBeenCalledWith('p1');
  });

  it('entryAdScenario 默认空 tkExtra + 自定义透传', () => {
    ATSplashAd.entryAdScenario('p1', 'sc1');
    expect(bridge.entrySplashScenario).toHaveBeenCalledWith('p1', 'sc1', {});
    ATSplashAd.entryAdScenario('p1', 'sc2', { tk: 1 });
    expect(bridge.entrySplashScenario).toHaveBeenCalledWith('p1', 'sc2', {
      tk: 1,
    });
  });

  it('setLocalExtra / setTKExtra 透传桥', () => {
    const ad = new ATSplashAd('p1');
    ad.setLocalExtra({ a: 1 });
    expect(bridge.setSplashLocalExtra).toHaveBeenCalledWith('p1', { a: 1 });
    ad.setTKExtra({ b: 2 });
    expect(bridge.setSplashTKExtra).toHaveBeenCalledWith('p1', { b: 2 });
  });

  it('AdSource 事件分流到 setAdSourceStatusListener', () => {
    const ad = new ATSplashAd('p1');
    const onSource = jest.fn();
    ad.setAdSourceStatusListener(onSource);
    emit({ callbackName: SplashCallback.adSourceAttempt, placementID: 'p1' });
    expect(onSource).toHaveBeenCalledTimes(1);
  });

  it('multipleLoaded 事件不命中 main 回调（走 default 分支）', () => {
    const ad = new ATSplashAd('p1');
    const onMulti = jest.fn();
    ad.setAdMultipleLoadedListener(onMulti);
    // multipleLoaded 非 AdSource，进 dispatchSplashEvent 的 default 分支
    expect(() =>
      emit({ callbackName: SplashCallback.multipleLoaded, placementID: 'p1' })
    ).not.toThrow();
  });

  it('setAdRevenueListener 注册不抛（保留 API）', () => {
    const ad = new ATSplashAd('p1');
    expect(() => ad.setAdRevenueListener(jest.fn())).not.toThrow();
  });

  it('重复设置监听器命中 ensureRegistered 已注册早返回分支', () => {
    const ad = new ATSplashAd('p1');
    ad.setAdListener({ onSplashAdLoaded: jest.fn() }); // registered = true
    expect(() => ad.setAdSourceStatusListener(jest.fn())).not.toThrow(); // 已注册早返回
  });

  it('removeAdListener 后事件不再分发', () => {
    const ad = new ATSplashAd('p1');
    const onLoaded = jest.fn();
    ad.setAdListener({ onSplashAdLoaded: onLoaded });
    ad.removeAdListener();
    emit({ callbackName: SplashCallback.loaded, placementID: 'p1' });
    expect(onLoaded).not.toHaveBeenCalled();
  });

  it('dispatchSplashEvent 覆盖剩余 callbackName 分支（click/deeplink/default）', () => {
    const ad = new ATSplashAd('p1');
    const cb = {
      onSplashAdClick: jest.fn(),
      onDeeplinkCallback: jest.fn(),
    };
    ad.setAdListener(cb);
    emit({ callbackName: SplashCallback.click, placementID: 'p1' });
    emit({
      callbackName: SplashCallback.deeplink,
      placementID: 'p1',
      isDeeplinkSuccess: true,
    });
    emit({ callbackName: SplashCallback.showFailed, placementID: 'p1' });
    emit({ callbackName: 'someUnknownCallback', placementID: 'p1' });
    expect(cb.onSplashAdClick).toHaveBeenCalledTimes(1);
    expect(cb.onDeeplinkCallback).toHaveBeenCalledTimes(1);
    expect(cb.onDeeplinkCallback.mock.calls[0]?.[1]).toBe(true);
  });
});
