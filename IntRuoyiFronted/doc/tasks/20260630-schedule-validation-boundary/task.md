# 任务：排产专用前端类型检查边界收敛

- Task ID: `20260630-schedule-validation-boundary`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

为排产链路建立正式的前端专用类型检查入口，让排产相关页面和 API 的类型校验不再被 eDHR 页面当前的无关报错阻塞，同时保留全仓 `ts:check` 作为完整校验入口。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-test-server-dcc-browser-cache-write-failure-followup\task.md`
- 状态：`blocked`
- 处理说明：用户已切换优先级到排产验证边界治理，上一前端任务已显式标记为阻塞暂停。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - JSON/TS/Vue/Markdown 统一显式 UTF-8，PowerShell 5.1 不使用 `&&`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式新增排产专用 tsconfig 与 npm script 收敛校验边界，不通过修改 eDHR 页面类型来掩盖问题。
- `是否存在临时补丁或绕过`：否。保留全量 `ts:check` 原样。

## BDD 场景

- `BDD: 排产专用类型检查只覆盖排产链路 -> Given 全仓 ts:check 会扫描所有 src 页面 / When 运行排产专用类型检查 / Then 只校验排产链路依赖的页面与 API。`
- `BDD: eDHR 页面不再阻塞排产专用检查 -> Given eDHR 页面当前存在独立类型错误 / When 运行排产专用类型检查 / Then 不会因为 eDHR 页面而失败。`
- `BDD: 全量前端类型检查仍保留真实问题暴露 -> Given 仓库仍需要完整类型校验入口 / When 继续运行全仓 ts:check / Then eDHR 当前问题仍会被真实暴露。`

## Milestones

1. M1：建立任务文档并锁定排产链路页面/API 范围。`completed`
2. M2：补 RED 静态合同测试与现状失败证据。`completed`
3. M3：实现排产专用 tsconfig 与 npm script。`completed`
4. M4：运行 GREEN 验证并回填证据。`completed`

## Expected Verification

- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check`
- `pnpm --dir D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 ts:check:schedule`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-schedule-validation-boundary-static.spec.js`

## Current Blockers

- 无。排产专用前端类型检查入口已完成并通过，保留全量 `ts:check` 继续暴露 eDHR 现有问题。

## Cleanup Keep

- `doc/tasks/20260630-schedule-validation-boundary/frontend-feature-evidence.md`
- `doc/tasks/20260630-schedule-validation-boundary/schedule-tsc-explain.txt`
