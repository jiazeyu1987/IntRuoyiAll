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

### 脏工作区功能分支融合增量门禁

- Trigger: 在 `int_main` 或共享分支上融合远端功能分支，同时工作区存在并行未暂存改动、残余基线、或 `git diff HEAD..branch` 显示大量非本分支文件。
- Preflight check: 先用 `git merge-base HEAD <branch>` 取真实分叉点，再运行 `git diff --name-status "<merge-base>..<branch>"` 获取该分支实际增量；同时用 `git diff --name-only` 列出当前未暂存文件，只按两个列表的交集判断是否需要先基线提交或阻塞。
- Blocker: 实际增量与未暂存文件有同文件重叠且无法可靠区分 hunks、工作区存在未解决冲突、或待融合分支会删除/覆盖主线证据文件时必须停止并让用户决定。
- Verification: 任务日志记录 merge-base、实际增量文件列表、未暂存交集、merge commit hash、目标回归命令和最终 `git status --short --branch`。
- Forbidden action: 禁止只看 `git diff HEAD..branch` 的宽泛差异就误判分支会改动所有主线新增文件；禁止为继续融合而使用 `git add -A` 混入并行改动；禁止用 reset、checkout 或 rebase 清空脏工作区。
- Evidence: `doc/tasks/20260806-commit-frontend-backend-merge-int-main/execution-log.md`，融合 `origin/codex/replan-current-route-after-feedback` 前通过 merge-base 确认实际增量仅为排产服务、测试和经验文档，与并行残余无重叠后安全合并。

### GitHub 推送大文件门禁

- Trigger: 推送到 GitHub remote、处理 `GH001`、`Large files detected`、`pre-receive hook declined`、Git LFS 或历史大文件问题。
- Preflight check: 推送前扫描待推送历史中的对象大小，至少确认没有超过 GitHub 100 MB 限制的 blob；必要时检查敏感文件和发布 evidence 是否误入 Git 历史。
- Blocker: 扫描发现超过 100 MB 的 blob，或 `git push` 返回 `GH001: Large files detected` / `pre-receive hook declined`。
- Verification: 记录对象扫描结果、`git push origin <branch>` 退出码、以及推送后的 `git status --short --branch`。
- Forbidden action: 禁止未经用户明确授权就执行历史重写、Git LFS 迁移、快照分支替代、force push 或删除远端历史。
- Evidence: `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24-github-推送前历史大文件门禁`。

### GitHub HTTPS 443 本地代理门禁

- Trigger: `git push`、`git fetch`、`git ls-remote` 或 GitHub HTTPS remote 报 `Failed to connect to github.com port 443 via 127.0.0.1`、`Could not connect to server`、`Connection timed out`、`Recv failure: Connection was reset`。
- Preflight check: 先运行 `git config --show-origin --list | Select-String -Pattern 'proxy|127\.0\.0\.1|7890|insteadOf|http'`，不要只依赖窄正则，因为 `http.https://github.com.proxy` 这类 URL 级代理可能被漏掉；再运行 `Test-NetConnection 127.0.0.1 -Port <proxyPort>`、`Test-NetConnection github.com -Port 443`、`reg query "HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings" /v ProxyServer` 和 `/v ProxyEnable`。如果用户有 FlClash/Clash/VPN，再确认其配置端口、Windows 用户代理端口和实际监听端口一致。
- Blocker: Git 配置指向本地代理但代理端口未监听、GitHub HTTPS 直连 443 不通、代理客户端仅 helper 进程在线但核心未监听、或 SSH 443 可达但 GitHub 不接受当前 SSH key。
- Verification: `git ls-remote origin HEAD` 成功返回 HEAD；如果 Git 全局代理端口陈旧但 Windows 用户代理端口正在监听，可先用一次性 `git -c http.https://github.com.proxy=http://127.0.0.1:<actualPort> ls-remote origin HEAD` 验证，再用同样一次性 `-c` 推送；若改用 SSH 443，必须先用 `ssh -T -o BatchMode=yes git@ssh.github.com -p 443` 验证账号认证成功，再改 remote 或 pushurl。
- Forbidden action: 禁止把删除 Git proxy 当作修复，除非已证明 GitHub HTTPS 直连 443 可用；禁止静默切换到 SSH remote，除非当前 SSH key 已被 GitHub 接受；禁止把 helper 服务监听端口误当作 HTTP/SOCKS 代理端口。
- Evidence: `doc\tasks\20260731-git-443-push-fix\execution-log.md`，GitHub HTTPS 直连失败、Git 全局代理指向 `127.0.0.1:7890` 但 FlClash 核心未监听，SSH 443 网络可达但公钥未授权；`doc\tasks\20260804-approval-center-applicant-column\execution-log.md`，窄正则漏掉 URL 级 GitHub proxy，完整 config 发现 `7890` 陈旧、Windows 用户代理实际为 `8902` 且监听，使用一次性 `-c` 对齐代理后 push 成功。

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

