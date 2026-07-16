# TopOn / AnyThink React Native SDK

`@anythink/react-native-sdk` — TopOn 广告 React Native 桥接层，支持激励视频、插屏、开屏、Banner、原生广告等。

## 目录说明


| 目录                | 说明                                                                       |
| ----------------- | ------------------------------------------------------------------------ |
| **根目录**           | SDK 源码（`src/`、`android/`、`ios/`）。集成到业务 App 时引用此包。                        |
| `example/`        | 官方 Demo，**React Native 0.85 + New Architecture**，与根目录 Yarn workspace 联动。 |
| `example-legacy/` | 低版本兼容 Demo，**React Native 0.72 + Paper（旧架构）**，独立安装依赖，与 `example/` 互不干扰。  |




---

## 一、集成 SDK 到业务工程

### 1. 安装依赖

将本仓库作为本地包或私有 npm 包引入：

```bash
# 方式 A：本地路径（开发 / 验收常用）
yarn add file:/path/to/taku_rn_project

# 方式 B：npm 发布后
yarn add @anythink/react-native-sdk
```

安装后需构建 JS 产物（`lib/`）：

```bash
cd /path/to/taku_rn_project
yarn install   # 会自动执行 yarn prepare → bob build
```



### 2. 原生配置

- **Android**：SDK 通过 React Native Autolinking 自动链接；按 TopOn 官方文档配置 `appId` / `appKey`、混淆、权限等。
- **iOS**：在业务工程 `ios/` 目录执行 `pod install`（会拉取 `ATReactNativeBridge` podspec）。



### 3. JS 初始化（最小示例）

```ts
import { ATSDK, ATAdEvents } from '@anythink/react-native-sdk';

// 建议在用户同意隐私协议后再初始化
ATSDK.setNetworkLogDebug(__DEV__);
ATAdEvents.init();
ATSDK.integrationChecking();
ATSDK.setPersonalizedAdStatus(ATSDK.PERSONALIZED); // 或 NONPERSONALIZED
ATSDK.init('your_app_id', 'your_app_key');
ATSDK.start();
```



### 4. 主要 API


| 模块                                          | 用途                   |
| ------------------------------------------- | -------------------- |
| `ATSDK`                                     | 初始化、GDPR、个性化、过滤、调试器等 |
| `ATRewardVideoAd` / `ATRewardVideoAutoAd`   | 激励视频                 |
| `ATInterstitialAd` / `ATInterstitialAutoAd` | 插屏                   |
| `ATSplashAd`                                | 开屏                   |
| `ATBannerView`                              | Banner               |
| `ATNative` / `NativeAd` / `ATNativeAdView`  | 原生广告（自渲染 / 模板）       |
| `ATAdEvents`                                | 全局广告事件监听             |


完整调用示例见 `example/src/` 与 `example-legacy/src/`（两 Demo UI 对齐）。

---



## 二、运行 `example`（RN 0.85 Demo）

**环境**：Node ≥ 22.11（见 `.nvmrc`）、Yarn 4、Android Studio / Xcode。

```bash
# 1. 根目录安装依赖（构建 SDK + 安装 example workspace）
yarn install

# 2. Android
cd example
yarn android

# 3. iOS（Mac）
cd example/ios && bundle install && bundle exec pod install && cd ..
yarn ios
```

也可在根目录：

```bash
yarn example start    # 启动 Metro
yarn example android
yarn example ios
```

Demo 配置在 `example/src/config/sdkConfig.ts`，请替换为你自己的 `appId` / `appKey` / 广告位 ID。

---



## 三、运行 `example-legacy`（RN 0.72 + Paper）

适用于仍在 **React Native 0.72.x + 旧架构（Paper）** 的业务工程对拍验收。

**注意**：

- 必须在 `example-legacy/` **单独**执行 `yarn install`，不要在根目录 workspace 里装（避免 RN 被提升到 0.85）。
- 先确保根目录已 `yarn prepare` 生成 `lib/`。
- Metro 必须从 `example-legacy` 启动；**8081 端口不能被** `example` **占用**，否则会出现 RN 版本不匹配。

```bash
# 1. 构建 SDK
cd /path/to/taku_rn_project
yarn install && yarn prepare

# 2. 安装 legacy Demo 依赖（仅本目录）
cd example-legacy
yarn install

# 3. iOS Pods
yarn pods

# 4. 启动 Metro
yarn start

# 5. 运行（另开终端）
yarn ios
# 或
yarn android   # 需 SDK android oldarch 已就绪
```

指定真机 UDID：

```bash
RN_IOS_UDID=<你的UDID> yarn ios
```

更多环境说明与已知问题见 `example-legacy/README.md`。

---



## 环境要求摘要


| 项目               | Node    | React Native | 架构       |
| ---------------- | ------- | ------------ | -------- |
| SDK + `example`  | ≥ 22.11 | 0.85         | New Arch |
| `example-legacy` | ≥ 18    | 0.72.5       | Paper    |




## License

MIT — 见 [LICENSE](./LICENSE)。