# Execution Log

## Intent

- 用户确认需求：批记录单元格链接时，左侧来源需要可以选择生产工单字段，使部分批次执行数据从生产工单自动带入。

## Preflight

- `git status --short --branch` on `E:\IntRuoyi` showed branch `int_main` ahead of origin with pre-existing dirty changes.
- Baseline commits created before task work: `5d85fade`, `a21d05b9`, `214e6fc8`, `3ba45ec8`.
- 主工作区继续出现并行改动，命中共享分支/工作区；创建隔离 worktree：`D:\IntRuoyiWorktree\work-order-field-cell-link-20260726`。
- Worktree branch: `codex/work-order-field-cell-link-20260726`; profile `int_main`; slot `4`; frontend/backend ports `8085/48085`; services not started.
- `GREEN: experience-preflight -> PASS`，已读取任务、PowerShell、前端、后端、worktree、端口矩阵规则。

## BDD

- `BDD: Work order field source selectable -> Given 批记录单元格链接页面打开且目标表单为批次执行批记录 When 用户在左侧来源切换到生产工单字段 Then 页面展示生产工单字段列表并允许选择字段作为链接来源`
- `BDD: Work order source link persists -> Given 用户选择生产工单字段和目标批记录单元格 When 点击建立链接 Then 保存的链接关系包含来源类型 PRODUCTION_WORK_ORDER、来源字段编码和目标单元格坐标`
- `BDD: Work order value fills batch execution cell -> Given 批次执行有关联生产工单且链接来源为生产工单字段 When 打开或生成批次执行记录 Then 目标单元格填入对应生产工单字段值，缺少字段或工单时 fail-fast 暴露错误`

## TDD Evidence

- `RED: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> FAIL, 既有前端静态契约缺少 batch-record-cell-link__source-type-select / 生产工单字段来源选择断言`
- `RED: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" test -> FAIL, 未实现生产工单字段来源回填测试且隔离分支存在并行基线编译阻塞`
- `GREEN: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> PASS, batch-record-cell-link static contract passed`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
- `GREEN: git diff --check -> PASS, no whitespace error`

## Milestone Notes

- 已实现前端“来源类型”切换：左侧可在“批记录表单”和“生产工单字段”之间切换；生产工单字段来源来自后端 `sourceFields` 白名单。
- 已扩展保存契约：规则保存 payload 和后端 DO/VO 增加 `sourceType`、`sourceFieldCode`、`sourceFieldName`，生产工单字段规则不再要求源表单单元格。
- 已扩展运行态回填：批次执行 `getPrefill` 遇到 `PRODUCTION_WORK_ORDER` 来源时，从目标执行记录关联的生产工单读取白名单字段；缺少工单或不支持字段时 fail-fast 报错。
- 已补充数据库前向迁移 `20260726_mes_batch_record_cell_link_work_order_source.sql`，并同步 fresh schema 与 H2 test schema。
- 前端完整 `ts:check` 未运行：隔离 worktree 的 `IntRuoyiFronted\node_modules` 缺失；本次已执行无需依赖安装的静态契约测试。
- 后端首次目标测试因隔离分支未同步 `int_main` 的并行路由配置基线、以及旧本地依赖 jar 失败；已快进到 `4533ac44`，并使用 `-am` 让 Maven 编译依赖模块源码后通过。
- `GREEN: project-experience-consolidation -> PASS, 已将本任务 PowerShell Maven -D 引号与 -am 依赖源码编译经验合并到 docs\powershell-memory.md 既有门禁`
- `BLOCKER: task-closeout-cleanup preview -> Main worktree is dirty and cannot receive ff-only merge: E:\IntRuoyi`
- `BLOCKER DETAIL: git -C E:\IntRuoyi status --short --branch -> int_main ahead 20 and contains unrelated/concurrent dirty files under route attachment owner, codex runner, dossier requirement, route flow add form count, docs/e2e-rules.md, docs/frontend-development.md, docs/powershell-memory.md 等；本任务未修改这些主工作区并行文件`
