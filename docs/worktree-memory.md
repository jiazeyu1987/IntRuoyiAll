# Worktree Memory

## Worktree 端口段与原子槽位门禁

- Trigger: 新建、登记或启动 `D:\IntRuoyiWorktree\` 下的 worktree；出现 `slot >= 20`、基准端口碰撞、重复活动槽位、重复活动端口，或 `E:\IntRuoyi` 被识别为 `int_main_d`。
- Preflight check: 附加 worktree 创建后、启动前运行 `scripts\runtime\reserve-worktree-slot.ps1`，由脚本在跨进程互斥锁内读取登记表并分配所属 profile 的最低空闲 `slot 1..19`；随后运行 `show-branch-runtime.ps1` 确认 profile、slot 和前后端端口。
- Blocker: 槽位不在 `1..19`、计算端口命中任一 profile 基准端口、活动登记项复用 `profile/slot` 或前后端端口、基准工作区请求非零槽位、路径与 profile 无法唯一匹配时必须 fail fast。
- Verification: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py`、`pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1`、目标工作区 `show-branch-runtime.ps1` 输出。
- Forbidden action: 禁止手工猜测槽位、并发直接改写登记表、使用 `slot >= 20`、基准工作区借用 worktree 槽位、冲突时随机换端口或按分支名猜测歧义 profile。
- Evidence: `doc/tasks/20260726-harden-worktree-port-slot-allocation/verification-report.md`。

## 多 Worktree 批量融合门禁

- Trigger: 将 `D:\IntRuoyiWorktree\` 下多个功能分支批量合入 `int_main`，尤其是 worktree 存在 dirty 状态、多个分支修改同一服务/API/测试文件，或合并后需要立即删除 worktree。
- Preflight check: 先冻结 `int_main` 的 dirty 基线；逐个 worktree 记录 branch、HEAD、`git status --short`、任务文档 `## Current Status`、验证报告结论和目标验证命令；dirty 内容必须在原分支形成独立可追溯提交。按依赖和冲突风险顺序逐分支 merge，每次冲突修复后运行该分支目标测试，不得等全部 merge 完才判断冲突语义。
- Blocker: 任一 dirty 内容无法归属或验证、分支 tip 未正式保存、任务文档仍为 `blocked` 或验证报告明确存在未达成目标、冲突只能靠整文件覆盖解决、目标回归失败、或无法证明 `git merge-base --is-ancestor <branch> int_main` 时必须停止，不得复制目录内容、跳过分支或直接删除 worktree。
- Stale branch guard: 分支 clean 只代表文件已提交，不代表适合合并；若 `git diff int_main..<branch>` 会删除大量主线已存在任务证据、发布脚本门禁或其它与该任务无关的新主线文件，先按任务状态和验证报告判定合并资格，禁止把 clean-but-blocked 的旧分支强行合入主线。
- Verification: 所有分支合入后逐项验证 ancestor 与 worktree clean；运行覆盖全部目标分支的聚焦组合回归。扩大到旧完整测试类时若出现失败，必须用 `git log`/`git diff <baseline>..HEAD -- <paths>` 判断是否由本批分支引入，并同时保留宽回归失败和目标回归结果，禁止把窄测通过冒充全量通过。
- Forbidden action: 禁止把多个 dirty worktree 内容直接复制到主工作区；禁止用 `--force` merge、整文件 `ours/theirs`、静默跳过失败测试、或在首次推送和分支 ancestor 验证前删除 worktree。
- Evidence: `doc/tasks/20260726-merge-worktrees-into-int-main/verification-report.md`，六个 worktree 在 dirty 内容独立提交、逐分支 merge、冲突后聚焦回归和 ancestor 验证后进入删除阶段。

### 跨分支运行时契约复验门禁

