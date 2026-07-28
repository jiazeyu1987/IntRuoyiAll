# 20260726-codex-test-target-item-input-display

## Task Goal

修复「新增测试项」弹窗中测试目标项名称输入框显示不完整的问题，确保目标项序号控件和名称输入框在同一行内完整可见。

## Milestones

- [x] 记录 BDD 场景和复现证据
- [x] 增加最小静态回归测试并先 RED
- [x] 修复前端布局根因
- [x] 运行目标验证并记录结果
- [x] 完成收尾记录

## Expected Verification

- 目标静态回归测试先失败后通过。
- 受影响页面源码布局不引入 fallback、降级或临时绕过。
- 任务记录包含 RED/GREEN 和最终验证结果。

## Current Status

ready_for_closeout

提交/推送未执行：工作区在本任务开始前已有大量非本任务脏改动，且 `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`、`IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`、`docs/e2e-rules.md`、`docs/experience-index.md` 均包含并行改动；为避免混入其它任务，当前仅保留本任务工作区改动和验证记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，目标是修正表单目标项行的控件宽度/布局约束。
- `是否存在临时补丁或绕过`：否

## 经验门禁

- 静态合同与真实 E2E 同步门禁：修改 `tests/e2e/*static.spec.js` 时，必须先确认静态合同覆盖当前真实页面结构；窄范围页面缺陷可新增聚焦断言，不得顺手修改无关产品逻辑或用过期流程断言。
- Codex Runner 自动测试门禁：涉及「系统管理 > 测试管理」时，真实 Runner/E2E 需要先确认前后端入口、测试租户、Runner token、Codex CLI 和任务数据；本任务仅执行页面源码静态合同，不启动真实 Runner。
