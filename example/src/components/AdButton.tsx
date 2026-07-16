import { TouchableOpacity, Text, StyleSheet } from 'react-native';

/** 通用操作按钮（load / isReady / show / 调 API 等）。简单蓝底白字，可禁用。 */
export function AdButton({
  label,
  onPress,
  disabled,
  color = '#2d6cdf',
}: {
  label: string;
  onPress: () => void;
  disabled?: boolean;
  color?: string;
}) {
  return (
    <TouchableOpacity
      style={[
        styles.btn,
        { backgroundColor: color, opacity: disabled ? 0.5 : 1 },
      ]}
      onPress={onPress}
      disabled={disabled}
    >
      <Text style={styles.label}>{label}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  btn: {
    marginHorizontal: 12,
    marginVertical: 5,
    paddingVertical: 11,
    borderRadius: 6,
    alignItems: 'center',
  },
  label: { color: '#fff', fontSize: 14, fontWeight: '600' },
});