- Trigger: 多个子分支分别实现相邻功能，且一个分支新增接口、Service、Mapper、模板或授权链路，另一个分支在运行时应调用它；典型现象是各分支单测都通过，但主线合并后 Spring 注入、接口实现、构造器参数或调用顺序仍可能缺失。
- Preflight check: 每次合并依赖分支后，先用 `rg` 查找新增接口的生产实现和调用点，再为跨分支调用链补一个最小 RED/GREEN 测试；测试必须证明调用发生在任何写库、状态推进或外部副作用之前。
- Blocker: 新接口只有测试 mock 或空接口、生产服务未注入、调用点只在前端/API payload 中体现、跨分支依赖靠默认值/空实现/可选 bean 运行，或无法证明失败会阻止后续写入时必须停止。
- Verification: 运行跨分支最小 JUnit/静态合同和合并后的组合回归；记录 RED 原因、GREEN 命令、调用顺序断言和 `git diff --check`。
- Forbidden action: 禁止把各分支独立 PASS 当作整体 PASS；禁止用 optional/autowired fallback、空实现、默认模板、默认员工或 API-only 验证掩盖运行时链路缺口；禁止等全部分支合完后才第一次看跨分支注入关系。
- Evidence: `doc/tasks/20260730-production-line-process-pool-implementation/execution-log.md`，F2 报工提交与 F4 设备账号员工授权各自通过后，主线新增 RED/GREEN，确保 `MesProFrontlineFeedbackSubmitServiceImpl` 在创建报工/记录本/工序池事件前调用 `MesFrontlineSubmitAuthorizationService`。

### 子 Agent 主工作区溢出基线门禁

