# Frontend Feature Evidence

## Feature Goal

- 将 `QaRegulationPage.vue` 的 QA 规程配置内容从一次性直铺展示改为 Tab 分区，规则、项目、发布检查和 PQC 预览列表统一由 `UnifiedListTemplate` 承载。

## Non-Goals

- 不修改 QA 保存草稿、发布规程、DCC 项目代码或项目配置状态后端接口。
- 不引入 mock 数据、静默降级或备用数据源。

## Requirements And Acceptance

- AC-M09：QA 可维护并发布检验规程，页面需降低信息密度且保持正式保存/发布链路。
- 验收：默认总览只展示 DCC 项目范围和适用范围；规则、项目、发布检查、PQC 预览分布在页签中；列表内容使用 `UnifiedListTemplate`。

## UI Entry Points And Owned Files

- Route: `/mes/pro/process-pool/qa-regulation`
- Component: `IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue`
- Static contract: `IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`

## API Contracts And Data States

- Existing API wrappers remain unchanged: DCC project code page, QA regulation project statuses, draft save, publish.
- Data states retained: loading, load error, retry, selected DCC project, configured/unconfigured status, save/publish failure messages.

## BDD Scenarios

- BDD: QA 页面默认聚焦总览 -> Given QA 用户进入独立 QA 规程配置页 When 页面加载 Then 默认只展示总览页签中的 DCC 项目范围和适用范围，规则、项目、发布检查和 PQC 预览不再首屏一次性直铺。
- BDD: QA 规则和项目标准列表化 -> Given QA 用户切换到检验规则或检验项目页签 When 查看和编辑列表 Then 内容通过 `UnifiedListTemplate` 承载，并保留原规则编辑、项目新增、项目删除和原文依据选择器。
- BDD: QA 发布检查标准列表化 -> Given QA 用户切换到发布检查页签 When 查看完整性检查和 PQC 任务预览 Then 完整性检查与 PQC 预览通过 `UnifiedListTemplate` 分区展示，保存草稿和发布规程操作仍在发布检查页签内可见。

## RED

- RED: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, expected reason:旧 QA 页面缺少 `UnifiedListTemplate` 导入和 Tab 分区，断言 "Standalone QA page must use the standard UnifiedListTemplate for dense QA lists." 失败。

## GREEN

- GREEN: `node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS，QA 页面已具备 Tab 分区和四个 `UnifiedListTemplate` 列表。
- GREEN: `node tests\e2e\unified-list-template-empty-tabs-system-static.spec.js` -> PASS，系统标准列表接入点从 84 更新为 88，显式隐藏筛选列表从 10 更新为 14。
- GREEN: `pnpm ts:check` -> PASS，Vue/TypeScript 类型检查通过。

## Verification

- Verification: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs IntRuoyiFronted/tests/e2e/unified-list-template-empty-tabs-system-static.spec.js doc/tasks/20260805-qa-regulation-publish-fix/task.md doc/tasks/20260805-qa-regulation-publish-fix/execution-log.md doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md doc/tasks/20260805-qa-regulation-publish-fix/frontend-feature-evidence.md` -> PASS，仅有 Git CRLF 工作区提示，无 whitespace error。
- Verification: QA 页面专属排序接线断言 -> PASS，输出 `PASS QA standard list sort wiring`。
- Verification: 全局 `unified-list-template-all-headers-sortable-static.spec.js` 仍被大量既有页面阻塞；QA 页面聚焦扫描显示四个新增列表均已接入 `sortColumnAttrs` 与 `handleTemplateSortChange`，未作为本次完成门禁。

## Responsive Accessibility Loading Empty Error Permission

- Preserve existing responsive `.qa-regulation-page__layout` behavior and Element Plus controls.
- Preserve visible DCC and QA status loading/error/retry states.
- Preserve existing route permission contract in `remaining.ts`.

## Blockers

- Original AC-M09 backend target JUnit remains blocked by the shared Maven target issue recorded in the task log.
- Global `unified-list-template-all-headers-sortable-static.spec.js` still fails on unrelated historical pages; QA新增列表已单独确认排序 helper 接线。