### 任务状态脚本串行写入门禁

- Trigger: 同一任务内运行会修改 `task-state.json`、`task.md`、`test-report.md` 或其它状态文件的脚本，例如 `record_phase_review.py`、`record_test_review.py`、`check_completion.py --apply`、cleanup apply，尤其准备并行执行以节省时间时。
- Preflight check: 先区分只读校验命令和状态写入命令；只读命令可以并行，任何会写同一状态文件的命令必须顺序执行。每次状态写入后立即运行 `render_task_status.py` 或等价只读状态检查，确认阶段状态、整体状态、`test_status` 和 `blocking_prereqs` 未被覆盖。
- Blocker: 并行状态写入后出现阶段状态回退、整体状态与阶段状态不一致、`blocking_prereqs` 丢失、`test_status` 被旧值覆盖，或无法确认哪个命令最后写入状态文件时必须停止并按预期状态顺序重放写入脚本。
- Verification: 记录顺序重放命令、重放后的 `render_task_status.py` 输出、结构 validator PASS 和 `git diff --check`；必要时在执行日志中说明并行写入造成的覆盖风险已修正。
- Forbidden action: 禁止把多个状态写入脚本放进 `multi_tool_use.parallel`、PowerShell background job 或同一异步批次；禁止只看单个写入脚本成功输出就跳过最终状态渲染；禁止手改 JSON 掩盖 race，除非脚本不可用且已记录阻塞原因。
- Evidence: `doc/tasks/20260805-production-leader-process-config-unification/execution-log.md`，P4 阻塞记录中并行运行 `record_phase_review.py` 与 `record_test_review.py` 后 P4 阶段状态被旧快照覆盖为 `pending`，顺序重跑阶段状态脚本并复核 `render_task_status.py` 后恢复为 `blocked`。

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

### Maven 单模块陈旧依赖门禁

- Trigger: 在多模块 Maven 项目中只运行单模块目标测试，失败发生在测试启动前，错误指向上游模块 API、DTO、枚举、Mapper 或 service contract 缺失/签名不一致。
- Preflight check: 对依赖其它 reactor 模块的目标 JUnit 使用 `mvn -pl <module> -am "-Dtest=..." "-Dsurefire.failIfNoSpecifiedTests=false" test`；如果为了诊断先运行了不带 `-am` 的命令，必须确认失败是否来自本地仓库陈旧依赖而不是业务断言。
- Blocker: 不带 `-am` 的单模块命令在 testCompile/compile 阶段失败且未进入 Surefire 时，不得把它记录为业务 RED/GREEN，也不得缩小测试范围绕过上游依赖编译。
- Verification: 使用带 `-am` 的 reactor 命令复跑，确认目标测试进入 Surefire 且 PASS/FAIL 反映真实业务行为；任务日志记录原单模块失败原因和最终 reactor 验证命令。
- Forbidden action: 禁止把本地 `.m2` 陈旧产物导致的编译失败误判为产品逻辑失败；禁止删除 `-am` 来节省时间后宣称目标 JUnit 已验证；禁止用旧 surefire 报告冒充当前命令结果。
- Evidence: `doc\tasks\20260806-schedule-default-shift-hours\execution-log.md`，排产班次小时默认值修复中不带 `-am` 的单模块 Maven 因本地 `system` API 依赖陈旧在测试前失败，最终使用 `-pl yudao-module-mes -am` 的目标 JUnit 命令通过 4 个用例。

### Maven 同模块 target/classes 陈旧门禁

- Trigger: 已修改当前模块 main 源码的方法签名、构造器参数或 mapper 默认方法，但 `mvn -pl <module> -am "-Dtest=..." test` 在 testCompile 阶段仍报旧签名、旧构造器或“找不到刚新增的方法”，且日志显示 main `compile` 为 `Nothing to compile - all classes are up to date`。
- Preflight check: 先确认生产源码确实包含新签名，再只对当前目标模块运行 `mvn -pl <module> clean test "-Dtest=..." "-Dsurefire.failIfNoSpecifiedTests=false"`；不得清理无关模块 target。
- Blocker: 当前模块 `clean` 后仍看见旧签名、`target` 删除失败、或存在并行 Maven 写同一模块 target 时，必须停止并按目标目录异常门禁处理。
- Verification: 记录第一次未重编译的失败、`clean test` 是否进入 Surefire、目标测试数量和 PASS/FAIL。
- Forbidden action: 禁止把同模块陈旧 class 的 testCompile 失败当成业务失败；禁止靠修改测试绕过旧 class；禁止用全仓清理替代当前模块最小 clean。
- Evidence: `doc\tasks\20260808-active-order-product-search\execution-log.md`，活跃订单产品搜索新增 `MesMdItemMapper` 构造器依赖和 mapper 方法后，`-pl yudao-module-mes -am` testCompile 仍看到旧 class；仅清理 `yudao-module-mes` 后 main 重新编译，43 个目标测试 PASS。

