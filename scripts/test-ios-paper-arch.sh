#!/usr/bin/env bash
# 在当前 example（RN 0.85）上测试 iOS Paper 旧架构，仅真机，不用模拟器。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXAMPLE_IOS="$ROOT/example/ios"
UDID="${RN_IOS_UDID:-00008101-001609260AC0001E}"

echo "==> iOS Paper 旧架构冒烟（example / RN 0.85 / RCT_NEW_ARCH_ENABLED=0）"
echo "    设备 UDID: $UDID"

cd "$EXAMPLE_IOS"
export RCT_NEW_ARCH_ENABLED=0
PATH="/opt/homebrew/opt/ruby/bin:$PATH"
pod install

cd "$ROOT/example"
/opt/homebrew/bin/node ../node_modules/react-native/cli.js run-ios \
  --udid "$UDID" \
  --mode Debug

echo "==> 完成。请在 Init 页确认 SDK 版本与初始化日志。"
