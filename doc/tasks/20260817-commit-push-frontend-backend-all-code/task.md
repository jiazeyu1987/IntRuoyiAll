# 提交并推送前后端全部代码

## Task Goal

- 核对 `int_main` 分支中的前后端代码状态。
- 提交当前尚未提交的前后端代码，并将全部本地前后端代码提交推送到 `origin/int_main`。
- 不提交或改动与本次前后端代码推送无关的用户文件和并行任务产物。

## Milestones

- [x] M1：读取 Git、PowerShell、任务收尾和经验门禁，盘点仓库与远端状态。
- [x] M2：运行分支运行端口保护检查，复核提交范围和待推送提交。
- [x] M3：提交仍未提交的前后端代码（如存在），推送 `int_main`。
- [x] M4：复核远端同步结果，完成任务记录和清理收尾。

## Expected Verification

- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- `IntRuoyiBackend` 与 `IntRuoyiFronted` 不存在未提交代码。
- 推送后 `HEAD` 与 `origin/int_main` 指向同一提交，当前分支不再领先远端。
- 暂存区和推送范围不包含凭据、运行态 PID 或本任务无关临时产物。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接核对并同步正式 Git 提交链，不创建替代分支或临时仓库。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md`。
- 适用门禁：`docs\powershell-memory.md` 中的提交推送、共享分支并发、提交后残余改动复扫、GitHub 网络与代理门禁。
- GitHub 大文件门禁检查结果：待推送历史最大 blob 为 1,392,582 字节，超过 100 MB 的 blob 数为 0。
- 当前仓库存在损坏的历史构建产物目录，Git 枚举会产生读取警告；不将该产物作为代码提交，也不删除不属于本任务的文件。

## Current Status

completed

- 前后端目录没有未提交代码，无需重复创建代码提交。
- 已将 25 个本地正式提交推送到 `origin/int_main`。
- 推送后本地与远端均指向 `3a523c3306b750b5a9aa0ccc7ebd896d75d5fd52`，领先/落后计数为 `0/0`。
- `task-closeout-cleanup` preview/apply 均通过，保留三份核心任务记录，未删除任何文件。
- 工作区其它任务改动保持原状，未纳入本次任务提交范围。
