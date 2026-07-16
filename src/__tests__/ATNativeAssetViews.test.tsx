import * as React from 'react';
import TestRenderer, { act } from 'react-test-renderer';

(
  globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }
).IS_REACT_ACT_ENVIRONMENT = true;

// 轻量 host 组件 mock，react-test-renderer 可渲染
jest.mock('react-native', () => {
  const ReactLib = require('react');
  const host = (name: string) =>
    ReactLib.forwardRef((props: Record<string, unknown>, ref: unknown) =>
      ReactLib.createElement(name, { ...props, ref }, props.children)
    );
  return {
    Platform: { OS: 'android' },
    View: host('View'),
    Text: host('Text'),
    Image: host('Image'),
    StyleSheet: { create: (s: unknown) => s, flatten: (s: unknown) => s },
  };
});

import { NativeAdContext } from '../nativead/NativeAdProvider';
import type { NativeAdContextType } from '../nativead/NativeAdProvider';
import {
  ATNativeTitleView,
  ATNativeDescView,
  ATNativeCtaView,
  ATNativeIconView,
  ATNativeMainImageView,
  ATNativeMediaView,
} from '../nativead/ATNativeAssetViews';
import type { ATAdMaterial } from '../nativead/types';

function ctx(material: ATAdMaterial): NativeAdContextType {
  const noopRef = { current: null };
  return {
    nativeAd: material,
    setNativeAd: jest.fn(),
    titleRef: noopRef,
    descRef: noopRef,
    ctaRef: noopRef,
    iconRef: noopRef,
    mainImageRef: noopRef,
    mediaRef: noopRef,
  } as unknown as NativeAdContextType;
}

function renderWith(material: ATAdMaterial, node: React.ReactElement) {
  let tr!: TestRenderer.ReactTestRenderer;
  act(() => {
    tr = TestRenderer.create(
      <NativeAdContext.Provider value={ctx(material)}>
        {node}
      </NativeAdContext.Provider>
    );
  });
  return tr!;
}

// 找出渲染树里某 host 类型的第一个节点
function findText(tr: TestRenderer.ReactTestRenderer): unknown {
  const t = tr.root.findAllByType('Text' as never);
  return t.length ? t[0]!.props.children : undefined;
}

describe('AssetView 文字组件从 Context 取素材', () => {
  it('TitleView 有素材时显示标题，无则空串', () => {
    expect(
      findText(renderWith({ title: '标题A' }, <ATNativeTitleView />))
    ).toBe('标题A');
    expect(findText(renderWith({}, <ATNativeTitleView />))).toBe('');
  });

  it('DescView 显示描述', () => {
    expect(findText(renderWith({ desc: '描述B' }, <ATNativeDescView />))).toBe(
      '描述B'
    );
    expect(findText(renderWith({}, <ATNativeDescView />))).toBe('');
  });

  it('CtaView 显示 CTA', () => {
    expect(findText(renderWith({ cta: '下载' }, <ATNativeCtaView />))).toBe(
      '下载'
    );
    expect(findText(renderWith({}, <ATNativeCtaView />))).toBe('');
  });
});

describe('AssetView 图片/媒体组件', () => {
  it('IconView 有 uri 时 source 带 uri，无则 undefined', () => {
    const withUri = renderWith(
      { appIcon: 'http://i.png' },
      <ATNativeIconView />
    );
    expect(withUri.root.findByType('Image' as never).props.source).toEqual({
      uri: 'http://i.png',
    });
    const noUri = renderWith({}, <ATNativeIconView />);
    expect(
      noUri.root.findByType('Image' as never).props.source
    ).toBeUndefined();
  });

  it('MainImageView 无 uri 时塌缩 (collapsed style)', () => {
    const withUri = renderWith(
      { mainImage: 'http://m.jpg' },
      <ATNativeMainImageView style={{ height: 160 }} />
    );
    expect(withUri.root.findByType('Image' as never).props.source).toEqual({
      uri: 'http://m.jpg',
    });
    const noUri = renderWith(
      {},
      <ATNativeMainImageView style={{ height: 160 }} />
    );
    // 无 uri：style 数组里含塌缩样式（height:0）
    const style = noUri.root.findByType('Image' as never).props.style as Array<
      Record<string, unknown>
    >;
    expect(JSON.stringify(style)).toContain('"height":0');
  });

  it('MediaView 按 isMediaViewAvailable 决定是否塌缩', () => {
    const avail = renderWith(
      { isMediaViewAvailable: true },
      <ATNativeMediaView style={{ height: 160 }} />
    );
    const availStyle = JSON.stringify(
      avail.root.findByType('View' as never).props.style
    );
    expect(availStyle).not.toContain('"height":0');

    const collapsed = renderWith(
      { isMediaViewAvailable: false },
      <ATNativeMediaView style={{ height: 160 }} />
    );
    const collapsedStyle = JSON.stringify(
      collapsed.root.findByType('View' as never).props.style
    );
    expect(collapsedStyle).toContain('"height":0');
  });
});
