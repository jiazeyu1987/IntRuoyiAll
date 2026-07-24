# 任务：展厅产品管理列表增加封面列

## Goal

按用户最新要求，在 `展厅 -> 产品管理` 列表中增加 `封面` 列，直接展示真实产品数据里的封面缩略图；保持现有真实接口、审批流、筛选行为和其他列表列不变，不引入 fallback、mock、兼容分支或静默降级。

## Scope

- 在 `ProductListTable` 中新增 `封面` 表格列。
- 复用真实列表数据中的 `displayRevision.fields.cover_image` / `coverImage` 字段完成封面展示。
- 先补会失败的源码级回归，再做最小前端实现。
- 使用真实前端入口 `http://localhost:8081/showroom/product` 做页面验证。
- 更新本任务文档、执行日志和前端功能证据。

## Non-Scope

- 不修改 `showroom/product/page` 后端接口和产品详情保存逻辑。
- 不顺带调整现有列顺序以外的页面布局、筛选条件或审批逻辑。
- 不新增测试专用前端控件、假数据或占位接口。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-tree-skip-inaccessible-frontend\task.md`
- Status before this task: `Completed with blockers on 2026-05-21`
- Impact: 上一同仓任务已显式记录阻塞且范围属于 NAS 管理页，不阻塞本次 `showroom-admin` 产品列表改动。

## Related Task Context

- Related product-list task: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-remove-status-assignee-columns\task.md`
- Reuse note: 当前产品列表已收敛为 `产品编码 / 中文名称 / 当前版本 / 审批状态 / 英文名称 / 持证人 / 获证状态 / 音频 / 音色 / 操作`；本次仅在真实列表字段基础上补 `封面` 列，不回退已删除列。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的在途文档与 showroom 改动。
- Impact: 本任务只修改产品列表相关源码、测试与当前任务目录，提交时只暂存本次直接产物。

## Milestones

- [x] M1: 创建任务文档并记录当前真实数据契约。
- [ ] M2: 补充“列表必须展示封面列”的 BDD/RED 证据。
- [ ] M3: 做最小前端实现并接通真实封面字段。
- [ ] M4: 运行源码级回归与真实页面验证，记录 GREEN。
- [ ] M5: 更新证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-cover-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-cover-column\scripts\verify-showroom-product-cover-column.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-showroom-product-cover-column/frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-cover-column --mode preview`

## Current Status

Completed on 2026-05-21.

已在 `showroom-admin` 产品列表中新增 `封面` 列，并将真实 `displayRevision.fields.cover_image` / `coverImage` 归一化为列表缩略图数据；无封面产品在列表中显式显示 `未上传`。

## Milestone Status

### M1

- Status: Completed
- Completed work:
  - 创建任务文档与执行日志。
  - 确认前一同仓任务状态不阻塞本次改动。
  - 核对真实列表数据契约已提供 `cover_image` / `coverImage`。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-cover-column\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-cover-column\execution-log.md`
- Remaining blockers:
  - None.

### M2

- Status: Completed
- Completed work:
  - 先修改 `scripts/showroom-admin-product-list.test.mjs`，把 `封面` 列、真实封面字段归一化和 `未上传` 状态写成失败断言。
  - 在未改组件前取得 RED 证据。
- Verification evidence:
  - FAIL: `node --test scripts/showroom-admin-product-list.test.mjs`
- Remaining blockers:
  - None.

### M3

- Status: Completed
- Completed work:
  - 在 `ProductListTable.vue` 新增 `封面` 列模板。
  - 新增 `coverImageUrl` 归一化逻辑，并兼容 `cover_image` / `coverImage` 字段。
  - 为无封面数据补充 `未上传` 显式展示与最小样式。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
  - `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- Remaining blockers:
  - None.

### M4

- Status: Completed
- Completed work:
  - 跑通源码级回归、ESLint 与真实页面 Playwright 验证。
  - 确认真实页面首屏可见 `封面` 表头，且封面单元格渲染图片或 `未上传`。
- Verification evidence:
  - PASS: `node --test scripts/showroom-admin-product-list.test.mjs`
  - PASS: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
  - PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-cover-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-cover-column\scripts\verify-showroom-product-cover-column.mjs`
  - PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-showroom-product-cover-column/frontend-feature-evidence.md`
- Remaining blockers:
  - None.

### M5

- Status: Completed
- Completed work:
  - 运行 closeout preview，确认只保留任务主记录。
  - 已执行 cleanup apply，删除本任务的一次性证据、脚本和截图。
- Verification evidence:
  - PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-cover-column --mode preview`
  - PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-cover-column --mode apply`
- Remaining blockers:
  - None.

## Blockers And Impact

- Blocker: none.
- Impact:
  - 产品管理列表已增加 `封面` 列。
  - 真实封面字段已直连列表展示。
  - 当前页无封面产品会显式显示 `未上传`。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-list.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-cover-column run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-cover-column\scripts\verify-showroom-product-cover-column.mjs`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260521-showroom-product-cover-column/frontend-feature-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-cover-column --mode preview`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-cover-column --mode apply`

## Cleanup Candidates

- `doc/tasks/20260521-showroom-product-cover-column/frontend-feature-evidence.md`
- `doc/tasks/20260521-showroom-product-cover-column/scripts/verify-showroom-product-cover-column.mjs`
- `output/playwright/showroom-product-cover-column.png`
