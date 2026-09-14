# Execution Log

## Intent

- 用户确认需求：批记录单元格链接时，左侧来源需要可以选择生产工单字段，使部分批次执行数据从生产工单自动带入。
- 用户截图反馈：左侧源选择框只显示批记录表单，看不到“生产工单”选项；按截图交互要求，生产工单必须直接出现在源选择框中。

## Preflight

- `git status --short --branch` on `E:\IntRuoyi` showed branch `int_main` ahead of origin with pre-existing dirty changes.
- Baseline commits created before task work: `5d85fade`, `a21d05b9`, `214e6fc8`, `3ba45ec8`.
- 主工作区继续出现并行改动，命中共享分支/工作区；创建隔离 worktree：`D:\IntRuoyiWorktree\work-order-field-cell-link-20260726`。
- Worktree branch: `codex/work-order-field-cell-link-20260726`; profile `int_main`; slot `4`; frontend/backend ports `8085/48085`; services not started.
- `GREEN: experience-preflight -> PASS`，已读取任务、PowerShell、前端、后端、worktree、端口矩阵规则。

## BDD

- `BDD: Work order field source selectable -> Given 批记录单元格链接页面打开且目标表单为批次执行批记录 When 用户在左侧来源切换到生产工单字段 Then 页面展示生产工单字段列表并允许选择字段作为链接来源`
- `BDD: Work order source option visible in source selector -> Given 用户打开批记录单元格链接页面 When 点击左侧源选择框 Then 下拉选项直接包含“生产工单”`
- `BDD: Work order source link persists -> Given 用户选择生产工单字段和目标批记录单元格 When 点击建立链接 Then 保存的链接关系包含来源类型 PRODUCTION_WORK_ORDER、来源字段编码和目标单元格坐标`
- `BDD: Work order value fills batch execution cell -> Given 批次执行有关联生产工单且链接来源为生产工单字段 When 打开或生成批次执行记录 Then 目标单元格填入对应生产工单字段值，缺少字段或工单时 fail-fast 暴露错误`

## TDD Evidence

- `RED: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> FAIL, 既有前端静态契约缺少 batch-record-cell-link__source-type-select / 生产工单字段来源选择断言`
- `RED: mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" test -> FAIL, 未实现生产工单字段来源回填测试且隔离分支存在并行基线编译阻塞`
- `GREEN: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> PASS, batch-record-cell-link static contract passed`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`
- `GREEN: git diff --check -> PASS, no whitespace error`
- `BLOCKED: node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs -> FAIL, 本地 Docker MySQL 缺少本任务正式迁移列 source_type/source_field_code/source_field_name，工作台接口返回 Unknown column 'source_type'`
- `GREEN: docker exec int-ruoyi-mysql ... SELECT COLUMN_NAME ... -> PASS, 已对本地 ruoyi-vue-pro 应用 20260726_mes_batch_record_cell_link_work_order_source.sql 并复核三列存在`
- `RED: node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs -> FAIL, 真实页面切换到生产工单字段后字段文字可见但 sourceSelectableCount=0，左侧字段格未成为可选来源`
- `GREEN: node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs -> PASS, forms=15, sourceFields=12, mesWriteRequests=0, 真实页面可选择生产数量字段和目标单元格，建立链接按钮变可用`
- `GREEN: pnpm ts:check -> PASS, vue-tsc relaxed type check completed after frontend dependencies were installed`
- `GREEN: project-experience-consolidation -> PASS, 已将 schema-backed E2E 迁移核对与字段矩阵可选态经验合并到 docs\e2e-rules.md，并在 docs\experience-index.md 增加关键词路由`
- `RED: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> FAIL, 源选择框未包含 <el-option label="生产工单" ...>，复现截图中看不到生产工单选项的问题`
- `GREEN: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> PASS, 源选择框直接包含“生产工单”，独立 source-type selector 已移除`
- `GREEN: node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs -> PASS, 真实页面打开源选择框并选择“生产工单”，随后可选择生产数量字段和目标单元格，mesWriteRequests=0`
- `GREEN: pnpm ts:check -> PASS, source selector merge type check passed`

## Milestone Notes

- 已实现前端“来源类型”切换：左侧可在“批记录表单”和“生产工单字段”之间切换；生产工单字段来源来自后端 `sourceFields` 白名单。
- 已扩展保存契约：规则保存 payload 和后端 DO/VO 增加 `sourceType`、`sourceFieldCode`、`sourceFieldName`，生产工单字段规则不再要求源表单单元格。
- 已扩展运行态回填：批次执行 `getPrefill` 遇到 `PRODUCTION_WORK_ORDER` 来源时，从目标执行记录关联的生产工单读取白名单字段；缺少工单或不支持字段时 fail-fast 报错。
- 已补充数据库前向迁移 `20260726_mes_batch_record_cell_link_work_order_source.sql`，并同步 fresh schema 与 H2 test schema。
- E2E 前置已将本任务正式迁移应用到当前 48085 后端连接的本地 Docker MySQL `ruoyi-vue-pro`，仅补齐 `source_type`、`source_field_code`、`source_field_name` 三列；未切换数据源、未写业务数据。
- 真实 E2E 发现并修复前端渲染根因：生产工单字段使用字段编码作为业务 `cellKey`，但渲染矩阵按坐标查找 cell meta，导致字段显示但不可选；现已支持业务 key 与坐标双映射，并按坐标识别选中态。
- 新增只读 E2E `IntRuoyiFronted\tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs`：使用 8085/48085、`芋道源码/admin` 标签、真实菜单入口，断言生产工单字段下拉与字段矩阵、可选择 `生产数量`、可选择目标单元格、建立链接按钮可用，且未发送 MES 写请求。
- 已按用户截图反馈调整交互：不再使用独立“来源类型”下拉；“生产工单”作为左侧源选择框的直接选项，选择后左侧字段矩阵展示生产工单字段。
- 前端依赖已安装，`pnpm ts:check` 已补跑通过；真实 E2E 后已停止本任务启动的 8085/48085 进程并复核端口释放。
- 后端首次目标测试因隔离分支未同步 `int_main` 的并行路由配置基线、以及旧本地依赖 jar 失败；已快进到 `4533ac44`，并使用 `-am` 让 Maven 编译依赖模块源码后通过。
- `GREEN: project-experience-consolidation -> PASS, 已将本任务 PowerShell Maven -D 引号与 -am 依赖源码编译经验合并到 docs\powershell-memory.md 既有门禁`
- `GREEN: project-experience-consolidation -> PASS, 已将本次真实 E2E 发现的 schema 缺列和字段可见但不可选门禁合并到 docs\e2e-rules.md 既有 E2E 规则`
- `BLOCKER: task-closeout-cleanup preview -> Main worktree is dirty and cannot receive ff-only merge: E:\IntRuoyi`
- `BLOCKER DETAIL: git -C E:\IntRuoyi status --short --branch -> int_main ahead 20 and contains unrelated/concurrent dirty files under route attachment owner, codex runner, dossier requirement, route flow add form count, docs/e2e-rules.md, docs/frontend-development.md, docs/powershell-memory.md 等；本任务未修改这些主工作区并行文件`
