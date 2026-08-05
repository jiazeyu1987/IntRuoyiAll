# Frontend Feature Evidence

## Feature Goal

- 将 `QaRegulationPage.vue` 的 QA 规程配置内容从一次性直铺展示改为“DCC 项目代码下拉选择 + 选中后 Tab 分区”，规则、项目、发布检查和 PQC 预览列表统一由 `UnifiedListTemplate` 承载。

## Non-Goals

- 不修改 QA 保存草稿、发布规程、DCC 项目代码或项目配置状态后端接口；本次仅移除页面选择区对旧已配置/待配置状态列表的展示和加载。
- 不引入 mock 数据、静默降级或备用数据源。

## Requirements And Acceptance

- AC-M09：QA 可维护并发布检验规程，页面需降低信息密度且保持正式保存/发布链路。
- 验收：未选择项目时只展示必填 `DCC 项目代码` 下拉；选择后展示 Tab、适用范围、检验规则、检验项目和发布检查；旧已配置/待配置列表不再显示；列表内容使用 `UnifiedListTemplate`。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/qa-regulation`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`

## API Contracts And Data States

- Existing API wrappers remain unchanged: DCC project code page, QA regulation project statuses, draft save, publish.
- Data states retained: DCC project loading, load error, retry, selected DCC project, save/publish failure messages.
- Removed from the page state: configured/unconfigured project status groups and selector-area project details.

## BDD Scenarios

- BDD: QA 页面默认聚焦总览 -> Given QA 用户进入独立 QA 规程配置页 When 页面加载 Then 默认只展示总览页签中的 DCC 项目范围和适用范围，规则、项目、发布检查和 PQC 预览不再首屏一次性直铺。
- BDD: QA 规则和项目标准列表化 -> Given QA 用户切换到检验规则或检验项目页签 When 查看和编辑列表 Then 内容通过 `UnifiedListTemplate` 承载，并保留原规则编辑、项目新增、项目删除和原文依据选择器。
- BDD: QA 发布检查标准列表化 -> Given QA 用户切换到发布检查页签 When 查看完整性检查和 PQC 任务预览 Then 完整性检查与 PQC 预览通过 `UnifiedListTemplate` 分区展示，保存草稿和发布规程操作仍在发布检查页签内可见。
- BDD: QA 项目选择区只保留下拉框 -> Given QA 用户进入页面 When 尚未选择 DCC 项目代码 Then 项目选择区只显示必填的 DCC 项目代码下拉框，不显示项目详情、配置状态、已配置列表或待配置列表。
- BDD: QA 内容选中后展示 -> Given QA 用户选择一个 DCC 项目代码 When 项目选择成功 Then 页面显示 Tab，并可查看该项目对应的适用范围、检验规则、检验项目和发布检查。

## RED

- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧 QA 页面缺少 `UnifiedListTemplate` 导入和 Tab 分区，断言 "Standalone QA page must use the standard UnifiedListTemplate for dense QA lists." 失败。
- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧选择区仍保留项目详情、配置状态或已配置/待配置列表，断言 "QA project selector area must only keep the required DCC project code select row." 失败。

## GREEN

- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 页面已具备 Tab 分区和四个 `UnifiedListTemplate` 列表。
- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 项目选择区只剩 1 个必填 DCC 项目代码下拉框，Tab 与内容在选中项目后显示，旧状态列表源码和 UI selector 均被禁止。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，系统标准列表接入点从 84 更新为 88，显式隐藏筛选列表从 10 更新为 14。
- GREEN: `pnpm ts:check` -> PASS，Vue/TypeScript 类型检查通过。

## Verification

- Verification: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs IntRuoyiFronted/tests/e2e/unified-list-template-empty-tabs-system-static.spec.js doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md` -> PASS，仅有 Git CRLF 工作区提示，无 whitespace error。
- Verification: QA 页面专属排序接线断言 -> PASS，输出 `PASS QA standard list sort wiring`。
- Verification: 全局 `unified-list-template-all-headers-sortable-static.spec.js` 仍被大量既有页面阻塞；QA 页面聚焦扫描显示四个新增列表均已接入 `sortColumnAttrs` 与 `handleTemplateSortChange`，未作为本次完成门禁。

## Responsive Accessibility Loading Empty Error Permission

- Preserve existing responsive `.qa-regulation-page__layout` behavior and Element Plus controls.
- Preserve visible DCC project loading/error/retry states.
- Hide QA regulation tabs/content until `selectedDccProjectCode` exists.
- Preserve existing route permission contract in `remaining.ts`.

## Blockers

- Original AC-M09 backend target JUnit remains blocked by the shared Maven target issue recorded in the task log.
- Global `unified-list-template-all-headers-sortable-static.spec.js` still fails on unrelated historical pages; QA新增列表已单独确认排序 helper 接线。
