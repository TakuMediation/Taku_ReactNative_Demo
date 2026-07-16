import { useEffect, useState } from 'react';
import {
  SafeAreaView,
  ScrollView,
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  type ImageSourcePropType,
} from 'react-native';
import { ATSDK } from '@anythink/react-native-sdk';
import type { ScreenName } from '../navigation/screens';

// 卡片入口（图标 + 标题 + 描述）。
const ENTRIES: {
  key: ScreenName;
  icon: ImageSourcePropType;
  title: string;
  desc: string;
}[] = [
  {
    key: 'reward',
    icon: require('../assets/home/ic_test_rewarded_video.png'),
    title: '激励视频',
    desc: '用户观看视频广告以换取应用内奖励。',
  },
  {
    key: 'interstitial',
    icon: require('../assets/home/ic_test_interstitial.png'),
    title: '插屏',
    desc: '含插屏与全屏，在自然停顿或切换节点展示。',
  },
  {
    key: 'splash',
    icon: require('../assets/home/ic_test_splash.png'),
    title: '开屏',
    desc: '应用启动后立即展示。',
  },
  {
    key: 'banner',
    icon: require('../assets/home/ic_test_banner.png'),
    title: '横幅',
    desc: '尺寸灵活，可放在应用顶部、中部或底部。',
  },
  {
    key: 'native',
    icon: require('../assets/home/ic_test_native.png'),
    title: '原生',
    desc: '含模板、自渲染与原生列表（多广告），与应用界面无缝融合。',
  },
  {
    key: 'feature',
    icon: require('../assets/home/ic_test_interstitial.png'),
    title: 'SDK 接口',
    desc: '验证 ATSDK Feture接口。',
  },
];

/** 主页：图标卡片入口 + 顶部标题 + 底部 SDK 版本水印。 */
export function HomeScreen({
  onNavigate,
}: {
  onNavigate: (s: ScreenName) => void;
}) {
  const [version, setVersion] = useState('(获取中)');
  useEffect(() => {
    setVersion(ATSDK.getSDKVersionName() || '(未初始化 / iOS 占位)');
  }, []);

  return (
    <SafeAreaView style={styles.root}>
      {/* 顶栏：标题 */}
      <View style={styles.topBar}>
        <Text style={styles.title}>ATSDK 示例</Text>
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        {ENTRIES.map((e) => (
          <TouchableOpacity
            key={e.key}
            style={styles.card}
            activeOpacity={0.7}
            onPress={() => onNavigate(e.key)}
          >
            <View style={styles.iconBox}>
              <Image source={e.icon} style={styles.icon} resizeMode="contain" />
            </View>
            <View style={styles.cardText}>
              <Text style={styles.cardTitle}>{e.title}</Text>
              <Text style={styles.cardDesc}>{e.desc}</Text>
            </View>
          </TouchableOpacity>
        ))}
        {/* 底部版本水印：RN 插件版本 + 原生 SDK 版本 */}
        <Text style={styles.watermark}>
          插件版本 v{ATSDK.VERSION} · SDK {version}
        </Text>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#f2f3f5' },
  topBar: {
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 12,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#1a1a2e',
    textAlign: 'center',
  },
  scroll: { padding: 12, paddingBottom: 40 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    // 卡片阴影（iOS + Android）
    shadowColor: '#000',
    shadowOpacity: 0.06,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  iconBox: {
    width: 72,
    height: 72,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#eef0f3',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 16,
  },
  icon: { width: 56, height: 56 },
  cardText: { flex: 1 },
  cardTitle: { fontSize: 18, color: '#1a1a2e', marginBottom: 6 },
  cardDesc: { fontSize: 13, color: '#9aa0a6', lineHeight: 19 },
  watermark: {
    textAlign: 'center',
    color: '#c4c8cc',
    fontSize: 12,
    marginTop: 12,
  },
});
