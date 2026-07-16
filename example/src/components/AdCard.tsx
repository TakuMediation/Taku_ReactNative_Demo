import type { ReactNode } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native';
import { TitleBar } from './TitleBar';
import { NetworkPicker } from './NetworkPicker';

/**
 * 广告页卡片骨架：
 *   TitleBar + 图标卡 + 网络选择下拉框 + Auto Load 复选 + 操作按钮区 + 日志区。
 */
export function AdCard({
  title,
  onBack,
  networks,
  network,
  onNetworkChange,
  autoLoad,
  onAutoLoadChange,
  actions,
  log,
}: {
  title: string;
  onBack: () => void;
  networks: string[];
  network: string;
  onNetworkChange: (v: string) => void;
  autoLoad: boolean;
  onAutoLoadChange: (v: boolean) => void;
  actions: ReactNode;
  log: ReactNode;
}) {
  return (
    <SafeAreaView style={styles.root}>
      <TitleBar title={title} onBack={onBack} />
      {/* 图标卡 + 标题 */}
      <View style={styles.iconCard}>
        <View style={styles.iconBox} />
        <Text style={styles.iconLabel}>{title}</Text>
      </View>

      <View style={styles.row}>
        <View style={styles.pickerWrap}>
          <NetworkPicker
            options={networks}
            value={network}
            onChange={onNetworkChange}
          />
        </View>
        <TouchableOpacity
          style={styles.checkRow}
          onPress={() => onAutoLoadChange(!autoLoad)}
        >
          <View style={[styles.checkbox, autoLoad && styles.checkboxOn]}>
            {autoLoad ? <Text style={styles.checkMark}>✓</Text> : null}
          </View>
          <Text style={styles.checkLabel}>Auto Load</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.actions}>{actions}</View>
      <View style={styles.logSlot}>{log}</View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },
  iconCard: {
    alignItems: 'center',
    paddingVertical: 16,
    marginHorizontal: 12,
    marginTop: 8,
    backgroundColor: '#f7f9ff',
    borderRadius: 8,
  },
  iconBox: {
    width: 64,
    height: 80,
    borderWidth: 2,
    borderColor: '#7aa7ff',
    borderRadius: 6,
    backgroundColor: '#eaf1ff',
  },
  iconLabel: { marginTop: 8, fontSize: 13, color: '#666' },
  row: { flexDirection: 'row', alignItems: 'flex-end' },
  pickerWrap: { flex: 1 },
  checkRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingBottom: 12,
  },
  checkbox: {
    width: 18,
    height: 18,
    borderWidth: 1,
    borderColor: '#888',
    borderRadius: 3,
    marginRight: 6,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkboxOn: { backgroundColor: '#2d6cdf', borderColor: '#2d6cdf' },
  checkMark: { color: '#fff', fontSize: 12 },
  checkLabel: { fontSize: 14, color: '#333' },
  actions: { paddingVertical: 6 },
  logSlot: { flex: 1 },
});
