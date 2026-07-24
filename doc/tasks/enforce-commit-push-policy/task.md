# Task: 强制任务提交与 Git 推送

## Task Goal

更新 `AGENTS.md` 的 Git 规则：每个任务完成后必须提交并推送当前分支；工作区存在脏改动时，先将脏改动作为独立基线提交，再提交本任务实现和收尾记录，最后推送。长任务在提交推送前必须先运行 `project-experience-consolidation`。

## Milestones

- [x] 创建任务目录并记录用户授权的脏工作区提交例外
- [x] 提交当前脏工作区基线
- [x] 更新 `AGENTS.md`、`docs/task-closeout-rules.md` 与 `docs/powershell-memory.md`
- [x] 验证提交和推送规则文本
- [x] 提交本任务实现
- [x] 提交本任务收尾记录
- [x] 推送 `int_main` 并记录远端验证

## Expected Verification

- `AGENTS.md` 明确每个任务完成后必须 commit + push。
- `AGENTS.md` 明确脏工作区先独立提交，再提交当前任务。
- `AGENTS.md` 明确长任务先运行 `project-experience-consolidation`。
- 当前 `int_main` 的所有本地提交均成功推送到 `origin`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将脏工作区、提交、推送和长任务经验沉淀固化为强制流程。
- `是否存在临时补丁或绕过`：是，用户明确授权将现有脏工作区作为独立基线提交；风险是其中可能包含并发任务内容，缓解方式是独立提交、完整记录文件清单并在当前任务后再推送。

## 经验门禁

- `docs/experience-index.md` 已读取，命中 GitHub 推送与 PowerShell 命令编排规则。
- 推送前必须核对 remote、分支、暂存区、提交文件列表和大文件/敏感信息风险。
- `docs/powershell-memory.md` 已按用户授权新建并读取，PowerShell/Git 编排门禁已补齐。
- GitHub 推送历史大文件门禁已读取；发现超过 100 MB blob 或 `GH001` 时必须停止，不得未授权重写历史或 force push。
- 既有脏工作区已保存为独立基线提交 `44fb3915`；后续新增非本任务脏区已继续保存为 `bb3c36ba`、`49a97fee`、`e646f935`、`4d894369`、`be06a6b1`、`6c95e640`、`dd271d39`、`648a57df`、`8f155b9c`、`574290d1`、`c15947b3`、`aec3ae64`、`298009eb`、`6361c4be`、`2dbade97`，文件清单记录于 `execution-log.md`。
- 本任务实现已提交为 `19e9573a`。
- task-closeout-cleanup preview/apply 已通过；当前为主 worktree，不涉及 worktree 合并或删除；无清理删除项。
- 当前任务不涉及服务器、数据库、发布、恢复、真实 E2E 或 worktree 合并/清理，因此不触发高风险 `experience-preflight`。

## Current Status

completed
