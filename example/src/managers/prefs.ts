import AsyncStorage from '@react-native-async-storage/async-storage';

/**
 * Demo 的本地持久化（隐私同意状态 + 个性化广告开关），关应用后保留。
 * 仅 example 演示用：键集中在此，避免散落字符串。
 */
const KEY_AGREED = 'demo.privacyAgreed';
const KEY_PERSONALIZED = 'demo.personalized';

export const Prefs = {
  async getAgreed(): Promise<boolean> {
    return (await AsyncStorage.getItem(KEY_AGREED)) === '1';
  },
  async setAgreed(v: boolean): Promise<void> {
    await AsyncStorage.setItem(KEY_AGREED, v ? '1' : '0');
  },
  async getPersonalized(): Promise<boolean> {
    return (await AsyncStorage.getItem(KEY_PERSONALIZED)) === '1';
  },
  async setPersonalized(v: boolean): Promise<void> {
    await AsyncStorage.setItem(KEY_PERSONALIZED, v ? '1' : '0');
  },
};
