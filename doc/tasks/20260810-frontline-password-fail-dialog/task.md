# 一线生产密码校验失败弹框

## Task Goal

让一线生产密码校验失败时与提交成功一样显示居中的业务弹框，弹框在全屏状态下可见，并复用提交成功弹框的样式与尺寸。

## Milestones

- [x] 记录 BDD/TDD 场景并定位现有提交成功弹框与密码失败提示链路。
- [x] 先补充失败场景静态合同，确认当前行为 RED。
- [x] 实现密码校验失败业务弹框，并保持提交成功弹框样式大小一致。
- [x] 运行目标合同、相邻回归与结构检查，记录证据。

## Expected Verification

- 目标静态合同先 RED 后 GREEN，覆盖密码校验失败使用提交结果弹框而不是顶部 message/toast。
- 相邻一线生产提交静态回归通过。
- `pnpm ts:check` 通过。
- `git diff --check` 通过。

## Current Status

ready_for_closeout

已完成实现与验证，等待 task-closeout-cleanup preview/apply 后标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；密码校验失败必须按正式错误态展示，不添加兼容或默认成功分支。
- `是否从根因和长期维护角度解决`：是；复用现有提交成功弹框展示层，统一提交结果反馈。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`：提交成功、失败和响应不确定必须分层展示，失败不得清空草稿或冒充成功。
- `docs/e2e-rules.md#顶部固定信息栏真实视口边界门禁`：全屏或大屏布局反馈必须考虑真实视口边界，目标反馈不可被顶部栏或侧栏遮挡。

## Cleanup Keep

- doc/tasks/20260810-frontline-password-fail-dialog/task.md
- doc/tasks/20260810-frontline-password-fail-dialog/execution-log.md
- doc/tasks/20260810-frontline-password-fail-dialog/verification-report.md
