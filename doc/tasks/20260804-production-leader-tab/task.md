# 20260804 生产组长内容独立页签

## Task Goal

将 eDHR 批记录中的生产组长内容从“组长工作台”拆出，改为使用专门“生产组长”页签展示；原“组长工作台”不再显示生产组长内容。

## Milestones

- [x] 识别现有组长工作台与生产/PQC 组长内容边界
- [x] 编写并运行最小 RED 静态合同，证明当前页签仍是 PQC 拆分口径
- [x] 实现生产组长专门页签，并从组长工作台移除生产组长内容
- [x] 运行定向 GREEN/REGRESSION 验证并记录证据
- [x] 完成收尾检查、清理和最终状态记录

## Expected Verification

- `workdir=IntRuoyiFronted; node tests/e2e/edhr-batch-record-leader-tabs-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`，若受历史无关问题阻塞，记录首个无关失败。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-production-leader-tab/frontend-feature-evidence.md`

## Current Status

blocked

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，按业务页签职责重新划分生产组长与组长工作台显示边界。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：本任务用聚焦静态合同先 RED/GREEN，避免被无关全量检查阻塞时误判。
- `docs/e2e-rules.md#windows-换行与脚本行为同步`：修改 `tests/e2e/*static.spec.js` 时，静态合同需使用稳定源码片段和路由名断言。
- `docs/powershell-memory.md#脏工作区基线门禁`：已先提交现有脏工作区基线 `08fa94cef`，本任务后续只提交任务自有变更。

## Cleanup Candidates

- doc/tasks/20260804-production-leader-tab/frontend-feature-evidence.md

## Final Verification

- PASS: `node tests\e2e\edhr-batch-record-leader-tabs-static.spec.js`
- PASS: `node tests\e2e\edhr-batch-page-graph-tab-static.spec.js`
- PASS: `node tests\e2e\mes-process-pool-team-leader-static.spec.js`
- PASS: `pnpm ts:check`
- PASS: frontend feature evidence validator before cleanup.
- PASS: task-closeout cleanup preview/apply kept `task.md`, `execution-log.md`, `verification-report.md` and deleted only temporary `frontend-feature-evidence.md`.
- BLOCKED: final re-verification could not remain stable because concurrent edits repeatedly restored the old `PQC组长` route/page-graph/test contract in task-owned files after repair.
