# Execution Log

## 2026-07-27 Start

- User intent: 实现单元格控件类型切换能力，支持文本切日期、电子签名、下拉框并配置选项、数字输入并配置上下限。
- Scope decision: 最小实现落在批记录表单“规则”弹窗和 `/cell-rules` 契约，不改 Jimu iframe 原生右键或右侧属性栏。
- Skill/readiness: 已读取 `frontend-feature-delivery`、`backend-api-delivery`、`bdd-tdd-acceptance-planner` 及对应 references；已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/engineering/technology-stack-routing.md`。
- Baseline: `git commit -m "chore: baseline existing worktree changes"` -> PASS, commit `b7dc3380`，保存任务开始前既有脏改动。
- Concurrent changes after baseline: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`、`doc/tasks/20260727-shared-word-parser-design/*`、`docs/system/shared-word-template-parser-design.md` 仍有并行脏改动；本任务不覆盖这些文件。

## BDD

- BDD: 控件类型切换 -> Given 用户在批记录表单规则弹窗中选中一个文本单元格 / When 切换为日期、日期时间、数字、下拉框或电子签名 / Then 保存后的 `edhrCellRule` 与 `fillForm.componentFlag` 必须一致，运行态按新控件类型渲染。
- BDD: 下拉框选项配置 -> Given 用户把文本单元格切换为下拉框 / When 添加至少两个选项并保存 / Then 后端保存 `constraints.selectionMode=single` 和 `constraints.options`，并同步到 `fillForm.options`。
- BDD: 数字上下限校验 -> Given 用户把文本单元格切换为数字输入 / When 设置最小值大于最大值 / Then 前端和后端都必须阻止保存并显示真实错误。
- BDD: 电子签名不降级 -> Given 用户把文本单元格切换为电子签名 / When 单元格缺少 enabled `edhrSignature` marker / Then 保存必须 fail-fast，不得退化为普通文本或伪造签名。

## Evidence

- RED/GREEN/REGRESSION 证据见下方 “2026-07-27 Implementation and Verification”。
## 2026-07-27 Implementation and Verification

- RED: `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` -> FAIL，缺少 `{ label: '下拉框 select', value: 'select' }`、下拉选项编辑、select 模拟渲染与 constraints options 契约。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 初次执行被并行编译问题阻塞；随后补齐后端实现后重新执行。
- Implementation: 前端在 `BatchRecordCellRulesConfirmDialog.vue` 增加 select 控件、下拉选项 textarea、签名提示、控件类型自动同步 valueType、保存前 select/number 校验；共享规则和模板预览支持 select options。
- Implementation: 后端 `MesProBatchRecordCellRuleSupport` 将 `select` 纳入单选控件同步，并拒绝 NUMBER min > max。
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-cell-control-type-switch-static.spec.js` -> PASS，输出 `PASS: eDHR cell control type switch static contract`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellRuleSupportTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
- GREEN: `git diff --check` -> PASS；仅出现 Windows LF/CRLF 工作区提示，无空白错误。
- GREEN: experience-consolidation -> PASS；本次经验已由 `docs/backend-development.md#jimu-fillform-组件类型语义优先边界` 覆盖，无需新增长期经验文档。
- Closeout note: 当前工作区仍存在多个非本任务并行改动和 `int_main...origin/int_main [ahead 10]`，本任务文件已完成验证，最终 push/完成态需在并行改动边界确认后执行。
