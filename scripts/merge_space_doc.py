#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""One-shot merge DESIGN.md + api-dictionary.md → SPACE doc.

Canonical doc after merge: docs/SPACE-TopOn-ReactNative-SDK技术设计.md
Edit that file directly. Re-run only if DESIGN.md / api-dictionary.md full sources are restored.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs"

HEADER = """# 【SPACE】TopOn React Native SDK 技术设计

> 飞书 / Confluence 知识库 **SPACE 主文档**（整合技术设计 + 方法/字典/通道登记）。  
> 导入 SPACE 时：标题用「标题 1～3」、表格与代码块保持即可。

---

## 文档信息

| 属性 | 内容 |
|------|------|
| 文档类型 | 技术设计 |
| 所属 SPACE | 跨端 SDK / React Native |
| 产品 | TopOn · AnyThink · thinkup · secmtp |
| 仓库 | `up_sdk_react_native` |
| 原生 SDK（Android） | **6.5.73.3** |
| RN 插件版本 | **1.0.0** 起 |
| npm（anythink） | `@anythink/react-native-sdk` |
| RN 版本 | **≥ 0.76**（TurboModule + Fabric） |
| 语义基准 | Android `com.anythink.*.api` |
| 字典基准 | Flutter `Const.java` |
| 状态 | 设计中 |
| 版本 | **v3.0 SPACE** |

---

## 目录

- [一、背景与目标](#一背景与目标)
- [二、总体架构](#二总体架构)
- [三、完整项目结构](#三完整项目结构)
- [四、TypeScript 对外 API](#四typescript-对外-api全量)
- [五、事件与回调体系](#五事件与回调体系)
- [六、参数字典规范](#六参数字典规范概要)
- [七、TurboModule 桥方法概要](#七turbomodule-桥方法清单概要)
- [八、Android 桥接层](#八android-桥接层设计)
- [九、iOS 桥接层](#九ios-桥接层设计)
- [十、L1 与 L2 映射](#十l1-与-l2-映射)
- [十一、实例与生命周期](#十一实例与生命周期)
- [十二、三品牌交付](#十二三品牌交付概要)
- [十三、宿主集成](#十三宿主集成要点)
- [十四、端到端时序](#十四端到端时序)
- [十五、三品牌发布流程](#十五三品牌发布流程)
- [十六、登记主表](#十六登记主表原生模块与-fabric)
- [十七、修订记录](#十七修订记录)

---

"""

SECTION_MAP = [
    ("## 1. 目标与边界", "## 一、背景与目标"),
    ("## 2. 总体架构", "## 二、总体架构"),
    ("## 15. 完整项目结构", "## 三、完整项目结构"),
    ("## 3. TypeScript 对外 API（全量）", "## 四、TypeScript 对外 API（全量）"),
    ("## 4. 事件与回调体系", "## 五、事件与回调体系"),
    ("## 5. 参数字典规范", "## 六、参数字典规范（概要）"),
    ("## 6. TurboModule 桥方法清单（L2/L3）", "## 七、TurboModule 桥方法清单（概要）"),
    ("## 7. Android 桥接层设计", "## 八、Android 桥接层设计"),
    ("## 8. iOS 桥接层设计", "## 九、iOS 桥接层设计"),
    ("## 9. L1 ↔ L2 映射示例", "## 十、L1 与 L2 映射"),
    ("## 10. 实例与生命周期", "## 十一、实例与生命周期"),
    ("## 11. 三品牌交付", "## 十二、三品牌交付概要"),
    ("## 12. 宿主集成要点", "## 十三、宿主集成要点"),
    ("## 13. 端到端时序（激励加载展示）", "## 十四、端到端时序"),
    ("## 16. 三品牌发布流程", "## 十五、三品牌发布流程"),
    ("## 17. 登记规范", None),  # skip
    ("## 14. 修订记录", "## 十七、修订记录"),
]

API_SECTION_MAP = [
    ("## 1. 原生模块与通道注册（按品牌）", "## 十六、登记主表：原生模块与 Fabric"),
    ("## 2. Event 通道登记表（callName）", "## 十六、登记主表：Event 通道（callName）"),
    ("## 3. callbackName 全量登记", "## 十六、登记主表：callbackName 全量"),
    ("## 4. 参数字典 key 登记", "## 十六、登记主表：参数字典 key"),
    ("## 5. TurboModule 方法全量登记", "## 十六、登记主表：TurboModule 方法全量"),
    ("## 6. L1 ↔ L2 映射示例（登记格式）", "## 十六、登记主表：L1 ↔ L2 映射"),
    ("## 7. 代码内登记数组（Const.java 路由用）", "## 十六、登记主表：Const 路由数组"),
    ("## 8. 同步检查清单", "## 十六、登记主表：同步检查清单"),
]

REG_NOTE = """
---

## 十六、登记规范说明

1. 新增桥接能力 → 先在 **第十六节登记主表** 填表 → 再改代码  
2. 同步四处：`Const.java` / `Const.ts` / `ATRNConfiguration.h` / `specs/NativeATBridge.ts`  
3. 发版前：`scripts/sync-const-check.sh`

---

## 十七、修订记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v3.0 SPACE | 2026-06-01 | 合并 DESIGN + api-dictionary 为 SPACE 主文档 |
| v2.1 | 2026-06-01 | 项目结构、三品牌发布 |
| v2.0 | 2026-06-01 | 全量 API、双端桥接 |

"""


def transform_design(text: str) -> str:
    # drop old header
    idx = text.find("## 1. 目标与边界")
    if idx > 0:
        text = text[idx:]
    for old, new in SECTION_MAP:
        if new is None:
            start = text.find(old)
            if start >= 0:
                end = text.find("\n## ", start + 1)
                if end < 0:
                    end = len(text)
                text = text[:start] + text[end:]
        else:
            text = text.replace(old, new, 1)
    text = text.replace("完整目录见 **§15**", "完整目录见 **第三节**")
    text = text.replace("`docs/api-dictionary.md`", "本文第十六节")
    text = text.replace("见 §5.2", "见第六节 / 第十六节")
    # remove old revision if still present before reg note
    rev = "## 十七、修订记录"
    if rev in text:
        text = text[: text.index(rev)]
    return text.strip()


def transform_api(text: str) -> str:
    idx = text.find("## 1. 原生模块与通道注册（按品牌）")
    if idx < 0:
        idx = text.find("## 2. TurboModule 方法登记表")
    if idx < 0:
        return ""
    text = text[idx:]
    cut = text.find("\n## 9. 修订记录")
    if cut >= 0:
        text = text[:cut]
    for old, new in API_SECTION_MAP:
        text = text.replace(old, new, 1)
    return text.strip()


def main():
    design = (DOCS / "DESIGN.md").read_text(encoding="utf-8")
    api_path = DOCS / "api-dictionary.source.md"
    if not api_path.exists():
        api_path = DOCS / "api-dictionary.md"
    api = api_path.read_text(encoding="utf-8")
    body = transform_design(design)
    reg = transform_api(api)
    out = HEADER + body + "\n\n---\n\n" + reg + "\n" + REG_NOTE
    path = DOCS / "SPACE-TopOn-ReactNative-SDK技术设计.md"
    path.write_text(out, encoding="utf-8")
    print(f"OK: {path} ({len(out.splitlines())} lines)")


if __name__ == "__main__":
    main()
