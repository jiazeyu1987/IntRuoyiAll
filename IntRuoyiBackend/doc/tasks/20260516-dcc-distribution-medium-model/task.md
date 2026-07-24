# Task: DCC 发放方式后端模型整改

## Goal

在 `ruoyi-vue-pro` 的 DCC 后端中补齐“发放方式”一等模型，让当前下发规则和发布收尾流程能够区分：

- `PUBLIC_FOLDER`：放在公盘对应文件夹
- `PAPER`：发放纸质文件

本任务只交付后端第一阶段能力：数据模型、规则契约、发布分支和测试，不改前端页面。

## Scope

- 为 DCC 分类分发规则补充发放方式字段和最小持久化支持。
- 为受控文件发布后的分发记录补充发放方式快照。
- 让发布收尾流程按发放方式分支：
  - `PUBLIC_FOLDER` 沿用当前数字分发记录逻辑
  - `PAPER` 生成纸质发放记录骨架，不再与公盘型规则完全混同
- 补充对应 SQL schema / runtime repair / 基础 schema 契约测试。
- 补充后端单元测试与契约测试。
- 不在本任务中修改前端 `DCC下发` 页面。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-workorder-erp-bom-sync/task.md`
- Status before this task: completed.
- Impact: no unfinished latest backend task blocks this delivery.

## Repository Context Risk

- The repository currently contains unrelated dirty MES files and an in-progress
  DCC training-closed-loop write set.
- Relevant DCC test / service files already had uncommitted training-related
  additions before this task started, including:
  - `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccBaseSchemaTest.java`
  - `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImplTest.java`
  - `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
  - `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileFinalizationServiceImpl.java`
- This task layered changes on top of those edits without reverting them.

## Milestones

- [x] M1: Create this backend task package before production code changes.
- [x] M2: Record BDD scenarios and RED evidence for missing distribution-medium support.
- [x] M3: Implement schema, DO/VO, and service-layer support for distribution medium.
- [x] M4: Run targeted backend verification and update evidence.
- [ ] M5: Commit only backend files produced by this task if verification fully passes.

## Expected Verification

- `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260516-dcc-distribution-medium-model\database-schema-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260516-dcc-distribution-medium-model\backend-api-evidence.md`

## Current Status

Completed for backend code delivery. The DCC backend now preserves
`distributionMedium` on category rules and published distribution records,
distinguishes `PUBLIC_FOLDER` from `PAPER` during finalization, and exposes the
medium back through controlled-file detail distribution statuses. Targeted DCC
tests are green.

## Blocker And Impact

- Blocker: a task-scoped backend commit is not yet safe because several shared
  DCC files touched by this slice already contained unrelated in-progress
  training-closed-loop edits before this task began.
- Impact: code and tests are complete for this backend slice, but creating a
  clean backend-only commit now would risk bundling another unfinished DCC task.

## Final Verification Result

- RED:
  - `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    -> FAIL initially, because `DccDistributionMediumEnum` and the new medium
    fields did not exist.
  - The same command then exposed an existing repository compile blocker in DCC
    tests that referenced a missing
    `DccControlledFileUploadNameOptionRespVO`; a minimal VO restoration was
    required before the target tests could run.
- GREEN:
  - `mvn --% -pl yudao-module-dcc -Dtest=DccBaseSchemaTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
    -> PASS, 19 tests green.
- Backend slice outcome:
  - `dcc_file_category_distribution_rule` now stores `distribution_medium`
  - `dcc_controlled_file_distribution` now stores `distribution_medium`
  - `PUBLIC_FOLDER` keeps the current digital recipient/message path
  - `PAPER` creates a distribution record but skips digital recipients/message
    jobs in this first backend slice
  - detail read-side now returns `distributionMedium` in
    `distributionStatuses`

## Cleanup Keep

- `doc/tasks/20260516-dcc-distribution-medium-model/database-schema-evidence.md`
- `doc/tasks/20260516-dcc-distribution-medium-model/backend-api-evidence.md`
