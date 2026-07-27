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

### 同文件并行改动选择性暂存门禁

- Trigger: 基线提交后继续出现并行改动，且当前任务与非本任务改动落在同一个源码或测试文件。
- Preflight check: 提交当前任务前先用 `git diff -- <path>` 区分本任务 hunks 与并行 hunks；使用选择性暂存或 cached patch，只暂存本任务变更。
- Blocker: 无法可靠区分 hunks、同一逻辑块互相覆盖、或测试只能在混入并行改动后通过时，必须停止并报告冲突。
- Verification: 提交前记录 `git diff --cached --name-status` 和目标文件 staged diff，确认 staged 内容只包含本任务改动；提交后用 `git status --short --branch` 确认并行改动仍留在工作区而未混入当前任务提交。
- Forbidden action: 禁止把同文件并行改动混进当前任务实现提交；禁止为了 clean 状态回滚、覆盖或删除并行改动。
- Evidence: `doc\tasks\20260726-route-flow-form-slot-count-badge\execution-log.md`，`RouteFlowGraphDesigner.vue` 在徽标任务中与并行表单配置改动同文件共存，需选择性暂存本任务 hunks。

### 提交后残余改动复扫门禁

- Trigger: 执行基线提交、实现提交或收尾提交后，准备继续下一步提交或推送前。
- Preflight check: 每次提交后立即运行 `git status --short --branch` 与 `git diff --name-status`，确认是否还有延迟保存、并行任务或新生成的源码/测试/文档改动。
- Blocker: 发现新的已修改源码、测试、任务文档或生成物且无法确认归属；发现新改动属于当前用户“全部提交”范围但尚未提交；发现不属于当前任务的并行目录将被 `git add -A` 混入。
- Verification: 对归属明确的残余改动单独暂存、提交并记录 commit hash；对并行任务目录保持未暂存并在任务日志说明未触碰原因。
- Forbidden action: 禁止只看最近一次 `git commit` 成功就直接推送；禁止在未复扫状态时使用宽泛 `git add -A`；禁止把提交后新出现的并行任务目录混入当前任务收尾提交。
- Evidence: `doc\tasks\20260727-commit-frontend-backend-code\execution-log.md`，提交前后端代码时两次提交后复扫分别发现前端 Runner 残余改动、后端批记录报表残余改动，并拆分提交保留边界。

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
- Preflight check: 不要把包含字面 `|` 的长脚本直接放进命令字符串；先改用 `apply_patch`，若 ACL 阻断，再用 `[char]124` 或占位符在 PowerShell 运行时组装目标文本，并在写入前后检查文件长度与 `git diff -- <path>`。不要用 `-replace` 拼接 `` `r`n`` 写源码 import 或测试文件；这类写法容易把反引号序列按字面写入文件。
- Blocker: `apply_patch` 或 PowerShell 写入出现 ACL、timeout、文件长度变 0、BOM/编码漂移、或 diff 显示超出目标行的变化时，必须立即停止后续写入并先从 Git/source worktree 恢复文件。
- Verification: 写入后运行 `Get-Item <path> | Select-Object Length`、目标片段只读检查、`git diff -- <path>`、相关类型检查或静态合同，并把恢复证据写入 `doc\tasks\<task-id>\execution-log.md`。
- Forbidden action: 禁止在未确认 diff 和文件长度前继续提交；禁止用默认编码 `Set-Content`/`Out-File` 写中文或源码；禁止把工具截断、反引号字面量、BOM 漂移造成的文件异常当成正常业务改动。
- Evidence: `doc\tasks\merge-jiluben-residual-20260725\execution-log.md`，`BatchRecordCellRulesConfirmDialog.vue` 一次 ACL 后 PowerShell 替换超时截断并恢复；`doc\tasks\20260726-route-start-batch-record-attachments\execution-log.md`，测试 import 追加时 `` `r`n`` 被写成字面量，改回 `apply_patch` 并复跑 Maven 通过。

### PowerShell Maven -D 参数引号门禁

- Trigger: 在 PowerShell 中运行 Maven 且参数包含带点属性名的 `-D`，例如 `-Dsurefire.failIfNoSpecifiedTests=false`。
- Preflight check: 将每个 Maven `-D...` 参数整体加双引号，例如 `"-Dtest=CodexTestCaseServiceImplTest"` 与 `"-Dsurefire.failIfNoSpecifiedTests=false"`；多模块目标测试继续保留 `-pl <module> -am`。
- Blocker: Maven 报 `Unknown lifecycle phase ".<property>=..."` 时必须停止并按 PowerShell 参数解析问题处理，不得改动测试范围或跳过目标 JUnit。
- Verification: 复跑加引号后的 Maven 命令，记录原失败与复跑 PASS；若上游 reactor 模块不含目标测试类，同时记录 `surefire.failIfNoSpecifiedTests=false` 的依据。
- Forbidden action: 禁止把 PowerShell 参数拆分错误误判为产品编译失败；禁止移除 `-am` 或改成更宽测试作为绕过。
- Evidence: `doc\tasks\20260726-codex-test-case-project-column\execution-log.md`，目标 JUnit 首次因 PowerShell 拆分 `-Dsurefire.failIfNoSpecifiedTests=false` 失败，整体加引号后通过；`doc\tasks\20260726-work-order-field-cell-link\execution-log.md`，目标 MES JUnit 需同时整体加引号 `"-Dtest=MesProBatchRecordCellLinkServiceImplTest,MesProBatchRecordCellLinkSchemaTest"` 与 `"-Dsurefire.failIfNoSpecifiedTests=false"`，并保留 `-am` 编译依赖模块源码。
## 执行顺序

1. 阶段 1：任务提交/推送预检
   必查项: 当前分支、remote、工作区脏状态、staged 文件清单、用户授权边界。
   推荐命令: `git status --short --branch`、`git branch --show-current`、`git remote -v`。
   Fail Fast: 缺 Git 仓库、缺 `origin`、分支错误、发现无法归属的敏感文件。
   必须记录: 分支、remote、脏文件摘要、是否需要基线提交。

2. 阶段 2：基线提交
   必查项: 当前任务文件不得混入基线；所有既有脏改动必须可追踪。
   推荐命令: `git diff --cached --name-status`、`git status --short`、`git diff --name-status`、`git show --name-status --oneline -1`。
   Fail Fast: 无法安全分离当前任务文件、存在冲突、存在秘密或大文件风险。
   必须记录: 基线提交 hash、文件清单和提交后残余改动复扫结果。

3. 阶段 3：任务提交与推送
   必查项: 当前任务实现、收尾记录、经验沉淀状态、GitHub 大文件门禁。
   推荐命令: `git diff --check`、对象大小扫描命令、`git push origin <branch>`、`git status --short --branch`。
   Fail Fast: verification 未通过、推送被拒、仍然 ahead、或缺凭据/网络。
   必须记录: 实现提交 hash、收尾提交 hash、推送结果和最终状态。
