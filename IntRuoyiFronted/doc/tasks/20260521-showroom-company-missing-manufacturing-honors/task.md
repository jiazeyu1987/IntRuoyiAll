# 任务：修复展厅公司页签“核心制造能力 / 荣誉资质”不显示

## Goal

修复 `展厅 -> 展厅公司` 页签中 `核心制造能力`、`荣誉资质` 两个公司字段未显示的问题，确保真实页面展示与编辑链路都能看到这两个字段，不通过 fallback、mock 或静默降级掩盖问题。

## Scope

- 复现 `http://localhost:8081` 下展厅公司页签缺失字段的真实用户路径。
- 记录 BDD 场景、RED 证据、根因与最小修复范围。
- 只修改与公司页签字段显示直接相关的前端代码、定向测试和本任务文档。
- 保持现有公司接口契约、保存行为与其他展厅页签行为不变。

## Non-Scope

- 不改动产品、展厅、审批、指派、讨论、讲解等无关页签。
- 不修改后端 Java、数据库或接口返回结构，除非复现证明当前前端违反既有契约。
- 不顺带重做展厅公司页签整体视觉样式。
- 不引入 fallback、兼容分支、mock 数据或静默降级。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-company-menu-direct-save\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 上一次公司页签直存链路已完成，本次可在该基线上继续处理字段缺失显示回归。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 存在多组与本任务无关的未提交 task 文档与前端修改。
- Impact: 本任务必须严格限定改动范围，避免覆盖或提交无关在途工作。

## Milestones

- [x] M1: 创建任务记录并确认上一同仓任务状态。
- [x] M2: 复现公司页签两个字段缺失问题并补 RED 回归测试。
- [x] M3: 实施最小前端修复，恢复字段显示。
- [x] M4: 运行定向测试、lint 与真实路径验证，记录 GREEN。
- [x] M5: 更新任务文档、执行 closeout preview，并在边界允许时准备提交。

## Expected Verification

- `node --test scripts/showroom-admin-company-dashboard-history.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue src/views/showroom-admin/company/CompanyProfileForm.vue src/views/showroom-admin/company/contracts.ts scripts/showroom-admin-company-dashboard-history.test.mjs --format stylish`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-missing-manufacturing-honors --mode preview`
- 如本地运行时前置齐全，补充真实路径验证：`http://localhost:8081/showroom/company`

## Current Status

Completed on 2026-05-21.

已完成真实路径排查。`/showroom/company` 后台公司工作台在当前运行时中本身能正常显示 `核心制造能力 / 荣誉资质`，实际用户问题落在前台公司介绍链路 `/showroom/company-intro`；最终根因收敛为后端 display 标签映射不一致，已在 companion backend 任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-display-company-field-labels\` 修复并完成运行时验证。

## Blockers And Impact

- Blocker: none.
- Impact: pending reproduction and fix result.

## Final Verification Result

- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-fields-visibility run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-missing-manufacturing-honors\scripts\verify-showroom-company-fields-visibility.mjs`，后台 `http://127.0.0.1:8081/showroom/company` 当前可见 `核心制造能力 / 荣誉资质`，证明该链路未复现用户问题。
- PASS: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-frontstage-company-fields run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-missing-manufacturing-honors\scripts\inspect-showroom-frontstage-company-fields.mjs`，前台 `http://127.0.0.1:8081/showroom/company-intro` 在后端修复并重启后显示 `核心制造能力 / 荣誉资质`，且不再出现旧标签 `荣誉奖项`。