- Trigger: 并行子 agent 本应只写 `D:\IntRuoyiWorktree\`，但主工作区出现同一任务的新增/修改文件，且这些文件会被后续分支 merge 覆盖或触发 untracked overwrite。
- Preflight check: 子 agent 开始后先确认 shell `workdir` 是目标 worktree；凡使用 `apply_patch` 修改文件，文件路径必须写目标 worktree 下的绝对路径，不能使用相对路径。合并任何子分支前运行主工作区 `git status --short --branch`、`git diff --name-status`、`git ls-files --others --exclude-standard`，按任务范围判断溢出文件是否属于当前任务；属于当前任务的先形成单独“spillover baseline”提交并记录文件列表和提交 hash。
- Blocker: 溢出文件无法确认归属、包含 unrelated 用户改动、包含 secret-bearing 输出、或与待合并分支语义冲突且无法证明保留策略时必须停止。
- Verification: baseline 提交后重新运行 `git status --short --branch`，再执行目标分支 merge；若发生 add/add 冲突，必须语义合并并运行该分支目标测试。
- Forbidden action: 禁止删除 untracked 文件来绕过 merge 保护；禁止用整文件 `ours/theirs` 覆盖任务日志；禁止把溢出内容混入后续实现提交。
- Evidence: `doc/tasks/20260730-production-line-process-pool-implementation/execution-log.md`，F2 子任务溢出文件先独立提交为 `028e2904`，随后再合并 F1/F2 分支并运行目标测试。

## 并行主工作区远端快进融合门禁

- Trigger: 主工作区持续被并行任务写入，任务分支已在干净 worktree 中完成实现、验证和推送，但本地 `int_main` 无法保持 clean 以接收 `task_closeout.py` 的 ff-only merge。
- Preflight check: 在任务 worktree 中先 `git fetch origin int_main`，确认 `origin/int_main` 是当前任务分支 HEAD 的祖先；融合后必须重跑该任务的目标后端、前端、迁移或端口守卫验证，并记录远端主线 hash、任务 HEAD 和验证结果。
- Blocker: `origin/int_main` 不是任务 HEAD 的祖先、目标验证失败、远端主线又前进导致非快进推送被拒、任务分支存在未提交改动、或缺少可用 `origin` push remote 时必须停止。若目标验证使用 `mvn -pl <module> -am`，上游依赖模块的 compile/testCompile 失败也属于目标验证失败；必须阻塞并记录缺失类/Mapper/测试文件，不得因为当前任务源码未改该依赖模块就跳过、排除或降级验证。
- Verification: `git merge-base --is-ancestor origin/int_main HEAD` 通过，目标验证命令 PASS，`git push origin HEAD:int_main` 成功，随后 `git fetch origin int_main` 并确认 `origin/int_main` 指向已验证 HEAD 或其后续已融合提交。
- Forbidden action: 禁止为了 closeout 强行清理、回滚或提交并行任务文件；禁止 force push；禁止在未验证融合后 HEAD 的情况下直接更新 `int_main`；禁止把本地 dirty 主工作区清洁失败当作远端主线已集成。
- Evidence: `doc/tasks/20260727-codex-test-node-chain/verification-report.md`、`doc/tasks/20260728-node-chain-route-filter/verification-report.md`，主工作区持续并行写入时，任务分支先融合 `origin/int_main`、完成目标验证，再按远端快进路径集成；`doc/tasks/20260730-edhr-frontline-fill-tabs/verification-report.md`，eDHR 集成在远端主线新增 DCC testCompile 缺失类后阻塞，未跳过依赖模块门禁。

## D-Main 本地主线滞后远端融合门禁

- Trigger: `D:\ProjectPackage\IntRuoyi\IntRuoyiAll` 的本地 `int_main` 在 `git fetch origin int_main` 后同时 `ahead` 和 `behind`，需要把远端 `origin/int_main` 融合回本地主线。
- Preflight check: 先记录 `git status --short --branch`、`git rev-list --left-right --count HEAD...origin/int_main`、本地 ahead 提交清单和远端 behind 数量；融合前确认未跟踪/脏文件仅属于当前任务文档或已形成基线提交。
- Blocker: 冲突文件无法通过语义合并保留双方门禁、当前任务文件可能被远端同名覆盖、远端更新后端口 guard 失败、或本地 ahead 提交无法解释时必须停止。
- Verification: 冲突解决后先用 `rg -n "<<<<<<<|=======|>>>>>>>" <冲突文件>` 清除冲突标记，再运行 `scripts\preflight\branch-runtime-port-guard.ps1`、受影响前端/后端验证和 `git status --short --branch`；若 `git diff --cached --check` 命中远端历史已存在的 whitespace，必须用 scoped diff 证明冲突解决文件和当前任务文件未新增 whitespace，再记录上游遗留项。
- Forbidden action: 禁止用整文件 `ours/theirs` 覆盖经验门禁文档；禁止因为上游 whitespace 遗留就静默改写大量远端历史文件；禁止在未重跑端口 guard 和目标验证前提交或推送融合结果。
- Evidence: `doc/tasks/merge-int-main-code-20260728/verification-report.md`，本地 `int_main` 领先 5、落后 445 后融合 `origin/int_main`，保留远端新增前端经验门禁并通过前端 `ts:check`、后端 `compile` 和端口 guard。

## Worktree 前端依赖启动门禁

- Trigger: 在 `D:\IntRuoyiWorktree\` 下新增或恢复 worktree 后启动前端、运行 Vite、执行前端 `pnpm` 脚本、执行真实 E2E，或日志出现 `Command "vite" not found`、`node_modules\.bin\vite` 缺失、`cross-env is not recognized`。
- Preflight check: 启动前端或运行前端脚本前先检查目标 worktree 的 `IntRuoyiFronted\package.json`、`pnpm-lock.yaml` 和目标命令需要的 `node_modules\.bin\<tool>.cmd`；例如 Vite/E2E 检查 `vite.cmd`，`pnpm ts:check` 检查 `cross-env.cmd` 与 `vue-tsc`。若依赖缺失，在目标 worktree 前端目录执行 `pnpm install --frozen-lockfile`，不得复制其他工作区的 `node_modules`。
- Blocker: `pnpm install --frozen-lockfile` 失败、修改 lockfile、依赖目录仍缺目标脚本工具、或目标路径不是当前任务 worktree 时必须停止，不得换端口、复用旧前端进程或把后端/API 验证冒充真实页面 E2E。
- Verification: 记录 `pnpm install --frozen-lockfile` 退出码、目标 `node_modules\.bin\<tool>.cmd` 存在性、目标前端脚本或前端入口 HTTP 200、Vite 进程命令行指向目标 worktree，以及任务结束后登记端口是否释放。
- Forbidden action: 禁止复制 `node_modules`、使用主工作区 Vite 进程冒充 worktree 前端、改共享 `.env` 抢端口、或在依赖缺失时切换到 API-only。
- Evidence: `doc/tasks/20260726-edhr-release-dossier-requirement-switches/execution-log.md`，slot 5 worktree 首次启动前端失败于 `Command "vite" not found`，补跑 `pnpm install --frozen-lockfile` 后 8086 前端真实启动并通过 E2E。

## Worktree Java 21 后端低内存启动门禁

- Trigger: `D:\IntRuoyiWorktree\` 下附加 worktree 启动后端 jar、运行真实 E2E、或日志出现 `There is insufficient memory for the Java Runtime Environment to continue`、`Chunk::new`、`C2 CompilerThread`、Surefire fork 超时。
- Preflight check: 先确认后端端口来自已登记 profile/slot，端口未被未知进程占用，jar 来自当前 worktree 构建产物；再确认当前 `java -version` 是否为 Java 21 且项目编译目标仍为 Java 17。若 Java 21 fork/C2 native 内存失败，只允许在当前任务运行命令中收敛 JVM 资源参数，例如 `-Xms128m -Xmx768m -Xss512k -XX:ActiveProcessorCount=4 -XX:-TieredCompilation -XX:ReservedCodeCacheSize=64m -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC`。
- Blocker: 端口归属不明、jar 非当前 worktree、低内存参数后 health 仍不上线、测试 fork 仍卡死且无同等目标验证可执行、或需要切换端口/数据源/旧 jar 才能继续时必须停止并记录；不得冒充运行态成功。
- Verification: 记录启动命令、PID、端口监听命令行、`/actuator/health` 为 `UP`、前端入口 HTTP 200；若 Maven `-am` fork 在本机资源下超时，需保留 dumpstream 证据，并用同模块目标测试或禁用 fork 的目标测试完成复核，明确说明不是 API-only 或跳过测试。
- Forbidden action: 禁止随机换端口、改共享 `application-local.yaml`、复用旧 jar、强杀未知 Java 进程、把 `health` 未达 `UP` 当通过、或把超时的 fork 测试静默忽略。
- Evidence: `doc/tasks/20260726-codex-test-process-route-case/verification-report.md`，slot 1 worktree 后端首次 Java 21 C2 native memory crash，使用当前 jar 与登记端口 48082 加低内存 JVM 参数恢复，真实页面 E2E 写入 4 个测试项后通过。

## Worktree 真实 E2E 运行产物门禁

- Trigger: 在 `D:\IntRuoyiWorktree\` 下执行真实 Playwright E2E，尤其需要通过登记 slot 启动前端与后端。
- Preflight check: 启动前同时检查目标 worktree 的后端可执行 Jar（如 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`）、前端 Vite 依赖（`IntRuoyiFronted\node_modules\.bin\vite.cmd`）、端口登记项、目标端口监听状态和 worktree 本地前端 env；`.env.local` 或等效启动环境必须显式指向登记后端端口，并关闭无人值守 E2E 需要关闭的验证码开关。
- Blocker: Jar 不存在、Vite 依赖不存在、端口未按登记 slot 成对启动、构建/安装失败、端口被非当前 worktree 占用、登录页验证码仍开启、或前端实际代理到其它后端端口时必须停止，不得静默切回 8081/48081、复用其他 worktree 进程、API-only 代替真实页面路径。
- Verification: 记录 Jar 存在性或构建命令退出码、Vite 依赖存在性、worktree env 中的前后端端口与验证码开关、前后端端口监听 PID 和命令行归属、前端 HTTP 200、后端 health UP，以及真实 E2E 命令和结果。
- Forbidden action: 禁止把缺运行产物或验证码开启解释为功能失败；禁止随机换端口、强杀未知进程、复制 node_modules、复用旧 Jar、只靠命令行临时 env 但未验证页面实际关闭验证码，或只跑静态合同冒充真实 E2E。

