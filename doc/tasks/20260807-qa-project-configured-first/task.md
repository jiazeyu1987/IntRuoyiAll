# QA 项目代码按配置状态排序

## Task Goal

在 QA 规程配置页的 DCC 项目代码下拉中，已配置 QA 规程的正式产品排在前面，未配置的正式产品排在后面；每个分组内严格保持 DCC 项目代码接口的原始顺序。配置状态只能使用正式 `project-statuses` 接口按 `productMasterId` 返回的 `configured` 字段。

没有正式 `productMasterId` 的历史 DCC 项目代码不推断为“未配置”；它们保留在所有已绑定产品之后，并继续由现有保存门禁阻止无产品绑定的配置操作。

## Milestones

- [ ] M1：确认 DCC 项目候选、产品绑定和正式 QA 配置状态接口契约。
- [ ] M2：新增最小排序静态合同并取得 RED。
- [ ] M3：实现稳定分组排序和状态返回完整性校验。
- [ ] M4：完成目标合同、相邻回归、类型检查和真实只读 Playwright 验证。
- [ ] M5：完成证据归档、清理、提交与推送。

## Expected Verification

- `node tests/e2e/qa-regulation-project-configured-first-static.spec.cjs`
- `node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs`
- `node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs`
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/qa-regulation-project-configured-first-real.e2e.cjs`
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260807-qa-project-configured-first/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260807-qa-project-configured-first/frontend-feature-evidence.md`
- `git diff --check -- <task-owned-paths>`
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Applicable Gates

- `docs/backend-development.md#QA 规程配置状态必须来自产品级规程记录`：状态必须从 DCC `productMasterId` 和 QA 规程正式记录关联得出；状态接口异常、缺项或非布尔状态必须 fail fast，不能归为待配置。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用本任务专用静态合同完成 RED/GREEN，避免无关全量失败覆盖当前行为。
- `docs/frontend-development.md#复合输入控件交互保留门禁`：排序不得移除 `el-select` 的远程搜索、候选渲染、清空或当前选择恢复。
- `docs/e2e-rules.md#E2E 脚本入口存在性门禁`：真实验证必须从 QA 页面操作项目下拉，并记录只读请求边界。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。正式配置状态响应不完整时显示加载失败，不将项目静默归为待配置。
- `是否从根因和长期维护角度解决`：是。页面按产品级正式状态稳定分组，而不是依赖项目代码、名称或现有前端草稿。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

正在建立排序静态合同并确认正式状态接口覆盖范围。
