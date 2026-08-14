# 问题 4：交期风险明确提示

## Status

completed

## Goal

计划开工晚于最晚开工、计划完成晚于承诺交期时，列表直接显示风险类型和超期量，不再只改变日期颜色。

## Bug Summary

排产工单列表仅用日期颜色表示开工或交期风险，未直接说明风险类型和超期量。

## Expected Behavior

对应计划日期下方必须显示可读、可访问且可量化的开工或承诺交期风险提示，无风险行不显示提示。

## Approved Contract

- 计划开工风险按时间差显示分钟、小时或天。
- 承诺交期是日期字段，逾期量按日历日计算并显示“逾承诺交期 N 天”。
- 风险提示必须有可见文本、警告图标和辅助技术标签；无风险时不占用额外高度。

## Reproduction And Root Cause

- 复现路径：本机 `http://127.0.0.1:8081/mes/pro/schedule-order`，进入“排产工单”列表并查看“计划开工”“计划完成”列。
- 原因：旧页面只通过日期文字颜色表示风险，未渲染风险类型和超期量；用户不能仅凭颜色判断是开工风险还是承诺交期风险，也无法直接获知超期程度。
- 正式修复：`getStartRiskText` 按计划开工与最晚开工的精确时间差生成分钟/小时/天文案；`getDeliveryRiskText` 按计划完成日期与承诺日期的日历日差生成交期风险文案。两类文案直接显示在对应日期下方。

## BDD / TDD Evidence

- `BDD: 开工和交期风险可见 -> Given 计划开工晚于最晚开工或计划完成日期晚于承诺交期 / When 用户查看排产工单列表 / Then 对应计划时间下方显示具体超期文本和警告图标。`
- `RED: node tests\e2e\mes-schedule-order-delivery-risk-indicator-static.spec.js -> FAIL, 页面只有风险颜色，没有风险类型和超期量文本。`
- `GREEN: node tests\e2e\mes-schedule-order-delivery-risk-indicator-static.spec.js -> PASS。`
- 独立复核强化了同一合同中的可执行边界断言：开工风险覆盖 30 分钟、1 小时、1 天和准时无风险；交期风险覆盖承诺日当天无风险，以及用户示例 `2026-07-14 -> 2026-10-28 / 2026-12-14` 分别显示 `106 / 153 天`。

## Verification

- 计划开工晚于最晚开工时，计划开工日期下方显示“晚于最晚开工 N 分钟/小时/天”。
- 计划完成晚于承诺交期时，计划完成日期下方显示“逾承诺交期 N 天”；承诺交期按日期字段的日历日计算。
- 两类提示都有 `ep:warning-filled`、`role="status"` 和准确 `aria-label`；容器尺寸稳定、文本可换行。
- `node tests\e2e\mes-schedule-order-delivery-risk-indicator-static.spec.js` -> PASS。
- `pnpm ts:check:schedule` -> PASS。
- Playwright 真实只读页面：首屏检出开工风险 6 条、交期风险 7 条；所有风险均可见、位于日期下方、未越出单元格，`role="status"` 和 `aria-label` 与可见文本完全一致，最大提示高度 32px。
- 真实页面控制台：Errors 0，Warnings 0；本项只读检查未触发写操作。

## Regression Scope And Blockers

- 回归范围：排产工单主列表计划开工/计划完成两列、日期风险算法、可见换行和辅助技术文本；不修改排产计算、承诺交期保存、排序、筛选或权限。
- 当前阻塞：无。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；风险量由统一日期函数计算。
- 是否存在临时补丁或绕过：否。
