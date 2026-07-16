#!/usr/bin/env python3
"""Reorder SPACE doc sections by implementation importance; renumber 一～十七."""
from __future__ import annotations

import re
from pathlib import Path

DOC = Path(__file__).resolve().parents[1] / "docs/SPACE-TopOn-ReactNative-SDK技术设计.md"

# old Chinese numeral prefix in ## title -> new numeral
ORDER = [
    "一",   # 背景
    "二",   # 架构
    "十七", # 类级 -> 三
    "四",   # TS API
    "五",   # 事件
    "六",   # 字典
    "七",   # TurboModule
    "十四", # 时序 -> 八
    "十",   # L1/L2 -> 九
    "十一", # 生命周期 -> 十
    "八",   # Android -> 十一
    "九",   # iOS -> 十二
    "三",   # 项目结构 -> 十三
    "十二", # 三品牌 -> 十四
    "十三", # 宿主 -> 十五
    "十五", # 发版 -> 十六
    "十六", # 登记 -> 十七
]

NEW_NUMERALS = [
    "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
    "十一", "十二", "十三", "十四", "十五", "十六", "十七",
]

# old section numeral -> new (for ### renumber and § refs)
OLD_TO_NEW: dict[str, str] = {}
for old, new in zip(ORDER, NEW_NUMERALS):
    OLD_TO_NEW[old] = new


def split_sections(text: str) -> tuple[str, dict[str, str]]:
    parts = re.split(r"(?=^## )", text, flags=re.M)
    preamble = ""
    sections: dict[str, str] = {}
    for p in parts:
        if not p.strip():
            continue
        m = re.match(r"^## ([一二三四五六七八九十]+)、(.+)$", p, re.M)
        if not m:
            preamble = p
            continue
        num, rest = m.group(1), m.group(2)
        sections[num] = p
    return preamble, sections


def renumber_section_body(body: str, old_num: str, new_num: str) -> str:
    body = re.sub(
        rf"^## {re.escape(old_num)}、",
        f"## {new_num}、",
        body,
        count=1,
        flags=re.M,
    )
    if old_num == new_num:
        return body

    def sub_heading(m: re.Match) -> str:
        prefix, sec, sub = m.group(1), m.group(2), m.group(3)
        if sec != old_num:
            return m.group(0)
        return f"{prefix}{new_num}.{sub} "

    body = re.sub(
        r"^(#{2,4}) " + re.escape(old_num) + r"\.(\d+(?:\.\d+)?) ",
        sub_heading,
        body,
        flags=re.M,
    )
    # #### 17.8.1 under ## 十七 -> ## 三 : fix #### 17.x -> #### 3.x
    if old_num != new_num:
        body = re.sub(
            rf"^(####) {re.escape(old_num)}\.(\d+(?:\.\d+)?) ",
            rf"\1 {new_num}.\2 ",
            body,
            flags=re.M,
        )
    return body


def patch_cross_refs(text: str) -> str:
    tokens: list[tuple[str, str]] = []
    for old, new in sorted(OLD_TO_NEW.items(), key=lambda x: -len(x[0])):
        if old == new:
            continue
        tok = f"@@SEC{old}@@"
        tokens.append((tok, f"§{new}"))
        text = text.replace(f"§{old}.", tok + ".")
        text = text.replace(f"§{old}", tok)
        text = text.replace(f"第{old}节", f"第{new}节")
        # anchors in links
        pass
    for tok, rep in tokens:
        text = text.replace(tok, rep)

    name_fixes = {
        "#十七类级设计与-ai-实现约束": "#三类级设计与-ai-实现约束",
        "#十六登记主表实现对照": "#十七登记主表实现对照",
        "#三完整项目结构": "#十三完整项目结构",
        "#十四端到端时序": "#八端到端时序",
        "#十五三品牌发布流程": "#十六三品牌发布流程",
        "#八android-桥接层设计": "#十一android-桥接层设计",
        "#九ios-桥接层设计": "#十二ios-桥接层设计",
        "#十三宿主集成要点": "#十五宿主集成要点",
        "#十二三品牌交付概要": "#十四三品牌交付概要",
        "#十l1-与-l2-映射": "#九l1-与-l2-映射",
        "#十一实例与生命周期": "#十实例与生命周期",
    }
    for a, b in name_fixes.items():
        text = text.replace(a, b)
    return text


def new_toc_block() -> str:
    return """## 目录

章节按**实现与接入优先级**排列（P0 先读）。Codegen 全量表见 **[§17](#十七登记主表实现对照)**。

| 优先 | 节 | 内容 |
|:----:|----|------|
| P0 | [一](#一背景与目标) | 目标、npm、版本 |
| P0 | [二](#二总体架构) | 三层、双向通信 |
| P0 | [三](#三类级设计与-ai-实现约束) | **类图、AI 规则（实现前必读）** |
| P0 | [四](#四typescript-对外-api对齐跨端最新) | **L1 TS API（含 ATSDK）** |
| P0 | [五](#五事件与回调体系) | callName、payload |
| P0 | [八](#八端到端时序) | 主链路时序 |
| P1 | [六](#六参数字典规范概要) | 字典 key 概要 |
| P1 | [七](#七turbomodule-桥方法清单概要) | 桥方法概要 |
| P1 | [九](#九l1-与-l2-映射) | L1↔L2 |
| P1 | [十](#十实例与生命周期) | Helper 实例池 |
| P1 | [十一](#十一android-桥接层设计) | Android L3 |
| P1 | [十二](#十二ios-桥接层设计) | iOS L3 |
| P2 | [十三](#十三完整项目结构) | 目录树 |
| P2 | [十四](#十四三品牌交付概要) | 三品牌 |
| P2 | [十五](#十五宿主集成要点) | Maven / Pod |
| P2 | [十六](#十六三品牌发布流程) | 发版 |
| 附录 | [十七](#十七登记主表实现对照) | 全量登记 |

---

"""


def main() -> None:
    raw = DOC.read_text(encoding="utf-8")
    preamble, sections = split_sections(raw)

    rebuilt: list[str] = [preamble.rstrip()]
    for old_num, new_num in zip(ORDER, NEW_NUMERALS):
        body = sections.get(old_num, "")
        if not body:
            raise SystemExit(f"Missing section: {old_num}")
        rebuilt.append(renumber_section_body(body, old_num, new_num).rstrip())

    text = "\n\n".join(rebuilt) + "\n"
    text = patch_cross_refs(text)

    # Update preamble § links
    text = text.replace(
        "**[§17](#十七类级设计与-ai-实现约束)**；Codegen 全量表见 **[§16](#十六登记主表实现对照)**",
        "**[§3](#三类级设计与-ai-实现约束)**；Codegen 全量表见 **[§17](#十七登记主表实现对照)**",
    )
    text = text.replace("实现前必读 **§17**", "实现前必读 **§3**")

    toc_start = text.find("## 目录")
    toc_end = text.find("---\n\n## 一、背景与目标")
    if toc_start >= 0 and toc_end >= 0:
        text = text[:toc_start] + new_toc_block() + text[toc_end + 4 :]

    DOC.write_text(text, encoding="utf-8")
    print("Reordered sections:", list(zip(ORDER, NEW_NUMERALS)))


if __name__ == "__main__":
    main()
