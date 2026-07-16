import type { ReactNode } from 'react';
import { View, ScrollView, StyleSheet, SafeAreaView } from 'react-native';
import { TitleBar } from './TitleBar';

/**
 * 页骨架：TitleBar + 可滚动内容区(上) + 日志区(下，由调用方传入 LogArea 元素)。
 */
export function Screen({
  title,
  onBack,
  children,
  log,
}: {
  title: string;
  onBack: () => void;
  children: ReactNode;
  /** 日志区元素（来自 useLogArea 的 <LogArea />）；放页面下半部。 */
  log?: ReactNode;
}) {
  return (
    <SafeAreaView style={styles.root}>
      <TitleBar title={title} onBack={onBack} />
      <ScrollView
        style={styles.content}
        contentContainerStyle={styles.contentInner}
        keyboardShouldPersistTaps="handled"
        nestedScrollEnabled
      >
        {children}
      </ScrollView>
      {log ? <View style={styles.logSlot}>{log}</View> : null}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },
  /** flex:1 限制在 TitleBar 与日志区之间滚动，避免内容把日志挤出屏幕。 */
  content: { flex: 1 },
  contentInner: { paddingVertical: 8, paddingHorizontal: 12 },
  /** 固定高度，保证日志区始终可见、内部可独立滚动。 */
  logSlot: { height: 200 },
});
