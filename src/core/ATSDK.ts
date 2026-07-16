import ATReactNativeBridge from '../specs/NativeATBridge';
import type { ATWaterfallFilterSpec } from '../types';
import { ATAdEvents } from '../events/ATAdEvents';
import { ATLog } from '../utils/ATLog';

/**
 * Core 入口（静态类）。
 * `Context`/`Activity` 不由 JS 传入，桥内获取。
 *`
 * 内部-only：`init` 时桥内调 `setSystemDevFragmentType("rn")` 上报 `os_fw=5`、日志双开（SDK + MsgTools）。
 */
export class ATSDK {
  static readonly VERSION = '1.0.0';
  static readonly PERSONALIZED = 0;
  static readonly NONPERSONALIZED = 1;
  static readonly UNKNOWN = 2;

  // —— 初始化 / 版本 ——
  static init(appId: string, appKey: string): void {
    ATLog.call('ATSDK', 'init', appId);
    // 建立 NativeEventEmitter 订阅，使各广告回调（load/show/click 等）能分发到 JS；早于任何广告 load。
    ATAdEvents.init();
    ATReactNativeBridge.initAnyThinkSDK(appId, appKey);
  }

  static start(): void {
    ATLog.call('ATSDK', 'start');
    ATReactNativeBridge.start();
  }

  static getSDKVersionName(): string {
    ATLog.call('ATSDK', 'getSDKVersionName');
    return ATReactNativeBridge.getSDKVersionName();
  }

  // —— 日志 / 渠道 / 自定义参数 ——
  static setNetworkLogDebug(debug: boolean): void {
    // 同步开关：RN 层 ATLog + 原生层 MsgTools/SDK 网络日志
    ATLog.enabled = debug;
    ATLog.call('ATSDK', 'setNetworkLogDebug', debug);
    ATReactNativeBridge.setLogEnabled(debug);
  }

  static setChannel(channel: string): void {
    ATLog.call('ATSDK', 'setChannel', channel);
    ATReactNativeBridge.setChannelStr(channel);
  }

  static initCustomMap(customMap: Record<string, unknown>): void {
    ATLog.call('ATSDK', 'initCustomMap', customMap);
    ATReactNativeBridge.setCustomDataDic(customMap);
  }

  static initPlacementCustomMap(
    placementId: string,
    customMap: Record<string, unknown>
  ): void {
    ATLog.call('ATSDK', 'initPlacementCustomMap', placementId, customMap);
    ATReactNativeBridge.setPlacementCustomData(placementId, customMap);
  }

  // —— GDPR / 隐私 ——
  static getGDPRDataLevel(): Promise<number> {
    ATLog.call('ATSDK', 'getGDPRDataLevel');
    return ATReactNativeBridge.getGDPRLevel();
  }

  static setGDPRUploadDataLevel(level: number): void {
    ATLog.call('ATSDK', 'setGDPRUploadDataLevel', level);
    ATReactNativeBridge.setDataConsentSet(level);
  }

  static showGDPRConsentDialog(appId: string = ''): void {
    ATLog.call('ATSDK', 'showGDPRConsentDialog', appId);
    ATReactNativeBridge.showGDPRConsentDialog(appId);
  }

  static showGDPRConsentSecondDialog(appId: string = ''): void {
    ATLog.call('ATSDK', 'showGDPRConsentSecondDialog', appId);
    ATReactNativeBridge.showGDPRConsentSecondDialog(appId);
  }

  static showGdprAuth(): void {
    ATLog.call('ATSDK', 'showGdprAuth');
    ATReactNativeBridge.showGdprAuth();
  }

  static checkIsEuTraffic(): Promise<boolean> {
    ATLog.call('ATSDK', 'checkIsEuTraffic');
    return ATReactNativeBridge.checkIsEuTraffic();
  }

  static isEUTraffic(): Promise<boolean> {
    ATLog.call('ATSDK', 'isEUTraffic');
    return ATReactNativeBridge.isEUTraffic();
  }

  static deniedUploadDeviceInfo(...deviceInfoKeys: string[]): void {
    ATLog.call('ATSDK', 'deniedUploadDeviceInfo', deviceInfoKeys);
    ATReactNativeBridge.deniedUploadDeviceInfo(deviceInfoKeys);
  }

  static setPersonalizedAdStatus(status: number): void {
    ATLog.call('ATSDK', 'setPersonalizedAdStatus', status);
    ATReactNativeBridge.setPersonalizedAdStatus(status);
  }

  /** 原生无对应 getter；保留此接口占位，始终返回 UNKNOWN。 */
  static getPersonalizedAdStatus(): Promise<number> {
    ATLog.call('ATSDK', 'getPersonalizedAdStatus');
    return Promise.resolve(ATSDK.UNKNOWN);
  }

  // —— 过滤 / 策略 / 调试 ——
  static setFilterAdSourceIdList(
    placementId: string,
    adSourceIdList: string[]
  ): void {
    ATLog.call('ATSDK', 'setFilterAdSourceIdList', placementId, adSourceIdList);
    ATReactNativeBridge.setFilterAdSourceIdList(placementId, adSourceIdList);
  }

