# Task: 单个封面生成后持久化到产品版本

## Goal

修复展厅产品单个 `AI生成` 成功后，列表页 `product_001` 仍显示“未上传”的问题。

本次修复要求：

- 单个 `POST /showroom/product/generate-cover-image` 不再只返回封面 URL；
- 对已审核通过的产品，生成完成后必须把 `cover_image` 持久化到产品当前版本链路；
- 列表接口 `/showroom/product/page` 随后必须能返回 `displayRevision.fields.cover_image`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- 需要时补充的 showroom 定向单元测试
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-single-cover-persist\**`

## Non-Scope

- 不回退到对象存储直链。
- 不改动 Codex CLI 图片生成本身。
- 不处理无关 showroom 在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-cover-file-url-proxy\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 文件代理地址修复已完成，本次继续收口“单个生成后列表仍未上传”的持久化缺口。

## Milestones

1. 创建任务文档、执行日志和后端证据骨架。
2. 先补 RED，锁定单个生成后必须持久化 `cover_image`。
3. 最小修复 runtime 单个生成链路。
4. 跑通定向 GREEN、后端证据校验与真实接口复验。
5. 单独提交本任务范围改动。

## Current Status

- Status: Completed
- Completed work:
  - 已确认真实 `/showroom/product/page` 返回里，`product_001` 的 `revisionCover/displayRevisionCover` 仍为 `null`。
  - 已定位根因：单个 `generateProductCoverImage(...)` 仅返回 URL，不像批量生成那样保存并发布新 revision。
  - 已完成 runtime 持久化修复并通过真实接口复验。
- Remaining blockers:
  - None.

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - 已创建任务文档、执行日志和后端证据骨架。
  - 已锁定真实症状为“单个生成成功但列表接口仍不带 `cover_image`”。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-single-cover-persist\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-single-cover-persist\execution-log.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-single-cover-persist\backend-api-evidence.md`
- Remaining blockers:
  - 需要完成 RED/修复/GREEN。

### Milestone 2

- Status: Completed
- Completed work:
  - 已新增 `ShowroomApiRuntimeProductCoverPersistenceTest`。
  - 已执行 RED，确认旧实现缺少持久化链路。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntimeProductCoverPersistenceTest.java`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeProductCoverPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test`（RED）
- Remaining blockers:
  - 需要完成 runtime 修复。

### Milestone 3

- Status: Completed
- Completed work:
  - 已将单个 `generateProductCoverImage(...)` 改为接收 `operatorUserId`。
  - 已补齐“生成 -> saveProductDraft -> publishProductRevision”链路。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- Remaining blockers:
  - 需要完成 GREEN 和运行时复验。

### Milestone 4

- Status: Completed
- Completed work:
  - 已通过主源码编译。
  - 已通过 focused runtime test。
  - 已确认真实 `/showroom/product/page` 现在能返回 `cover_image`。
  - 已通过 backend evidence 校验与 closeout preview。
- Verification evidence:
  - `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomApiRuntimeProductCoverPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-single-cover-persist\backend-api-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-single-cover-persist --mode preview`
- Remaining blockers:
  - 待完成任务范围提交。

### Milestone 5

- Status: Completed
- Completed work:
  - 已将变更范围收敛到 runtime、controller、小型回归测试与当前任务目录。
  - 已创建本任务独立 commit `ddfb60ca8c`。
- Verification evidence:
  - `git commit -m "任务: 修复单个封面生成持久化"`
- Remaining blockers:
  - None.