### Maven 静态源码合同工作目录门禁

- Trigger: JUnit 静态合同通过 `Files.readString`、`Path.of` 或 `readSource` 读取源码文件，且命令使用 `mvn -pl <module> -am "-Dtest=..." test`；失败文本包含 `NoSuchFileException`、重复模块路径如 `yudao-module-mes\yudao-module-mes\src`，或断言没有命中实际生产实现类。
- Preflight check: 静态合同读取源码前先按 Surefire 实际 `user.dir` 兼容模块根和仓库根两种工作目录；若被测职责已拆到独立 validator/service，不要只断言入口 service 源码字符串，需读取真正承载业务约束的实现类。
- Blocker: 测试在目标 Surefire 前因源码路径错误失败、合同断言落在错误类导致误判业务实现缺失、或为了通过测试把生产代码塞回入口类时必须停止并修正测试合同。
- Verification: 复跑原 Maven 命令，确认目标测试类已进入 Surefire 且 PASS；任务日志同时记录原路径/断言失败和修正后的目标类。
- Forbidden action: 禁止把 `NoSuchFileException` 写成业务 RED；禁止改 Maven 工作目录、复制源码到重复模块目录、或为了静态字符串断言破坏正式服务分层。
- Evidence: `doc/tasks/20260806-production-reporting-submit-implementation/execution-log.md`，报工提交参数明细实现中 `MesFrontlineRuntimeConfigProcessScopeTest` 首次在 Surefire 模块目录下读取 `yudao-module-mes\src` 失败，随后静态合同改为兼容模块根并读取 `MesFrontlineDeviceParameterValidatorImpl`，目标 Maven 2 个用例 PASS。

### Maven 目标目录文件系统异常门禁

- Trigger: Maven 编译或 `clean` 报 `target\classes` `NoSuchFileException`、同模块类大量缺失、或 `jcmd` 显示 `WinNTFileSystem.delete0` / `getBooleanAttributes0` 长时间停在目标目录。
- Preflight check: 先枚举同模块 Maven/Java 进程并确认是否属于当前任务；只停止当前任务或同一测试命令的陈旧 PID，停止后复查没有同模块 Maven 正在写 `target`，再尝试一次标准 `-pl <module> -am` 验证。
- Blocker: `mvn clean` 也卡在 `WinNTFileSystem.delete0`、目标目录无法安全删除、或同模块编译持续报 `target\classes` 缺失时，必须标记验证阻塞；不得继续叠加 Maven 命令、不得用单模块非 `-am` 编译失败替代业务 RED/GREEN。
- Verification: 记录 PID、`jcmd Thread.print` 关键栈、失败命令、是否停止了任务自有进程、以及后续标准 Maven 命令是否到达 Surefire。
- Forbidden action: 禁止强杀全部 Java/Maven、禁止删除无关模块 `target`、禁止在目标目录损坏时提交实现、禁止把环境编译失败写成业务测试失败。
- Evidence: `doc\tasks\20260803-dcc-docx-preview-system-exception\execution-log.md`，DCC 预览任务中同模块 Maven 卡在 `WinNTFileSystem.delete0`，后续 DCC 编译出现大量 `target\classes` `NoSuchFileException`，最终保持 blocked 未提交。
- Supplementary evidence: `doc/tasks/20260808-remove-pqc-extra-restrictions/verification-report.md`，一线 PQC 额外限制移除任务中，同一工作区多轮并发 Maven/`clean test`/`compile` 重建 `yudao-module-mes\target`，导致目标测试复跑在 testCompile 阶段出现大量 `target\classes` class 文件缺失；最终仅记录前端类型和静态合同 PASS，后端 Maven 动态验证保持 blocked，待无并发 Maven 窗口复跑。
- Supplementary evidence: `doc/tasks/20260808-frontline-pqc-requirement-alignment/execution-log.md`，一线 PQC 需求口径对齐任务中，目标 Maven 首轮在 testCompile 前报 `yudao-module-mes\target\classes` 大量 class 缺失；确认源文件存在、等待同模块 Maven 释放并运行 `mvn -pl yudao-module-mes -DskipTests compile` 重建主类后，复跑标准目标 JUnit 到达 Surefire 且 7 个测试 PASS。