  static setFilterNetworkFirmIdList(
    placementId: string,
    networkFirmIdList: string[]
  ): void {
    ATLog.call(
      'ATSDK',
      'setFilterNetworkFirmIdList',
      placementId,
      networkFirmIdList
    );
    ATReactNativeBridge.setFilterNetworkFirmIdList(
      placementId,
      networkFirmIdList
    );
  }

  static setForbidNetworkFirmIdList(networkFirmIdList: string[]): void {
    ATLog.call('ATSDK', 'setForbidNetworkFirmIdList', networkFirmIdList);
    ATReactNativeBridge.setForbidNetworkFirmIdList(networkFirmIdList);
  }

  static setForbidShowNetworkFirmIdList(
    placementId: string,
    networkFirmIdList: string[]
  ): void {
    ATLog.call(
      'ATSDK',
      'setForbidShowNetworkFirmIdList',
      placementId,
      networkFirmIdList
    );
    ATReactNativeBridge.setForbidShowNetworkFirmIdList(
      placementId,
      networkFirmIdList
    );
  }

  static setAllowedShowNetworkFirmIdList(
    placementId: string,
    networkFirmIdList: string[]
  ): void {
    ATLog.call(
      'ATSDK',
      'setAllowedShowNetworkFirmIdList',
      placementId,
      networkFirmIdList
    );
    ATReactNativeBridge.setAllowedShowNetworkFirmIdList(
      placementId,
      networkFirmIdList
    );
  }

  static setRiskFilterNetworkFirmIdList(
    risk: number,
    networkFirmIdList: string[]
  ): void {
    ATLog.call(
      'ATSDK',
      'setRiskFilterNetworkFirmIdList',
      risk,
      networkFirmIdList
    );
    ATReactNativeBridge.setRiskFilterNetworkFirmIdList(risk, networkFirmIdList);
  }

  // —— area / location ——
  static getArea(): Promise<string> {
    ATLog.call('ATSDK', 'getArea');
    return ATReactNativeBridge.getArea();
  }

  static isCnSDK(): Promise<boolean> {
    ATLog.call('ATSDK', 'isCnSDK');
    return ATReactNativeBridge.isCnSDK();
  }

  static setLocation(location: { latitude: number; longitude: number }): void {
    ATLog.call('ATSDK', 'setLocation', location);
    ATReactNativeBridge.setLocation(location);
  }

  static setDeviceInformationData(data: Record<string, unknown>): void {
    ATLog.call('ATSDK', 'setDeviceInformationData', data);
    ATReactNativeBridge.setDeviceInformationData(data);
  }

  // —— debug ——
  static integrationChecking(): void {
    ATLog.call('ATSDK', 'integrationChecking');
    ATReactNativeBridge.integrationChecking();
  }

  static isNetworkLogDebug(): Promise<boolean> {
    ATLog.call('ATSDK', 'isNetworkLogDebug');
    return ATReactNativeBridge.isNetworkLogDebug();
  }

  // —— 策略 / 渠道 ——
  static setChannelSource(channelFrom: number): void {
    ATLog.call('ATSDK', 'setChannelSource', channelFrom);
    ATReactNativeBridge.setChannelSource(channelFrom);
  }

  static setLocalStrategyAssetPath(path: string): void {
    ATLog.call('ATSDK', 'setLocalStrategyAssetPath', path);
    ATReactNativeBridge.setLocalStrategyAssetPath(path);
  }

  static setSharedPlacementConfig(config: Record<string, unknown>): void {
    ATLog.call('ATSDK', 'setSharedPlacementConfig', config);
    ATReactNativeBridge.setSharedPlacementConfig(config);
  }

  // —— 过滤 / 策略 / 调试（原有） ——
  static putFilter(placementId: string, filter: ATWaterfallFilterSpec): void {
    ATLog.call('ATSDK', 'putFilter', placementId, filter);
    ATReactNativeBridge.putFilter(placementId, filter as unknown as Object);
  }

  static removeFilters(): void {
    ATLog.call('ATSDK', 'removeFilters');
    ATReactNativeBridge.removeFilters();
  }

  static removeFilterWithPlacementId(placementId: string): void {
    ATLog.call('ATSDK', 'removeFilterWithPlacementId', placementId);
    ATReactNativeBridge.removeFilterWithPlacementId(placementId);
  }

  static setAdSourcePrivacyPolicy(policyJson: string): void {
    ATLog.call('ATSDK', 'setAdSourcePrivacyPolicy', policyJson);
    ATReactNativeBridge.setAdSourcePrivacyPolicy(policyJson);
  }

  static showDebuggerUI(debugKey: string = ''): void {
    ATLog.call('ATSDK', 'showDebuggerUI');
    ATReactNativeBridge.showDebuggerUI(debugKey);
  }
}
