# Execution Log

- 2026-09-15: 已核对分支 `int_main`、远端 `origin` 和工作区脏改动；当前改动包含 MES 测试、文档及一个未跟踪 E2E 探针脚本。
- 2026-09-15: `scripts/preflight/branch-runtime-port-guard.ps1` -> PASS（int_main/int_main，前端 8081，后端 48081）。
- 2026-09-15: 首次 Maven 命令因 PowerShell 参数拆分失败：`Unknown lifecycle phase ".failIfNoSpecifiedTests=false"`；已修正参数格式后重跑。
- BDD: 提交推送并重启运行态 -> Given 当前分支有待提交前后端代码 When 完成验证、提交、推送并按端口矩阵重启 Then origin 与本地一致且前后端健康可访问。
- RED: `mvn -pl yudao-module-mes '-Dtest=MesTeamLeaderActiveOrderServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，testCompile 15 个编译错误：缺少 `FormActionExecutionContext`、`FormActionInstance.setExecutionContext` 及 `FormCenterRuntimeService.submitVerifiedBackfillInstance` 方法/签名。
- Blocker: 验证失败，按仓库规则禁止提交/推送；标准后端重启也不得跳过构建或使用旧 Jar。当前任务状态设为 `blocked`，未修改用户既有代码改动。
- 2026-09-15: 用户确认错误已修复，恢复任务执行；工作区出现后端 MES 生产代码/测试、系统登录代码/测试、前端登录/反馈/组长工作台/注册证配置及文档/E2E 改动，继续按当前工作区整体门禁验证。
- 2026-09-15: `mvn -pl yudao-module-mes -am '-Dtest=MesTeamLeaderActiveOrderServiceTest#shouldExposeActiveOrderWhenFormalRouteIsMissing+shouldExposeInvalidActiveOrderWithoutBlockingValidRows+shouldRetainMissingOutputSnapshotAndHealthyOrder+shouldExposeActiveOrderWhenVersionDoesNotBelongToRoute' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> GREEN，4/4 PASS。
- 2026-09-15: `mvn -pl yudao-module-system -am '-Dtest=AdminAuthServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> GREEN，20/20 PASS。
- 2026-09-15: `pnpm ts:check` -> GREEN，退出码 0。
- 2026-09-15: 前端静态合同测试 `active-order-read-blocked.spec.cjs`、`edhr-90-step-probe-verdict.spec.cjs`、`frontline-production-order-refresh-process-state.spec.cjs`、`registration-certificate-threshold-recipient-config-static.spec.mjs`、`forced-password-change-login-static.spec.mjs` -> GREEN，全部通过。
- 2026-09-15: `mvn -pl yudao-server -am '-DskipTests' package` -> GREEN，31 个 reactor 模块成功，生成 `yudao-server/target/yudao-server-exec.jar`。
- 2026-09-15: 经验沉淀复核：复用已有 `docs/powershell-memory.md` 的 Maven `-D` 参数引用与 stale blocker 复验门禁，以及 `docs/local-runtime.md` 的标准重启构建门禁；本次无需新建长期经验文档。
- 2026-09-15: 脏工作区基线提交 `2aa120ebc`，包含当前 31 个前后端、测试与文档文件；提交前 `git diff --cached --check` 与端口 guard 均通过。
