# 子 Agent 调度基线

## Task Goal

将用户确认的子 Agent 调度调用方式写入工作区根目录 `AGENTS.md`，供后续任务长期遵循。

## Milestones

- [x] 检查现有 `AGENTS.md` 是否存在同义或冲突规则。
- [x] 追加唯一一条已确认的 Thread baseline。
- [x] 完成结构与重复性验证。

## Expected Verification

- 根目录 `AGENTS.md` 仅新增一条目标基线。
- 规则明确要求直接调用 collaboration 调度接口，禁止嵌套 `functions.exec`，并区分 `wait_agent` 与 exec `wait`。
- `git diff --check -- AGENTS.md doc/tasks/20260809-subagent-collaboration-baseline` 通过。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；把已确认的工具边界固化为工作区长期规则。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260809-subagent-collaboration-baseline/task.md
- doc/tasks/20260809-subagent-collaboration-baseline/execution-log.md
- doc/tasks/20260809-subagent-collaboration-baseline/verification-report.md
