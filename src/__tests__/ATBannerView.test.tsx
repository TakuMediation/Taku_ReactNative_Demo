import TestRenderer, { act } from 'react-test-renderer';

(
  globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;

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
    loadBannerAd: jest.fn(),
    checkBannerLoadStatus: jest.fn(() => Promise.resolve({})),
    getBannerValidAds: jest.fn(() => Promise.resolve([])),
    entryBannerScenario: jest.fn(),
    setBannerShowConfig: jest.fn(),
    setBannerLocalExtra: jest.fn(),
    setBannerTKExtra: jest.fn(),
    destroyBanner: jest.fn(),
  },
}));

// 记录原生组件收到的 props
const nativeProps: Record<string, unknown>[] = [];
jest.mock('../specs/ATBannerViewNativeComponent', () => ({
  __esModule: true,
  default: (props: Record<string, unknown>) => {
    nativeProps.push(props);
    return null;
  },
}));

import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATBannerView } from '../banner/ATBannerView';
import { ATBannerViewComponent } from '../banner/ATBannerViewComponent';
import { CallName, BannerCallback } from '../constants/Const';

const bridge = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;

function emit(raw: Record<string, unknown>): void {
  mockListeners[CallName.BannerCall]?.(raw);
}

describe('ATBannerView 命令式 (phase-2d)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    jest.clearAllMocks();
    ATAdEvents.init();
  });

  it('loadAd(adRequest) 包成 {atAdRequest} 透传', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    ad.loadAd({ channelSource: 1 });
    expect(bridge.loadBannerAd).toHaveBeenCalledWith('b1', {
      size: { x: 0, y: 0, width: 320, height: 50 },
      atAdRequest: { channelSource: 1 },
    });
  });

  it('事件分发：loaded/show/close/autoRefresh → 对应回调', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const cb = {
      onBannerAdLoaded: jest.fn(),
      onBannerAdShow: jest.fn(),
      onBannerAdClose: jest.fn(),
      onBannerAdAutoRefreshed: jest.fn(),
    };
    ad.setBannerAdListener(cb);

    emit({ callbackName: BannerCallback.loaded, placementID: 'b1' });
    emit({ callbackName: BannerCallback.show, placementID: 'b1' });
    emit({ callbackName: BannerCallback.close, placementID: 'b1' });
    emit({ callbackName: BannerCallback.refresh, placementID: 'b1' });

    expect(cb.onBannerAdLoaded).toHaveBeenCalledTimes(1);
    expect(cb.onBannerAdShow).toHaveBeenCalledTimes(1);
    expect(cb.onBannerAdClose).toHaveBeenCalledTimes(1);
    expect(cb.onBannerAdAutoRefreshed).toHaveBeenCalledTimes(1);
  });

  it('多 placement 不串', () => {
    const ad1 = new ATBannerView();
    ad1.setPlacementId('b1');
    const onLoaded1 = jest.fn();
    ad1.setBannerAdListener({ onBannerAdLoaded: onLoaded1 });
    emit({ callbackName: BannerCallback.loaded, placementID: 'b2' });
    expect(onLoaded1).not.toHaveBeenCalled();
  });

  it('loadAd() 无 adRequest 时 extraDic 为 {}', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    ad.loadAd();
    expect(bridge.loadBannerAd).toHaveBeenCalledWith('b1', {
      size: { x: 0, y: 0, width: 320, height: 50 },
    });
  });

  it('setPlacementId 重复设置时移除旧 placement 监听并注册新的', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const onLoaded = jest.fn();
    ad.setBannerAdListener({ onBannerAdLoaded: onLoaded });
    // 改 placement，旧 b1 监听应被移除
    ad.setPlacementId('b2');
    emit({ callbackName: BannerCallback.loaded, placementID: 'b1' });
    expect(onLoaded).not.toHaveBeenCalled();
    // 新 placement 生效
    emit({ callbackName: BannerCallback.loaded, placementID: 'b2' });
    expect(onLoaded).toHaveBeenCalledTimes(1);
  });

  it('事件分发：click/loadFail/refreshFail/deeplink → 对应回调', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const cb = {
      onBannerAdClicked: jest.fn(),
      onBannerAdLoadFail: jest.fn(),
      onBannerAdAutoRefreshFail: jest.fn(),
      onDeeplinkCallback: jest.fn(),
    };
    ad.setBannerAdListener(cb);

    emit({ callbackName: BannerCallback.click, placementID: 'b1' });
    emit({
      callbackName: BannerCallback.loadFail,
      placementID: 'b1',
      requestMessage: 'load err',
    });
    emit({
      callbackName: BannerCallback.refreshFail,
      placementID: 'b1',
      requestMessage: 'refresh err',
    });
    emit({
      callbackName: BannerCallback.deeplink,
      placementID: 'b1',
      isDeeplinkSuccess: true,
    });

    expect(cb.onBannerAdClicked).toHaveBeenCalledTimes(1);
    expect(cb.onBannerAdLoadFail).toHaveBeenCalledWith({
      code: '',
      desc: 'load err',
      fullErrorInfo: 'load err',
    });
    expect(cb.onBannerAdAutoRefreshFail).toHaveBeenCalledWith({
      code: '',
      desc: 'refresh err',
      fullErrorInfo: 'refresh err',
    });
    expect(cb.onDeeplinkCallback).toHaveBeenCalledWith({}, true);
  });

  it('loadFail 无 requestMessage 时走 asError 兜底（空 AdError）', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const onBannerAdLoadFail = jest.fn();
    ad.setBannerAdListener({ onBannerAdLoadFail });
    emit({ callbackName: BannerCallback.loadFail, placementID: 'b1' });
    expect(onBannerAdLoadFail).toHaveBeenCalledWith({
      code: '',
      desc: '',
      fullErrorInfo: '',
    });
  });

  it('deeplink 无 isDeeplinkSuccess 时默认 false', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const onDeeplinkCallback = jest.fn();
    ad.setBannerAdListener({ onDeeplinkCallback });
    emit({ callbackName: BannerCallback.deeplink, placementID: 'b1' });
    expect(onDeeplinkCallback).toHaveBeenCalledWith({}, false);
  });

  it('未知 callbackName 走 default 分支不抛错', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const onBannerAdLoaded = jest.fn();
    ad.setBannerAdListener({ onBannerAdLoaded });
    expect(() =>
      emit({ callbackName: 'bannerUnknownEvent', placementID: 'b1' })
    ).not.toThrow();
    expect(onBannerAdLoaded).not.toHaveBeenCalled();
  });

  it('route：含 AdSource 的 callbackName 走 adSourceListener', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const adSourceListener = jest.fn();
    const onBannerAdLoaded = jest.fn();
    ad.setAdSourceStatusListener(adSourceListener);
    ad.setBannerAdListener({ onBannerAdLoaded });
    emit({
      callbackName: BannerCallback.adSourceAttempt,
      placementID: 'b1',
    });
    expect(adSourceListener).toHaveBeenCalledTimes(1);
    expect(adSourceListener.mock.calls[0][0].callbackName).toBe(
      BannerCallback.adSourceAttempt
    );
    // 不应同时进 mainListener 分发
    expect(onBannerAdLoaded).not.toHaveBeenCalled();
  });

  it('route：multipleLoaded 走 multipleLoadedListener，回传 extraDic', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const multipleLoadedListener = jest.fn();
    ad.setAdMultipleLoadedListener(multipleLoadedListener);
    emit({
      callbackName: BannerCallback.multipleLoaded,
      placementID: 'b1',
      extraDic: { foo: 'bar' },
    });
    expect(multipleLoadedListener).toHaveBeenCalledWith({ foo: 'bar' });
  });

  it('route：multipleLoaded 无 extraDic 时回传 {}', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const multipleLoadedListener = jest.fn();
    ad.setAdMultipleLoadedListener(multipleLoadedListener);
    emit({ callbackName: BannerCallback.multipleLoaded, placementID: 'b1' });
    expect(multipleLoadedListener).toHaveBeenCalledWith({});
  });

  it('route：无 mainListener 时主事件不抛错', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    expect(() =>
      emit({ callbackName: BannerCallback.loaded, placementID: 'b1' })
    ).not.toThrow();
  });

  it('setShowConfig 透传 setBannerShowConfig', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const config = { useRewardedVideoAsRewardAd: true } as never;
    ad.setShowConfig(config);
    expect(bridge.setBannerShowConfig).toHaveBeenCalledWith('b1', config);
  });

  it('setLocalExtra 透传 setBannerLocalExtra', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    ad.setLocalExtra({ k: 'v' });
    expect(bridge.setBannerLocalExtra).toHaveBeenCalledWith('b1', { k: 'v' });
  });

  it('setTKExtra 透传 setBannerTKExtra', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    ad.setTKExtra({ k: 'v' });
    expect(bridge.setBannerTKExtra).toHaveBeenCalledWith('b1', { k: 'v' });
  });

  it('checkAdStatus 透传 checkBannerLoadStatus 并 resolve', async () => {
    bridge.checkBannerLoadStatus.mockResolvedValueOnce({
      isLoading: false,
    } as never);
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const status = await ad.checkAdStatus();
    expect(bridge.checkBannerLoadStatus).toHaveBeenCalledWith('b1');
    expect(status).toEqual({ isLoading: false });
  });

  it('checkValidAdCaches 透传 getBannerValidAds 并 resolve', async () => {
    bridge.getBannerValidAds.mockResolvedValueOnce([{ a: 1 }] as never);
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const ads = await ad.checkValidAdCaches();
    expect(bridge.getBannerValidAds).toHaveBeenCalledWith('b1');
    expect(ads).toEqual([{ a: 1 }]);
  });

  it('entryAdScenario(static) 透传 entryBannerScenario，tkExtra 默认 {}', () => {
    ATBannerView.entryAdScenario('b1', 's1');
    expect(bridge.entryBannerScenario).toHaveBeenCalledWith('b1', 's1', {});
    ATBannerView.entryAdScenario('b1', 's2', { t: 1 });
    expect(bridge.entryBannerScenario).toHaveBeenCalledWith('b1', 's2', {
      t: 1,
    });
  });

  it('setAdRevenueListener 可调用（no-op）不抛错', () => {
    const ad = new ATBannerView();
    expect(() => ad.setAdRevenueListener(jest.fn())).not.toThrow();
  });

  it('destroy 透传 destroyBanner 并移除监听', () => {
    const ad = new ATBannerView();
    ad.setPlacementId('b1');
    const onBannerAdLoaded = jest.fn();
    ad.setBannerAdListener({ onBannerAdLoaded });
    ad.destroy();
    expect(bridge.destroyBanner).toHaveBeenCalledWith('b1');
    // 销毁后事件不再分发
    emit({ callbackName: BannerCallback.loaded, placementID: 'b1' });
    expect(onBannerAdLoaded).not.toHaveBeenCalled();
  });

  it('destroy 在未设置 placementId 时也不抛错', () => {
    const ad = new ATBannerView();
    expect(() => ad.destroy()).not.toThrow();
    expect(bridge.destroyBanner).toHaveBeenCalledWith('');
  });
});

