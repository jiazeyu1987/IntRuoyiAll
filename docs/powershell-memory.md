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

### Git index.lock 陈旧锁恢复门禁

- Trigger: `git add`、`git commit`、`git merge` 或其它索引写操作报 `Unable to create '.git/index.lock': File exists`。
- Preflight check: 检查 `.git/index.lock` 的绝对路径、长度和最后写入时间，并只读枚举活动 `git` / `git-lfs` 进程；只有锁文件为 0 字节、超过 60 秒未更新且不存在活动 Git 进程时，才允许删除该精确锁文件。
- Blocker: 存在活动 Git 进程、锁文件非空、锁文件不足 60 秒、目标不是当前仓库精确 `.git/index.lock`，或删除后仍无法正常读取 Git 状态时必须停止。
- Verification: 删除后确认锁文件不存在，重新运行 `git status --short --branch`、原失败的索引写操作和 staged 文件清单检查；记录未停止任何运行进程。
- Forbidden action: 禁止看到 `index.lock` 就直接删除、强杀全部 Git/IDE 进程、删除其它 `.lock` 文件、清空 `.git` 或用新仓库绕过当前索引状态。
- Evidence: `doc/tasks/20260728-commit-int-main-frontend-backend-code-round2/execution-log.md`，基线暂存前确认零字节陈旧锁且无活动 Git 进程后恢复。

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

### 共享分支并发基线提交门禁

- Trigger: 多个任务共用同一分支或主工作区时，最近一次提交不是当前任务发起，且 `git show --name-status -1` 包含当前任务文件、其它任务文档、非当前模块源码或运行产物。
- Preflight check: 每次实现改动后、提交前、cleanup 前都运行 `git log --oneline -5`、`git show --name-status --oneline -1`、`git status --short --branch` 和当前任务文件的 `git diff HEAD -- <paths>`，确认当前任务改动是否已被并发基线提交吞入。
- Blocker: 当前任务实现被并发基线提交混入且需要严格任务独立提交、最近提交包含敏感文件/超大产物、或同一文件还有未区分 hunks 时，必须停止并让用户决定是否重建独立提交历史；不得继续用宽泛 `git add -A`。
- Verification: 若用户允许继续，任务日志必须记录并发提交 hash、混入范围、保留/不触碰的非任务文件、已通过的目标验证和后续只选择性暂存的文件清单。
- Forbidden action: 禁止把并发基线提交伪装成本任务实现提交；禁止因为 HEAD 已包含代码就跳过验证、任务文档或冲突记录；禁止擅自 amend、reset、force-push 或用新提交覆盖他人并发改动。
- Evidence: `doc\tasks\20260730-dcc-product-catalog-remove-toolbar-buttons\execution-log.md`，共享分支基线提交曾把 DCC 按钮删除任务文件和其它任务文件一起提交，后续只能记录异常并选择性收尾；`doc\tasks\20260804-approval-center-applicant-column\execution-log.md`，审批中心申请人列实现被并行基线提交吞入，后续保留验证证据并仅选择性提交收尾记录。

### 同文件并行改动选择性暂存门禁

- Trigger: 基线提交后继续出现并行改动，且当前任务与非本任务改动落在同一个源码或测试文件。
- Preflight check: 提交当前任务前先用 `git diff -- <path>` 区分本任务 hunks 与并行 hunks；使用选择性暂存或 cached patch，只暂存本任务变更。
- Blocker: 无法可靠区分 hunks、同一逻辑块互相覆盖、或测试只能在混入并行改动后通过时，必须停止并报告冲突。
- Verification: 提交前记录 `git diff --cached --name-status` 和目标文件 staged diff，确认 staged 内容只包含本任务改动；提交后用 `git status --short --branch` 确认并行改动仍留在工作区而未混入当前任务提交。
- Forbidden action: 禁止把同文件并行改动混进当前任务实现提交；禁止为了 clean 状态回滚、覆盖或删除并行改动。
- Evidence: `doc\tasks\20260726-route-flow-form-slot-count-badge\execution-log.md`，`RouteFlowGraphDesigner.vue` 在徽标任务中与并行表单配置改动同文件共存，需选择性暂存本任务 hunks。

