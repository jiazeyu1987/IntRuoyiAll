# 20260728 Commit Current Code

## Task Goal

- 按用户要求提交并推送当前 `int_main` 代码状态。
- 若当前工作区没有业务代码脏改动，则只记录本次提交/推送门禁证据并推送任务收尾记录。

## Milestones

- [x] 读取提交、推送、PowerShell 编排和收尾门禁。
- [x] 检查当前分支、远端和工作区状态。
- [x] 完成提交前验证并提交本次任务记录。
- [x] 推送当前分支到 `origin` 并确认不再 ahead。
- [x] 运行 closeout cleanup preview/apply，记录最终验证并标记完成。

## Expected Verification

- `git status --short --branch` 显示当前分支状态。
- `git branch --show-current` 确认为 `int_main`。
- `git remote -v` 显示可用 `origin` fetch/push。
- `git diff --check` 通过。
- GitHub 推送前历史大文件扫描无超过 100 MB 的对象。
- `git push origin int_main` 成功。
- 推送后 `git status --short --branch` 不再显示 ahead。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本任务仅执行 Git 提交推送门禁，不改变业务逻辑。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### Git 提交与推送门禁

- Trigger: 用户要求提交、推送、处理当前代码状态。
- Preflight check: 运行 `git status --short --branch`、`git branch --show-current`、`git remote -v`，提交前检查 staged 文件清单。
- Blocker: 当前目录不是 Git 仓库、当前分支异常、缺少可用 `origin`、存在无法解释冲突、推送凭据/网络不可用、或 staged 文件混入秘密文件。
- Verification: 记录提交 hash、文件清单、推送结果和推送后的分支状态。
- Forbidden action: 禁止 force push、历史重写、destructive reset、丢弃脏改动、跳过 push，或把无关改动混入提交。
- Evidence: `docs/powershell-memory.md`、`docs/task-closeout-rules.md`、`doc/tasks/20260728-commit-current-code/execution-log.md`。

### PowerShell UTF-8 与命令编排门禁

- Trigger: PowerShell 中读取/写入中文 Markdown、编排 Git 提交推送。
- Preflight check: 使用 UTF-8 读取中文文件；串联命令不用 `&&`；每个关键命令记录退出码和摘要。
- Blocker: 编码路径不明确、命令失败被吞掉、可能输出敏感信息、或写入可能造成乱码。
- Verification: 关键文档写入后用 UTF-8 方式读取，并运行 `git diff --check`。
- Forbidden action: 禁止默认编码写中文、禁止吞错、禁止记录密码/token/私钥。
- Evidence: `docs/powershell-encoding.md`、`docs/powershell-memory.md`。

### GitHub 推送前历史大文件门禁

- Trigger: 将当前分支推送到 GitHub remote。
- Preflight check: 推送前扫描待推送历史中的 blob 大小，确认没有超过 GitHub 100 MB 单文件限制的对象。
- Blocker: `git push` 返回 `GH001: Large files detected` / `pre-receive hook declined`，或本地历史扫描发现任一 blob 超过 100 MB。
- Verification: 记录对象扫描结果、目标远端 URL、分支和推送退出结果。
- Forbidden action: 禁止未经用户明确授权执行历史重写、Git LFS 迁移、快照分支替代、force push 或删除远端历史。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24-github-推送前历史大文件门禁`。
