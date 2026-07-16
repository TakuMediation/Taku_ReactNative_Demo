import { useEffect } from 'react';
import { Text, StyleSheet } from 'react-native';
import { ATSDK, ATAdEvents, InitCallback } from '@anythink/react-native-sdk';
import type { ATAdEventPayload } from '@anythink/react-native-sdk';
import { Screen } from '../components/Screen';
import { AdButton } from '../components/AdButton';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

/**
 * ATSDK API 验证页。单页分 5 组，
 * 每方法一按钮 → 调 ATSDK → 调用名+返回值 append 日志（Promise append resolve；void append called）。
 */
export function FeatureScreen({ onBack }: ScreenProps) {
  const { LogArea, append } = useLogArea();
  const pid = sdkConfig.placements.reward ?? '';

  useEffect(() => {
    const onInitEvent = (p: ATAdEventPayload) => {
      const extra = p.extraDic ?? {};
      const isConsentDismiss =
        p.callbackName === InitCallback.consentDismiss ||
        extra.consentDismiss !== undefined;
      if (!isConsentDismiss) {
        return;
      }
      if (p.callbackName === InitCallback.consentDismiss) {
        append(
          `consentDismiss: infoMsg=${String(extra.infoMsg ?? '')} dismissType=${String(extra.dismissType ?? '')}`
        );
      } else {
        append(`consentDismiss: ${JSON.stringify(extra)}`);
      }
    };
    ATAdEvents.addGlobalListener(onInitEvent);
    return () => ATAdEvents.removeGlobalListener(onInitEvent);
  }, [append]);

  // void 方法：调用后 append "called"
  const call = (name: string, fn: () => void) => () => {
    try {
      fn();
      append(`${name}: called`);
    } catch (e) {
      append(`${name}: error ${String(e)}`);
    }
  };
  // Promise 方法：append resolve 值
  const callAsync = (name: string, fn: () => Promise<unknown>) => async () => {
    try {
      append(`${name} = ${JSON.stringify(await fn())}`);
    } catch (e) {
      append(`${name}: error ${String(e)}`);
    }
  };

  return (
    <Screen title="SDK API" onBack={onBack} log={<LogArea />}>
      {/* ① GDPR / 隐私 */}
      <Text style={styles.group}>① GDPR / 隐私</Text>
      <AdButton
        label="showGDPRConsentDialog"
        onPress={call('showGDPRConsentDialog', () =>
          ATSDK.showGDPRConsentDialog()
        )}
      />
      <AdButton
        label="showGDPRConsentSecondDialog"
        onPress={call('showGDPRConsentSecondDialog', () =>
          ATSDK.showGDPRConsentSecondDialog()
        )}
      />
      <AdButton
        label="getGDPRDataLevel"
        onPress={callAsync('getGDPRDataLevel', () => ATSDK.getGDPRDataLevel())}
      />
      <AdButton
        label="checkIsEuTraffic"
        onPress={callAsync('checkIsEuTraffic', () => ATSDK.checkIsEuTraffic())}
      />
      <AdButton
        label="isEUTraffic"
        onPress={callAsync('isEUTraffic', () => ATSDK.isEUTraffic())}
      />
      <AdButton
        label="setGDPRUploadDataLevel(1)"
        onPress={call('setGDPRUploadDataLevel', () =>
          ATSDK.setGDPRUploadDataLevel(1)
        )}
      />
      <AdButton
        label="setPersonalizedAdStatus(0)"
        onPress={call('setPersonalizedAdStatus', () =>
          ATSDK.setPersonalizedAdStatus(0)
        )}
      />
      <AdButton
        label="getPersonalizedAdStatus"
        onPress={callAsync('getPersonalizedAdStatus', () =>
          ATSDK.getPersonalizedAdStatus()
        )}
      />

      {/* ② filter */}
      <Text style={styles.group}>② filter</Text>
      <AdButton
        label="setFilterAdSourceIdList"
        onPress={call('setFilterAdSourceIdList', () =>
          ATSDK.setFilterAdSourceIdList(pid, ['demo_src'])
        )}
      />
      <AdButton
        label="setFilterNetworkFirmIdList"
        onPress={call('setFilterNetworkFirmIdList', () =>
          ATSDK.setFilterNetworkFirmIdList(pid, ['1'])
        )}
      />
      <AdButton
        label="setForbidNetworkFirmIdList"
        onPress={call('setForbidNetworkFirmIdList', () =>
          ATSDK.setForbidNetworkFirmIdList(['1'])
        )}
      />
      <AdButton
        label="removeFilters"
        onPress={call('removeFilters', () => ATSDK.removeFilters())}
      />

      {/* ③ area / location */}
      <Text style={styles.group}>③ area / location</Text>
      <AdButton
        label="getArea"
        onPress={callAsync('getArea', () => ATSDK.getArea())}
      />
      <AdButton
        label="isCnSDK"
        onPress={callAsync('isCnSDK', () => ATSDK.isCnSDK())}
      />
      <AdButton
        label="setLocation"
        onPress={call('setLocation', () =>
          ATSDK.setLocation({ latitude: 39.9, longitude: 116.4 })
        )}
      />

      {/* ④ debug / 渠道 / 策略 */}
      <Text style={styles.group}>④ debug / 渠道 / 策略</Text>
      <AdButton
        label="integrationChecking"
        onPress={call('integrationChecking', () => ATSDK.integrationChecking())}
      />
      <AdButton
        label="isNetworkLogDebug"
        onPress={callAsync('isNetworkLogDebug', () =>
          ATSDK.isNetworkLogDebug()
        )}
      />
      <AdButton
        label="setNetworkLogDebug(true)"
        onPress={call('setNetworkLogDebug', () =>
          ATSDK.setNetworkLogDebug(true)
        )}
      />
      <AdButton
        label="showDebuggerUI"
        onPress={call('showDebuggerUI', () => ATSDK.showDebuggerUI())}
      />
      <AdButton
        label="setChannel(test)"
        onPress={call('setChannel', () => ATSDK.setChannel('test'))}
      />
      <AdButton
        label="setChannelSource(1)"
        onPress={call('setChannelSource', () => ATSDK.setChannelSource(1))}
      />

      {/* ⑤ 版本 / 基础 */}
      <Text style={styles.group}>⑤ 版本 / 基础</Text>
      <AdButton
        label="getSDKVersionName"
        onPress={call('getSDKVersionName', () =>
          append('version = ' + ATSDK.getSDKVersionName())
        )}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  group: {
    fontSize: 13,
    fontWeight: '700',
    color: '#888',
    marginTop: 14,
    marginBottom: 2,
    marginLeft: 14,
  },
});
