import * as React from 'react';
import { createContext, useMemo, useRef, useState } from 'react';
import type { View, Text, Image } from 'react-native';
import type { ATAdMaterial } from './types';

/**
 * 自渲染 Context。
 * 持广告素材 + 各 AssetView 的 ref；AssetView 组件从这里取素材自渲染、注册 ref。
 * 加载完成后 ATNativeAdView 调 setNativeAd(素材) → AssetView 自动填值。
 */
export type NativeAdContextType = {
  /** 当前广告素材（加载完成后由 ATNativeAdView 填）。 */
  nativeAd: ATAdMaterial;
  setNativeAd: (m: ATAdMaterial) => void;
  titleRef: React.RefObject<React.ComponentRef<typeof Text> | null>;
  descRef: React.RefObject<React.ComponentRef<typeof Text> | null>;
  ctaRef: React.RefObject<React.ComponentRef<typeof Text> | null>;
  iconRef: React.RefObject<React.ComponentRef<typeof Image> | null>;
  mainImageRef: React.RefObject<React.ComponentRef<typeof Image> | null>;
  mediaRef: React.RefObject<React.ComponentRef<typeof View> | null>;
  closeRef: React.RefObject<React.ComponentRef<typeof View> | null>;
  /** 文字类 AssetView 把自己 style 的 color 存这（type→#rrggbb），native 重建直接用。 */
  textColors: React.RefObject<Record<string, string>>;
};

export const NativeAdContext = createContext<NativeAdContextType>(
  {} as NativeAdContextType
);

export function NativeAdProvider({ children }: { children: React.ReactNode }) {
  const [nativeAd, setNativeAd] = useState<ATAdMaterial>({});
  const titleRef = useRef<React.ComponentRef<typeof Text> | null>(null);
  const descRef = useRef<React.ComponentRef<typeof Text> | null>(null);
  const ctaRef = useRef<React.ComponentRef<typeof Text> | null>(null);
  const iconRef = useRef<React.ComponentRef<typeof Image> | null>(null);
  const mainImageRef = useRef<React.ComponentRef<typeof Image> | null>(null);
  const mediaRef = useRef<React.ComponentRef<typeof View> | null>(null);
  const closeRef = useRef<React.ComponentRef<typeof View> | null>(null);
  const textColors = useRef<Record<string, string>>({});

  const value = useMemo<NativeAdContextType>(
    () => ({
      nativeAd,
      setNativeAd,
      titleRef,
      descRef,
      ctaRef,
      iconRef,
      mainImageRef,
      mediaRef,
      closeRef,
      textColors,
    }),
    [nativeAd]
  );

  return (
    <NativeAdContext.Provider value={value}>
      {children}
    </NativeAdContext.Provider>
  );
}
