# 任务：提交展厅前端当前代码快照

## 目标

基于 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 当前工作区，提交已完成并通过验证的 showroom 前端代码快照；仅包含 `showroom-admin` 相关页面、脚本与对应任务证据，不混入权限路由、CRM 页签与其他未完成改动。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-*.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-frontend-current-code-commit\**`

## 非范围

- 不提交 `src/store/modules/permission.ts` 当前改动。
- 不提交 `src/views/crm/statistics/**` 当前改动。
- 不提交 frontstage reviewer 证据、其他历史未跟踪目录与无关脚本。

## 前置任务检查

- `20260519-showroom-company-bilingual-audio-generation`：已完成并有 PASS 证据。
- `20260519-showroom-product-workflow-closure`：已完成并有 PASS 证据。
- `20260520-showroom-product-codex-bilingual-narration`：`completed`

## 里程碑

- [x] M1：识别 showroom 前端可提交边界并排除非 showroom 残留。
- [x] M2：运行当前 showroom 前端快照验证与静态检查。
- [x] M3：完成 Git 提交并复核剩余未提交改动。

## 预期验证

- `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-product-workflow-closure.test.mjs scripts/showroom-admin-workflow-workbenches.test.mjs`
- `node --test scripts/showroom-admin-product-hall-operability.test.mjs scripts/showroom-admin-product-workflow-closure.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/approval/ApprovalTaskPanel.vue src/views/showroom-admin/approval/contracts.ts src/views/showroom-admin/assignment/AssignmentWorkbench.vue src/views/showroom-admin/assignment/contracts.ts src/views/showroom-admin/company/CompanyProfileForm.vue src/views/showroom-admin/company/CompanyWorkbench.vue src/views/showroom-admin/company/contracts.ts src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/history/CompanyHistoryWorkbench.vue src/views/showroom-admin/history/contracts.ts src/views/showroom-admin/narration/NarrationWorkbench.vue src/views/showroom-admin/narration/NarrationWorkspace.vue src/views/showroom-admin/narration/contracts.ts src/views/showroom-admin/product/ProductDetailDialog.vue src/views/showroom-admin/product/contracts.ts src/views/showroom-admin/shared/roleModel.ts scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-product-workflow-closure.test.mjs scripts/showroom-admin-workflow-workbenches.test.mjs`

## 当前状态

Completed on 2026-05-20.

## 最终验证结果

- PASS：`node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-product-workflow-closure.test.mjs scripts/showroom-admin-workflow-workbenches.test.mjs`
- PASS：`node --test scripts/showroom-admin-product-hall-operability.test.mjs scripts/showroom-admin-product-workflow-closure.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs`
- PASS：`pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/approval/ApprovalTaskPanel.vue src/views/showroom-admin/approval/contracts.ts src/views/showroom-admin/assignment/AssignmentWorkbench.vue src/views/showroom-admin/assignment/contracts.ts src/views/showroom-admin/company/CompanyProfileForm.vue src/views/showroom-admin/company/CompanyWorkbench.vue src/views/showroom-admin/company/contracts.ts src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/history/CompanyHistoryWorkbench.vue src/views/showroom-admin/history/contracts.ts src/views/showroom-admin/narration/NarrationWorkbench.vue src/views/showroom-admin/narration/NarrationWorkspace.vue src/views/showroom-admin/narration/contracts.ts src/views/showroom-admin/product/ProductDetailDialog.vue src/views/showroom-admin/product/contracts.ts src/views/showroom-admin/shared/roleModel.ts scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-product-workflow-closure.test.mjs scripts/showroom-admin-workflow-workbenches.test.mjs`
- PASS：`git commit -m "任务: 提交展厅前端当前代码"` -> commit `47473b30`
- PASS：提交后复核 `git status --short`，确认本次 showroom-admin 当前代码快照已出工作区，仅剩权限路由、CRM 页签与 frontstage 残留
