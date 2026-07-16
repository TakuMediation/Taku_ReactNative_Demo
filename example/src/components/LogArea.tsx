import { useCallback, useRef, useState } from 'react';
import { ScrollView, Text, StyleSheet, View, Platform } from 'react-native';

/**
 * 页内日志区。
 * 命令式 append：广告回调、ATSDK 方法返回都手动追加（新行置顶，上限 100 行）。
 *
 * 用法：
 *   const { LogArea, append } = useLogArea();
 *   ... append('onAdLoaded') ...
 *   return <Screen ...><LogArea /></Screen>;
 */
export function useLogArea() {
  const [lines, setLines] = useState<string[]>([]);
  const seq = useRef(0);

  const append = useCallback((msg: string) => {
    const t = new Date();
    const ts =
      `${String(t.getHours()).padStart(2, '0')}:` +
      `${String(t.getMinutes()).padStart(2, '0')}:` +
      `${String(t.getSeconds()).padStart(2, '0')}`;
    const id = seq.current++;
    setLines((prev) => [`[${ts}] ${msg}`, ...prev].slice(0, 100));
    return id;
  }, []);

  const clear = useCallback(() => setLines([]), []);

  const LogArea = useCallback(
    () => (
      <View style={styles.box}>
        <Text style={styles.title}>日志（{lines.length}）</Text>
        <ScrollView
          style={styles.scroll}
          nestedScrollEnabled
          showsVerticalScrollIndicator
        >
          {lines.map((l, i) => (
            <Text key={i} style={styles.line}>
              {l}
            </Text>
          ))}
        </ScrollView>
      </View>
    ),
    [lines]
  );

  return { LogArea, append, clear };
}

const styles = StyleSheet.create({
  box: {
    flex: 1,
    margin: 12,
    marginTop: 0,
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 6,
    backgroundColor: '#fafafa',
    overflow: 'hidden',
  },
  title: {
    fontSize: 12,
    fontWeight: '600',
    color: '#666',
    padding: 6,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  scroll: { flex: 1, padding: 6 },
  line: {
    fontSize: 11,
    color: '#333',
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    marginBottom: 2,
  },
});
