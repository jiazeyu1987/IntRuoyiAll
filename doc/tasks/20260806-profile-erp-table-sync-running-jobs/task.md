# ERP 表格自动同步正在进行 Job 列表

## Task Goal

在个人工作台配置页签的 `ERP表格自动同步` 页面内新增“正在进行的同步 Job”列表，只展示当前运行中的 ERP 同步记录，便于用户看到哪些表格正在同步中。

## Milestones

- [x] M1 任务规则与现有页面契约核对
- [x] M2 BDD/TDD 静态合同 RED
- [x] M3 前端页面实现运行中 Job 列表
- [x] M4 目标静态合同、类型检查与回归验证
- [x] M5 收尾清理与最终记录

## Expected Verification

- `node IntRuoyiFronted\tests\e2e\profile-erp-table-auto-sync-static.spec.js`
- `node IntRuoyiFronted\tests\e2e\profile-nas-table-auto-sync-static.spec.js`
- `pnpm ts:check`（工作目录：`IntRuoyiFronted`）
- `git diff --check -- IntRuoyiFronted/src/views/Profile/components/ProfileErpTableAutoSyncSetting.vue IntRuoyiFronted/tests/e2e/profile-erp-table-auto-sync-static.spec.js doc/tasks/20260806-profile-erp-table-sync-running-jobs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-profile-erp-table-sync-running-jobs/frontend-feature-evidence.md`

## Applicable Gates

- ERP 表格同步 Job 链路门禁：继续使用 `ErpKingdeeSyncApi.runIncrementalSyncJob(handlerName)`、`ErpKingdeeSyncApi.getRunPage` 与 `infra/job` 正式链路，禁止恢复 `/erp/kingdee-table-auto-sync/**` 或 mock 成功。
- 业务运行记录用户可读展示门禁：运行记录必须展示中文业务列名、可读日期时间和中文状态，不得直出数字状态、内部字段名或 epoch 时间戳。
- 静态合同隔离门禁：本需求通过 `profile-erp-table-auto-sync-static.spec.js` 的聚焦合同完成 RED/GREEN，不以无关历史全量失败冒充当前需求结果。
- E2E 脚本入口存在性门禁：区分静态合同 PASS、TypeScript PASS 与真实 Playwright E2E；本次改动不新增写入型真实 E2E。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用正式运行记录分页接口的 `status=10` 查询展示当前运行中的 Job。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

代码实现、目标静态合同、相邻 NAS 静态回归、生产全屏相邻合同、全量类型检查、diff 检查、frontend evidence validator 和 cleanup apply 均已通过。