### 文档目录精确暂存门禁

- Trigger: 用户询问或授权将 `scripts`、`doc`、`docs` 纳入 Git，且工作区同时存在源码、测试或并行任务改动。
- Preflight check: 先用 `git status --short --untracked-files=all -- scripts doc docs` 盘点候选文件，再用显式路径 `git add -- <paths>` 暂存；暂存后必须运行 `git diff --cached --name-status`。
- Blocker: 暂存区出现非本次授权范围的源码、测试、超大产物、临时日志、pid 或无法归属文件时，必须停止提交并清理暂存边界。
- Verification: 对误入暂存区但仍需保留工作区内容的文件，只使用 `git restore --staged -- <path>` 移出暂存区，并再次复核 cached 清单只剩授权目录。
- Forbidden action: 禁止用 `git add doc docs scripts` 或 `git add -A` 代替候选清单；禁止为了修正暂存区而回滚、删除或覆盖工作区改动。
- Evidence: 2026-07-28 文档目录加入 Git 前，复核发现前端源码/测试误在暂存区，使用 `git restore --staged` 保留工作区内容并只留下 `doc`、`docs` 文档文件。

### 批量暂存脚本被拦截时的显式路径门禁

- Trigger: 准备提交大量前后端、文档和证据文件，复杂 PowerShell 脚本或 `pathspec-from-file` 暂存命令被 Codex/终端策略拦截。
- Preflight check: 不继续扩大脚本权限；改用明确目录和文件路径分批 `git add -- <paths>`，随后用 `git restore --staged -- <paths>` 移出当前任务记录、`.pid`、`diff --check` 失败文件和明显临时产物。
- Blocker: 无法列出明确路径、暂存区混入当前任务收尾记录、PID、超大文件、敏感文件、`git diff --cached --check` 失败项，或提交后仍有同一源码文件持续被并行任务写入且无法稳定归属。
- Verification: 每批暂存后运行 `git diff --cached --name-status` 和 `git diff --cached --check`；每次提交后运行 `git status --short --branch` 与 `git diff --name-status`，直到只剩明确不可提交项或当前任务收尾文件。
- Forbidden action: 禁止因批量脚本被拦截就改用宽泛 `git add -A`；禁止为了通过 `diff --check` 修改无关任务证据文件或 PDF；禁止把 `.pid` 运行态文件混入提交。
- Evidence: `doc/tasks/20260802-commit-current-frontend-backend-code/execution-log.md`，批量 pathspec 暂存被策略拦截后改用显式路径、移出 PID 和 `diff --check` 失败证据文件，并对多轮提交后残余改动逐轮复扫提交。

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

### GitHub HTTPS 443 本地代理门禁

- Trigger: `git push`、`git fetch`、`git ls-remote` 或 GitHub HTTPS remote 报 `Failed to connect to github.com port 443 via 127.0.0.1`、`Could not connect to server`、`Connection timed out`、`Recv failure: Connection was reset`。
- Preflight check: 先运行 `git config --show-origin --get-regexp "^(http|https)\.(proxy|sslVerify|version|postBuffer)|^url\..*\.insteadOf$"`、`Test-NetConnection 127.0.0.1 -Port <proxyPort>`、`Test-NetConnection github.com -Port 443`；如果用户有 FlClash/Clash/VPN，再确认其配置端口和实际监听端口一致。
- Blocker: Git 配置指向本地代理但代理端口未监听、GitHub HTTPS 直连 443 不通、代理客户端仅 helper 进程在线但核心未监听、或 SSH 443 可达但 GitHub 不接受当前 SSH key。
- Verification: `git ls-remote origin HEAD` 成功返回 HEAD；若改用 SSH 443，必须先用 `ssh -T -o BatchMode=yes git@ssh.github.com -p 443` 验证账号认证成功，再改 remote 或 pushurl。
- Forbidden action: 禁止把删除 Git proxy 当作修复，除非已证明 GitHub HTTPS 直连 443 可用；禁止静默切换到 SSH remote，除非当前 SSH key 已被 GitHub 接受；禁止把 helper 服务监听端口误当作 HTTP/SOCKS 代理端口。
- Evidence: `doc\tasks\20260731-git-443-push-fix\execution-log.md`，GitHub HTTPS 直连失败、Git 全局代理指向 `127.0.0.1:7890` 但 FlClash 核心未监听，SSH 443 网络可达但公钥未授权。

