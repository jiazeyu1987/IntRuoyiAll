# Execution Log：排产员工作台全量数据包跨租户恢复优化

## User Intent

- 用户基于截图询问导入导出按钮能否把芋道源码排产相关全部数据导出后导入测试租户恢复一致；确认存在策略设置缺失和跨租户 `tenantId` 风险后，要求“帮我优化”。

## Preflight

- `git status --short --branch` -> DIRTY，存在其它任务文档改动。
- `git branch --show-current` -> `int_main`。
- `git remote -v` -> origin `https://github.com/jiazeyu1987/IntRuoyiAll.git`。
- Baseline commit: `3a007be5 chore: baseline dirty task docs before scheduler package optimization`，保存本任务开始前已存在的其它任务文档改动。
- `GREEN: experience-preflight -> PASS`，已读取 `docs/experience-index.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/task-closeout-rules.md` 和相关技能合同。

## BDD Scenarios

- `BDD: 全量包包含策略设置 -> Given 源租户保存了排产策略设置 / When 导出排产员工作台全部数据包 / Then 数据包包含正式策略设置子包，导入后目标租户返回相同策略设置。`
- `BDD: 跨租户导入重写租户上下文 -> Given 手动重排数据包内对象携带源租户 tenantId / When 在测试租户导入全部数据包 / Then 所有租户型对象写入测试租户 tenantId，不保留源租户 tenantId。`
- `BDD: 导入摘要展示策略计数 -> Given 用户导入全量数据包成功 / When 前端收到导入结果 / Then 成功提示展示用户角色、手动重排数据和策略设置计数。`

## RED / GREEN / REGRESSION

- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" test -> FAIL`，上游 reactor 模块无匹配测试；按 Maven Reactor 兄弟模块验证门禁改用 `surefire.failIfNoSpecifiedTests=false` 复跑。
- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，`MesProSchedulerWorkbenchFullConfigImportRespVO` 缺少 `getPolicySettingsCount()`，符合预期 RED。
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`，11 tests，0 failures，0 errors。
- `RED: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> FAIL`，缺少 `policySettingsCount: number`。
- `GREEN: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> PASS`。
- `GREEN: pnpm ts:check -> PASS`。

## Implementation Notes

- 后端全量包新增 `policySettings`，导出时读取 `schedulerWorkbenchService.getPolicySettings()`，导入时调用正式 `savePolicySettings(...)` 校验并保存。
- 导入响应新增 `policySettingsCount`，前端导入成功提示同步展示策略设置导入数量。
- 手动重排数据包导入前遍历所有列表，只对 `TenantBaseDO` 实例写入当前 `TenantContextHolder.getRequiredTenantId()`，不对非租户表做默认改写。
- 导入没有引入空值兜底；缺少策略设置仍按 `CONFIG_PACKAGE_CONTENT_INVALID` fail fast。

## Parallel Workspace Notes

- 基线提交后，工作区又出现与本任务无关的 `IntRuoyiFronted/src/views/mes/pro/route/index.vue`、历史版本 E2E 删除、其它任务文档和经验文档改动；这些改动未纳入本任务实现范围，提交时只选择性暂存本任务文件。

## Blockers

- 当前分支 `int_main` 在任务开始时为 `ahead 14, behind 8`；若最终 push 被远端拒绝，将按项目规则记录为收尾 blocker。
