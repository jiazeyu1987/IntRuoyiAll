# 任务：修复展厅产品管理首屏中的资料状态/指派对象列可见性回归

## Goal

修复 `展厅 -> 产品管理` 列表在真实页面首屏中 `资料状态`、`指派对象` 列不可见的问题，确保用户进入列表后无需额外横向滚动即可看到这两个业务关键列，不引入 fallback、mock、兼容分支或静默降级。

## Scope

- 复现 `showroom-admin` 产品管理列表首屏列可见性问题并锁定根因。
- 先补会失败的源码/真实页面回归证据，再做最小修复。
- 仅调整与该列表列可见性直接相关的表格布局、列宽或验证脚本。
- 更新任务文档、执行日志和 bug regression evidence。

## Non-Scope

- 不改动产品列表的数据契约、审批状态机、指派逻辑或后端接口。
- 不顺带改造产品管理页的整体视觉风格、筛选结构或其他无关列。
- 不为测试额外增加前端临时控件、假数据或 mock 数据。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-columns-restore\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一同仓任务已闭环；本次按“首屏列可见性回归”新开任务，不覆盖旧记录。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在与本任务无关的在途文档改动。
- Impact: 本任务只修改产品管理列表相关源码、测试和当前任务目录，避免覆盖无关变更。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [ ] M2: 用真实页面复现“资料状态/指派对象首屏不可见”，记录 BDD/RED 证据。
- [ ] M3: 做最小修复并补充针对首屏可见性的回归测试。
- [ ] M4: 运行源码级回归与真实页面验证，记录 GREEN。
- [ ] M5: 更新证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-list.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-status-columns-regression run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-viewport-regression\scripts\verify-showroom-product-status-columns.mjs`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-status-assignee-viewport-regression\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-status-assignee-viewport-regression --mode preview`

## Current Status

Blocked on 2026-05-21.

## Blockers And Impact

- Blocker: 用户在 2026-05-21 明确改为“不要挤掉，直接把这两列删掉”，否决当前“保留列并确保首屏可见”的方案。
- Impact: 本任务按原目标不再继续实施；相关代码和测试改动转入新的“删除资料状态/指派对象列”任务跟踪。
