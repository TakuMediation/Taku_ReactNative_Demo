// 捕获 NativeEventEmitter 注册的监听器，供测试手动触发事件
// 变量名以 mock 开头，才能在 jest.mock 工厂内引用
const mockListeners: Record<string, (raw: unknown) => void> = {};

jest.mock('react-native', () => ({
  NativeEventEmitter: jest.fn().mockImplementation(() => ({
    addListener: (event: string, cb: (raw: unknown) => void) => {
      mockListeners[event] = cb;
      return { remove: jest.fn() };
    },
  })),
}));
jest.mock('../specs/NativeATBridge', () => ({ __esModule: true, default: {} }));

import { ATAdEvents } from '../events/ATAdEvents';
import { CallName, InitCallback, CommonCallback } from '../constants/Const';
import type { ATAdEventPayload, ATAdInfo } from '../types';

describe('ATAdEvents (P1-Q02/Q04)', () => {
  beforeEach(() => {
    ATAdEvents.dispose();
    for (const k of Object.keys(mockListeners)) {
      delete mockListeners[k];
    }
  });

  it('init 订阅全部 8 个 callName', () => {
    ATAdEvents.init();
    expect(Object.keys(mockListeners).sort()).toEqual(
      Object.values(CallName).sort()
    );
  });

  it('init 幂等：重复调用不重复订阅', () => {
    ATAdEvents.init();
    ATAdEvents.init();
    expect(Object.keys(mockListeners)).toHaveLength(
      Object.values(CallName).length
    );
  });

  it('Init 事件分发到全局监听，callbackName/placementID 正确', () => {
    ATAdEvents.init();
    const received: ATAdEventPayload[] = [];
    ATAdEvents.addGlobalListener((p) => received.push(p));

    mockListeners[CallName.InitCallName]?.({
      callbackName: InitCallback.sdkInitSuccess,
      placementID: '',
    });

    expect(received).toHaveLength(1);
    expect(received[0]?.callbackName).toBe('sdkInitSuccess');
    expect(received[0]?.placementID).toBe('');
  });

  it('按 placementID 分发，只命中对应 listener', () => {
    ATAdEvents.init();
    const hits: ATAdEventPayload[] = [];
    ATAdEvents.setAdListener('placement-1', (p) => hits.push(p));

    mockListeners[CallName.RewardedVideoCall]?.({
      callbackName: 'a',
      placementID: 'placement-1',
    });
    mockListeners[CallName.RewardedVideoCall]?.({
      callbackName: 'b',
      placementID: 'placement-2',
    });

    expect(hits).toHaveLength(1);
    expect(hits[0]?.callbackName).toBe('a');
  });

  it('requestMessage 透传（失败语义）', () => {
    ATAdEvents.init();
    const got: ATAdEventPayload[] = [];
    ATAdEvents.addGlobalListener((p) => got.push(p));

    mockListeners[CallName.RewardedVideoCall]?.({
      callbackName: 'fail',
      placementID: 'p',
      requestMessage: 'no fill',
    });

    expect(got[0]?.requestMessage).toBe('no fill');
  });

  it('removeAdListener 后不再收到该 placement 的回调', () => {
    ATAdEvents.init();
    const got: ATAdEventPayload[] = [];
    ATAdEvents.setAdListener('p', (p) => got.push(p));
    ATAdEvents.removeAdListener('p');

    mockListeners[CallName.BannerCall]?.({
      callbackName: 'x',
      placementID: 'p',
    });

    expect(got).toHaveLength(0);
  });

  it('removeAdListener 不删除 revenue 监听', () => {
    ATAdEvents.init();
    const revenueGot: ATAdInfo[] = [];
    ATAdEvents.setRevenueListener('p', (info) => revenueGot.push(info));
    ATAdEvents.removeAdListener('p');

    mockListeners[CallName.CommonADCall]?.({
      callbackName: CommonCallback.adShowRevenue,
      placementID: 'p',
      extraDic: { ecpm: 1 },
    });

    expect(revenueGot).toHaveLength(1);
  });

  it('addGlobalListener 收所有事件；removeGlobalListener 后不再收', () => {
    ATAdEvents.init();
    const got: ATAdEventPayload[] = [];
    const gl = (p: ATAdEventPayload) => got.push(p);
    ATAdEvents.addGlobalListener(gl);

    mockListeners[CallName.BannerCall]?.({
      callbackName: 'a',
      placementID: 'p1',
    });
    mockListeners[CallName.RewardedVideoCall]?.({
      callbackName: 'b',
      placementID: 'p2',
    });
    expect(got).toHaveLength(2);

    ATAdEvents.removeGlobalListener(gl);
    mockListeners[CallName.BannerCall]?.({
      callbackName: 'c',
      placementID: 'p1',
    });
    expect(got).toHaveLength(2);
  });

  it('有 adId 且命中 instance 时，placement 不再收到', () => {
    ATAdEvents.init();
    const placementGot: ATAdEventPayload[] = [];
    const instanceGot: ATAdEventPayload[] = [];
    ATAdEvents.setAdListener('nList', (p) => placementGot.push(p));
    ATAdEvents.setAdInstanceListener('nList', 'nList#1', (p) =>
      instanceGot.push(p)
    );

    mockListeners[CallName.NativeCall]?.({
      callbackName: 'nativeAdDidShowNativeAd',
      placementID: 'nList',
      adId: 'nList#1',
    });

    expect(instanceGot).toHaveLength(1);
    expect(placementGot).toHaveLength(0);
  });

  it('有 adId 但未注册 instance 时，placement 仍收到（setNativeEventListener 回退）', () => {
    ATAdEvents.init();
    const placementGot: ATAdEventPayload[] = [];
    ATAdEvents.setAdListener('nSingle', (p) => placementGot.push(p));

    mockListeners[CallName.NativeCall]?.({
      callbackName: 'nativeAdDidShowNativeAd',
      placementID: 'nSingle',
      adId: 'nSingle#1',
    });

    expect(placementGot).toHaveLength(1);
    expect(placementGot[0]?.adId).toBe('nSingle#1');
  });

  it('无 adId 时 placement 正常收到 load 事件', () => {
    ATAdEvents.init();
    const placementGot: ATAdEventPayload[] = [];
    const instanceGot: ATAdEventPayload[] = [];
    ATAdEvents.setAdListener('nLoad', (p) => placementGot.push(p));
    ATAdEvents.setAdInstanceListener('nLoad', 'nLoad#1', (p) =>
      instanceGot.push(p)
    );

    mockListeners[CallName.NativeCall]?.({
      callbackName: 'nativeAdDidFinishLoading',
      placementID: 'nLoad',
    });

    expect(placementGot).toHaveLength(1);
    expect(placementGot[0]?.callbackName).toBe('nativeAdDidFinishLoading');
    expect(instanceGot).toHaveLength(0);
  });
});
