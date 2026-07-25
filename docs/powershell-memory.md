# IntRuoyi PowerShell / Git 共同前置经验

## 触发场景

- PowerShell 中编排 Git、提交、推送、清理、长链路命令或多行脚本前，先读本文件。
- 任务要求提交、推送、基线保存、远端同步、处理脏工作区或检查 `origin` 前，先读本文件。
- 本文件是共同门禁；涉及中文读写时还必须读取 `docs\powershell-encoding.md`，涉及任务收尾时还必须读取 `docs\task-closeout-rules.md`。

## Git 提交与推送门禁

### 任务提交推送前置门禁

- Trigger: 任何任务进入实现提交、收尾提交、`git push`、处理 `ahead` 状态或保存脏工作区基线。
- Preflight check: 运行 `git status --short --branch`、`git branch --show-current`、`git remote -v`，并在提交前检查 staged 文件清单。
- Blocker: 当前目录不是 Git 仓库、当前分支不是预期分支、缺少可用 `origin`、存在无法解释的冲突、推送凭据/网络不可用、或 staged 文件混入不应提交的秘密文件。
- Verification: 记录每个提交的 commit hash、文件清单和 `git status --short --branch` 结果；推送后必须确认本地分支不再领先 `origin`。
- Forbidden action: 禁止用 force push、历史重写、destructive reset、丢弃脏改动、跳过 push、或把基线提交与当前任务提交混在一起作为绕过。
- Evidence: `doc\tasks\<task-id>\execution-log.md`、`docs\task-closeout-rules.md`、当前 Git 命令输出。
### 提交前 stale blocker 复验门禁

- Trigger: 准备提交时任务文档仍标记 `blocked`，但阻塞项可能已被其它并行改动修复。
- Preflight check: 不得直接按旧文档状态提交；先重跑旧 blocker 对应的最小门禁，例如 Maven compile、目标 JUnit、`pnpm ts:check` 或静态合同。
- Blocker: 复验仍失败、命令被沙箱/ACL 拦截且无法按审批重跑、或无法证明 blocker 已解除时，保持 blocked 并停止提交。
- Verification: 复验通过后更新 `task.md`、`execution-log.md` 和 `verification-report.md`，记录旧 blocker 解除、命令输出摘要和新的收尾状态。
- Forbidden action: 禁止只因用户要求提交就绕过 `blocked` 状态；禁止把旧 blocker 当作已解决而不重跑原门禁。
- Evidence: `doc\tasks\20260724-batch-fda-audit-log-coverage\execution-log.md`，2026-07-25 提交前重跑 Maven compile、目标 JUnit、`pnpm ts:check` 后解除旧阻塞。

### 脏工作区基线门禁

- Trigger: 开始当前任务或准备提交/推送时，`git status --short --branch` 显示 tracked、untracked 或 staged 脏改动。
- Preflight check: 先识别当前任务自有文件；提交基线前查看 staged 文件清单，确保当前任务实现文件未进入基线提交，除非用户明确要求。
- Blocker: 无法区分当前任务文件和既有脏改动、发现敏感凭据、发现超大文件、存在合并冲突、或用户未授权把既有脏改动作为独立基线保存。
- Verification: 基线提交完成后记录 commit hash、`git show --name-status --oneline -1` 文件清单、以及新的 `git status --short --branch`。
- Forbidden action: 禁止为了获得 clean worktree 而删除、回滚、覆盖或静默忽略别人/并发任务的脏改动。
- Evidence: 用户授权记录、基线提交 hash、任务日志中的文件清单。

### GitHub 推送大文件门禁

