import { ATSDK } from '../core/ATSDK';
import ATReactNativeBridge from '../specs/NativeATBridge';
import { ATLog } from '../utils/ATLog';

jest.mock('react-native', () => ({
  NativeEventEmitter: jest.fn().mockImplementation(() => ({
    addListener: () => ({ remove: jest.fn() }),
  })),
}));

// 整模块 mock TurboModule（避免 getEnforcing 在测试环境抛错）。
// 用 Proxy 让任意方法名都返回一个 jest.fn（部分有返回值的下面单独覆盖）。
jest.mock('../specs/NativeATBridge', () => {
  const fns: Record<string, jest.Mock> = {};
  const handler: ProxyHandler<Record<string, jest.Mock>> = {
    get(target, prop: string) {
      if (!target[prop]) {
        target[prop] = jest.fn();
      }
      return target[prop];
    },
  };
  return { __esModule: true, default: new Proxy(fns, handler) };
});

// Proxy mock 后任意属性都是 jest.Mock。声明成 typeof 桥的 Mocked 类型（具名方法，属性访问非可选，
// 绕 noUncheckedIndexedAccess）；参数化用例里按字符串名取用 indexed 访问另走 callBridge。
const bridge = ATReactNativeBridge as unknown as Record<string, jest.Mock> &
  jest.Mocked<typeof ATReactNativeBridge>;
function callBridge(name: string): jest.Mock {
  const m = (ATReactNativeBridge as unknown as Record<string, jest.Mock>)[name];
  if (!m) {
    throw new Error(`bridge.${name} not mocked`);
  }
  return m;
}

beforeEach(() => {
  jest.clearAllMocks();
  // 有返回值的桥方法给默认 resolve
  bridge.getSDKVersionName.mockReturnValue('UA_6.5.73');
  bridge.getGDPRLevel.mockResolvedValue(2);
  bridge.checkIsEuTraffic.mockResolvedValue(true);
  bridge.isEUTraffic.mockResolvedValue(false);
  bridge.getArea.mockResolvedValue('cn');
  bridge.isCnSDK.mockResolvedValue(true);
  bridge.isNetworkLogDebug.mockResolvedValue(true);
});

describe('ATSDK 同步转发方法（dispatch → 桥）', () => {
  // [ATSDK 方法名, 调用参数, 期望命中的桥方法, 期望桥收到的参数]
  const cases: Array<[string, unknown[], string, unknown[]]> = [
    ['init', ['a', 'k'], 'initAnyThinkSDK', ['a', 'k']],
    ['start', [], 'start', []],
    ['setChannel', ['ch'], 'setChannelStr', ['ch']],
    ['initCustomMap', [{ x: 1 }], 'setCustomDataDic', [{ x: 1 }]],
    [
      'initPlacementCustomMap',
      ['p1', { y: 2 }],
      'setPlacementCustomData',
      ['p1', { y: 2 }],
    ],
    ['setGDPRUploadDataLevel', [1], 'setDataConsentSet', [1]],
    ['showGDPRConsentDialog', ['app'], 'showGDPRConsentDialog', ['app']],
    [
      'showGDPRConsentSecondDialog',
      ['app'],
      'showGDPRConsentSecondDialog',
      ['app'],
    ],
    ['showGdprAuth', [], 'showGdprAuth', []],
    [
      'deniedUploadDeviceInfo',
      ['imei', 'oaid'],
      'deniedUploadDeviceInfo',
      [['imei', 'oaid']],
    ],
    ['setPersonalizedAdStatus', [1], 'setPersonalizedAdStatus', [1]],
    [
      'setFilterAdSourceIdList',
      ['p1', ['s1']],
      'setFilterAdSourceIdList',
      ['p1', ['s1']],
    ],
    [
      'setFilterNetworkFirmIdList',
      ['p1', ['n1']],
      'setFilterNetworkFirmIdList',
      ['p1', ['n1']],
    ],
    [
      'setForbidNetworkFirmIdList',
      [['n1']],
      'setForbidNetworkFirmIdList',
      [['n1']],
    ],
    [
      'setForbidShowNetworkFirmIdList',
      ['p1', ['n1']],
      'setForbidShowNetworkFirmIdList',
      ['p1', ['n1']],
    ],
    [
      'setAllowedShowNetworkFirmIdList',
      ['p1', ['n1']],
      'setAllowedShowNetworkFirmIdList',
      ['p1', ['n1']],
    ],
    [
      'setRiskFilterNetworkFirmIdList',
      [2, ['n1']],
      'setRiskFilterNetworkFirmIdList',
      [2, ['n1']],
    ],
    [
      'setLocation',
      [{ latitude: 1, longitude: 2 }],
      'setLocation',
      [{ latitude: 1, longitude: 2 }],
    ],
    [
      'setDeviceInformationData',
      [{ d: 1 }],
      'setDeviceInformationData',
      [{ d: 1 }],
    ],
    ['integrationChecking', [], 'integrationChecking', []],
    ['setChannelSource', [3], 'setChannelSource', [3]],
    ['setLocalStrategyAssetPath', ['/p'], 'setLocalStrategyAssetPath', ['/p']],
    [
      'setSharedPlacementConfig',
      [{ c: 1 }],
      'setSharedPlacementConfig',
      [{ c: 1 }],
    ],
    ['removeFilters', [], 'removeFilters', []],
    [
      'removeFilterWithPlacementId',
      ['p1'],
      'removeFilterWithPlacementId',
      ['p1'],
    ],
    ['setAdSourcePrivacyPolicy', ['{}'], 'setAdSourcePrivacyPolicy', ['{}']],
    ['showDebuggerUI', ['key'], 'showDebuggerUI', ['key']],
  ];

  it.each(cases)('%s → 桥 %s', (method, args, bridgeMethod, bridgeArgs) => {
    const sdkFn = (
      ATSDK as unknown as Record<string, (...a: unknown[]) => unknown>
    )[method]!;
    sdkFn(...(args as unknown[]));
    expect(callBridge(bridgeMethod)).toHaveBeenCalledTimes(1);
    expect(callBridge(bridgeMethod)).toHaveBeenCalledWith(
      ...(bridgeArgs as unknown[])
    );
  });

  it('showDebuggerUI 默认空 debugKey', () => {
    ATSDK.showDebuggerUI();
    expect(callBridge('showDebuggerUI')).toHaveBeenCalledWith('');
  });
});

