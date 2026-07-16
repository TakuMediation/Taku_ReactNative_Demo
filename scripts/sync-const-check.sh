#!/usr/bin/env bash
# Const.java / Const.ts 字典一致性门禁（Phase 1+ 启用）
# 语义：Const.ts 只含 RN 实际使用的子集，可省略 Flutter/Android 独有键；
#       但凡 Const.ts 里出现的字符串值，都必须在 Const.java 里存在（防拼写漂移）。
# 用法: bash scripts/sync-const-check.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_CONST="${ROOT}/android/src/main/java/com/anythink/reactnative/utils/Const.java"
TS_CONST="${ROOT}/src/constants/Const.ts"

if [[ ! -f "${ANDROID_CONST}" ]]; then
  echo "SKIP: ${ANDROID_CONST} 不存在"; exit 0
fi
if [[ ! -f "${TS_CONST}" ]]; then
  echo "SKIP: ${TS_CONST} 不存在（Phase 1 后会有）"; exit 0
fi

# 提取所有字符串字面量值（Java 用双引号；TS 用单/双引号）
java_values() { grep -oE '"[^"]*"' "${ANDROID_CONST}" | tr -d '"' | sort -u; }
ts_values()   { grep -oE "'[^']*'|\"[^\"]*\"" "${TS_CONST}" | sed -E "s/^['\"]//;s/['\"]$//" | sort -u; }

TMP_J="$(mktemp)"; TMP_T="$(mktemp)"
trap 'rm -f "${TMP_J}" "${TMP_T}"' EXIT
java_values > "${TMP_J}"
ts_values   > "${TMP_T}"

# 找 Const.ts 里有、但 Const.java 里没有的值（= 漂移/拼写错）
missing="$(comm -23 "${TMP_T}" "${TMP_J}" || true)"

if [[ -z "${missing}" ]]; then
  echo "sync-const-check: OK — Const.ts 的所有字符串值都能在 Const.java 找到（无漂移）"
  exit 0
else
  echo "sync-const-check: FAIL — 以下 Const.ts 值在 Const.java 中不存在（请对齐，或在 Const.java 补上）：" >&2
  echo "${missing}" | sed 's/^/  - /' >&2
  exit 1
fi
