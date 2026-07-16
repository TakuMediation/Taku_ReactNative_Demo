#!/usr/bin/env python3
"""One-off tidy for SPACE design doc: section order, numbering, dedupe, drop changelog."""
from __future__ import annotations

import re
from pathlib import Path

DOC = Path(__file__).resolve().parents[1] / "docs/SPACE-TopOn-ReactNative-SDK技术设计.md"


def split_major_sections(text: str) -> list[tuple[str, str]]:
    parts = re.split(r"(?=^## )", text, flags=re.M)
    out: list[tuple[str, str]] = []
    for p in parts:
        p = p.rstrip("\n")
        if not p.strip():
            continue
        m = re.match(r"^## (.+)$", p, re.M)
        title = m.group(1).strip() if m else "(preamble)"
        out.append((title, p))
    return out


def merge_section_16(blocks: list[str]) -> str:
    """Merge multiple '## 十六、登记主表：…' into one section with ### 16.x."""
    titles = [
        ("原生模块与 Fabric", "16.1"),
        ("Event 通道（callName）", "16.2"),
        ("callbackName 全量", "16.3"),
        ("参数字典 key", "16.4"),
        ("TurboModule 方法全量", "16.5"),
        ("L1 ↔ L2 映射", "16.6"),
        ("Const 路由数组", "16.7"),
        ("同步检查清单", "16.8"),
    ]
    merged_body: list[str] = []
    spec_re = re.compile(r"^## 十六、登记主表：(.+)$", re.M)
    spec_blocks: list[tuple[str, str]] = []
    for b in blocks:
        m = spec_re.match(b)
        if m:
            spec_blocks.append((m.group(1).strip(), b))
        else:
            merged_body.append(b)

    if not spec_blocks:
        return "\n\n".join(blocks)

    header = (
        "## 十六、登记主表（实现对照）\n\n"
        "> 发版与 Codegen 的**全量**登记表；正文第五～七节为概要，此处为权威明细。"
        " 新增能力须先改本节再改代码。\n"
    )
    sections_out: list[str] = []
    for (subtitle, num) in titles:
        body = None
        for st, raw in spec_blocks:
            if st == subtitle or st.startswith(subtitle.split("（")[0]):
                body = spec_re.sub("", raw, count=1).strip()
                break
        if body is None:
            continue
        # Renumber inner ### N.M under this appendix chunk → #### (keep tables)
        body = re.sub(r"^### (\d+)\.(\d+) ", r"#### \1.\2 ", body, flags=re.M)
        sections_out.append(f"### {num} {subtitle}\n\n{body}")

    # 登记规范说明
    norm = [b for b in merged_body if "登记规范说明" in b]
    if norm:
        t = norm[0]
        t = re.sub(r"^## 十六、登记规范说明\s*\n", "", t).strip()
        sections_out.append(f"### 16.9 登记规范\n\n{t}")
        merged_body = [b for b in merged_body if b not in norm]

    return header + "\n\n---\n\n".join(sections_out) + ("\n\n---\n\n" + "\n\n".join(merged_body) if merged_body else "")


