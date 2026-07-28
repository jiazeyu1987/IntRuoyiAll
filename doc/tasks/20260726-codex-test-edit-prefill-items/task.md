# 20260726-codex-test-edit-prefill-items

## Task Goal

修复「测试管理」点击“修改”时的表单回显：测试方法项和测试目标项必须先按当前测试项已有内容逐条显示出来，用户再在这些现有条目基础上修改。

## Milestones

- [x] 记录 BDD 场景和经验门禁
- [x] 增加最小静态合同并先 RED
- [x] 实现编辑回显归一化
- [x] 运行目标验证并记录结果
- [x] 完成收尾记录

## Expected Verification

- `pnpm e2e:system:codex-test-management:static` 先 RED 后 GREEN。
- `pnpm ts:check` 通过。
- `git diff --check` 针对本任务相关文件通过。

## Current Status

ready_for_closeout

提交/推送未执行：当前工作区存在大量非本任务脏改动，且 `IntRuoyiFronted/src/views/system/codex-test-management/index.vue` 与 `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js` 已有同文件并行改动；为避免混入其它任务，仅保留本任务工作区改动和验证记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，编辑时归一化现有 `methodText` 和 `checkpoints`，保持既有 API 契约。
- `是否存在临时补丁或绕过`：否

## 经验门禁

- 静态合同与真实 E2E 同步门禁：修改 `tests/e2e/*static.spec.js` 时，必须确保断言覆盖当前真实页面结构；本任务使用测试管理静态合同做窄范围 RED/GREEN。
- Codex Runner 自动测试门禁：涉及「系统管理 > 测试管理」自然语言测试项；本任务仅修复编辑回显 UI，不启动真实 Runner。
- Element Plus 选择框显示门禁：紧凑弹窗多列表单要使用专用布局类和静态合同锁定，方法/目标逐条回显不得只依赖 tooltip 或列表文本。

## Cleanup Keep

- doc/tasks/20260726-codex-test-edit-prefill-items/frontend-feature-evidence.md
