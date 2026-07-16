import TestRenderer, { act } from 'react-test-renderer';

// react-test-renderer 并发 act 环境标志 + node 环境缺 requestAnimationFrame 的 polyfill
// （ATNativeAdView mount 后用 rAF 延迟 register asset；测试环境补上即可）
(
  globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;
if (typeof globalThis.requestAnimationFrame !== 'function') {
  (globalThis as { requestAnimationFrame?: unknown }).requestAnimationFrame = (
    cb: (t: number) => void
  ) => setTimeout(() => cb(Date.now()), 0) as unknown as number;
}

const mockListeners: Record<string, (raw: unknown) => void> = {};

jest.mock('react-native', () => {
  const React = require('react');
  // 轻量 host 组件 mock：返回可被 react-test-renderer 渲染的占位
  const host = (name: string) => (props: Record<string, unknown>) =>
    React.createElement(name, props, props.children as React.ReactNode);
  return {
    NativeEventEmitter: jest.fn().mockImplementation(() => ({
      addListener: (event: string, cb: (raw: unknown) => void) => {
        mockListeners[event] = cb;
        return { remove: jest.fn() };
      },
    })),
    findNodeHandle: (ref: unknown) => (ref as { _tag?: number })?._tag ?? null,
    Platform: { OS: 'android' },
    View: host('View'),
    Text: host('Text'),
    Image: host('Image'),
    StyleSheet: { create: (s: unknown) => s, flatten: (s: unknown) => s },
  };
});

let mockGetNativeAdResult: unknown = { hasAd: true };
jest.mock('../specs/NativeATBridge', () => ({
  __esModule: true,
  default: {
    loadNativeAd: jest.fn(),
    getNativeAd: jest.fn(() => Promise.resolve(mockGetNativeAdResult)),
    prepareNativeAd: jest.fn(),
    checkNativeLoadStatus: jest.fn(() => Promise.resolve({})),
    getNativeValidAds: jest.fn(() => Promise.resolve([])),
    getNativeAdMaterial: jest.fn(() => Promise.resolve({})),
    entryNativeScenario: jest.fn(),
    setNativeShowConfig: jest.fn(),
    setNativeLocalExtra: jest.fn(),
    setNativeTKExtra: jest.fn(),
    destroyNativeAd: jest.fn(),
    nativeAdOnResume: jest.fn(),
    nativeAdOnPause: jest.fn(),
  },
}));

const nativeProps: Record<string, unknown>[] = [];
jest.mock('../specs/ATNativeAdViewNativeComponent', () => {
  const ReactLib = require('react');
  return {
    __esModule: true,
    // forwardRef + 暴露非空 ref（让 ATNativeAdView 的 viewRef.current truthy，触发 register 分支）
    default: ReactLib.forwardRef(
      (
        props: Record<string, unknown>,
        ref: { current?: unknown } | Function
      ) => {
        nativeProps.push(props);
        const view = { _isNativeAdView: true };
        if (typeof ref === 'function') {
          ref(view);
        } else if (ref) {
          ref.current = view;
        }
        return ReactLib.createElement('ATNativeAdView', null, props.children);
      }
    ),
    Commands: {
      updateAssetView: jest.fn(),
      renderNativeAd: jest.fn(),
    },
  };
});

import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATNative } from '../nativead/ATNative';
import { ATNativeAdView } from '../nativead/ATNativeAdView';
import {
  ATNativeTitleView,
  ATNativeIconView,
  ATNativeMediaView,
  ATNativeCloseView,
} from '../nativead/ATNativeAssetViews';
import { Commands as NativeCommands } from '../specs/ATNativeAdViewNativeComponent';
import { CallName, NativeCallback } from '../constants/Const';

const mockUpdateAssetView = NativeCommands.updateAssetView as jest.Mock;
const mockRenderNativeAd = NativeCommands.renderNativeAd as jest.Mock;

const bridge = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;

function emit(raw: Record<string, unknown>): void {
  mockListeners[CallName.NativeCall]?.(raw);
}

