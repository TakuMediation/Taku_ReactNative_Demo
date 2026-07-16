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
    loadInterstitial: jest.fn(),
    showInterstitial: jest.fn(() => Promise.resolve()),
    showInterstitialWithConfig: jest.fn(() => Promise.resolve()),
    interstitialReady: jest.fn(() => Promise.resolve(true)),
    checkInterstitialLoadStatus: jest.fn(() =>
      Promise.resolve({ isLoading: false, isReady: true })
    ),
    getInterstitialValidAds: jest.fn(() => Promise.resolve([])),
    entryInterstitialScenario: jest.fn(),
    setInterstitialLocalExtra: jest.fn(),
    setInterstitialTKExtra: jest.fn(),
  },
}));

import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATInterstitialAd } from '../interstitial/ATInterstitialAd';
import { CallName, InterstitialCallback } from '../constants/Const';

const bridge = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;

function emit(raw: Record<string, unknown>): void {
  mockListeners[CallName.InterstitialCall]?.(raw);
}

describe('ATInterstitialAd (phase-2b)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    jest.clearAllMocks();
    ATAdEvents.init();
  });

  it('I-Q01: load(adRequest) 包成 {atAdRequest} 透传到 TurboModule', () => {
    const ad = new ATInterstitialAd('p-inter');
    ad.load({ channelSource: 1 });
    expect(bridge.loadInterstitial).toHaveBeenCalledWith('p-inter', {
      atAdRequest: { channelSource: 1 },
    });
  });

  it('Event 分发: loaded → onInterstitialAdLoaded', () => {
    const ad = new ATInterstitialAd('p1');
    const onLoaded = jest.fn();
    ad.setAdListener({ onInterstitialAdLoaded: onLoaded });
    emit({ callbackName: InterstitialCallback.loaded, placementID: 'p1' });
    expect(onLoaded).toHaveBeenCalledTimes(1);
  });

  it('showFail → onInterstitialAdShowFailed 携带 fullErrorInfo', () => {
    const ad = new ATInterstitialAd('p1');
    const onShowFail = jest.fn();
    ad.setAdListener({ onInterstitialAdShowFailed: onShowFail });
    emit({
      callbackName: InterstitialCallback.showFail,
      placementID: 'p1',
      requestMessage: 'show failed',
    });
    expect(onShowFail.mock.calls[0]?.[0]?.fullErrorInfo).toBe('show failed');
  });

  it('loadFail → onInterstitialAdLoadFail 携带 fullErrorInfo', () => {
    const ad = new ATInterstitialAd('p1');
    const onFail = jest.fn();
    ad.setAdListener({ onInterstitialAdLoadFail: onFail });
    emit({
      callbackName: InterstitialCallback.loadFail,
      placementID: 'p1',
      requestMessage: 'code:1, no fill',
    });
    expect(onFail.mock.calls[0]?.[0]?.fullErrorInfo).toBe('code:1, no fill');
  });

  it('show/video/close 分发到对应回调', () => {
    const ad = new ATInterstitialAd('p1');
    const cb = {
      onInterstitialAdShow: jest.fn(),
      onInterstitialAdVideoStart: jest.fn(),
      onInterstitialAdClose: jest.fn(),
    };
    ad.setAdListener(cb);
    emit({ callbackName: InterstitialCallback.show, placementID: 'p1' });
    emit({ callbackName: InterstitialCallback.playStart, placementID: 'p1' });
    emit({ callbackName: InterstitialCallback.close, placementID: 'p1' });
    expect(cb.onInterstitialAdShow).toHaveBeenCalledTimes(1);
    expect(cb.onInterstitialAdVideoStart).toHaveBeenCalledTimes(1);
    expect(cb.onInterstitialAdClose).toHaveBeenCalledTimes(1);
  });

  it('多 placement 不串', () => {
    const ad1 = new ATInterstitialAd('p1');
    const onLoaded1 = jest.fn();
    ad1.setAdListener({ onInterstitialAdLoaded: onLoaded1 });
    emit({ callbackName: InterstitialCallback.loaded, placementID: 'p2' });
    expect(onLoaded1).not.toHaveBeenCalled();
  });

  it('load() 无参 → extraDic 为空对象', () => {
    const ad = new ATInterstitialAd('p-inter');
    ad.load();
    expect(bridge.loadInterstitial).toHaveBeenCalledWith('p-inter', {});
  });

  it('show() 无 config → showInterstitial(pid,"")；string → scenario；object → WithConfig', () => {
    const ad = new ATInterstitialAd('p1');
    ad.show();
    expect(bridge.showInterstitial).toHaveBeenCalledWith('p1', '');
    ad.show('sc');
    expect(bridge.showInterstitial).toHaveBeenCalledWith('p1', 'sc');
    ad.show({ scenarioId: 'sc2' });
    expect(bridge.showInterstitialWithConfig).toHaveBeenCalledWith('p1', {
      scenarioId: 'sc2',
    });
  });

  it('isAdReady 返回 Promise<boolean> 并透传 TurboModule', async () => {
    const ad = new ATInterstitialAd('p1');
    await expect(ad.isAdReady()).resolves.toBe(true);
    expect(bridge.interstitialReady).toHaveBeenCalledWith('p1');
  });

  it('checkAdStatus / checkValidAdCaches 透传桥', async () => {
    const ad = new ATInterstitialAd('p1');
    await ad.checkAdStatus();
    expect(bridge.checkInterstitialLoadStatus).toHaveBeenCalledWith('p1');
    await ad.checkValidAdCaches();
    expect(bridge.getInterstitialValidAds).toHaveBeenCalledWith('p1');
  });

  it('entryAdScenario 默认空 tkExtra + 自定义透传', () => {
    ATInterstitialAd.entryAdScenario('p1', 'sc1');
    expect(bridge.entryInterstitialScenario).toHaveBeenCalledWith(
      'p1',
      'sc1',
      {}
    );
    ATInterstitialAd.entryAdScenario('p1', 'sc2', { tk: 1 });
    expect(bridge.entryInterstitialScenario).toHaveBeenCalledWith('p1', 'sc2', {
      tk: 1,
    });
  });

  it('setLocalExtra / setTKExtra 透传桥', () => {
    const ad = new ATInterstitialAd('p1');
    ad.setLocalExtra({ a: 1 });
    expect(bridge.setInterstitialLocalExtra).toHaveBeenCalledWith('p1', {
      a: 1,
    });
    ad.setTKExtra({ b: 2 });
    expect(bridge.setInterstitialTKExtra).toHaveBeenCalledWith('p1', { b: 2 });
  });

  it('AdSource 事件分流到 setAdSourceStatusListener', () => {
    const ad = new ATInterstitialAd('p1');
    const onSource = jest.fn();
    ad.setAdSourceStatusListener(onSource);
    emit({
      callbackName: InterstitialCallback.adSourceAttempt,
      placementID: 'p1',
    });
    expect(onSource).toHaveBeenCalledTimes(1);
  });

  it('multipleLoaded 事件分流到 setAdMultipleLoadedListener', () => {
    const ad = new ATInterstitialAd('p1');
    const onMulti = jest.fn();
    ad.setAdMultipleLoadedListener(onMulti);
    emit({
      callbackName: InterstitialCallback.multipleLoaded,
      placementID: 'p1',
      extraDic: { x: 1 },
    });
    expect(onMulti).toHaveBeenCalledWith({ x: 1 });
  });

  it('setAdRevenueListener 注册不抛（保留 API）', () => {
    const ad = new ATInterstitialAd('p1');
    expect(() => ad.setAdRevenueListener(jest.fn())).not.toThrow();
  });

  it('ensureRegistered 幂等：重复 setAdListener 只注册一次', () => {
    const spy = jest.spyOn(ATAdEvents, 'setAdListener');
    const ad = new ATInterstitialAd('p1');
    ad.setAdListener({});
    ad.setAdMultipleLoadedListener(jest.fn());
    expect(spy).toHaveBeenCalledTimes(1);
    spy.mockRestore();
  });

  it('removeAdListener 后事件不再分发', () => {
    const ad = new ATInterstitialAd('p1');
    const onLoaded = jest.fn();
    ad.setAdListener({ onInterstitialAdLoaded: onLoaded });
    ad.removeAdListener();
    emit({ callbackName: InterstitialCallback.loaded, placementID: 'p1' });
    expect(onLoaded).not.toHaveBeenCalled();
  });

  it('dispatchInterstitialEvent 覆盖剩余 callbackName 分支（含 default）', () => {
    const ad = new ATInterstitialAd('p1');
    const cb = {
      onInterstitialAdClicked: jest.fn(),
      onInterstitialAdVideoEnd: jest.fn(),
      onInterstitialAdVideoError: jest.fn(),
      onInterstitialAdShowFailed: jest.fn(),
      onDeeplinkCallback: jest.fn(),
    };
    ad.setAdListener(cb);
    const names = [
      InterstitialCallback.click,
      InterstitialCallback.playEnd,
      InterstitialCallback.playFail,
      InterstitialCallback.showFail,
      InterstitialCallback.deeplink,
      'someUnknownCallback', // default 分支
    ];
    for (const callbackName of names) {
      emit({ callbackName, placementID: 'p1' });
    }
    expect(cb.onInterstitialAdClicked).toHaveBeenCalledTimes(1);
    expect(cb.onInterstitialAdVideoEnd).toHaveBeenCalledTimes(1);
    expect(cb.onInterstitialAdVideoError).toHaveBeenCalledTimes(1);
    expect(cb.onInterstitialAdShowFailed).toHaveBeenCalledTimes(1);
    expect(cb.onDeeplinkCallback).toHaveBeenCalledTimes(1);
  });
});
