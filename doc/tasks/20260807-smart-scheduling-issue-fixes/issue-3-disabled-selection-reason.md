# 问题 3：禁用复选框原因可见

## Status

completed

## Goal

排产工单因冻结、已完成或已取消而不能参与手动重排时，在不可被个性化配置隐藏的固定列直接显示“不可重排”原因，避免用户只能看到禁用复选框。

## Bug

部分排产工单的复选框被禁用，但列表没有直接说明禁用原因；若把说明放在可由用户隐藏的业务列中，问题仍会复现。

## Expected

选择框旁的固定“重排状态”列必须始终可见，并对每行明确显示“可重排”或“不可重排 + 具体原因”；禁用资格、选中集合过滤和原因展示必须复用同一正式门禁。

## Reproduction

- 静态复现：`node tests\e2e\mes-schedule-order-disabled-selection-reason-static.spec.js`。
- 真实页面：本机 `http://127.0.0.1:8081/mes/pro/schedule-order`，只读查看当前第一页冻结和已完成工单；未触发写请求。

## Root Cause

选择框原先只有禁用态，没有稳定的业务原因展示。把原因附着到工单编码等可配置列仍会受用户字段个性化隐藏影响，因此正式修复使用紧邻选择框的不可配置左侧固定列，并复用统一重排资格函数。

## Approved Contract

- 手动重排资格继续只排除冻结、已完成、已取消。
- `blockingIssueCount`、物料清单缺失和当前工序展示为空均不新增复选框门禁。
- 原因必须在列表直接可见，并提供辅助技术可读标签。

## BDD / TDD Evidence

- `BDD: 禁选原因可见 -> Given 排产工单因冻结、已完成或已取消不可参与手动重排 / When 用户在桌面或移动端查看该行且个性化配置隐藏了部分业务列 / Then 选择框旁的固定列直接显示“不可重排 + 具体原因”，禁用资格、选中集合过滤和显示原因来自同一门禁。`
- `RED: node tests\e2e\mes-schedule-order-disabled-selection-reason-static.spec.js -> FAIL, 工单编码单元格没有直接显示不可重排原因。`
- `RED: node tests\e2e\mes-schedule-order-frozen-state-static.spec.js -> FAIL, 相邻合同仍要求冻结判断内联在 selectable 函数中，无法识别统一门禁复用。`
- `RED: node tests\e2e\mes-schedule-order-replan-finished-disabled-static.spec.js -> FAIL, 相邻合同仍要求直接比较 row.status，无法识别 Number 归一化后的正式状态比较。`
- `GREEN: node tests\e2e\mes-schedule-order-disabled-selection-reason-static.spec.js -> PASS。`
- `GREEN: node tests\e2e\mes-schedule-order-frozen-state-static.spec.js -> PASS。`
- `GREEN: node tests\e2e\mes-schedule-order-replan-finished-disabled-static.spec.js -> PASS。`

## Verification

- 选择框旁的固定“重排状态”列显示 `可重排` 或 `不可重排 + 已冻结/已完成/已取消`；该列不经过字段个性化配置，原因与复选框共用 `isScheduleOrderReplanable` 和 `getScheduleOrderReplanBlockReason`。
- 状态比较统一经 `Number(row.status)` 归一化，避免数字字符串绕过禁选规则。
- 原因使用 `role="status"` 和具体 `aria-label`，并允许换行；固定列宽为 104px。
- 桌面真实页面：冻结行显示 `不可重排 / 已冻结`，已完成行显示 `不可重排 / 已完成`，对应复选框均为 disabled。
- 移动端真实页面（390x844）：冻结行状态框为 48x47px，文本完整可见，`aria-label=不可重排：已冻结`，对应复选框 disabled；`bodyScrollWidth=bodyClientWidth=390`，页面本体无横向溢出。
- 真实页面截图：`IntRuoyiFronted/output/playwright/20260807-smart-scheduling-issue-fixes/desktop-blocked-reason.png`、`issue3-mobile-blocked.png`。
- `node tests\e2e\mes-schedule-order-disabled-selection-reason-static.spec.js` -> PASS。
- `node tests\e2e\mes-schedule-order-frozen-state-static.spec.js` -> PASS。
- `node tests\e2e\mes-schedule-order-replan-finished-disabled-static.spec.js` -> PASS。
- `pnpm ts:check:schedule` -> PASS（exit 0）。
- `pnpm exec prettier --check tests/e2e/mes-schedule-order-disabled-selection-reason-static.spec.js tests/e2e/mes-schedule-order-frozen-state-static.spec.js tests/e2e/mes-schedule-order-replan-finished-disabled-static.spec.js` -> PASS。
- Playwright 当前页面 `console error` -> 0。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260807-smart-scheduling-issue-fixes/issue-3-disabled-selection-reason.md` -> PASS。

## Blockers

无。当前本机数据覆盖冻结和已完成行；已取消原因由同一状态门禁的静态合同覆盖，本轮未制造或修改业务数据。

## Risk / Regression Scope

- 风险仅限主排产工单表新增 104px 左侧固定状态列；桌面与 390px 移动视口均已核对。
- `blockingIssueCount`、生产用料清单缺失和当前工序为空不参与该选择门禁，未扩大业务限制。
- 相邻旧合同已改为核对统一门禁函数，避免通过复制条件制造分叉。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用正式手动重排资格函数。
- 是否存在临时补丁或绕过：否。
