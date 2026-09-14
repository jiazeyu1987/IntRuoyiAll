# Codex 测试管理单项执行按钮

## Task Goal

- 在测试管理列表“操作”列为每个测试项增加“执行”按钮。
- 点击“执行”只针对当前测试项触发执行，不依赖左侧复选框选择状态。
- 保持现有批量执行、修改、删除流程不变。

## Milestones

- [x] 记录 BDD/TDD 与经验门禁。
- [x] 先补静态合同，要求操作列存在单项执行按钮和当前行执行处理函数。
- [x] 修改测试管理页面，复用现有执行 API 只传当前测试项 ID。
- [x] 运行静态、类型与证据校验。
- [x] cleanup、提交并推送任务自有改动。

## Expected Verification

- `node tests/e2e/system-codex-test-management-static.spec.js`
- `node --check tests/e2e/system-codex-test-management-real.e2e.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260725-codex-test-row-execute-button/frontend-feature-evidence.md`

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260725-codex-test-row-execute-button/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用现有测试执行 API，显式按当前行 ID 发起执行。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端表格/样式：按 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，行级操作使用紧凑 inline text action，不新增大按钮样式。
- 静态合同与真实 E2E 同步门禁：修改 `system-codex-test-management-static.spec.js` 后重跑该静态合同，并确认真实 E2E 脚本文案不与页面冲突。
- Element Plus 表格操作门禁：行级按钮必须绑定当前可见业务行，不能依赖选中集合或表头选择状态。
- Codex Runner 自动测试门禁：本次只增加发起执行入口，不运行真实 Runner；不以静态合同冒充 Runner 执行通过。
