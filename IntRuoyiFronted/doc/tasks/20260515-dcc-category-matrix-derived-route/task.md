# Task: DCC 文件类别自动派生四层审批矩阵

## Goal

把当前 DCC 前端里“审批路线自由录入 + 文件类别治理页单人四阶段配置”改成“文件类别治理页维护第二层/第三层角色集合，审批路线页只读预览固定四层”，并让上传页的路线预览与之保持一致。

## Scope

- 检查并显式阻塞上一条未闭环前端任务后再开始当前任务。
- 先创建当前前端任务文档、执行日志和前端证据文件，再开始生产代码变更。
- 严格按 BDD + TDD 先补失败验证，再做最小前端实现。
- 改造 DCC 文件类别治理页、审批路线页、上传页相关 API 类型和展示逻辑。
- 删除审批路线页中的自由新增/编辑表单入口，仅保留派生四层预览。
- 保持现有 Int 运营台前端风格，不做无关视觉重构。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-tool-header-search-always-visible/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused header-visibility task does not block this DCC frontend delivery.

## Milestones

- [x] M1: Block the previous unfinished frontend task and create this task directory.
- [x] M2: Record BDD scenarios plus RED verification for category governance, route preview, and upload preview.
- [x] M3: Implement front-end API/type changes for category matrix and derived route preview.
- [x] M4: Refactor category governance page to edit only second-layer and third-layer position collections.
- [x] M5: Refactor approval-route page into read-only derived preview and remove free-form route editing.
- [x] M6: Update upload route preview UI and run GREEN verification.
- [x] M7: Update evidence and commit only this task's frontend changes.

## Expected Verification

- Category governance page edits only:
  - second-layer signoff position multi-select
  - third-layer approval position double-select
  - effective time and remark
- Approval-route page:
  - fixed four-stage preview only
  - no free-form add/edit route flow
- Upload preview shows the same four derived stages and semantics as the governance page.

## Current Status

Completed. The front-end structure, API types, category-matrix entry, read-only route preview page, and upload route preview all align with the new category-derived fixed-four-stage behavior.

## Blocker And Impact

- Blocker: none remaining for the front-end implementation itself.
- Impact: DCC users now configure category approval relationships from the category side and read the same derived route semantics consistently in governance, route preview, and upload preview.

## Final Verification Result

- `pnpm exec eslint src/api/dcc/controlledFile/fileCategories.ts src/api/dcc/controlledFile/approvalRoutes.ts src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/categories/index.vue src/views/dcc/controlled-file/categories/components/CategoryMatrixDialog.vue src/views/dcc/controlled-file/routes/index.vue src/views/dcc/controlled-file/routes/components/RouteForm.vue src/views/dcc/controlled-file/upload/submitter.ts` -> PASS
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit --pretty false` -> FAIL only on unrelated non-DCC `ElMessageBox` type errors
- Playwright verification script `doc/tasks/20260515-dcc-category-matrix-derived-route/scripts/verify-dcc-category-matrix-derived-route.mjs` -> PASS
- Scoped frontend commit: `11e2f3bb` `任务: 对齐DCC矩阵派生预览契约`
