# Worktree Memory

## 多 Worktree 批量融合门禁

- Trigger: 将 `D:\IntRuoyiWorktree\` 下多个功能分支批量合入 `int_main`，尤其是 worktree 存在 dirty 状态、多个分支修改同一服务/API/测试文件，或合并后需要立即删除 worktree。
- Preflight check: 先冻结 `int_main` 的 dirty 基线；逐个 worktree 记录 branch、HEAD、`git status --short` 和目标验证命令；dirty 内容必须在原分支形成独立可追溯提交。按依赖和冲突风险顺序逐分支 merge，每次冲突修复后运行该分支目标测试，不得等全部 merge 完才判断冲突语义。
- Blocker: 任一 dirty 内容无法归属或验证、分支 tip 未正式保存、冲突只能靠整文件覆盖解决、目标回归失败、或无法证明 `git merge-base --is-ancestor <branch> int_main` 时必须停止，不得复制目录内容、跳过分支或直接删除 worktree。
- Verification: 所有分支合入后逐项验证 ancestor 与 worktree clean；运行覆盖全部目标分支的聚焦组合回归。扩大到旧完整测试类时若出现失败，必须用 `git log`/`git diff <baseline>..HEAD -- <paths>` 判断是否由本批分支引入，并同时保留宽回归失败和目标回归结果，禁止把窄测通过冒充全量通过。
- Forbidden action: 禁止把多个 dirty worktree 内容直接复制到主工作区；禁止用 `--force` merge、整文件 `ours/theirs`、静默跳过失败测试、或在首次推送和分支 ancestor 验证前删除 worktree。
- Evidence: `doc/tasks/20260726-merge-worktrees-into-int-main/verification-report.md`，六个 worktree 在 dirty 内容独立提交、逐分支 merge、冲突后聚焦回归和 ancestor 验证后进入删除阶段。

## Worktree 前端依赖启动门禁

- Trigger: 在 `D:\IntRuoyiWorktree\` 下新增或恢复 worktree 后启动前端、运行 Vite、执行真实 E2E，或日志出现 `Command "vite" not found` / `node_modules\.bin\vite` 缺失。
- Preflight check: 启动前端前先检查目标 worktree 的 `IntRuoyiFronted\package.json`、`IntRuoyiFronted\node_modules\.bin\vite` 和 `pnpm-lock.yaml`；若缺少 `vite`，在目标 worktree 前端目录执行 `pnpm install --frozen-lockfile`，不得复制其他工作区的 `node_modules`。
- Blocker: `pnpm install --frozen-lockfile` 失败、修改 lockfile、依赖目录仍缺 `vite`、或目标路径不是当前任务 worktree 时必须停止，不得换端口、复用旧前端进程或把后端/API 验证冒充真实页面 E2E。
- Verification: 记录 `pnpm install --frozen-lockfile` 退出码、`node_modules\.bin\vite` 存在性、前端入口 HTTP 200、Vite 进程命令行指向目标 worktree，以及任务结束后登记端口是否释放。
- Forbidden action: 禁止复制 `node_modules`、使用主工作区 Vite 进程冒充 worktree 前端、改共享 `.env` 抢端口、或在依赖缺失时切换到 API-only。
- Evidence: `doc/tasks/20260726-edhr-release-dossier-requirement-switches/execution-log.md`，slot 5 worktree 首次启动前端失败于 `Command "vite" not found`，补跑 `pnpm install --frozen-lockfile` 后 8086 前端真实启动并通过 E2E。

## Worktree Java 21 后端低内存启动门禁

- Trigger: `D:\IntRuoyiWorktree\` 下附加 worktree 启动后端 jar、运行真实 E2E、或日志出现 `There is insufficient memory for the Java Runtime Environment to continue`、`Chunk::new`、`C2 CompilerThread`、Surefire fork 超时。
- Preflight check: 先确认后端端口来自已登记 profile/slot，端口未被未知进程占用，jar 来自当前 worktree 构建产物；再确认当前 `java -version` 是否为 Java 21 且项目编译目标仍为 Java 17。若 Java 21 fork/C2 native 内存失败，只允许在当前任务运行命令中收敛 JVM 资源参数，例如 `-Xms128m -Xmx768m -Xss512k -XX:ActiveProcessorCount=4 -XX:-TieredCompilation -XX:ReservedCodeCacheSize=64m -XX:MaxMetaspaceSize=512m -XX:+UseSerialGC`。
- Blocker: 端口归属不明、jar 非当前 worktree、低内存参数后 health 仍不上线、测试 fork 仍卡死且无同等目标验证可执行、或需要切换端口/数据源/旧 jar 才能继续时必须停止并记录；不得冒充运行态成功。
- Verification: 记录启动命令、PID、端口监听命令行、`/actuator/health` 为 `UP`、前端入口 HTTP 200；若 Maven `-am` fork 在本机资源下超时，需保留 dumpstream 证据，并用同模块目标测试或禁用 fork 的目标测试完成复核，明确说明不是 API-only 或跳过测试。
- Forbidden action: 禁止随机换端口、改共享 `application-local.yaml`、复用旧 jar、强杀未知 Java 进程、把 `health` 未达 `UP` 当通过、或把超时的 fork 测试静默忽略。
- Evidence: `doc/tasks/20260726-codex-test-process-route-case/verification-report.md`，slot 1 worktree 后端首次 Java 21 C2 native memory crash，使用当前 jar 与登记端口 48082 加低内存 JVM 参数恢复，真实页面 E2E 写入 4 个测试项后通过。

## Worktree 删除门禁

- Trigger: 删除、清理、合并后移除、修复残留目录、处理 `git worktree remove` 失败、`Directory not empty`、`Invalid argument`、或断链 worktree。
- Preflight check: 先读取 `docs\worktree-restrictions.md`，确认目标绝对路径位于 `D:\IntRuoyiWorktree\` 下；用 `git worktree list --porcelain` 确认 Git 注册状态；用 `git status --short` 记录每个目标 worktree 的未提交变更；用 `git merge-base --is-ancestor <branch> int_main` 或等效命令确认分支提交是否已合入目标基线。
- Blocker: 目标不在 `D:\IntRuoyiWorktree\` 下、目标不是用户明确指定的当前任务对象、分支仍有未合入提交、存在未提交变更但用户未明确授权丢弃、目录被运行进程占用、或端口登记表需要释放但无法验证目录已删除。
- Verification: 删除后必须重新运行 `git worktree list --porcelain`，并对每个目标执行 `Test-Path`；若存在端口登记项，只有在目录已删除且任务记录完成后才允许将槽位标记为可复用；验证结果写入当前 `doc\tasks\<task-id>\execution-log.md`。
- Forbidden action: 禁止用 `Remove-Item -Recurse` 替代正常 `git worktree remove` 作为首选路径；禁止删除未指定 worktree；禁止因为 `Directory not empty` 就扩大清理范围；禁止静默丢弃未提交变更；禁止删除或释放其他任务的端口登记项。
- Evidence: 2026-07-26 删除已合入 worktree 前补齐长期经验门禁，要求先确认合入状态、未提交变更授权、路径边界和删除后注册状态。

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
