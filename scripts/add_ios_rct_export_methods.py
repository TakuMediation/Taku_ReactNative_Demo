#!/usr/bin/env python3
"""Phase 7.2: wrap ATReactNativeBridge.mm method bodies with RCT_EXPORT_* macros."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MM = ROOT / "ios/Classes/ATReactNativeBridge.mm"

BLOCKING = {
    "getSDKVersionName",
    "autoLoadRewardedVideoReady",
    "autoLoadInterstitialReady",
}

# Methods that stay as plain ObjC (not exported to RN)
SKIP_PREFIXES = (
    "+ (BOOL)requiresMainQueueSetup",
    "+ (NSString *)moduleName",
    "- (dispatch_queue_t)methodQueue",
    "- (instancetype)init",
    "- (ATRewardVideoManager *)rewardMgr",
    "- (ATInterstitialManager *)interstitialMgr",
    "- (ATSplashAdManager *)splashMgr",
    "- (ATBannerAdManager *)bannerMgr",
    "- (ATNativeManager *)nativeMgr",
    "- (ATPlatformNativeManager *)platformNativeMgr",
    "- (void)emitEventName:",
    "- (std::shared_ptr",
)


def transform_methods(content: str) -> str:
    lines = content.splitlines(keepends=True)
    out: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.lstrip().startswith("- ("):
            out.append(line)
            i += 1
            continue

        if any(line.strip().startswith(p) for p in SKIP_PREFIXES):
            out.append(line)
            i += 1
            continue

        # Collect multi-line signature until '{'
        sig_lines = [line]
        i += 1
        while i < len(lines) and "{" not in sig_lines[-1]:
            sig_lines.append(lines[i])
            i += 1

        sig = "".join(sig_lines)
        m = re.match(
            r"-\s*\(([^)]+)\)\s*([\s\S]+?)\s*\{",
            sig,
            re.MULTILINE,
        )
        if not m:
            out.extend(sig_lines)
            continue

        ret_type = m.group(1).strip()
        selector = m.group(2).strip()
        method_base = selector.split(":")[0] if ":" in selector else selector

        if method_base in BLOCKING:
            macro = f"RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD({selector})"
        else:
            macro = f"RCT_EXPORT_METHOD({selector})"

        out.append(f"{macro}\n")
        out.append("{\n")
    return "".join(out)


def main() -> None:
    content = MM.read_text(encoding="utf-8")
    if "RCT_EXPORT_MODULE()" in content:
        print("already transformed")
        return

    # imports for old-arch RCTBridgeModule (harmless when unused)
    if "#import <React/RCTBridgeModule.h>" not in content:
        content = content.replace(
            "#import <React/RCTEventDispatcher.h>",
            "#import <React/RCTBridgeModule.h>\n#import <React/RCTEventDispatcher.h>",
        )

    # Fabric init block
    content = content.replace(
        "        // 显式把 Fabric ComponentView 注册到工厂（RN 0.85 不走 dlsym，详见顶部注释）。\n"
        "        // 这两次 (void)Cls() 同时起到强引用作用，防 ATRNBannerView.o / ATRNNativeAdView.o 被 dead-strip。\n"
        "        Class bannerCls = ATBannerViewCls();",
        "        // 显式把 Fabric ComponentView 注册到工厂（RN 0.85 不走 dlsym，详见顶部注释）。\n"
        "        // 这两次 (void)Cls() 同时起到强引用作用，防 ATRNBannerView.o / ATRNNativeAdView.o 被 dead-strip。\n"
        "#ifdef RCT_NEW_ARCH_ENABLED\n"
        "        Class bannerCls = ATBannerViewCls();",
    )
    content = content.replace(
        '            ATLog(@"ATReactNativeBridge.init registered Fabric component ATNativeADView (%@)", nativeCls);\n'
        "        }\n"
        "    }\n"
        "    return self;\n"
        "}",
        '            ATLog(@"ATReactNativeBridge.init registered Fabric component ATNativeADView (%@)", nativeCls);\n'
        "        }\n"
        "#endif\n"
        "    }\n"
        "    return self;\n"
        "}",
    )

    # getTurboModule ifdef
    content = content.replace(
        "- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:",
        "#ifdef RCT_NEW_ARCH_ENABLED\n"
        "- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:",
    )
    content = content.replace(
        "    return std::make_shared<facebook::react::NativeATBridgeSpecJSI>(params);\n"
        "}\n\n"
        "+ (NSString *)moduleName",
        "    return std::make_shared<facebook::react::NativeATBridgeSpecJSI>(params);\n"
        "}\n"
        "#endif\n\n"
        "+ (NSString *)moduleName",
    )

    content = content.replace(
        "@implementation ATReactNativeBridge\n\n"
        "// RCTBridgeModule 协议里 bridge / callableJSModules 是 @optional property，",
        "@implementation ATReactNativeBridge\n\n"
        "RCT_EXPORT_MODULE();\n\n"
        "// RCTBridgeModule 协议里 bridge / callableJSModules 是 @optional property，",
    )

    content = transform_methods(content)
    MM.write_text(content, encoding="utf-8")
    print(f"updated {MM.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
