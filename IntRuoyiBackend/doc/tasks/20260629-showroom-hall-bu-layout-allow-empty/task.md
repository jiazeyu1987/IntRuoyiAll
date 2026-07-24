# 任务：展柜 BU 排版允许空商品

- Task ID: `20260629-showroom-hall-bu-layout-allow-empty`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

修复 `ShowroomApiRuntime.calculateHallBuCanvasLayout(...)` 在没有任何 `PRODUCT` 元素时错误抛出必填异常的问题；接口应允许只有奖项等非产品元素的展柜不做 BU 排版，并保持有产品时的现有排序规则。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-dcc-subtab-four-char-rename\task.md`
- 状态：`blocked`
- 处理说明：用户切换到 showroom 缺陷修复，上一后端任务已显式阻塞。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文日志与任务文档维护必须显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。修正 BU 自动排版接口对空产品输入的正式业务语义，避免继续把合法空排版误判为缺字段。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: hall BU 自动排版在没有产品时保持非产品布局 -> Given 展柜画布只有 AWARD 等非 PRODUCT 元素 / When 调用 calculateHallBuCanvasLayout / Then 不抛出 SHOWROOM_REQUIRED_FIELD_MISSING，并原样返回这些非产品元素。`
- `BDD: hall BU 自动排版在存在产品时仍按 BU 重排 -> Given 展柜画布包含多个 PRODUCT 元素 / When 调用 calculateHallBuCanvasLayout / Then 产品仍按 BU 分组后的顺序与网格布局重排。`

## Milestones

1. M1：建立后端任务文档与执行日志，定位现有测试和实现。`completed`
2. M2：先改测试并执行 RED。`completed`
3. M3：修改实现并执行 GREEN。`completed`
4. M4：更新证据并完成任务收尾。`completed`

## Expected Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeHallBuCanvasLayoutTest test`

## Current Blockers

- 无。

## Final Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeHallBuCanvasLayoutTest test`：PASS
