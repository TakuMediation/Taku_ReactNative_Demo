#!/usr/bin/env bash
# 统一入口：在 example (RN 0.85) 与 example-legacy (RN 0.72) 之间切换 Metro
# 用法: ./scripts/start-metro.sh example|legacy [--reset-cache]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-}"
shift || true

usage() {
  cat <<'EOF'
用法:
  yarn start:metro:example   # RN 0.85 → ReactNativeSdkExample
  yarn start:metro:legacy    # RN 0.72 → ReactNativeSdkExampleLegacy

或:
  ./scripts/start-metro.sh example [--reset-cache]
  ./scripts/start-metro.sh legacy  [--reset-cache]

切换 git / 切换版本后:
  1. 本脚本会自动 kill 占用 8081 的旧 Metro
  2. 首次 legacy 需在 example-legacy/ 执行过 yarn install
  3. 真机运行对应 App 后 Reload
EOF
}

kill_metro() {
  if lsof -ti :8081 >/dev/null 2>&1; then
    echo "==> 停止 8081 上的旧 Metro"
    lsof -ti :8081 | xargs kill -9 2>/dev/null || true
    sleep 0.5
  fi
}

start_example() {
  kill_metro
  echo "==> Metro: example (RN 0.85)"
  cd "$ROOT" && yarn prepare
  cd "$ROOT/example"
  exec node ../node_modules/react-native/cli.js start "$@"
}

start_legacy() {
  kill_metro
  local LEGACY="$ROOT/example-legacy"
  if [[ ! -f "$LEGACY/index.js" ]]; then
    echo "错误: example-legacy 缺少 index.js，请确认已 checkout 完整目录"
    exit 1
  fi
  echo "==> Metro: example-legacy (RN 0.72.5 Paper)"
  cd "$ROOT" && yarn prepare
  cd "$LEGACY"
  if [[ ! -d node_modules/react-native ]]; then
    echo "==> example-legacy 首次 yarn install"
    yarn install
  fi
  export NODE_OPTIONS="${NODE_OPTIONS:---preserve-symlinks}"
  exec node node_modules/react-native/cli.js start "$@"
}

case "$TARGET" in
  example|ex|0.85) start_example "$@" ;;
  legacy|leg|0.72) start_legacy "$@" ;;
  -h|--help|help) usage; exit 0 ;;
  "")
    usage
    exit 1
    ;;
  *)
    echo "未知目标: $TARGET"
    usage
    exit 1
    ;;
esac
