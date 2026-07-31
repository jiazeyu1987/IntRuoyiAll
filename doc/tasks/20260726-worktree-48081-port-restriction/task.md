# 20260726 Worktree 48081 Port Restriction

## Task Goal

- 在根 `AGENTS.md` 中增加限制：`D:\IntRuoyiWorktree\` 下的 worktree 不能占用 `48081` 端口。

## Milestones

- [x] 记录任务范围、适用规则与端口治理约束。
- [x] 在 `AGENTS.md` 中补充 `D:\IntRuoyiWorktree\` worktree 禁用 `48081` 的明确限制。
- [x] 验证规则文本可被搜索定位，且未修改无关文件。

## Expected Verification

- `rg "D:\\IntRuoyiWorktree|48081|worktree" AGENTS.md docs\branch-runtime-ports.md docs\worktree-restrictions.md`
- `git diff -- AGENTS.md doc\tasks\20260726-worktree-48081-port-restriction`

## Current Status

ready_for_closeout

## Remaining Blockers

- Git closeout is blocked by pre-existing unrelated workspace state: branch `int_main` was already ahead of `origin/int_main` by 20 commits and the worktree already contained many modified/untracked files before this task. This task did not stage, commit, push, or modify unrelated files.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过根 Agent 规则补充端口占用限制，避免 `D:\IntRuoyiWorktree\` 任务 worktree 误用主工作区后端端口。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Worktree 隔离运行态 URL 门禁：worktree 运行态必须使用同一 runtime slot 的前后端端口，不能静默切回 `8081/48081` 或 API-only。
- 本地运行端口门禁：`48081` 是 `E:\IntRuoyi` 的 `int_main` 后端专属端口，worktree 必须按 `docs\worktree-restrictions.md` 的 profile + slot 规则使用独立端口。
- 规则优先级门禁：已读取全局 `C:\Users\BJB110\.codex\AGENTS.md` 和当前根 `AGENTS.md`；本次仅修改当前项目根规则。