def renumber_subsections(text: str) -> str:
    """Fix wrong ### numbers under each major ## section."""
    replacements = [
        # §三
        (r"(?<=## 三、完整项目结构\n(?:.*?\n)*?)### 15\.(\d+)", r"### 3.\1"),
        # §四 ad classes 3.x → 4.x (only after 4.4)
        (r"(?<=### 4\.4 首版不暴露.*?\n(?:.*?\n)*?)### 3\.(\d+)", r"### 4.\1"),
        # But 3.3 should become 4.5: map 3.3→4.5, 3.4→4.6, ... 3.10→4.12
    ]
    # Simpler line-by-line mapping for §四
    sec4_map = {
        "### 3.3 ": "### 4.5 ",
        "### 3.4 ": "### 4.6 ",
        "### 3.5 ": "### 4.7 ",
        "### 3.6 ": "### 4.8 ",
        "### 3.7 ": "### 4.9 ",
        "### 3.8 ": "### 4.10 ",
        "### 3.9 ": "### 4.11 ",
        "### 3.10 ": "### 4.12 ",
    }
    for old, new in sec4_map.items():
        text = text.replace(old, new)

    text = re.sub(r"^### 15\.(\d+) ", r"### 3.\1 ", text, flags=re.M)

    # §五
    in_five = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 五、"):
            in_five = True
        elif line.startswith("## 六、"):
            in_five = False
        elif in_five and line.startswith("### 4."):
            line = "### 5." + line[6:]
        out.append(line)
    text = "\n".join(out)

    # §六 5.x → 6.x
    in_six = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 六、"):
            in_six = True
        elif line.startswith("## 七、"):
            in_six = False
        elif in_six and line.startswith("### 5."):
            line = "### 6." + line[6:]
        out.append(line)
    text = "\n".join(out)

    # §七 6.x → 7.x
    in_seven = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 七、"):
            in_seven = True
        elif line.startswith("## 八、"):
            in_seven = False
        elif in_seven and line.startswith("### 6."):
            line = "### 7." + line[6:]
        out.append(line)
    text = "\n".join(out)

    # §八 7.x → 8.x
    in_eight = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 八、"):
            in_eight = True
        elif line.startswith("## 九、"):
            in_eight = False
        elif in_eight and line.startswith("### 7."):
            line = "### 8." + line[6:]
        out.append(line)
    text = "\n".join(out)

    # §九 8.x → 9.x
    in_nine = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 九、"):
            in_nine = True
        elif line.startswith("## 十、"):
            in_nine = False
        elif in_nine and line.startswith("### 8."):
            line = "### 9." + line[6:]
        out.append(line)
    text = "\n".join(out)

    # §十三 12.x → 13.x
    in_13 = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 十三、"):
            in_13 = True
        elif line.startswith("## 十四、"):
            in_13 = False
        elif in_13 and re.match(r"^### 12\.\d+ ", line):
            line = re.sub(r"^### 12\.", "### 13.", line)
        out.append(line)
    text = "\n".join(out)

    # §十五 16.x → 15.x
    in_15 = False
    lines = text.split("\n")
    out = []
    for line in lines:
        if line.startswith("## 十五、"):
            in_15 = True
        elif line.startswith("## 十六、"):
            in_15 = False
        elif in_15 and re.match(r"^### 16\.\d+ ", line):
            line = re.sub(r"^### 16\.", "### 15.", line)
        out.append(line)
    text = "\n".join(out)

    return text


def shorten_section_five(text: str) -> str:
    """Replace duplicate callbackName tables with pointer to §16.3."""
    start = text.find("## 五、事件与回调体系")
    end = text.find("## 六、参数字典规范")
    if start < 0 or end < 0:
        return text
    head = text[:start]
    tail = text[end:]
    mid = text[start:end]
    marker = "### 5.3 callbackName 全表"
    js_marker = "### 5.4 JS 订阅"
    i0 = mid.find(marker)
    i1 = mid.find(js_marker)
    if i0 < 0 or i1 < 0:
        return text
    before = mid[: i0 + len(marker)]
    after = mid[i1:]
    bridge = (
        "\n\n"
        "callbackName 与 Android Listener 的完整对照见 **§16.3**（与 Flutter `Const.java` 一致）。"
        " 业务侧通过 `setAdListener` 接收的 TS 方法名见第四节各 `*Listener` 接口。\n\n"
    )
    return head + before + bridge + after + tail


