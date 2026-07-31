# Codex 测试管理方法项与目标项展示

## Task Goal

- 将测试管理列表中红框区域拆分为“测试方法项”和“测试目标项”。
- 支持方法与目标按 a/b/c/d/e/f/g 等多行顺序展示；一个或多个条目均可展示。
- 沿用现有 `methodText` 与 `checkpoints.expectedText` 契约，不新增后端 fallback 或兼容分支。

## Milestones

- [x] 记录 BDD/TDD 约束与现有契约边界。
- [x] 先补静态测试，验证列表必须包含测试方法项与测试目标项展示契约。
- [x] 修改前端页面列表、表单文案和展示样式。
- [x] 运行相关验证并记录结果。

## Expected Verification

- `node tests/e2e/system-codex-test-management-static.spec.js`
- `pnpm ts:check`
- `node --check tests/e2e/system-codex-test-management-real.e2e.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260725-codex-test-method-target-items/frontend-feature-evidence.md`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用现有方法文本与检查点目标数据模型，在 UI 上明确分栏展示。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs\experience-index.md` 未找到；本次为低风险前端展示文案与列表渲染调整，不涉及发布、数据、权限、服务或 schema 变更。
## Cleanup Keep

- doc/tasks/20260725-codex-test-method-target-items/frontend-feature-evidence.md