## Worktree 删除门禁

- Trigger: 删除、清理、合并后移除、修复残留目录、处理 `git worktree remove` 失败、`Directory not empty`、`Invalid argument`、或断链 worktree。
- Preflight check: 先读取 `docs\worktree-restrictions.md`，确认目标绝对路径位于 `D:\IntRuoyiWorktree\` 下；用 `git worktree list --porcelain` 确认 Git 注册状态；用 `git status --short` 记录每个目标 worktree 的未提交变更；用 `git merge-base --is-ancestor <branch> int_main` 或等效命令确认分支提交是否已合入目标基线。
- Blocker: 目标不在 `D:\IntRuoyiWorktree\` 下、目标不是用户明确指定的当前任务对象、分支仍有未合入提交、存在未提交变更但用户未明确授权丢弃、目录被运行进程占用、或端口登记表需要释放但无法验证目录已删除。
- Verification: 删除后必须重新运行 `git worktree list --porcelain`，并对每个目标执行 `Test-Path`；若存在端口登记项，只有在目录已删除且任务记录完成后才允许将槽位标记为可复用；验证结果写入当前 `doc\tasks\<task-id>\execution-log.md`。
- Forbidden action: 禁止用 `Remove-Item -Recurse` 替代正常 `git worktree remove` 作为首选路径；禁止删除未指定 worktree；禁止因为 `Directory not empty` 就扩大清理范围；禁止静默丢弃未提交变更；禁止删除或释放其他任务的端口登记项。
- Evidence: 2026-07-26 删除已合入 worktree 前补齐长期经验门禁，要求先确认合入状态、未提交变更授权、路径边界和删除后注册状态。

### Git 注册已移除但物理目录被运行态锁住

- Trigger: `git worktree remove <path>` 返回 `Invalid argument`、Git 注册列表已不再显示目标 worktree，但物理目录仍存在，或残留目录内 `runtime-backend.err.log` / Vite / Java / esbuild 文件被占用。
- Preflight check: 先确认 `git worktree list --porcelain` 已无该路径、残留目录没有 `.git` 文件、目标绝对路径仍在 `D:\IntRuoyiWorktree\` 下；再按命令行和端口定位只属于该残留 worktree 的进程，例如 `Get-CimInstance Win32_Process` 匹配目标路径，`Get-NetTCPConnection` 核对登记端口。
- Blocker: 若仍有 Git 注册、残留目录存在 `.git`、占用进程无法证明属于目标 worktree、占用端口属于其他 profile/任务、或目录路径越界，必须停止，不得删除目录或停止进程。
- Verification: 记录被停止进程的 PID、名称、命令行、端口和归属依据；停止后确认目标端口不再监听、目标路径 `Test-Path` 为 `False`、端口登记项仅对该目标标记 `active=false/deletedAt/cleanupTask`。
- Forbidden action: 禁止因残留目录删除失败就强杀未知 Java/Node/PowerShell；禁止用父目录批量删除；禁止在未确认 `.git` 已消失前把注册 worktree 当普通目录删除；禁止释放其他 active worktree 的端口登记项。
- Evidence: `doc/tasks/20260727-merge-d-worktrees/verification-report.md`，`20260727_pici` 在 Git 注册移除后被 8084/48084 运行态锁住，确认 PID 和命令行归属后停止目标进程并删除残留目录。

### Git 注册已移除但空目录删除被当前进程占用

- Trigger: `task-closeout-cleanup --mode apply` 已完成 ff-only merge 和 cleanup commit，但 `git worktree remove --force <path>` 返回 `Permission denied`；`git worktree list` 已不再显示目标 worktree，残留目录为空且没有 `.git`。
- Preflight check: 从主工作区或目标外部目录执行核对，确认 `git worktree list` 无该路径、`Test-Path <path>\.git` 为 false、`Get-ChildItem -Force <path>` 计数为 0、目标绝对路径仍在 `D:\IntRuoyiWorktree\` 下。
- Blocker: 残留目录非空、存在 `.git`、仍有 Git 注册、路径越界、或目录内有无法归属文件时必须停止；不得用递归删除扩大清理范围。
- Verification: 仅对确认空目录执行 `Remove-Item -LiteralPath <path>`，随后记录 `Test-Path <path>` 为 false、`git worktree list` 不含该路径、主工作区 `git status --short --branch` clean 或仅 ahead。
- Forbidden action: 禁止从残留 worktree 当前目录反复运行删除；禁止把 `Permission denied` 直接升级为 `Remove-Item -Recurse`；禁止删除父级 `D:\IntRuoyiWorktree\` 或其他任务目录。
- Evidence: `doc/tasks/20260731-dcc-file-category-rules/execution-log.md`，cleanup apply 已创建清理提交并从 Git 注册移除 worktree，但 Windows 因当前目录占用留下空目录，确认无 `.git` 且子项计数 0 后从 `E:\IntRuoyi` 删除空目录。

### Git 注册已移除但前端依赖目录残留

- Trigger: `git worktree remove <path>` 已移除 Git 注册，但返回 `Directory not empty`，且残留主要位于 `IntRuoyiFronted\node_modules`、pnpm/esbuild 依赖目录、或 Windows 报 `Access to the path '<name>' is denied`。
- Preflight check: 先确认 `git worktree list --porcelain` 已无该路径、残留目录没有 `.git` 文件、目标绝对路径仍在 `D:\IntRuoyiWorktree\` 下、目标登记端口没有监听、且 `Get-CimInstance Win32_Process` 未发现命令行指向该目标路径的 Node/Java/PowerShell 进程。
- Blocker: 若仍有 Git 注册、残留目录存在 `.git`、仍有归属不明进程或端口、路径越界、或拒绝访问文件不在当前目标目录内，必须停止，不得扩大删除范围。
- Cleanup rule: 若 `cmd /c rmdir /s /q <path>` 对 pnpm `node_modules` 输出大量 `The system cannot find the path specified` 或留下空壳目录，先用当前任务专用空目录对目标 `node_modules` 执行 `robocopy <empty-dir> <target-node_modules> /MIR /R:0 /W:0`，确认子项计数为 0 后再逐层删除 `node_modules`、`IntRuoyiFronted` 和目标 worktree 根目录。
- Verification: 仅对目标残留目录清理属性或空目录镜像后删除，之后重新验证 `Test-Path <path>` 为 `False`、`git worktree list --porcelain` 不含该路径、目标登记项已标记 `active=false/deletedAt/cleanupTask`。
- Forbidden action: 禁止为了处理 `node_modules` 残留删除父级 worktree 根目录；禁止跳过进程和端口核验；禁止在 Git 注册仍存在时把 worktree 当普通目录强删。
- Evidence: `doc/tasks/20260727-merge-remaining-worktrees/verification-report.md`，`codex-test-process-route` 在 Git 注册移除后残留前端依赖目录，确认无 `.git`、无目标进程和 8082/48082 监听后仅清理目标目录并复核不存在；`doc/tasks/20260730-worktree-prune-keep-banzuzhang/verification-report.md`，多个 worktree 的 pnpm `node_modules` 残留需先用空目录 `robocopy /MIR` 清空再删除空目录链。

## 删除操作顺序

1. 阶段 1：目标确认
   必查项：用户指定路径、绝对路径、Git worktree 注册、当前分支、HEAD、是否在 `D:\IntRuoyiWorktree\` 下。
   推荐命令：`git worktree list --porcelain`、`Resolve-Path`、`git -C <path> status --short`。
   Fail Fast：路径越界、路径不存在但仍有 Git 注册残留、或目标不是当前任务指定对象。
   必须记录：路径、分支、HEAD、dirty 文件数量。

2. 阶段 2：合入与脏变更检查
   必查项：目标分支是否已合入 `int_main`，是否存在未提交变更，用户是否授权丢弃。
   推荐命令：`git merge-base --is-ancestor <branch> int_main`、`git rev-list --count int_main..<branch>`、`git -C <path> status --short`。
   Fail Fast：未合入提交数量大于 0，或 dirty worktree 未获得明确删除授权。
   必须记录：是否已合入、未合入提交数、dirty 文件数量和授权依据。

3. 阶段 3：删除与残留处理
   必查项：优先使用 `git worktree remove <path>`；dirty worktree 仅在已获授权时使用 `--force`。
   推荐命令：`git worktree remove --force <path>`、`git worktree prune`。
   Fail Fast：删除失败且原因不是当前目标自身残留；不要扩大到父目录或其他 worktree。
   必须记录：删除命令、退出码、失败文本或成功结果。

4. 阶段 4：收尾验证
   必查项：Git 注册列表、物理目录、端口登记项、任务日志。
   推荐命令：`git worktree list --porcelain`、`Test-Path <path>`、读取 `D:\IntRuoyiWorktree\.ports\worktree-ports.json`。
   Fail Fast：任一目标仍注册、物理目录仍存在、或端口登记状态无法解释。
   必须记录：最终 worktree 列表、目录存在性、端口登记表处理结果。
