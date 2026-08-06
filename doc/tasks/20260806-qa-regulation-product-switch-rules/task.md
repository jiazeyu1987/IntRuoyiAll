# QA 规程检验规则产品切换回归修复

## Task Goal

修复 QA 规程配置中“切换到其它产品仍显示按压式球囊扩充压力泵检验规则”的回归。检验规则必须严格跟随正式 `productMasterId`；只有压力泵正式绑定产品可以显示压力泵规则，其它产品应显示自己的规则草稿或空规则，并保持保存门禁。

## Milestones

- [x] M1：复现并定位其它产品仍显示压力泵规则的根因。
- [x] M2：新增或更新最小静态回归合同，先取得 RED。
- [x] M3：实现按正式产品身份过滤、清空和恢复检验规则的最小修复。
- [x] M4：完成目标合同、相邻 QA 合同、类型检查和 evidence validator。
- [ ] M5：完成清理、提交、推送和 worktree/slot 收尾。

## Expected Verification

- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs`
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/frontend-feature-evidence.md`
- `git diff --check -- <task-owned-paths>`
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Applicable Gates

- `docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录`：页面内未保存的规程字段、检验规则和检验项目必须以正式 `productMasterId` 为唯一状态 key；缺产品绑定或目标产品未配置时清空并阻塞，不得用压力泵样例模板、项目代码或共享页面单例替代。
- `docs/frontend-development.md#前端静态契约隔离门禁`：本回归先用最小静态合同证明“其它产品仍显示压力泵规则”的 RED，再实现 GREEN；相邻 QA 合同与 `pnpm ts:check` 作为回归门禁。

## Worktree

- Path: `D:\IntRuoyiWorktree\qa-regulation-product-switch-fix`
- Branch: `codex/qa-regulation-product-switch-fix`
- Runtime profile: `int_main`
- Reserved slot: `3` (`8084/48084`)
- 本任务暂不启动前后端服务。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；修复移除了空产品继承默认检验规则行和固定 5% 巡检提示的问题，检验规则展示、预览和保存门禁均跟随当前正式 `productMasterId` 的规则数组。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

实现与验证已完成；实现提交 `bb9cb14fb` 已创建，并已合并最新 `origin/int_main`（merge commit `36951d8fd`）后复跑验证。临时 evidence 已按 cleanup apply 清理。待提交并推送任务记录；worktree/slot 删除因主工作区 `E:\IntRuoyi` 存在无关脏改动，不能执行 ff-only 合并收尾。

## Verification Evidence

- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs` -> PASS。
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/bug-regression-evidence.md` -> PASS（cleanup 前已执行，结论已复制到保留文档）。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-qa-regulation-product-switch-rules/frontend-feature-evidence.md` -> PASS（cleanup 前已执行，结论已复制到保留文档）。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/QaRegulationPage.vue IntRuoyiFronted/tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs doc/tasks/20260806-qa-regulation-product-switch-rules/task.md doc/tasks/20260806-qa-regulation-product-switch-rules/execution-log.md` -> PASS，只有 Git CRLF 工作区提示。
- 合并 `origin/int_main` 后复跑：目标 QA 静态合同、3 个相邻 QA 静态合同与 `pnpm ts:check` 均 PASS。

## Closeout Notes

- `task-closeout-cleanup` preview（auto）显示核心保留文件正确、临时 evidence 可删除，但因 `E:\IntRuoyi` 主工作区脏状态和 ff-only 合并条件阻塞 worktree closeout。
- `task-closeout-cleanup` preview/apply（`--worktree-closeout off`）已执行，只删除 `bug-regression-evidence.md` 与 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- `project-experience-consolidation` 已执行归属检查；现有 `docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录` 已覆盖本次经验，无需新增长期经验文档。
