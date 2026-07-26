# 20260726-codex-test-method-items-split

## Task Goal

将「新增测试项」弹窗中的测试方法项从单个多行文本框改为可逐项新增/删除的结构化录入方式，和测试目标项一样按 1、2、3、4 项分开维护；保存时继续使用既有 `methodText` 换行文本字段，不改后端契约。

## Milestones

- [x] 记录 BDD 场景和经验门禁
- [x] 增加最小静态合同并先 RED
- [x] 实现方法项逐项录入、编辑回填和保存序列化
- [x] 运行目标验证并记录结果
- [x] 完成收尾记录

## Expected Verification

- `pnpm e2e:system:codex-test-management:static` 先 RED 后 GREEN。
- `pnpm ts:check` 通过。
- `git diff --check` 针对本任务相关文件通过。

## Current Status

ready_for_closeout

提交/推送未执行：当前分支已 `ahead 1`，且工作区存在大量非本任务脏改动；本任务涉及的 `IntRuoyiFronted/src/views/system/codex-test-management/index.vue` 与 `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js` 已存在并行改动，为避免混入其它任务，仅保留工作区改动和验证记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，复用现有 `methodText` 契约，仅把前端录入交互结构化。
- `是否存在临时补丁或绕过`：否

## 经验门禁

- 静态合同与真实 E2E 同步门禁：修改 `tests/e2e/*static.spec.js` 时，必须确保断言覆盖当前真实页面结构；本任务使用现有测试管理静态合同做窄范围 RED/GREEN。
- Codex Runner 自动测试门禁：涉及「系统管理 > 测试管理」和自然语言测试方法；本任务只改录入 UI 和静态合同，不启动真实 Runner。
- Element Plus 选择框显示门禁：紧凑弹窗多列表单要使用专用布局类和静态合同锁定，避免控件互相挤压。

## Cleanup Keep

- doc/tasks/20260726-codex-test-method-items-split/frontend-feature-evidence.md
