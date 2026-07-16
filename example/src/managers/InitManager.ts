import { ATSDK, ATAdEvents } from '@anythink/react-native-sdk';
import { sdkConfig } from '../config/sdkConfig';

/**
 * SDK 初始化管理器。
 * 改为**手动触发**：InitScreen 用户同意隐私协议后才调 initSdk()。
 * 顺序（不可打乱）：日志 → 全局监听 → integrationChecking → 个性化 → init → start。
 */
export const InitManager = {
  initialized: false,

  /** 用户同意隐私后调用。personalized: 个性化广告开关。 */
  initSdk(personalized: boolean): void {
    if (InitManager.initialized) {
      return;
    }
    // 1. 日志（早于 init；SDK + 桥接层 MsgTools 双开）
    ATSDK.setNetworkLogDebug(true);
    // 2. 全局事件监听（订阅各 callName）
    ATAdEvents.init();
    // 3. 集成检测
    ATSDK.integrationChecking();
    // 4. 个性化广告（PERSONALIZED=允许, NONPERSONALIZED=限制）
    ATSDK.setPersonalizedAdStatus(
      personalized ? ATSDK.PERSONALIZED : ATSDK.NONPERSONALIZED
    );
    // 5. init + start
    ATSDK.init(sdkConfig.appId, sdkConfig.appKey);
    // ATSDK.setLocalStrategyAssetPath('预制策略自定义路程名称');
    ATSDK.start();

    InitManager.initialized = true;
    console.log('[InitManager] initSdk done', {
      appId: sdkConfig.appId,
      personalized,
      version: ATSDK.getSDKVersionName(),
    });
  },

  /** 个性化开关（init 前后均可调）。 */
  setPersonalized(personalized: boolean): void {
    ATSDK.setPersonalizedAdStatus(
      personalized ? ATSDK.PERSONALIZED : ATSDK.NONPERSONALIZED
    );
  },
};
