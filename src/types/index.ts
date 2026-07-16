/**
 * 跨端基础类型：Init/事件 等用到的公共类型定义。
 */

/** 展平后的广告信息（来自 payload.extraDic，snake_case key 与原生 EventCallbackInfo 一致）。 */
export type ATAdInfo = Record<string, unknown>;

/** 广告错误（从 payload.requestMessage 解析；fullErrorInfo = AdError.getFullErrorInfo()）。 */
export interface AdError {
  code: string | number;
  desc: string;
  fullErrorInfo: string;
}

/** 统一事件 payload 根字段（与 Const.CallbackKey 对应）。 */
export interface ATAdEventPayload {
  callbackName: string;
  placementID: string;
  /** native 列表多广告：事件来源的广告实例引用（show/click 等带，load 类不带）。 */
  adId?: string;
  extraDic?: ATAdInfo;
  requestMessage?: string;
  isDeeplinkSuccess?: boolean;
  isTimeout?: boolean;
}

/** 广告监听器（各广告类的 setAdListener 用）。 */
export type ATAdListener = (payload: ATAdEventPayload) => void;

/** 展示配置。 */
export interface ATShowConfig {
  scenarioId?: string;
  showCustomExt?: string;
  atCustomContentResult?: Record<string, unknown>;
}

/** 加载请求参数。 */
export interface ATAdRequest {
  channelSource?: number;
  adxBidFloorInfo?: {
    bidFloor?: number;
    currency?: string;
    extraMap?: Record<string, unknown>;
  };
  preLoadInfo?: {
    requestId?: string;
    psId?: string;
    placementId?: string;
    cpEcpmSwitch?: number;
    cpEcpmTimeout?: number;
  };
}

/** 广告就绪/缓存状态。 */
export interface ATAdStatusInfo {
  isLoading: boolean;
  isReady: boolean;
  atTopAdInfo?: ATAdInfo;
}

/** 瀑布过滤规则（putFilter 用，组间 OR、组内 AND）。 */
export interface ATWaterfallFilterSpec {
  groups: Array<{
    networkId?: string[];
    biddingType?: string[];
    networkPlacementId?: string[];
    e_cpm?: {
      currency?: string;
      moreThanPrice?: number;
      lessThanPrice?: number;
    };
  }>;
}

/** 从 requestMessage 解析为 AdError（成功时 requestMessage 常为空串）。 */
export function toAdError(requestMessage?: string): AdError | undefined {
  if (!requestMessage) {
    return undefined;
  }
  return { code: '', desc: requestMessage, fullErrorInfo: requestMessage };
}
