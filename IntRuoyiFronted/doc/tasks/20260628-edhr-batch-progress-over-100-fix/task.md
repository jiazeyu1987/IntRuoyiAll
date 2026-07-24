# 任务：eDHR 批次执行完成进度超过 100% 前端修复

## 任务目标

- 修复 `src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue` 中批次完成进度可能超过 `100%` 的问题。
- 让列表页百分比与详情页 `任务进度` 文案都只统计“绑定批记录模板的必填任务”。
- 保持现有 API 合同、页面布局、状态标签和关闭逻辑不变，不引入临时兜底分支。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-schedule-calendar-past-card-gray\task.md`
- 状态：`COMPLETED`
- 处理说明：上一前端任务已完成；当前工作区虽存在其他未提交排程/ SRM 改动，但本次只处理 eDHR 批次进度口径问题，不混入其他页面变更。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 前端页面保持 IntPP 运维台样式，完成进度列继续使用紧凑百分比 + 细进度条表现。
  - PowerShell 读取和记录中文内容时必须显式使用 UTF-8。
  - 本轮先做静态/脚本回归；如需真实 E2E，再单独补 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过统一模板必填任务完成数与总数口径，消除特殊节点混入导致的进度失真。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 批次完成进度不把特殊节点计入模板进度 -> Given 批次包含必填批记录任务和必填特殊节点 / When 列表页展示完成进度 / Then 进度百分比只由已完成模板必填任务数除以模板必填任务总数得出，不能超过 100%。`
- `BDD: 批次详情任务进度文案与列表口径一致 -> Given 批次详情包含特殊节点 / When 查看“任务进度”描述 / Then 文案只显示模板必填任务完成数与模板必填任务总数。`

## 里程碑

1. M1：补任务文档与执行日志。`COMPLETED`
2. M2：新增 RED 回归测试，覆盖特殊节点混入口径场景。`COMPLETED`
3. M3：最小化修复进度 helper 与展示文案。`COMPLETED`
4. M4：运行定向验证、回填 evidence 并做 closeout 预览。`COMPLETED`

## 预期验证

- `node scripts/edhr-batch-required-progress.test.mjs`
- `node tests/e2e/edhr-batch-template-preview-static.spec.js`
- `pnpm exec eslint src/views/mes/pro/edhr-batch/progress.ts src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue scripts/edhr-batch-required-progress.test.mjs`

## 当前阻塞

- 无。`2026-06-29` 用户明确要求继续后，本任务已恢复并完成收口。

## 最终验证结果

- `node scripts/edhr-batch-required-progress.test.mjs` -> PASS
- `node tests/e2e/edhr-batch-template-preview-static.spec.js` -> PASS
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/edhr-batch/progress.ts src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue scripts/edhr-batch-required-progress.test.mjs` -> PASS