describe('ATBannerViewComponent Fabric props (B-Q01)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    nativeProps.length = 0;
    ATAdEvents.init();
  });

  it('props placementID/width/height 映射到原生组件', () => {
    act(() => {
      TestRenderer.create(
        <ATBannerViewComponent placementID="pX" width={320} height={50} />
      );
    });
    const last = nativeProps[nativeProps.length - 1];
    expect(last?.placementID).toBe('pX');
    expect(last?.adWidth).toBe(320);
    expect(last?.adHeight).toBe(50);
  });

  it('onAdEvent 按本视图 placementID 过滤', () => {
    const onAdEvent = jest.fn();
    act(() => {
      TestRenderer.create(
        <ATBannerViewComponent
          placementID="pX"
          width={320}
          height={50}
          onAdEvent={onAdEvent}
        />
      );
    });
    emit({ callbackName: BannerCallback.loaded, placementID: 'pX' });
    expect(onAdEvent).toHaveBeenCalledTimes(1);
    emit({ callbackName: BannerCallback.loaded, placementID: 'other' });
    expect(onAdEvent).toHaveBeenCalledTimes(1); // 不收 other
  });

  it('isAdaptiveHeight 时 style.height 为 undefined（自适应高度）', () => {
    act(() => {
      TestRenderer.create(
        <ATBannerViewComponent
          placementID="pAdapt"
          width={320}
          height={50}
          isAdaptiveHeight
        />
      );
    });
    const last = nativeProps[nativeProps.length - 1];
    expect(last?.isAdaptiveHeight).toBe(true);
    const style = last?.style as Array<Record<string, unknown>>;
    expect(style[0]).toEqual({ width: 320, height: undefined });
  });

  it('卸载时移除 placement 监听', () => {
    const onAdEvent = jest.fn();
    let renderer: TestRenderer.ReactTestRenderer;
    act(() => {
      renderer = TestRenderer.create(
        <ATBannerViewComponent
          placementID="pUnmount"
          width={320}
          height={50}
          onAdEvent={onAdEvent}
        />
      );
    });
    act(() => {
      renderer.unmount();
    });
    emit({ callbackName: BannerCallback.loaded, placementID: 'pUnmount' });
    expect(onAdEvent).not.toHaveBeenCalled();
  });
});