describe('ATSDK 有返回值方法（Promise 透传）', () => {
  it('getSDKVersionName', () => {
    expect(ATSDK.getSDKVersionName()).toBe('UA_6.5.73');
  });
  it('getGDPRDataLevel', async () => {
    await expect(ATSDK.getGDPRDataLevel()).resolves.toBe(2);
  });
  it('checkIsEuTraffic', async () => {
    await expect(ATSDK.checkIsEuTraffic()).resolves.toBe(true);
  });
  it('isEUTraffic', async () => {
    await expect(ATSDK.isEUTraffic()).resolves.toBe(false);
  });
  it('getArea', async () => {
    await expect(ATSDK.getArea()).resolves.toBe('cn');
  });
  it('isCnSDK', async () => {
    await expect(ATSDK.isCnSDK()).resolves.toBe(true);
  });
  it('isNetworkLogDebug', async () => {
    await expect(ATSDK.isNetworkLogDebug()).resolves.toBe(true);
  });
});

describe('ATSDK 特殊行为', () => {
  it('setNetworkLogDebug 同步 ATLog.enabled + 路由 setLogEnabled', () => {
    ATSDK.setNetworkLogDebug(true);
    expect(ATLog.enabled).toBe(true);
    expect(bridge.setLogEnabled).toHaveBeenCalledWith(true);
    ATSDK.setNetworkLogDebug(false);
    expect(ATLog.enabled).toBe(false);
  });

  it('getPersonalizedAdStatus 为 N/A，返回 UNKNOWN 且不打桥', async () => {
    await expect(ATSDK.getPersonalizedAdStatus()).resolves.toBe(ATSDK.UNKNOWN);
  });

  it('showGDPRConsentDialog 默认 appId 空串', () => {
    ATSDK.showGDPRConsentDialog();
    expect(bridge.showGDPRConsentDialog).toHaveBeenCalledWith('');
  });

  it('showGDPRConsentSecondDialog 默认 appId 空串', () => {
    ATSDK.showGDPRConsentSecondDialog();
    expect(bridge.showGDPRConsentSecondDialog).toHaveBeenCalledWith('');
  });

  it('putFilter 透传结构化对象', () => {
    const spec = { groups: [{ networkId: ['1'], biddingType: ['NORMAL'] }] };
    ATSDK.putFilter('p1', spec);
    expect(bridge.putFilter).toHaveBeenCalledWith('p1', spec);
  });

  it('setDeviceInformationData 透传 Android 性能数据字段', () => {
    const performanceData = {
      anr_count: 0,
      dynamic_score: 80,
      total_score: 90,
      is_high_consumption: false,
      anr_timestamps: '1700000000,1700000001',
      mmed_m_a_c: { sample: 1 },
    };
    ATSDK.setDeviceInformationData(performanceData);
    expect(callBridge('setDeviceInformationData')).toHaveBeenCalledTimes(1);
    expect(callBridge('setDeviceInformationData')).toHaveBeenCalledWith(
      performanceData
    );
  });

  it('setSharedPlacementConfig 透传共享位 localExtra 结构', () => {
    const config = {
      nativeLocalExtra: { ad_width: 1080, ad_height: 600 },
      bannerLocalExtra: { ad_width: 320, ad_height: 50 },
      rewardVideoLocalExtra: { user_id: 'u1' },
      interstitialLocalExtra: { scene: 'home' },
      splashLocalExtra: { timeout: 5000 },
    };
    ATSDK.setSharedPlacementConfig(config);
    expect(callBridge('setSharedPlacementConfig')).toHaveBeenCalledTimes(1);
    expect(callBridge('setSharedPlacementConfig')).toHaveBeenCalledWith(config);
  });

  it('常量值正确', () => {
    expect(ATSDK.PERSONALIZED).toBe(0);
    expect(ATSDK.NONPERSONALIZED).toBe(1);
    expect(ATSDK.UNKNOWN).toBe(2);
  });
});
