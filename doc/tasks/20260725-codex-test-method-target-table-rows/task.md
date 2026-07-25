# Codex 测试管理方法目标表格行展示

## Task Goal

- 将测试管理列表中“测试方法项 / 测试目标项”从单元格内多行展示改成表格级多行展示。
- 每个方法项或目标项占用一条独立表格行；同一测试项的名称、检查点、默认方法、状态和操作列合并显示。
- 修正排产工单手动重排样例：黄色范围内的两条核验描述属于测试目标项，不再放在测试方法项里。

## Milestones

- [x] 记录 BDD/TDD 与经验门禁。
- [x] 先补静态契约，要求列表使用展开表格行与合并行逻辑。
- [x] 修改前端页面与样例 E2E 数据归属。
- [x] 修复后端检查点重复替换的软删除唯一键冲突。
- [x] 运行静态、类型、后端回归与证据校验。
- [x] cleanup、提交并推送任务自有改动。

## Expected Verification

- `node tests/e2e/system-codex-test-management-static.spec.js`
- `node --check tests/e2e/system-codex-test-management-real.e2e.js`
- `node --check doc/tasks/20260725-test-management-manual-replan-881mo/test-management-manual-replan-full.e2e.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-system -am -Dtest=CodexTestCaseServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260725-codex-test-method-target-table-rows/frontend-feature-evidence.md`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过表格展示模型展开行，并修复检查点替换时软删除唯一键冲突的根因；不改变前后端 API 契约。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端表格/样式：按 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持操作台式紧凑表格、明确列宽和可扫描行。
- Codex Runner 自动测试门禁：本次只改测试管理页面展示和静态/语法验证，不执行真实 Runner；不以静态验证冒充真实 Runner 通过。
- 静态合同与真实 E2E 同步门禁：修改静态合同时同步真实 E2E 可见文案和样例脚本，不保留过期定位文案。
- PowerShell/UTF-8 门禁：中文文档与脚本读取使用 UTF-8；PowerShell 串联不使用 `&&`。

## Cleanup Keep

- `doc/tasks/20260725-codex-test-method-target-table-rows/frontend-feature-evidence.md`