## PowerShell 编排门禁

### PowerShell 命令编排

- Trigger: 需要在 PowerShell 中串联 Git、构建、测试、清理、SSH、MySQL、Node、Python 或多行命令。
- Preflight check: 明确 `workdir`、命令退出码、输入输出编码和敏感信息脱敏方式；串联命令用分行或分号。
- Blocker: 命令依赖未确认、路径未解析、退出码不可见、可能输出凭据、或需要中文 stdin/文件但未设计 UTF-8 路径。
- Verification: 每个关键命令记录退出码和必要摘要；中文文件写入后使用 UTF-8 方式重新读取。
- Forbidden action: 禁止使用 `&&`、默认编码 `Set-Content`/`Out-File` 写中文、吞掉错误、或记录密码/token/私钥。
- Evidence: `docs\powershell-encoding.md`、`doc\tasks\<task-id>\execution-log.md`。

### PowerShell 分号串联测试退出码门禁

- Trigger: 在 PowerShell 中用分号串联多个 Node、Python、Maven、pnpm 或静态合同测试命令。
- Preflight check: 每个会影响验收结论的测试命令必须单独执行，或在每条命令后检查 `$LASTEXITCODE` 并失败即停止。
- Blocker: 前一个测试输出断言失败、异常栈或非零退出码，但后续命令继续执行并让最终命令返回 0。
- Verification: 对目标测试逐条记录退出码；批量命令必须证明中间失败不会被最后一条 PASS 掩盖。
- Forbidden action: 禁止把 `cmd1; cmd2; cmd3` 的最终退出码 0 当作全部测试通过；禁止只引用最后一条 PASS 输出。
- Evidence: `doc\tasks\20260728-edhr-detail-assist-preview-switch\execution-log.md`，相邻静态合同串联运行时 `edhr-assist-fill-mode-static.spec.js` 断言失败被后续 PASS 命令掩盖，改为单独复跑后正确记录失败。

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

### Maven 目标目录文件系统异常门禁

- Trigger: Maven 编译或 `clean` 报 `target\classes` `NoSuchFileException`、同模块类大量缺失、或 `jcmd` 显示 `WinNTFileSystem.delete0` / `getBooleanAttributes0` 长时间停在目标目录。
- Preflight check: 先枚举同模块 Maven/Java 进程并确认是否属于当前任务；只停止当前任务或同一测试命令的陈旧 PID，停止后复查没有同模块 Maven 正在写 `target`，再尝试一次标准 `-pl <module> -am` 验证。
- Blocker: `mvn clean` 也卡在 `WinNTFileSystem.delete0`、目标目录无法安全删除、或同模块编译持续报 `target\classes` 缺失时，必须标记验证阻塞；不得继续叠加 Maven 命令、不得用单模块非 `-am` 编译失败替代业务 RED/GREEN。
- Verification: 记录 PID、`jcmd Thread.print` 关键栈、失败命令、是否停止了任务自有进程、以及后续标准 Maven 命令是否到达 Surefire。
- Forbidden action: 禁止强杀全部 Java/Maven、禁止删除无关模块 `target`、禁止在目标目录损坏时提交实现、禁止把环境编译失败写成业务测试失败。
- Evidence: `doc\tasks\20260803-dcc-docx-preview-system-exception\execution-log.md`，DCC 预览任务中同模块 Maven 卡在 `WinNTFileSystem.delete0`，后续 DCC 编译出现大量 `target\classes` `NoSuchFileException`，最终保持 blocked 未提交。
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