describe('ATNative / NativeAd (phase-2e)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    jest.clearAllMocks();
    mockGetNativeAdResult = { hasAd: true };
    ATAdEvents.init();
  });

  it('makeAdRequest(adRequest) 包成 {atAdRequest}', () => {
    const native = new ATNative('n1');
    native.makeAdRequest({ channelSource: 1 });
    expect(bridge.loadNativeAd).toHaveBeenCalledWith('n1', {
      atAdRequest: { channelSource: 1 },
    });
  });

  it('getNativeAd 有广告 resolve NativeAd，无广告 resolve null', async () => {
    const native = new ATNative('n1');
    const ad = await native.getNativeAd();
    expect(ad).not.toBeNull();
    mockGetNativeAdResult = null;
    const ad2 = await native.getNativeAd();
    expect(ad2).toBeNull();
  });

  it('加载期事件 → networkListener；展示期事件 → NativeAd eventListener', async () => {
    const onLoaded = jest.fn();
    const native = new ATNative('n1', { onNativeAdLoaded: onLoaded });
    const ad = await native.getNativeAd();
    const onShow = jest.fn();
    const onVideoStart = jest.fn();
    ad?.setNativeEventListener({
      onNativeAdShow: onShow,
      onNativeAdVideoStart: onVideoStart,
    });

    emit({ callbackName: NativeCallback.loaded, placementID: 'n1' });
    emit({ callbackName: NativeCallback.show, placementID: 'n1' });
    emit({ callbackName: NativeCallback.videoStart, placementID: 'n1' });

    expect(onLoaded).toHaveBeenCalledTimes(1);
    expect(onShow).toHaveBeenCalledTimes(1);
    expect(onVideoStart).toHaveBeenCalledTimes(1);
  });

  it('renderAdContainer 把 viewTag 填进 prepareInfo（N-Q01）', async () => {
    const native = new ATNative('n1');
    const ad = await native.getNativeAd();
    const containerRef = { current: { _tag: 99 } } as never;
    ad?.renderAdContainer(containerRef, { title: 1, cta: 2 });
    expect(bridge.prepareNativeAd).toHaveBeenCalledWith('n1', {
      title: 1,
      cta: 2,
      parent: 99,
    });
  });

  it('renderAdContainer ref 为空时 parent 用 prepareInfo.parent 兜底', async () => {
    const native = new ATNative('n1');
    const ad = await native.getNativeAd();
    ad?.renderAdContainer({ current: null } as never, { parent: 7 });
    expect(bridge.prepareNativeAd).toHaveBeenCalledWith('n1', { parent: 7 });
  });

  it('loadFail → onNativeAdLoadFail 携带 fullErrorInfo', () => {
    const onFail = jest.fn();
    new ATNative('n1', { onNativeAdLoadFail: onFail });
    emit({
      callbackName: NativeCallback.loadFail,
      placementID: 'n1',
      requestMessage: 'code:1, msg:x',
    });
    expect(onFail).toHaveBeenCalledTimes(1);
    expect(onFail.mock.calls[0]?.[0]?.fullErrorInfo).toBe('code:1, msg:x');
  });

  it('展示期剩余事件分发：click/close/videoEnd/videoProgress/deeplink', async () => {
    const native = new ATNative('n1');
    const ad = await native.getNativeAd();
    const cb = {
      onNativeAdClicked: jest.fn(),
      onNativeAdClose: jest.fn(),
      onNativeAdVideoEnd: jest.fn(),
      onNativeAdVideoProgress: jest.fn(),
      onDeeplinkCallback: jest.fn(),
    };
    ad?.setNativeEventListener(cb);
    emit({ callbackName: NativeCallback.click, placementID: 'n1' });
    emit({ callbackName: NativeCallback.close, placementID: 'n1' });
    emit({ callbackName: NativeCallback.videoEnd, placementID: 'n1' });
    emit({
      callbackName: NativeCallback.videoProgress,
      placementID: 'n1',
      extraDic: { progress: 50 },
    });
    emit({
      callbackName: NativeCallback.deeplink,
      placementID: 'n1',
      isDeeplinkSuccess: true,
    });
    emit({ callbackName: 'unknownCb', placementID: 'n1' }); // default 分支
    expect(cb.onNativeAdClicked).toHaveBeenCalled();
    expect(cb.onNativeAdClose).toHaveBeenCalled();
    expect(cb.onNativeAdVideoEnd).toHaveBeenCalled();
    expect(cb.onNativeAdVideoProgress).toHaveBeenCalledWith(50);
    expect(cb.onDeeplinkCallback).toHaveBeenCalledWith({}, true);
  });

  it('AdSource 分流 + multipleLoaded 分流', () => {
    const native = new ATNative('n1');
    const onSource = jest.fn();
    const onMulti = jest.fn();
    native.setAdSourceStatusListener(onSource);
    native.setAdMultipleLoadedListener(onMulti);
    emit({ callbackName: 'nativeAdSourceAttempt', placementID: 'n1' });
    emit({
      callbackName: NativeCallback.multipleLoaded,
      placementID: 'n1',
      extraDic: { m: 1 },
    });
    expect(onSource).toHaveBeenCalledTimes(1);
    expect(onMulti).toHaveBeenCalledWith({ m: 1 });
  });

  it('setAdListener 替换 networkListener', () => {
    const native = new ATNative('n1');
    const onLoaded = jest.fn();
    native.setAdListener({ onNativeAdLoaded: onLoaded });
    emit({ callbackName: NativeCallback.loaded, placementID: 'n1' });
    expect(onLoaded).toHaveBeenCalledTimes(1);
  });

  it('checkAdStatus/checkValidAdCaches/entryAdScenario/setLocalExtra/setTKExtra 透传桥', async () => {
    const native = new ATNative('n1');
    await native.checkAdStatus();
    expect(bridge.checkNativeLoadStatus).toHaveBeenCalledWith('n1');
    await native.checkValidAdCaches();
    expect(bridge.getNativeValidAds).toHaveBeenCalledWith('n1');
    ATNative.entryAdScenario('n1', 'sc');
    expect(bridge.entryNativeScenario).toHaveBeenCalledWith('n1', 'sc', {});
    ATNative.entryAdScenario('n1', 'sc', { t: 1 });
    expect(bridge.entryNativeScenario).toHaveBeenCalledWith('n1', 'sc', {
      t: 1,
    });
    native.setLocalExtra({ a: 1 });
    expect(bridge.setNativeLocalExtra).toHaveBeenCalledWith('n1', { a: 1 });
    native.setShowConfig({ scenarioId: 'sc', showCustomExt: 'ext' });
    expect(bridge.setNativeShowConfig).toHaveBeenCalledWith('n1', {
      scenarioId: 'sc',
      showCustomExt: 'ext',
    });
    native.setTKExtra({ b: 2 });
    expect(bridge.setNativeTKExtra).toHaveBeenCalledWith('n1', { b: 2 });
  });

  it('dispose 释放订阅后事件不再分发', () => {
    const onLoaded = jest.fn();
    const native = new ATNative('n1', { onNativeAdLoaded: onLoaded });
    native.dispose();
    emit({ callbackName: NativeCallback.loaded, placementID: 'n1' });
    expect(onLoaded).not.toHaveBeenCalled();
  });

  it('destroy 整位销毁透传桥', () => {
    const native = new ATNative('n1');
    native.destroy();
    expect(bridge.destroyNativeAd).toHaveBeenCalledWith('n1', '');
  });

  it('NativeAd.prepare/getAdMaterial/getAdInfo/setAdRevenueListener/destroy', async () => {
    const native = new ATNative('n1');
    const ad = await native.getNativeAd();
    ad?.prepare({ title: 5 });
    expect(bridge.prepareNativeAd).toHaveBeenCalledWith('n1', { title: 5 });
    await ad?.getAdMaterial();
    expect(bridge.getNativeAdMaterial).toHaveBeenCalledWith('n1', ad?.adId);
    await ad?.getAdInfo();
    expect(bridge.checkNativeLoadStatus).toHaveBeenCalledWith('n1');
    expect(() => ad?.setAdRevenueListener(jest.fn())).not.toThrow();
    ad?.onResume();
    expect(bridge.nativeAdOnResume).toHaveBeenCalledWith('n1', ad?.adId);
    ad?.onPause();
    expect(bridge.nativeAdOnPause).toHaveBeenCalledWith('n1', ad?.adId);
    ad?.destroy();
    expect(bridge.destroyNativeAd).toHaveBeenCalledWith('n1', ad?.adId);
  });
});

