# Execution Log: 批记录测试黄框区域隐藏

## User Intent

用户基于截图要求黄框里的内容不显示。截图黄框包含：顶部“批记录测试/通过受控 Codex Runner...”说明页头与“独立测试页签”标签；列表工具栏右侧 Runner 状态、刷新状态按钮和“显示字段”入口。

## BDD

- BDD: 顶部说明页头隐藏 -> Given 用户打开批记录测试页 / When 页面加载完成 / Then 不显示“批记录测试”说明页头、副标题和“独立测试页签”标签。
- BDD: 工具栏 Runner 状态隐藏 -> Given 用户查看任一内部页签列表 / When 查看列表工具栏 / Then 不显示 Runner 状态、Runner 心跳消息和“刷新状态”按钮。
- BDD: 显示字段入口隐藏 -> Given 用户查看生产组长、一线PQC、一线生产三张列表 / When 查看列表工具栏 / Then 不显示“显示字段”按钮，但测试租户筛选和行级测试/修改/删除仍保留。

## TDD Log

- RED: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> FAIL, expected reason: old page still rendered the yellow boxed header and Runner toolbar area.
- GREEN: `node IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs` -> PASS, `edhr-batch-record-test-tab-static PASS`.
- GREEN: `pnpm ts:check` from `IntRuoyiFronted` -> PASS.
- GREEN: `git diff --check -- IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue IntRuoyiFronted\tests\e2e\edhr-batch-record-test-tab-static.spec.cjs doc\tasks\20260808-batch-record-test-hide-yellow-ui` -> PASS with CRLF warnings only.
- GREEN: `rg -n "edhr-batch-record-test-page__header|edhr-batch-record-test-page__title|edhr-batch-record-test-page__subtitle|独立测试页签|Runner：|刷新状态|edhr-batch-record-test-page__runner-message" IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue` -> PASS, exit code 1 confirms yellow-box header/Runner visible text removed.
- GREEN: `rg -n --fixed-strings ':show-column-settings="false"' IntRuoyiFronted\src\views\mes\pro\edhr-batch\BatchRecordTestPage.vue` -> PASS, 3 matches.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260808-batch-record-test-hide-yellow-ui/frontend-feature-evidence.md` -> PASS, `Frontend feature evidence is valid.`

## Milestone Status

- M1 completed: 已定位黄框来源为 `BatchRecordTestPage.vue` 页头、三个 `#actions` 插槽和 `UnifiedListTemplate` 的 `showColumnSettings` 默认入口。
- M2 completed: 已补充 RED 静态合同，锁定黄框区域不可见。
- M3 completed: 已移除顶部页头、Runner 状态/刷新控件，并为三张列表显式关闭显示字段入口。
- M4 completed: 目标静态合同、TypeScript、diff check 和负向扫描均通过。
- M5 completed: cleanup preview/apply 已完成，仅删除本任务临时 `frontend-feature-evidence.md`，保留 task、execution-log、verification-report。

## Rule Evidence

- Read: `docs\task-closeout-rules.md`
- Read: `docs\powershell-encoding.md`
- Read: `docs\frontend-development.md`
- Read: `docs\e2e-rules.md` relevant static/E2E sections
- Read: `docs\experience-index.md`; applicable gates copied into `task.md`
- Read skill: `frontend-feature-delivery`
- Read skill: `task-closeout-cleanup`
- Read skill: `project-experience-consolidation`; 既有截图按钮、统一列表工具栏和显示字段门禁已覆盖本次经验，无需新增长期经验文档。

## Cleanup Evidence

- Preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-batch-record-test-hide-yellow-ui --mode preview` -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`; delete `frontend-feature-evidence.md`; blocked `<none>`; warnings `<none>`.
- Apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-batch-record-test-hide-yellow-ui --mode apply` -> PASS, deleted `frontend-feature-evidence.md`.
