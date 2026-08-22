# 统一分支运行时端口合同到 V5

## Task Goal

将主工作区的分支运行时端口合同、槽位校验、槽位分配、守卫文档和回归测试统一到共享登记表使用的 `2026-08-21-branch-runtime-v5`，支持 slot `1..40`，消除主工作区 V4 对合法 slot 31..40 的误判。

## Milestones

1. 复现主工作区 V4 检测拒绝共享登记表 V5 slot 31 的失败。
2. 对比并同步 V5 端口合同相关文件，不迁移业务改动。
3. 运行回归测试和端口守卫，确认主工作区可识别共享 V5 登记表。
4. 收尾清理并记录最终验证。

## Expected Verification

- 主工作区运行时脚本声明 `2026-08-21-branch-runtime-v5`。
- V5 slot `31..40` 端口映射与共享登记表一致。
- 主工作区检测、槽位分配测试和回归测试通过。
- 共享登记表中的 V5 active 项（包含 slot 31）不再被主工作区校验误判；本任务验证时为 14 项，最终复核时并行任务新增登记为 16 项，均未由本任务修改。
- 不修改其他 worktree 的业务文件、分支、进程或端口登记。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；统一合同版本和唯一端口映射来源。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gate

- `docs\worktree-memory.md#Worktree 端口段与原子槽位门禁`：槽位范围、端口映射、登记合同和守卫必须一致；合同不一致必须 fail fast。
- `docs\worktree-memory.md#Worktree 旧无监听槽位释放门禁`：共享登记表修改必须使用与槽位分配相同的 mutex，并验证 active 唯一性。

## Current Status

completed

## Cleanup Candidates

No cleanup candidates.

## Cleanup Keep

- `doc/tasks/20260822-unify-branch-runtime-v5/task.md`
- `doc/tasks/20260822-unify-branch-runtime-v5/execution-log.md`
- `doc/tasks/20260822-unify-branch-runtime-v5/bug-regression-evidence.md`
- `doc/tasks/20260822-unify-branch-runtime-v5/verification-report.md`