def fix_tree_and_meta(text: str) -> str:
    text = text.replace(
        "| 版本 | **v3.6 SPACE** |",
        "| 状态 | 设计中（以仓库 `docs/SPACE-TopOn-ReactNative-SDK技术设计.md` 为准） |",
    )
    # Remove duplicate 版本 row if we added 状态 wrong - read doc info table
    text = re.sub(
        r"\| 版本 \| \*\*v3\.\d+ SPACE\*\* \|\n",
        "",
        text,
    )
    text = text.replace(
        "├── docs/\n│   ├── DESIGN.md\n│   ├── api-dictionary.md             # 方法 / 字典 / 通道登记主表",
        "├── docs/\n│   ├── SPACE-TopOn-ReactNative-SDK技术设计.md  # 主文档\n│   ├── IMPLEMENTATION-PLAN.md\n│   ├── MULTI-BRAND-DELIVERY.md",
    )
    text = re.sub(
        r"│           ├── autoload/\n│           │   ├── ATAutoLoadRewardHelper\.java\n│           │   └── ATAutoLoadInterstitialHelper\.java\n│           │   ├── AdRevenueListenerImpl\.java\n│           │   └── AdSourceListenerImpl\.java",
        "│           ├── autoload/\n│           │   ├── ATAutoLoadRewardHelper.java\n│           │   └── ATAutoLoadInterstitialHelper.java\n│           ├── listener/\n│           │   ├── AdRevenueListenerImpl.java\n│           │   └── AdSourceListenerImpl.java",
        text,
    )
    return text


def new_toc() -> str:
    return """## 目录

**设计正文（一～十五）** · **登记附录（十六）** · **类级约束（十七，实现前必读）**

| 节 | 内容 |
|----|------|
| [一](#一背景与目标) | 目标、npm、版本策略 |
| [二](#二总体架构) | 三层、双向通信 |
| [三](#三完整项目结构) | 目录树与依赖 |
| [四](#四typescript-对外-api对齐跨端最新) | L1 TS API |
| [五](#五事件与回调体系) | callName、payload、订阅 |
| [六](#六参数字典规范概要) | 字典 key 概要 |
| [七](#七turbomodule-桥方法清单概要) | 桥方法概要 |
| [八](#八android-桥接层设计) | Android L3 |
| [九](#九ios-桥接层设计) | iOS L3 |
| [十](#十l1-与-l2-映射) | L1↔L2 |
| [十一](#十一实例与生命周期) | 实例池 |
| [十二](#十二三品牌交付概要) | 三品牌 |
| [十三](#十三宿主集成要点) | Maven / Pod |
| [十四](#十四端到端时序) | 时序图 |
| [十五](#十五三品牌发布流程) | 发版步骤 |
| [十六](#十六登记主表实现对照) | **全量**登记（Codegen） |
| [十七](#十七类级设计与-ai-实现约束) | 类图、状态机、AI 规则 |

---

"""


def fix_cross_refs(text: str) -> str:
    text = text.replace("§4.6 起", "§4.5 起")
    text = text.replace("| §4.6 起 |", "| §4.5 起 |")
    text = text.replace("| §4.6 |", "| §4.6 |")  # AutoAd ok
    refs = {
        "| `ATRewardVideoAd` | load/show/listener | §4.6 起 |": "| `ATRewardVideoAd` | load/show/listener | §4.5 |",
        "| `ATRewardVideoAutoAd` | 静态 AutoLoad | §4.6 |": "| `ATRewardVideoAutoAd` | 静态 AutoLoad | §4.6 |",
        "| `ATInterstitial` / `ATInterstitialAutoAd` | 插屏 | §4.6 |": "| `ATInterstitial` / `ATInterstitialAutoAd` | 插屏 | §4.7、§4.8 |",
        "| `ATBannerView` / `ATBannerViewComponent` | Banner 命令式 + Fabric | §4.7 |": "| `ATBannerView` / `ATBannerViewComponent` | Banner | §4.9 |",
        "| `ATNative` / `NativeAd` / `ATNativeAdView` | 原生 | §4.8 |": "| `ATNative` / `NativeAd` | 原生 | §4.10 |",
        "| `ATSplashAd` | 开屏 | §4.9 |": "| `ATSplashAd` | 开屏 | §4.11 |",
        "| `AdError` / `ATAdInfo` | 回调参数类型 | §4.10、`src/types/` |": "| `AdError` / `ATAdInfo` | 类型 | §4.12 |",
    }
    for a, b in refs.items():
        text = text.replace(a, b)
    text = text.replace("见 §3", "见 §16.3")
    text = re.sub(
        r"initMethodNames\[\]\s+// §5\.1",
        "initMethodNames[]          // §16.5.1",
        text,
    )
    text = re.sub(
        r"rewardVideoMethodNames\[\]\s+// §5\.3",
        "rewardVideoMethodNames[]   // §16.5.3",
        text,
    )
    text = re.sub(
        r"interstitialMethodNames\[\]\s+// §5\.4",
        "interstitialMethodNames[]  // §16.5.4",
        text,
    )
    text = re.sub(
        r"bannerMethodNames\[\]\s+// §5\.5",
        "bannerMethodNames[]        // §16.5.5",
        text,
    )
    text = re.sub(
        r"nativeMethodNames\[\]\s+// §5\.6",
        "nativeMethodNames[]        // §16.5.6",
        text,
    )
    text = re.sub(
        r"splashMethodNames\[\]\s+// §5\.7",
        "splashMethodNames[]        // §16.5.7",
        text,
    )
    text = text.replace("| 本表 | `docs/api-dictionary.md` |", "| 本表 | 本文 **§16** |")
    text = text.replace("- [十八、修订记录](#十八修订记录)\n", "")
    return text


