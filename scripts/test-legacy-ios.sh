#!/usr/bin/env bash
# RN 0.72.5 + iOS Paper 低版本冒烟（example-legacy，真机）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LEGACY="$ROOT/example-legacy"
UDID="${RN_IOS_UDID:-00008101-001609260AC0001E}"

echo "==> 低版本冒烟 example-legacy (RN 0.72.5 / Paper)"
echo "    设备 UDID: $UDID"

cd "$ROOT"
yarn prepare

cd "$LEGACY"
if [[ ! -d node_modules/react-native ]]; then
  echo "==> yarn install (example-legacy 独立依赖)"
  yarn install
fi

bash scripts/patch-rn72-boost.sh

export RCT_NEW_ARCH_ENABLED=0
export NO_FLIPPER=1
cd ios
PATH="/opt/homebrew/opt/ruby/bin:$PATH"
pod install
cd ..

echo "==> xcodebuild 真机构建"
cd ios
xcodebuild \
  -workspace ReactNativeSdkExampleLegacy.xcworkspace \
  -scheme ReactNativeSdkExampleLegacy \
  -configuration Debug \
  -destination "id=$UDID" \
  -derivedDataPath build \
  | tail -20

echo ""
echo "==> BUILD 完成。请另开终端："
echo "    cd example-legacy && yarn start"
echo "    然后在 Xcode 打开 ios/ReactNativeSdkExampleLegacy.xcworkspace 运行到真机"
echo "    或使用 ios-deploy / Xcode Run 安装已编好的 app"
echo "    确认 Init 页 SDK 版本与 [InitManager] legacy initSdk done 日志"
