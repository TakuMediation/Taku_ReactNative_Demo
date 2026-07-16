#!/usr/bin/env bash
# 统一入口：真机运行 example 或 example-legacy（需另开终端已 start:metro）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-example}"
UDID="${RN_IOS_UDID:-00008101-001609260AC0001E}"

case "$TARGET" in
  example|ex|0.85)
    echo "==> run-ios: example (RN 0.85)"
    cd "$ROOT/example"
    exec node ../node_modules/react-native/cli.js run-ios --udid "$UDID"
    ;;
  legacy|leg|0.72)
    echo "==> run-ios: example-legacy (RN 0.72 Paper)"
    cd "$ROOT/example-legacy"
    export RCT_NEW_ARCH_ENABLED=0
    exec node node_modules/react-native/cli.js run-ios --udid "$UDID"
    ;;
  *)
    echo "用法: ./scripts/run-ios.sh example|legacy"
    exit 1
    ;;
esac
