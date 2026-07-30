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
- 2026-07-30 21:08 +08:00 检查发现并行任务新增
  `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineWorkstationPostRouteBindingSourceTest.java`，
  测试引用的生产类尚不存在；该文件不属于本任务，未修改、未删除、未暂存。
- 同期 Git 索引状态在短时间内由并行任务改变；为避免提交对方 staged 内容，本任务未执行 `git add` 或
  `git commit`。

## Blockers

- 当前分支 `int_main` 在任务开始时为 `ahead 14, behind 8`；若最终 push 被远端拒绝，将按项目规则记录为收尾 blocker。
- `BLOCKED: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，
  当前共享工作区在 `testCompile` 阶段缺少并行任务生产类
  `MesFrontlineWorkstationPostRouteBindingSource`，本任务 11 个目标测试尚未开始执行。
- `GREEN: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> PASS`。
- `GREEN: pnpm ts:check -> PASS`。
- `GREEN: backend/frontend evidence validators -> PASS`。
- `RESOLVED: 2026-07-30 21:19 +08:00`，并行任务已新增
  `MesFrontlineWorkstationPostRouteBindingSource.java`；复跑目标 Maven -> PASS，
  11 tests、0 failures、0 errors。
- 并行基线提交 `67282a86 基线: 保存当前工作区并行改动` 已混入本任务全部实现、测试、领域合同和
  初始任务证据，同时包含其它并行任务文件。按共享分支并发基线门禁，不 amend、不 reset、不把该提交
  伪装成本任务独立实现提交；用户要求“继续”后，本任务只提交后续收尾记录。
- 剩余收尾条件：复查 staged 清单、运行 cleanup、提交本任务收尾记录，并处理 `int_main` 与
  `origin/int_main` 的 `behind 8` 分叉。
- Experience consolidation: 已按现有长期文档归宿更新
  `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md`，记录全量包策略设置和跨租户
  `tenantId` 重写门禁；该改动位于混合基线提交 `67282a86`，未新建长期经验文档。
- Cleanup preview: `task_closeout.py --task-id 20260730-scheduler-workbench-full-package-tenant-policy --mode preview`
  -> PASS，keep 5，delete `<none>`，blocked `<none>`，warnings `<none>`。
- Cleanup apply: `task_closeout.py --task-id 20260730-scheduler-workbench-full-package-tenant-policy --mode apply`
  -> PASS，deleted paths `<none>`。