### Maven javac/Lombok class 写入长时间运行门禁

- Trigger: Maven 目标测试或编译长时间无 surefire 报告，`jcmd <pid> Thread.print` 显示主线程在 `java.io.FileDescriptor.close0`、`lombok.core.PostCompiler$1.close`、`ClassWriter.writeClass`、`JavaCompiler.generate/compile`，但仍处于 `RUNNABLE`。
- Preflight check: 先枚举同一 `maven.multiModuleProjectDirectory` 的 Maven/Java 进程，区分当前任务 PID 与并行任务 PID；不要在同一模块 `target` 上继续叠加 Maven。若当前任务 PID 已超过命令超时且无 surefire 报告，只停止当前任务 PID，并记录未触碰的并行 PID。
- Blocker: 同仓并行 Maven 仍写同一 `target`、目标测试没有生成 surefire、或标准 `-pl <module> -am` 编译仍长时间停在 javac/Lombok 写 class 阶段时，必须把验证标记为 blocked；不得宣称 JUnit 通过。
- Verification: 记录目标命令、超时秒数、PID、`jcmd` 关键栈、已停止的任务自有 PID、未停止的并行 PID、是否生成目标 surefire，以及后续释放并行 Maven 后的复跑结果。
- Forbidden action: 禁止把 `RUNNABLE` 状态误判为业务测试失败；禁止强杀并行任务 Maven；禁止在未释放同仓 Maven 时提交实现或继续跑更多 Maven；禁止用旧 surefire 报告冒充本次目标命令结果。
- Evidence: `doc\tasks\20260805-role-matrix-code-repair\execution-log.md`，AC-M22 放行预检修复中目标 Maven 多次停在 javac/Lombok 写 class 阶段，本任务仅停止自有超时 PID，保留同仓并行 Maven；并行阻塞释放后复跑标准 `-pl ... -am` 目标 JUnit 并以 Surefire PASS 作为最终 GREEN。
- Supplementary evidence: 若必须在 Maven 阻塞时证明 RED/GREEN，可用 JUnit Console + 显式 javac 参数文件运行任务目标测试，但只能记录为补充证据；必须把 classpath 隔离清楚（旧实现 RED、新实现 GREEN）、同时保留标准 Maven 为 blocked，禁止把该补充结果写成 Maven/Surefire 通过。

### Windows Maven 页面文件不足门禁

- Trigger: Maven 编译或测试报 JVM native memory allocation / `G1 virtual space` / Windows `页面文件太小，无法完成操作`，或同仓存在多个并发 Maven/Java 编译测试进程且工作集持续增长。
- Preflight check: 先用 `Get-Process -Name java,mvn -ErrorAction SilentlyContinue` 识别当前任务 PID、同仓并发 Maven PID 和常驻运行态；只停止当前任务启动且已超时/失控的 PID，不停止其它任务或运行态进程。
- Blocker: 页面文件不足、并发 Maven 持续占用内存、或低内存 `MAVEN_OPTS` 重试仍超时时，必须把目标 JUnit 标记为环境阻塞；不得继续叠加 Maven 重试、不得把静态检查写成 JUnit PASS、不得强杀全部 Java 进程腾内存。
- Verification: 记录原 Maven 命令、内存失败摘要、低内存重试命令、停止的任务自有 PID、保留的其它 PID，以及后续需要重跑的精确测试命令。
- Forbidden action: 禁止用更小测试范围冒充目标验证通过；禁止在未达到 Surefire 测试结果时宣称业务 GREEN；禁止因为环境阻塞而提交未记录的验证缺口。
- Evidence: `doc\tasks\20260805-ac-m19-deterministic-backfill\verification-report.md`，AC-M19 聚合回填修复中目标 Maven 首次因页面文件不足失败，低内存重试超时，仅停止本任务 PID 55008 并记录剩余阻塞。
- Evidence: `doc/tasks/20260805-ac-m20-pqc-review-fix/execution-log.md`，AC-M20 PQC 复核修复中，标准 Maven 先因 JVM native memory/pagefile 失败，低内存参数后仍长时间处于 Lombok/Javac 编译且并发 Java 任务较多，最终停止本任务 Maven PID 并保持后端 JUnit blocked，待资源释放后复跑标准 Maven 取得 GREEN。
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
