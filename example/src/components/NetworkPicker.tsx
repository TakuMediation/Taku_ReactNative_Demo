import { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Modal,
  FlatList,
  StyleSheet,
} from 'react-native';

/**
 * 网络选择下拉框。现各广告通常只有 All（缺各网络位）；
 * 传入 options（网络名数组）+ 当前值 + onChange。轻量自绘 Modal 下拉，无 native 依赖。
 */
export function NetworkPicker({
  options,
  value,
  onChange,
}: {
  options: string[];
  value: string;
  onChange: (v: string) => void;
}) {
  const [open, setOpen] = useState(false);
  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>Select Network</Text>
      <TouchableOpacity style={styles.field} onPress={() => setOpen(true)}>
        <Text style={styles.value}>{value}</Text>
        <Text style={styles.caret}>▾</Text>
      </TouchableOpacity>
      <Modal visible={open} transparent animationType="fade">
        <TouchableOpacity
          style={styles.backdrop}
          activeOpacity={1}
          onPress={() => setOpen(false)}
        >
          <View style={styles.sheet}>
            <FlatList
              data={options}
              keyExtractor={(o) => o}
              renderItem={({ item }) => (
                <TouchableOpacity
                  style={styles.opt}
                  onPress={() => {
                    onChange(item);
                    setOpen(false);
                  }}
                >
                  <Text
                    style={[styles.optText, item === value && styles.optActive]}
                  >
                    {item}
                  </Text>
                </TouchableOpacity>
              )}
            />
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { marginHorizontal: 12, marginVertical: 6 },
  label: { fontSize: 13, fontWeight: '600', color: '#2d6cdf', marginBottom: 4 },
  field: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 6,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  value: { fontSize: 14, color: '#333' },
  caret: { fontSize: 12, color: '#999' },
  backdrop: { flex: 1, backgroundColor: '#0006', justifyContent: 'center' },
  sheet: {
    backgroundColor: '#fff',
    marginHorizontal: 40,
    borderRadius: 8,
    maxHeight: 320,
  },
  opt: { paddingVertical: 14, paddingHorizontal: 18 },
  optText: { fontSize: 15, color: '#333' },
  optActive: { color: '#2d6cdf', fontWeight: '700' },
});
