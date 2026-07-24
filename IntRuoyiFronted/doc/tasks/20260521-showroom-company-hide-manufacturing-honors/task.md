# 任务：隐藏展厅公司页签的核心制造能力与荣誉资质

## Goal

在 `展厅 -> 展厅公司` 后台工作台中隐藏 `核心制造能力`、`荣誉资质` 两个公司字段，确保当前页面的展示区与编辑弹框都不再渲染这两个字段，同时不清空已存量数据，也不引入 fallback、mock 或静默降级。

## Scope

- 只调整后台 `showroom/company` 页面里公司字段的可见范围。
- 为本次隐藏行为补一条定向前端回归测试。
- 保持现有公司保存接口契约不变，隐藏字段在未编辑情况下仍原样随 payload 传回，避免保存时误丢数据。
- 更新任务文档与执行日志。

## Non-Scope

- 不改动前台 `showroom/company-intro` 展示链路。
- 不删除数据库字段或后端字段定义。
- 不改动产品、展厅、审批、指派、讲解等其他页面。
- 不顺带修改这两个字段在其他模块中的显示逻辑。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-missing-manufacturing-honors\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一任务已确认后台公司工作台能显示这两个字段；本次按新要求将它们从后台公司页签隐藏。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 存在多组与本任务无关的未提交 task 文档和展厅前端在途改动。
- Impact: 本任务必须严格限制在公司工作台相关文件与本任务文档，不覆盖无关变更。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [x] M2: 补 RED 回归测试，锁定后台公司页签不应渲染这两个字段。
- [x] M3: 最小修改公司工作台与表单可见字段定义，隐藏目标字段且保持保存链路不丢值。
- [x] M4: 运行定向测试、lint 与真实路径验证，记录 GREEN。
- [x] M5: 更新任务文档、执行 closeout preview，并在边界允许时准备提交。

## Expected Verification

- `node --test scripts/showroom-admin-company-hidden-fields.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/company/contracts.ts src/views/showroom-admin/company/CompanyProfileForm.vue src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-hidden-fields.test.mjs --format stylish`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-hidden-fields run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-hide-manufacturing-honors\scripts\verify-showroom-company-hidden-fields.mjs`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-hide-manufacturing-honors --mode preview`

## Current Status

Completed on 2026-05-21.

已完成后台公司工作台字段隐藏、定向回归测试、lint 与真实页面验证。当前 `showroom/company` 的公司内容卡片和“编辑公司”弹框都不再显示 `核心制造能力 / 荣誉资质`，同时保存 payload 仍保留这两个隐藏字段的原值。

## Blockers And Impact

- Blocker: none.
- Impact: pending implementation result.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-company-hidden-fields.test.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/company/contracts.ts src/views/showroom-admin/company/CompanyProfileForm.vue src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-hidden-fields.test.mjs --format stylish`
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-hidden-fields run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-hide-manufacturing-honors\scripts\verify-showroom-company-hidden-fields.mjs`
