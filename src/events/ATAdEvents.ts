import { NativeEventEmitter, type EventSubscription } from 'react-native';
import ATReactNativeBridge from '../specs/NativeATBridge';
import { CallName, CommonCallback } from '../constants/Const';
import { ATLog } from '../utils/ATLog';
import type { ATAdEventPayload, ATAdListener, ATAdInfo } from '../types';

/**
 * 广告事件分发器（单例）。
 *
 * 订阅 8 个 callName（唯一通道，禁止第二套 EventBus），按 `payload.placementID`
 * 分发到各广告类的 `setAdListener`；全局监听器接收所有事件（供 Example EventLogPanel / Init）。
 * 上行回调由原生 `ATReactNativeEventEmitter` 在主线程 emit。
 */
export class ATAdEvents {
  private static emitter: NativeEventEmitter | null = null;
  private static subscriptions: EventSubscription[] = [];
  private static placementListeners = new Map<string, ATAdListener>();
  /** Revenue 监听（CommonADCall / adShowRevenueCallbackKey），按 placementID 分流。 */
  private static revenueListeners = new Map<string, (info: ATAdInfo) => void>();
  /** native 列表多广告：按 placementID#adId 注册的视图级监听（show/click 等带 adId 的事件分流到对应 cell）。 */
  private static adListeners = new Map<string, ATAdListener>();
  private static globalListeners: ATAdListener[] = [];

  private static adKey(placementID: string, adId: string): string {
    return `${placementID}#${adId}`;
  }

  /** 注册全局事件监听（订阅 8 callName）。InitManager 启动顺序中早于 init 调用。 */
  static init(): void {
    if (ATAdEvents.emitter) {
      return;
    }
    ATAdEvents.emitter = new NativeEventEmitter(ATReactNativeBridge as never);
    for (const callName of Object.values(CallName)) {
      ATAdEvents.subscriptions.push(
        ATAdEvents.emitter.addListener(callName, (raw: unknown) =>
          ATAdEvents.dispatch(raw as Record<string, unknown>)
        )
      );
    }
  }

  private static dispatch(raw: Record<string, unknown>): void {
    const adId = raw.adId != null ? String(raw.adId) : undefined;
    const payload: ATAdEventPayload = {
      callbackName: String(raw.callbackName ?? ''),
      placementID: String(raw.placementID ?? ''),
      adId,
      extraDic: (raw.extraDic as ATAdEventPayload['extraDic']) ?? undefined,
      requestMessage:
        raw.requestMessage != null ? String(raw.requestMessage) : undefined,
      isDeeplinkSuccess: raw.isDeeplinkSuccess as boolean | undefined,
      isTimeout: raw.isTimeout as boolean | undefined,
    };

    // 回调联调日志（唯一中央分发点，覆盖所有广告类型的所有回调；受开关控制）
    ATLog.callback(`AdEvents:${payload.placementID}`, payload.callbackName, {
      extraDic: payload.extraDic,
      requestMessage: payload.requestMessage,
    });

    // Revenue（CommonADCall，全 placement 共享通道）
    if (payload.callbackName === CommonCallback.adShowRevenue) {
      const revenueListener = ATAdEvents.revenueListeners.get(payload.placementID);
      if (revenueListener) {
        revenueListener(payload.extraDic ?? {});
      }
    }

    // 全局监听（LogPanel / Init）
    for (const l of ATAdEvents.globalListeners) {
      l(payload);
    }
    // native 列表多广告：带 adId 的视图级事件按 placementID#adId 分流到对应 cell。
    // 命中 instance 监听后不再走 placement，避免 ATNative.route 串位。
    let handledByInstance = false;
    if (adId) {
      const adListener = ATAdEvents.adListeners.get(
        ATAdEvents.adKey(payload.placementID, adId)
      );
      if (adListener) {
        adListener(payload);
        handledByInstance = true;
      }
    }
    // load 类无 adId；或有 adId 但未注册 instance 监听 → 回退 placement
    if (!handledByInstance) {
      const listener = ATAdEvents.placementListeners.get(payload.placementID);
      listener?.(payload);
    }
  }

  /** 按 placementID 注册广告监听（各广告类 setAdListener 用；load 类事件走这里）。 */
  static setAdListener(placementID: string, listener: ATAdListener): void {
    if (ATAdEvents.placementListeners.has(placementID)) {
      ATLog.call('ATAdEvents', 'setAdListener', placementID, 'listener overwritten');
    }
    ATAdEvents.placementListeners.set(placementID, listener);
  }

  static removeAdListener(placementID: string, listener?: ATAdListener): void {
    // 同 placementId 多实例（如单条页与列表页共用位）下，dispose 只能删「当前还是自己注册的」监听，
    // 否则会误删另一个存活实例的监听（致它收不到 onNativeAdLoaded 等回调）。传 listener 校验占位者一致才删。
    if (listener && ATAdEvents.placementListeners.get(placementID) !== listener) {
      return;
    }
    ATAdEvents.placementListeners.delete(placementID);
  }

  static setRevenueListener(
    placementID: string,
    listener: (info: ATAdInfo) => void
  ): void {
    ATAdEvents.revenueListeners.set(placementID, listener);
  }

  static removeRevenueListener(placementID: string): void {
    ATAdEvents.revenueListeners.delete(placementID);
  }

  /** 按 placementID#adId 注册视图级监听（某条广告的 show/click 等分流到对应 cell）。 */
  static setAdInstanceListener(
    placementID: string,
    adId: string,
    listener: ATAdListener
  ): void {
    ATAdEvents.adListeners.set(ATAdEvents.adKey(placementID, adId), listener);
  }

  static removeAdInstanceListener(placementID: string, adId: string): void {
    ATAdEvents.adListeners.delete(ATAdEvents.adKey(placementID, adId));
  }

  /** 注册全局监听（接收所有 callName 事件）。 */
  static addGlobalListener(listener: ATAdListener): void {
    ATAdEvents.globalListeners.push(listener);
  }

  static removeGlobalListener(listener: ATAdListener): void {
    ATAdEvents.globalListeners = ATAdEvents.globalListeners.filter(
      (l) => l !== listener
    );
  }

  /** 释放全部订阅与监听（一般无需调用；测试/热重载场景用）。 */
  static dispose(): void {
    ATAdEvents.subscriptions.forEach((s) => s.remove());
    ATAdEvents.subscriptions = [];
    ATAdEvents.emitter = null;
    ATAdEvents.placementListeners.clear();
    ATAdEvents.revenueListeners.clear();
    ATAdEvents.adListeners.clear();
    ATAdEvents.globalListeners = [];
  }
}
