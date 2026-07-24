# 任务：展厅企宣指定用户修改产品信息（前端）

## Goal

确认并补齐 `展厅 -> 产品管理` 中企宣用户将产品整单指派给指定用户修改产品信息的前端行为：指派弹窗使用真实用户列表，支持按账号/昵称过滤选择；点击指派按钮后调用真实指派接口并刷新产品列表；后端返回整单 OPEN 指派态时，产品状态显示为 `指派中`，列表显示 `指派对象`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ProductWholeAssignmentDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\contracts.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-assignee-scope.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-product-whole-assignment.spec.js`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-publicity-product-assignment\**`

## Non-Scope

- 不新增 mock 用户或静态候选数据。
- 不改变展厅产品详情表单字段结构。
- 不隐藏接口错误或用前端状态替代后端真实状态。
- 不改动企宣角色授权来源，继续依赖真实菜单和真实角色。
- 不把站内信驳回深链或通知列表定位并入本次收尾。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-assignee-product-scope\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 无，可继续处理企宣整单指派确认任务。

## Milestones

- [x] M1: 创建前端任务文档并确认上一同仓任务状态。
- [x] M2: 记录 BDD 与前端定向测试，锁定账号/昵称过滤选择、指派按钮、状态与指派对象展示。
- [x] M3: 完成或确认最小前端实现，整单指派弹窗调用真实 `createAssignment` 并成功后刷新产品列表。
- [x] M4: 运行前端定向验证并记录 GREEN。
- [x] M5: 运行 task-closeout-cleanup 预览，完成任务文档。
- [x] M6: 使用真实测试租户、真实产品和真实用户执行 Playwright E2E，确认 UI 指派链路。

## Expected Verification

- `node tests/e2e/showroom-product-whole-assignment.spec.js`
- `node --test scripts\showroom-admin-product-assignee-scope.test.mjs scripts\showroom-admin-frontend.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/product/ProductWholeAssignmentDialog.vue src/views/showroom-admin/product/contracts.ts scripts/showroom-admin-product-assignee-scope.test.mjs tests/e2e/showroom-product-whole-assignment.spec.js --format stylish`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-publicity-product-assignment --mode preview`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-publicity-assignment-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-publicity-product-assignment\scripts\verify-publicity-whole-assignment-real-e2e.mjs`

## Current Status

Completed on 2026-05-20.

## Final Verification Result

- PASS: `node tests\e2e\showroom-product-whole-assignment.spec.js`
- PASS: `node --test scripts\showroom-admin-product-assignee-scope.test.mjs scripts\showroom-admin-frontend.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/index.vue src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/product/ProductWholeAssignmentDialog.vue src/views/showroom-admin/product/contracts.ts scripts/showroom-admin-product-assignee-scope.test.mjs tests/e2e/showroom-product-whole-assignment.spec.js --format stylish`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-publicity-product-assignment --mode preview`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-publicity-assignment-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-publicity-product-assignment\scripts\verify-publicity-whole-assignment-real-e2e.mjs`，真实产品 `E2E-ASSIGN-1779285960748` 指派给 `展厅编辑 / showroomeditor` 后显示 `指派中` 与指派对象。

## Notes

- 当前工作区同仓库存在多处本任务之外的未提交改动，提交时必须仅暂存本任务文档与 E2E 脚本，避免混入无关变更。
- `ProductWholeAssignmentDialog` 使用 Element Plus `filterable` 选择框，候选项展示为 `昵称 / 账号`，数据来自 `getSimpleUserList()`。
- 列表通过后端返回的 `activeAssignment.assigneeUserId` 映射真实用户昵称/账号；若后端未返回对应用户，只显示用户 id，不做 mock 或降级替代。
- 2026-05-20 追加真实数据 E2E 验证：仅使用测试租户真实账号、真实产品创建接口、真实用户精简列表与真实指派接口；不使用 mock 数据。
- 真实 E2E 截图保存在忽略目录 `output/playwright/showroom-publicity-assignment-real-e2e.png`，不纳入 Git 提交。

## Cleanup Keep

- `doc/tasks/20260520-showroom-publicity-product-assignment/frontend-feature-evidence.md`
- `doc/tasks/20260520-showroom-publicity-product-assignment/scripts/verify-publicity-whole-assignment-real-e2e.mjs`