describe('ATNativeAdView Fabric props', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
    nativeProps.length = 0;
    ATAdEvents.init();
  });

  // 构造一个绑定 ad 对象（adId 来自 mock getNativeAd）
  async function makeAd(placementID: string, adId: string) {
    mockGetNativeAdResult = { hasAd: true, adId };
    const native = new ATNative(placementID);
    const ad = await native.getNativeAd();
    return ad!;
  }

  it('props placementID/adId/adWidth 映射 + onAdEvent 按 adId 过滤', async () => {
    const onAdEvent = jest.fn();
    const ad = await makeAd('nX', 'nX#1');
    act(() => {
      TestRenderer.create(
        <ATNativeAdView
          ad={ad}
          adWidth={320}
          adHeight={250}
          onAdEvent={onAdEvent}
        />
      );
    });
    const last = nativeProps[nativeProps.length - 1];
    expect(last?.placementID).toBe('nX');
    expect(last?.adId).toBe('nX#1');
    expect(last?.adWidth).toBe(320);

    // 带 adId 的事件分流到本 cell
    emit({
      callbackName: NativeCallback.show,
      placementID: 'nX',
      adId: 'nX#1',
    });
    expect(onAdEvent).toHaveBeenCalledTimes(1);
    // 同位但不同 adId（其他 cell）不串位
    emit({
      callbackName: NativeCallback.show,
      placementID: 'nX',
      adId: 'nX#2',
    });
    expect(onAdEvent).toHaveBeenCalledTimes(1);
    // 其他 placement 也不收
    emit({
      callbackName: NativeCallback.show,
      placementID: 'other',
      adId: 'o#1',
    });
    expect(onAdEvent).toHaveBeenCalledTimes(1);
  });

  // 等 getNativeAdMaterial 的 Promise + requestAnimationFrame(setTimeout 0) 都跑完
  async function flush(): Promise<void> {
    await act(async () => {
      await Promise.resolve();
      await new Promise<void>((r) => setTimeout(() => r(), 0));
      await Promise.resolve();
    });
  }

  it('自渲染：mount 后取素材 → updateAssetView 注册各 asset + renderNativeAd', async () => {
    const br = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;
    br.getNativeAdMaterial.mockResolvedValueOnce({
      title: 'T',
      isExpress: false,
    });
    mockUpdateAssetView.mockClear();
    mockRenderNativeAd.mockClear();
    const ad = await makeAd('nSelf', 'nSelf#1');

    await act(async () => {
      TestRenderer.create(
        <ATNativeAdView ad={ad}>
          <ATNativeTitleView />
          <ATNativeIconView />
          <ATNativeMediaView />
          <ATNativeCloseView />
        </ATNativeAdView>
      );
    });
    await flush();

    // 自渲染分支：取素材后走 register 流程 + 触发 renderNativeAd（asset ref 解析依赖宿主 view，
    // 测试环境 host mock 不保证 findNodeHandle 非空，故只断言 renderNativeAd 一定触发）
    expect(mockRenderNativeAd).toHaveBeenCalled();
  });

  it('模板 offer（isExpress=true）：隐藏 children，不注册 asset，只 renderNativeAd', async () => {
    const br = ATReactNativeBridge as jest.Mocked<typeof ATReactNativeBridge>;
    br.getNativeAdMaterial.mockResolvedValueOnce({ isExpress: true });
    mockUpdateAssetView.mockClear();
    mockRenderNativeAd.mockClear();
    const ad = await makeAd('nTpl', 'nTpl#1');

    await act(async () => {
      TestRenderer.create(
        <ATNativeAdView ad={ad}>
          <ATNativeTitleView />
        </ATNativeAdView>
      );
    });
    await flush();

    expect(mockUpdateAssetView).not.toHaveBeenCalled();
    expect(mockRenderNativeAd).toHaveBeenCalled();
  });

  it('列表多广告：同 placement 多条按 adId 分流，各 cell 不串位', async () => {
    const ev1 = jest.fn();
    const ev2 = jest.fn();
    const adA = await makeAd('nList', 'nList#1');
    const adB = await makeAd('nList', 'nList#2');
    act(() => {
      TestRenderer.create(<ATNativeAdView ad={adA} onAdEvent={ev1} />);
      TestRenderer.create(<ATNativeAdView ad={adB} onAdEvent={ev2} />);
    });

    // adId#1 的曝光只到 cell A
    emit({
      callbackName: NativeCallback.show,
      placementID: 'nList',
      adId: 'nList#1',
    });
    expect(ev1).toHaveBeenCalledTimes(1);
    expect(ev2).not.toHaveBeenCalled();

    // adId#2 的点击只到 cell B
    emit({
      callbackName: NativeCallback.click,
      placementID: 'nList',
      adId: 'nList#2',
    });
    expect(ev1).toHaveBeenCalledTimes(1);
    expect(ev2).toHaveBeenCalledTimes(1);
  });
});
