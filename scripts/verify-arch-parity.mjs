#!/usr/bin/env node
/**
 * 双端方法集对齐校验。基准 = TS 真相源 NativeATBridge.ts。
 * 对账：Android oldarch(@ReactMethod) / newarch(@Override) / iOS .mm(RCT_EXPORT_*)。
 */
import { readFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');
const TS = join(ROOT, 'src/specs/NativeATBridge.ts');
const AND_NEW = join(
  ROOT,
  'android/src/newarch/java/com/anythink/reactnative/bridge/ATReactNativeBridgeModule.java'
);
const AND_OLD = join(
  ROOT,
  'android/src/oldarch/java/com/anythink/reactnative/bridge/ATReactNativeBridgeModule.java'
);
const AND_MAIN = join(
  ROOT,
  'android/src/main/java/com/anythink/reactnative/bridge/ATReactNativeBridgeModule.java'
);
const IOS = join(ROOT, 'ios/Classes/ATReactNativeBridge.mm');
const SYNC = new Set([
  'getSDKVersionName',
  'autoLoadRewardedVideoReady',
  'autoLoadInterstitialReady',
]);

/** @param {string} src */
function tsMethods(src) {
  const set = new Set();
  const iface = src.match(/export interface Spec[\s\S]*?\n\}/);
  if (!iface) return set;
  for (const m of iface[0].matchAll(/^\s{2,}(\w+)\s*\(/gm)) set.add(m[1]);
  return set;
}

// 框架重写/构造方法（非桥方法，对账时排除）
const JAVA_INTERNAL = new Set(['ATReactNativeBridgeModule', 'getName']);
/** @param {string} src */
function javaMethods(src) {
  const set = new Set();
  for (const m of src.matchAll(/public\s+[\w<>,\s\[\]?]+?\s+(\w+)\s*\(/g)) {
    if (!JAVA_INTERNAL.has(m[1])) set.add(m[1]);
  }
  return set;
}

/** @param {string} src */
function iosExportedMethods(src) {
  const set = new Set();
  const flat = src.replace(/\n/g, ' ');
  for (const m of flat.matchAll(
    /RCT_EXPORT(?:_BLOCKING_SYNCHRONOUS)?_METHOD\s*\(\s*(\w+)/g
  )) {
    set.add(m[1]);
  }
  return set;
}

/** @param {string} label @param {Set<string>} set @param {Set<string>} truth @param {string[]} errors */
function diff(label, set, truth, errors) {
  for (const name of truth) {
    if (!set.has(name)) errors.push(`[${label}] MISSING: ${name}`);
  }
  for (const name of set) {
    if (!truth.has(name)) errors.push(`[${label}] EXTRA: ${name}`);
  }
}

const truth = tsMethods(readFileSync(TS, 'utf8'));
const errors = [];

if (existsSync(AND_OLD)) {
  const src = readFileSync(AND_OLD, 'utf8');
  const set = javaMethods(src);
  const flat = src.replace(/\n/g, ' ');
  diff('android-oldarch', set, truth, errors);
  for (const name of set) {
    if (!new RegExp(`@ReactMethod[^}]*?\\b${name}\\s*\\(`).test(flat)) {
      errors.push(`[android-oldarch] NO @ReactMethod: ${name}`);
    }
    if (
      SYNC.has(name) &&
      !new RegExp(`isBlockingSynchronousMethod[^;]*?\\b${name}\\s*\\(`).test(
        flat
      )
    ) {
      errors.push(
        `[android-oldarch] SYNC missing isBlockingSynchronousMethod: ${name}`
      );
    }
  }
} else {
  console.log('  (android-oldarch: skip — file not present)');
}

const andNewPath = existsSync(AND_NEW) ? AND_NEW : AND_MAIN;
if (existsSync(andNewPath)) {
  diff('android-newarch', javaMethods(readFileSync(andNewPath, 'utf8')), truth, errors);
} else {
  console.log('  (android-newarch: skip — file not present)');
}

if (existsSync(IOS)) {
  const src = readFileSync(IOS, 'utf8');
  const set = iosExportedMethods(src);
  diff('ios', set, truth, errors);
  const flat = src.replace(/\n/g, ' ');
  for (const name of set) {
    if (SYNC.has(name)) {
      if (
        !new RegExp(
          `RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD\\s*\\(\\s*${name}\\b`
        ).test(flat)
      ) {
        errors.push(
          `[ios] SYNC needs RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD: ${name}`
        );
      }
    } else if (
      !new RegExp(`RCT_EXPORT_METHOD\\s*\\(\\s*${name}\\b`).test(flat)
    ) {
      errors.push(`[ios] NO RCT_EXPORT_METHOD: ${name}`);
    }
  }
  if (!src.includes('RCT_EXPORT_MODULE()')) {
    errors.push('[ios] missing RCT_EXPORT_MODULE()');
  }
} else {
  console.log('  (ios: skip — file not present)');
}

if (errors.length) {
  console.error(`❌ arch parity FAILED (${errors.length}):`);
  errors.forEach((e) => console.error('  - ' + e));
  process.exit(1);
}
console.log(
  `✅ arch parity OK: all ${truth.size} truth-source methods covered across present ends.`
);
