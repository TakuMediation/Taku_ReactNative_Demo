import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';

/** 顶部标题栏 + 返回。 */
export function TitleBar({
  title,
  onBack,
}: {
  title: string;
  onBack: () => void;
}) {
  return (
    <View style={styles.bar}>
      <TouchableOpacity onPress={onBack} style={styles.back} hitSlop={8}>
        <Text style={styles.backText}>‹ 返回</Text>
      </TouchableOpacity>
      <Text style={styles.title}>{title}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 28,
    paddingHorizontal: 12,
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  back: { paddingVertical: 4, paddingRight: 14 },
  backText: { color: '#2d6cdf', fontSize: 16 },
  title: { fontSize: 18, fontWeight: 'bold', color: '#222' },
});