- Trigger: 推送到 GitHub remote、处理 `GH001`、`Large files detected`、`pre-receive hook declined`、Git LFS 或历史大文件问题。
- Preflight check: 推送前扫描待推送历史中的对象大小，至少确认没有超过 GitHub 100 MB 限制的 blob；必要时检查敏感文件和发布 evidence 是否误入 Git 历史。
- Blocker: 扫描发现超过 100 MB 的 blob，或 `git push` 返回 `GH001: Large files detected` / `pre-receive hook declined`。
- Verification: 记录对象扫描结果、`git push origin <branch>` 退出码、以及推送后的 `git status --short --branch`。
- Forbidden action: 禁止未经用户明确授权就执行历史重写、Git LFS 迁移、快照分支替代、force push 或删除远端历史。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24-github-推送前历史大文件门禁`。

## PowerShell 编排门禁

### PowerShell 命令编排

- Trigger: 需要在 PowerShell 中串联 Git、构建、测试、清理、SSH、MySQL、Node、Python 或多行命令。
- Preflight check: 明确 `workdir`、命令退出码、输入输出编码和敏感信息脱敏方式；串联命令用分行或分号。
- Blocker: 命令依赖未确认、路径未解析、退出码不可见、可能输出凭据、或需要中文 stdin/文件但未设计 UTF-8 路径。
- Verification: 每个关键命令记录退出码和必要摘要；中文文件写入后使用 UTF-8 方式重新读取。
- Forbidden action: 禁止使用 `&&`、默认编码 `Set-Content`/`Out-File` 写中文、吞掉错误、或记录密码/token/私钥。
- Evidence: `docs\powershell-encoding.md`、`doc\tasks\<task-id>\execution-log.md`。

### Codex 文件 ACL 受限写入门禁

- Trigger: `apply_patch`、Node、Python 或普通 PowerShell 对已存在源码/测试/任务文档返回 `apply deny-read ACLs`，但任务必须继续进行受控修改或验证。
- Preflight check: 先确认目标文件属于当前任务范围，并优先用 `rg`/`git diff -- <path>` 读取最小片段；若必须提升权限，命令必须限定到明确文件和单一验证命令。
- Blocker: 无法确认目标文件归属、需要修改并发任务文件、提升命令可能触碰任务外路径、或写入后无法用静态合同/类型检查验证时，停止并报告。
- Verification: 写入后立即执行目标片段搜索、`git diff -- <path>`、相关静态测试或类型检查，并在任务日志记录 ACL 原因、提升范围和验证结果。
- Forbidden action: 禁止因 ACL 拦截改用宽泛脚本批量扫描/重写目录；禁止跳过 diff 与测试直接宣称完成。
- Evidence: `doc\tasks\20260725-codex-test-method-target-items\execution-log.md`，前端测试管理页面方法项/目标项展示改造遇到 ACL 后采用限定文件 UTF-8 写入并通过静态测试与类型检查。
### PowerShell 命令文本管道字符门禁

- Trigger: 在 PowerShell 命令参数、字符串替换、TypeScript 类型联合、正则或 Markdown 内容中需要出现字面 `|`、`||`、尖括号、中文或多行文本，并且命令会通过 Codex sandbox/approval 执行。
- Preflight check: 不要把包含字面 `|` 的长脚本直接放进命令字符串；先改用 `apply_patch`，若 ACL 阻断，再用 `[char]124` 或占位符在 PowerShell 运行时组装目标文本，并在写入前后检查文件长度与 `git diff -- <path>`。
- Blocker: `apply_patch` 或 PowerShell 写入出现 ACL、timeout、文件长度变 0、BOM/编码漂移、或 diff 显示超出目标行的变化时，必须立即停止后续写入并先从 Git/source worktree 恢复文件。
- Verification: 写入后运行 `Get-Item <path> | Select-Object Length`、目标片段只读检查、`git diff -- <path>`、相关类型检查或静态合同，并把恢复证据写入 `doc\tasks\<task-id>\execution-log.md`。
- Forbidden action: 禁止在未确认 diff 和文件长度前继续提交；禁止用默认编码 `Set-Content`/`Out-File` 写中文；禁止把工具截断造成的空文件当成正常业务改动。
- Evidence: `doc\tasks\merge-jiluben-residual-20260725\execution-log.md`，`BatchRecordCellRulesConfirmDialog.vue` 一次 ACL 后 PowerShell 替换超时截断并恢复。
## 执行顺序

1. 阶段 1：任务提交/推送预检
   必查项: 当前分支、remote、工作区脏状态、staged 文件清单、用户授权边界。
   推荐命令: `git status --short --branch`、`git branch --show-current`、`git remote -v`。
   Fail Fast: 缺 Git 仓库、缺 `origin`、分支错误、发现无法归属的敏感文件。
   必须记录: 分支、remote、脏文件摘要、是否需要基线提交。

2. 阶段 2：基线提交
   必查项: 当前任务文件不得混入基线；所有既有脏改动必须可追踪。
   推荐命令: `git diff --cached --name-status`、`git status --short`、`git show --name-status --oneline -1`。
   Fail Fast: 无法安全分离当前任务文件、存在冲突、存在秘密或大文件风险。
   必须记录: 基线提交 hash 和文件清单。

3. 阶段 3：任务提交与推送
   必查项: 当前任务实现、收尾记录、经验沉淀状态、GitHub 大文件门禁。
   推荐命令: `git diff --check`、对象大小扫描命令、`git push origin <branch>`、`git status --short --branch`。
   Fail Fast: verification 未通过、推送被拒、仍然 ahead、或缺凭据/网络。
   必须记录: 实现提交 hash、收尾提交 hash、推送结果和最终状态。
