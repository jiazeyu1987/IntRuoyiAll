# 任务：排产员工作台全量数据包跨租户恢复优化

## Task Goal

优化排产员工作台“导出全部数据包 / 导入全部数据包”，让全量包补齐策略设置并在导入测试租户时写入目标租户上下文，降低从“芋道源码”导出后在测试租户恢复排产数据时的错租户和策略缺失风险。

## Milestones

- [x] M1：建立任务文档、BDD 场景和经验门禁。
- [x] M2：补后端 RED 测试，覆盖策略设置包和租户重写。
- [x] M3：实现后端正式导出/导入优化，不引入 fallback。
- [x] M4：补前端类型与导入提示静态契约。
- [x] M5：运行目标验证并记录结果。
- [ ] M6：按收尾规则更新证据和状态。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" test`
- `node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260730-scheduler-workbench-full-package-tenant-policy/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260730-scheduler-workbench-full-package-tenant-policy/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少子包、策略包格式错误、目标用户或角色缺失时必须 fail fast。
- `是否从根因和长期维护角度解决`：是；补正式全量包合同字段，并在导入边界统一写入目标租户上下文。
- `是否存在临时补丁或绕过`：否；不通过空值、默认租户、默认策略或 mock 数据冒充恢复成功。

## 经验门禁

### 手动重排数据包完整性

- Trigger: 排产员工作台“导出全部数据包/导入全部数据包”、跨环境排产数据迁移。
- Preflight check: 区分路线配置包和全部数据包；全量包承诺恢复排产时必须承载排产工单、生产工单、排产工序快照、路线拓扑、日历规则、计划/实际产能、现有任务、任务扩展、报工、用料、物料和库存。
- Blocker: 缺少手动重排数据包、排产工序快照、任务扩展、日历产能、用料或库存任一正式字段时必须 fail fast。
- Verification: 后端契约证明全量包包含手动重排数据包、缺包导入失败、导入结果返回主数据/排产工单数据/运行态数据计数；前端提示展示这些计数。
- Forbidden action: 禁止把“导出排产工艺路线”扩大成业务数据导出；禁止用空列表、默认路线、默认产能或重新同步冒充完整包。
- Evidence: `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md#手动重排数据包门禁`。

### Git 基线与提交

- Trigger: 当前 `int_main` 开始任务前存在其它任务文档脏改动。
- Preflight check: 先检查当前分支、remote、脏文件和 staged 清单；当前任务文件不得混入基线提交。
- Blocker: 无法区分当前任务文件和既有脏改动、发现敏感凭据或冲突时停止。
- Verification: 基线提交 `3a007be5` 已记录文件清单；后续实现提交只暂存本任务文件。
- Forbidden action: 禁止删除、回滚或覆盖其它任务文档；禁止把基线提交和当前任务实现混为一体。
- Evidence: `docs/powershell-memory.md#脏工作区基线门禁`。

## Verification Evidence

- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，生产 VO 缺少 `getPolicySettingsCount()`。
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchManualReplanDataPackageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`，11 tests，0 failures，0 errors。
- `RED: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> FAIL`，缺少 `policySettingsCount: number`。
- `GREEN: node tests/e2e/mes-scheduler-workbench-import-timeout-static.spec.js -> PASS`。
- `GREEN: pnpm ts:check -> PASS`。

## Cleanup Keep

- doc/tasks/20260730-scheduler-workbench-full-package-tenant-policy/backend-api-evidence.md
- doc/tasks/20260730-scheduler-workbench-full-package-tenant-policy/frontend-feature-evidence.md
