# 任务：应用重排开始日期改为任意日期选择

## 任务目标

将排产工单页“应用重排开始日期”弹窗从“今天/明天”单选改为日期选择器，默认选中明天日期，并允许用户选择任意有效日期；确认后仍按所选日期整天重新预检、重新预览并写入正式排程。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本轮涉及 PowerShell 与中文文件读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，需遵循统一前端样式来源。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次仅调整弹窗控件，不做无关视觉重设计，保持运维控制台风格。
- 已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接将确认弹窗的数据模型从枚举单选改为日期值，并复用整天开始时间构造逻辑。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 应用重排默认从明天日期开始 -> Given 用户在排产工单页已满足应用重排前置条件 / When 点击“应用重排”打开开始日期弹窗 / Then 日期选择器默认显示明天日期，起排时间为明天 `00:00:00`。
- BDD: 应用重排可选择任意有效日期 -> Given 开始日期弹窗已打开 / When 用户在日期选择器中选择任意有效日期 / Then 当前选择日期与起排时间同步更新为该日期 `00:00:00`。
- BDD: 应用重排确认链路保持门禁 -> Given 用户确认所选开始日期 / When 点击“确认应用重排” / Then 前端继续按该日期重新执行预检、重新生成预览，并使用本次预览 token 调用应用接口。

## 里程碑

1. M1：建立任务文档与 RED 静态契约。`DONE`
2. M2：将弹窗单选改为日期选择器并默认明天。`DONE`
3. M3：运行聚焦静态与类型验证。`DONE`
4. M4：完善证据文档并执行收尾清理预览。`DONE`
5. M5：只提交本任务相关改动。`DONE`
6. M6：执行真实数据 E2E 验证。`BLOCKED`

## 预期验证

- RED：`node tests/e2e/mes-replan-whole-day-apply-static.spec.js` 先失败，证明旧页面仍使用“今天/明天”单选。
- GREEN：`node tests/e2e/mes-replan-whole-day-apply-static.spec.js` 通过。
- REGRESSION：应用重排超时、进度、权限与 scope 静态契约通过。
- TYPE：`pnpm ts:check:schedule` 通过或明确记录阻塞。

## 当前状态

已完成。应用重排开始日期弹窗已改为日期选择器并默认明天；聚焦静态契约、真实流脚本语法、相关回归契约、`pnpm ts:check:schedule`、前端证据校验和 task-closeout-cleanup 预览均已通过。本任务相关改动已单独提交。

追加真实数据 E2E 验证已执行登录与数据前置检查；本机测试租户 `测试租户/aoteman` 可进入 `/mes/pro/schedule-order`，但当前缺少可完成应用重排写入链路的真实排产数据，真实写入 E2E 按 fail fast 阻塞，未使用 mock、未绕过预检/预览/token 门禁。

## 验证结果

- RED：`node tests/e2e/mes-replan-whole-day-apply-static.spec.js` -> FAIL，失败原因符合预期：旧页面仍包含“从今天开始重排”单选按钮。
- GREEN：`node tests/e2e/mes-replan-whole-day-apply-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/mes-replan-whole-day-apply-real-flow.e2e.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-replan-apply-timeout-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-replan-settings-progress-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-replan-scope-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN：`pnpm ts:check:schedule` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260708-replan-apply-start-date-picker/frontend-feature-evidence.md` -> PASS。
- GREEN：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-replan-apply-start-date-picker --mode preview` -> PASS，无删除项。
- GREEN：`login-preflight` -> PASS，真实登录进入本机 `http://localhost:8081/mes/pro/schedule-order`，租户 `测试租户`，账号 `aoteman`，目标文案“手动重排”可见。
- BLOCKER：`node tests/e2e/mes-replan-whole-day-apply-real-flow.e2e.js` -> FAIL，测试租户没有可通过页面同步工单加入排产池的真实生产工单。
- BLOCKER：真实数据诊断 -> 排产候选 34 条，抽查前 25 条均因“工艺路线已被禁用”或“缺少可用工艺路线”无法通过预检/预览；`admission-diff?admissionStatus=READY_TO_ADMIT` 返回 `total=0`。证据文件：`tests/output/replan-date-data-diagnostic/diagnostic.json`。

## Cleanup Keep

- `doc/tasks/20260708-replan-apply-start-date-picker/frontend-feature-evidence.md`
