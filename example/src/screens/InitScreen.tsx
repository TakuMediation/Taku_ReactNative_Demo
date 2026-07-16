import { useEffect, useState } from 'react';
import {
  SafeAreaView,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Switch,
  Modal,
} from 'react-native';
import { WebView } from 'react-native-webview';
import { ATSDK } from '@anythink/react-native-sdk';
import { InitManager } from '../managers/InitManager';
import { Prefs } from '../managers/prefs';

const PRIVACY_URL = 'https://www.toponad.com/zh-cn/privacy-policy';

/**
 * 启动入口页（带自绘隐私弹框）：
 *  - 初始化SDK → 弹隐私 overlay（内嵌 WebView 加载完整隐私政策，不跳外部浏览器 + 同意/不同意）→ 同意后 initSdk
 *  - 个性化广告开关 → setPersonalizedAdStatus(0/1)
 *  - 同意状态 + 个性化开关持久化（Prefs）：已同意过的，重开应用不再弹框、自动初始化、恢复开关
 *  - 进入 Demo → 须先 init，否则提示
 */
export function InitScreen({ onEnterDemo }: { onEnterDemo: () => void }) {
  const [inited, setInited] = useState(false);
  const [personalized, setPersonalized] = useState(false);
  const [showPrivacy, setShowPrivacy] = useState(false);
  const [hint, setHint] = useState('');

  // 启动恢复：读已存的个性化选择；若之前已同意过隐私协议，直接初始化、不再弹框。
  // __DEV__ 真机 Init 冒烟：未同意时也自动 init，避免手工点隐私弹框。
  useEffect(() => {
    (async () => {
      const savedPersonalized = await Prefs.getPersonalized();
      setPersonalized(savedPersonalized);
      const agreed = await Prefs.getAgreed();
      console.log('[InitScreen] launch', {
        agreed,
        version: ATSDK.getSDKVersionName(),
      });
      if (agreed || __DEV__) {
        if (!agreed && __DEV__) {
          await Prefs.setAgreed(true);
        }
        InitManager.initSdk(savedPersonalized);
        setInited(true);
        setHint(agreed ? '请先初始化SDK' : '初始化成功 (DEV smoke)');
      }
    })();
  }, []);

  const onAgree = () => {
    setShowPrivacy(false);
    void Prefs.setAgreed(true);
    InitManager.initSdk(personalized);
    setInited(true);
    setHint('初始化成功');
  };

  const onTogglePersonalized = (next: boolean) => {
    setPersonalized(next);
    void Prefs.setPersonalized(next);
    InitManager.setPersonalized(next);
  };

  return (
    <SafeAreaView style={styles.root}>
      <Text style={styles.header}>AnyThink RN SDK</Text>
      <Text style={styles.version}>
        SDK version: {ATSDK.getSDKVersionName() || '(stub)'}
      </Text>

      <TouchableOpacity
        style={styles.card}
        onPress={() =>
          inited ? setHint('SDK 已初始化') : setShowPrivacy(true)
        }
      >
        <Text style={styles.cardTitle}>初始化 SDK</Text>
        <Text style={styles.cardDesc}>用户同意隐私协议后，进行 SDK 初始化</Text>
      </TouchableOpacity>

      <View style={styles.card}>
        <View style={styles.switchRow}>
          <View style={styles.flex}>
            <Text style={styles.cardTitle}>个性化广告服务开关</Text>
            <Text style={styles.cardDesc}>
              当前状态：{personalized ? '开启' : '关闭'}
            </Text>
          </View>
          <Switch value={personalized} onValueChange={onTogglePersonalized} />
        </View>
      </View>

      <TouchableOpacity
        style={[styles.card, !inited && styles.cardDisabled]}
        onPress={() => (inited ? onEnterDemo() : setHint('请先初始化 SDK'))}
      >
        <Text style={styles.cardTitle}>展示广告</Text>
        <Text style={styles.cardDesc}>需要初始化 SDK 后才能进行广告测试</Text>
      </TouchableOpacity>

      {hint ? <Text style={styles.hint}>{hint}</Text> : null}

      {/* 隐私 overlay（内嵌 WebView 加载完整隐私政策，不跳外部浏览器 + 同意/不同意） */}
      <Modal visible={showPrivacy} animationType="slide" transparent>
        <View style={styles.backdrop}>
          <View style={styles.dialog}>
            <Text style={styles.dlgTitle}>隐私政策</Text>
            <View style={styles.dlgBody}>
              <WebView
                source={{ uri: PRIVACY_URL }}
                style={styles.web}
                startInLoadingState
              />
            </View>
            <View style={styles.privacyBtns}>
              <TouchableOpacity
                style={[styles.pBtn, styles.pBtnGhost]}
                onPress={() => setShowPrivacy(false)}
              >
                <Text style={styles.pBtnGhostText}>不同意</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.pBtn} onPress={onAgree}>
                <Text style={styles.pBtnText}>同意</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, paddingTop: 32, backgroundColor: '#f2f2f2' },
  header: { fontSize: 20, fontWeight: 'bold', textAlign: 'center' },
  version: { textAlign: 'center', marginVertical: 8, color: '#666' },
  card: {
    backgroundColor: '#fff',
    marginHorizontal: 12,
    marginVertical: 6,
    padding: 16,
    borderRadius: 8,
  },
  cardDisabled: { opacity: 0.6 },
  cardTitle: { fontSize: 16, color: '#222' },
  cardDesc: { fontSize: 12, color: '#999', marginTop: 4 },
  switchRow: { flexDirection: 'row', alignItems: 'center' },
  flex: { flex: 1 },
  hint: { textAlign: 'center', color: '#2d6cdf', marginTop: 12 },
  backdrop: {
    flex: 1,
    backgroundColor: '#0008',
    paddingHorizontal: 20,
    paddingVertical: 48,
  },
  dialog: { backgroundColor: '#fff', borderRadius: 10, padding: 18, flex: 1 },
  dlgTitle: { fontSize: 17, fontWeight: 'bold', marginBottom: 10 },
  dlgBody: { flex: 1, borderRadius: 6, overflow: 'hidden' },
  web: { flex: 1 },
  privacyBtns: { flexDirection: 'row', marginTop: 16 },
  pBtn: {
    flex: 1,
    marginHorizontal: 6,
    paddingVertical: 12,
    borderRadius: 6,
    alignItems: 'center',
    backgroundColor: '#2d6cdf',
  },
  pBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  pBtnGhost: {
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#2d6cdf',
  },
  pBtnGhostText: { color: '#2d6cdf', fontSize: 16, fontWeight: '600' },
});
