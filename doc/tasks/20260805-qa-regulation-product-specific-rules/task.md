# QA 检验规则按产品隔离

## Task Goal

QA 规程配置中的检验规则必须跟随 DCC 项目正式绑定的 MDM 产品。当前按压式球囊扩充压力泵规则只属于其正式产品；切换到其它产品时不得继续显示、编辑或保存压力泵规则，同一产品从不同 DCC 项目入口进入时应复用同一份页面草稿状态。

## Milestones

- [x] M1：确认正式保存 payload 已使用 `productId`，定位前端规则状态仍由 `IDI` 项目代码初始化且跨产品共享。
- [x] M2：新增产品级规则隔离静态契约并取得 RED。
- [x] M3：实现按 `productMasterId` 初始化、缓存和切换规则状态。
- [x] M4：完成聚焦回归、类型检查和技能证据校验。
- [x] M5：完成清理、提交、推送和主线集成。

## Expected Verification

- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs`
- `node tests/e2e/qa-regulation-version-publish-header-static.spec.cjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-qa-regulation-product-specific-rules/bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-qa-regulation-product-specific-rules/frontend-feature-evidence.md`
- `git diff --check -- <task-owned-paths>`
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Applicable Gates

- `docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录`：页面必须以 DCC `productMasterId` 作为产品身份，不能用项目代码、产品名称或压力泵模板推断产品级状态。
- `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用最小静态契约，先证明旧实现按项目代码共享规则，再实现 GREEN。
- `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`：主工作区存在并行任务，当前任务在隔离 worktree 中实现并只提交任务自有文件。
- 缺少正式 `productMasterId` 时必须显示空规则并阻塞保存；不得继续用 `IDI` 或产品名称套用压力泵规则。

## Worktree

- Path: `D:\IntRuoyiWorktree\qa-regulation-product-rules`
- Branch: `codex/qa-regulation-product-rules`
- Runtime profile: `int_main`
- Reserved slot: `3` (`8084/48084`)
- 本任务不启动前后端服务。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；页面规则状态以正式 `productMasterId` 为唯一 key，并在产品切换时保存/恢复各自草稿。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

产品级规则隔离、相邻回归、类型检查、技能证据校验、经验沉淀、临时 evidence 清理、任务分支推送、`origin/int_main` 主线融合、两个任务 worktree 删除及槽位释放均已完成。
