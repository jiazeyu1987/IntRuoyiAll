# 20260727 Todo Task Hidden Status System Error

## Task Goal

修复用户反馈的“待办任务加载失败 / 隐藏任务状态：系统异常”问题，定位根因并通过回归测试证明待办任务加载与隐藏任务状态展示不再触发系统异常。

## Milestones

- [x] 创建隔离 worktree 与任务记录，读取缺陷修复、前端、后端、PowerShell、worktree 和收尾规则。
- [x] 定位“待办任务加载失败 / 隐藏任务状态”相关前后端链路与现有测试。
- [x] 先补充可复现的失败回归测试，记录 RED 证据。
- [x] 实施最小根因修复，不引入 fallback、吞异常或默认成功。
- [x] 运行目标验证与相关回归，记录 GREEN 与风险范围。
- [ ] 完成验证报告、经验沉淀、提交与推送。

## Expected Verification

- 聚焦前端静态合同或组件测试覆盖隐藏任务状态加载失败场景。
- 如根因在后端，则运行对应 Maven 目标测试。
- 相关前端类型检查或最小合同测试通过。
- `scripts\preflight\branch-runtime-port-guard.ps1` 在提交前通过。

## Current Status

ready_for_closeout：已确认本机运行库缺少 `system_profile_workbench_task_visibility` 表，已应用既有正式迁移并补充迁移契约测试；个人工作台真实 E2E 已通过，待完成经验沉淀、提交和推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是定位真实异常来源并用回归测试约束。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- `docs\database-rules.md`：页面出现 `系统异常` 且后端代码引用 schema 字段或表时，先只读核对当前真实库是否已应用对应迁移；不得用前端隐藏错误、后端默认值、mock 成功或跳过迁移替代。
- `docs\frontend-development.md#前端静态契约隔离门禁`：本任务使用聚焦静态合同与迁移契约验证，不把无关全量类型检查作为通过证据。
- `docs\e2e-rules.md`：真实页面验证使用本机 `localhost:8081` / `127.0.0.1:48081`，只读检查个人工作台待办加载，不改业务数据。

## Cleanup Keep

- doc/tasks/20260727-todo-task-hidden-status/bug-regression-evidence.md
- doc/tasks/20260727-todo-task-hidden-status/profile-workbench-visibility-migration-policy-gate.json