def update_preamble(text: str) -> str:
    text = re.sub(
        r"> 飞书 / Confluence 知识库 \*\*SPACE 主文档\*\*.*?\n",
        "> **SPACE 主文档**（技术设计 + 方法字典登记 + 类级 AI 约束）。实现前必读 **[§17](#十七类级设计与-ai-实现约束)**；Codegen 全量表见 **[§16](#十六登记主表实现对照)**。\n",
        text,
        count=1,
    )
    old_toc_end = text.find("---\n\n## 一、背景与目标")
    if old_toc_end < 0:
        return text
    toc_start = text.find("## 目录")
    if toc_start < 0:
        return text
    return text[:toc_start] + new_toc() + text[old_toc_end + 4 :]  # skip first ---


def main() -> None:
    raw = DOC.read_text(encoding="utf-8")
    raw = re.sub(r"\n## 十八、修订记录[\s\S]*$", "\n", raw)
    raw = re.sub(r"\n---\n\n---\n\n## 十六、登记规范说明", "\n\n## 十六、登记规范说明", raw)

    sections = split_major_sections(raw)
    preamble = sections[0][1] if sections[0][0] == "(preamble)" else ""
    by_title: dict[str, str] = {}
    for title, body in sections:
        if title == "(preamble)":
            preamble = body
            continue
        key = title.split("、")[0] if "、" in title else title
        by_title[title] = body

    order_keys = [
        "一、背景与目标",
        "二、总体架构",
        "三、完整项目结构",
        "四、TypeScript 对外 API（对齐跨端最新）",
        "五、事件与回调体系",
        "六、参数字典规范（概要）",
        "七、TurboModule 桥方法清单（概要）",
        "八、Android 桥接层设计",
        "九、iOS 桥接层设计",
        "十、L1 与 L2 映射",
        "十一、实例与生命周期",
        "十二、三品牌交付概要",
        "十三、宿主集成要点",
        "十四、端到端时序",
        "十五、三品牌发布流程",
    ]

    parts: list[str] = [preamble.rstrip()]
    for want in order_keys:
        for t, b in by_title.items():
            if want in t:
                parts.append(b)
                break

    sec16_chunks: list[str] = []
    sec17 = ""
    for t, b in by_title.items():
        if t.startswith("十六、"):
            sec16_chunks.append(b)
        elif t.startswith("十七、"):
            sec17 = b
        elif "登记规范说明" in t:
            sec16_chunks.append(b)

    if sec16_chunks:
        parts.append(merge_section_16(sec16_chunks))
    if sec17:
        parts.append(sec17.rstrip())

    text = "\n\n".join(parts) + "\n"
    text = renumber_subsections(text)
    text = shorten_section_five(text)
    text = fix_tree_and_meta(text)
    text = fix_cross_refs(text)
    text = update_preamble(text)

    DOC.write_text(text, encoding="utf-8")
    print(f"Wrote {DOC} ({len(text)} chars)")


if __name__ == "__main__":
    main()
