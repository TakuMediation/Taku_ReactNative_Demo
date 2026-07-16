import { useCallback, useEffect, useRef, useState } from 'react';
import { View, Text, FlatList, StyleSheet, AppState } from 'react-native';
import { ATNative, type NativeAd } from '@anythink/react-native-sdk';

/** FlatList onViewableItemsChanged 的 changed 项（RN 公共入口未导出 ViewToken，本地最小声明）。 */
type ViewableToken = { item: Row; isViewable: boolean };
import { TitleBar } from '../components/TitleBar';
import { ListNativeAd } from '../components/ListNativeAd';
import { useLogArea } from '../components/LogArea';
import { sdkConfig } from '../config/sdkConfig';
import type { ScreenProps } from '../navigation/screens';

/**
 * 广告列表。FlatList 内容条目间轮换插入模板 Native、自渲染 Native，
 * 下滑到底 onEndReached 加载更多。验滚动多广告不串位、不崩。
 *
 * （Banner 不放进列表：一个 placementId 的 banner 是单实例视图，列表里多个同位 banner
 * 会互相抢同一个原生 view 导致滑动时消失；banner 单独在 Banner 页验证。）
 */
type Row =
  | { kind: 'content'; id: string; n: number }
  | { kind: 'tplNative'; id: string }
  | { kind: 'selfNative'; id: string };

const PAGE = 8;

const AD_KINDS = ['tplNative', 'selfNative'] as const;

// 广告密度：每 AD_GAP 条内容插一个广告。太密（如每 2 条）会让多广告 cell 扎堆同时渲染、
// 抢资源致时序乱/空白；隔开后渲染不扎堆、体验更稳。
const AD_GAP = 6;

function makePage(start: number): Row[] {
  const out: Row[] = [];
  for (let i = start; i < start + PAGE; i++) {
    out.push({ kind: 'content', id: `c-${i}`, n: i });
    // 每 AD_GAP 条内容插一个广告，模板 / 自渲染 依次轮换。
    if (i % AD_GAP === AD_GAP - 1) {
      const adIndex = Math.floor(i / AD_GAP); // 第几个广告
      const kind = AD_KINDS[adIndex % AD_KINDS.length]!;
      out.push({ kind, id: `ad-${i}` });
    }
  }
  return out;
}

export function ListScreen({ onBack }: ScreenProps) {
  const { LogArea, append } = useLogArea();
  const [rows, setRows] = useState<Row[]>(() => makePage(0));
  // 下一页起始下标 + 防重入标志（onEndReached 会连续触发多次，避免重复追加导致 key 冲突）
  const nextStart = useRef(PAGE);
  const loadingRef = useRef(false);

  // 整个列表共用两个 ATNative（模板位/自渲染位各一个），各 cell 从对应缓存池取一条不同的 NativeAd。
  // 惰性初始化：ATNative 构造即注册全局订阅，写成 useRef(new ATNative(...)) 每次 render 都会 new、
  // 覆盖订阅并指向废弃实例；只在 current 为空时 new 一次。
  const tplRef = useRef<ATNative | null>(null);
  if (tplRef.current === null) {
    tplRef.current = new ATNative(sdkConfig.placements.nativeExpress ?? '');
  }
  const tplNative = tplRef.current;
  const selfRef = useRef<ATNative | null>(null);
  if (selfRef.current === null) {
    selfRef.current = new ATNative(sdkConfig.placements.nativeSelfRender ?? '');
  }
  const selfNative = selfRef.current;

  // 首次填池 + 卸载整体释放
  useEffect(() => {
    tplNative.makeAdRequest();
    selfNative.makeAdRequest();
    return () => {
      tplNative.dispose();
      selfNative.dispose();
    };
  }, [tplNative, selfNative]);

  // —— P2 生命周期：前台×可见 才 onResume —— //
  const liveAds = useRef(new Map<string, NativeAd>()); // cellId -> NativeAd（存活集合）
  const visibleCells = useRef(new Set<string>()); // 当前可见 cellId
  const appActive = useRef(true);

  const applyState = useCallback((cellId: string) => {
    const ad = liveAds.current.get(cellId);
    if (!ad) return;
    if (appActive.current && visibleCells.current.has(cellId)) {
      ad.onResume();
    } else {
      ad.onPause();
    }
  }, []);

  // 维度1：App 前后台 → 重算所有存活 ad
  useEffect(() => {
    const sub = AppState.addEventListener('change', (s) => {
      appActive.current = s === 'active';
      liveAds.current.forEach((_, cellId) => applyState(cellId));
    });
    return () => sub.remove();
  }, [applyState]);

  // 维度2：cell 可见性（FlatList）
  const onViewableItemsChanged = useRef(
    ({ changed }: { changed: ViewableToken[] }) => {
      changed.forEach((vt) => {
        const cellId = vt.item.id;
        if (vt.isViewable) visibleCells.current.add(cellId);
        else visibleCells.current.delete(cellId);
        applyState(cellId);
      });
    }
  ).current;

  const onAdReady = useCallback(
    (cellId: string, ad: NativeAd) => {
      liveAds.current.set(cellId, ad);
      applyState(cellId);
    },
    [applyState]
  );
  const onAdGone = useCallback((cellId: string) => {
    liveAds.current.delete(cellId);
    visibleCells.current.delete(cellId);
  }, []);

  const loadMore = useCallback(() => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    const start = nextStart.current;
    append(`onEndReached → 加载更多 (start=${start})`);
    setRows((prev) => [...prev, ...makePage(start)]);
    nextStart.current = start + PAGE;
    // 下一帧再放开，避免同一次滚动里重复触发
    requestAnimationFrame(() => {
      loadingRef.current = false;
    });
  }, [append]);

  const renderItem = useCallback(
    ({ item }: { item: Row }) => {
      switch (item.kind) {
        case 'content':
          return (
            <View style={styles.contentRow}>
              <Text style={styles.contentText}>内容条目 #{item.n}</Text>
            </View>
          );
        case 'tplNative':
          return (
            <ListNativeAd
              native={tplNative}
              cellId={item.id}
              mode="template"
              onEvent={(name) => append(`${item.id}(tpl): ${name}`)}
              onAdReady={onAdReady}
              onAdGone={onAdGone}
            />
          );
        case 'selfNative':
          return (
            <ListNativeAd
              native={selfNative}
              cellId={item.id}
              mode="self"
              onEvent={(name) => append(`${item.id}(self): ${name}`)}
              onAdReady={onAdReady}
              onAdGone={onAdGone}
            />
          );
        default:
          return null;
      }
    },
    [append, tplNative, selfNative, onAdReady, onAdGone]
  );

  return (
    <View style={styles.root}>
      <TitleBar title="广告列表" onBack={onBack} />
      <FlatList
        style={styles.list}
        data={rows}
        keyExtractor={(r) => r.id}
        renderItem={renderItem}
        onEndReached={loadMore}
        onEndReachedThreshold={0.4}
        onViewableItemsChanged={onViewableItemsChanged}
        // 广告 cell 是带副作用的重型原生视图：关掉裁剪回收、放大窗口，
        // 避免滚动中被销毁重建导致反复 load / 来不及加载。
        removeClippedSubviews={false}
        windowSize={21}
        initialNumToRender={8}
        maxToRenderPerBatch={4}
      />
      <View style={styles.logSlot}>
        <LogArea />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },
  list: { flex: 2 },
  contentRow: {
    padding: 18,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  contentText: { fontSize: 15, color: '#333' },
  logSlot: {
      height: 200
    },
});
