# 执行日志：修复运行控制台空工作目录启动失败

BDD: 空 repoRoot 阻止命令启动 -> Given 运行控制台 `repoRoot` 配置为空 / When 执行本机 PowerShell 状态命令 / Then 后端返回明确的 `repoRoot missing` 错误，且不触发 Windows `CreateProcess error=123`。

BDD: 有效 repoRoot 正常执行命令 -> Given 运行控制台 `repoRoot` 指向存在目录 / When 执行本机 PowerShell 状态命令 / Then 命令在该目录下启动并返回输出。

INFO: 已检查后端前置任务 `20260608-backup-incremental-manifest-short-loop`，状态为 `blocked`，阻塞原因是测试服真实 B1-B5 连续备份与 B3/B4/B5 恢复闭环证据缺失；本任务只处理本机命令启动前置配置校验。

Bug: 运行控制台执行本机 PowerShell 命令失败，UI 显示 `Cannot run program "powershell.exe" (in directory ""): CreateProcess error=123`。本机 PowerShell 存在，失败点是后端把空 `repoRoot` 传给 `ProcessBuilder.directory`。

Expected: 运行控制台命令执行器必须在启动进程前校验 `yudao.runtime-control.repo-root`。配置为空、非法或目录不存在时返回明确业务错误；配置有效时，PowerShell 命令以该目录作为工作目录运行。

Reproduction: UI 路径为 `http://localhost:8081/infra/monitors/runtime-control`；当前 48081 Java 命令行中 `--yudao.runtime-control.repo-root=` 与实际路径分离，Spring 解析到空 `repoRoot`；回归测试通过空 `repoRoot` 执行临时 PowerShell 脚本复现。

Root Cause: `RuntimeControlCommandExecutorImpl.execute` 无条件执行 `Path.of(properties.getRepoRoot())` 并传入 `ProcessBuilder.directory`，未在启动进程前校验 `repoRoot` 是否为空、非法或不存在。Windows 收到空工作目录后返回 `CreateProcess error=123`，错误不指向正式前置配置。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest" test` -> FAIL，预期原因：空 `repoRoot` 未在进入 `ProcessBuilder.directory` 前被拦截，回归断言未看到 `repoRoot missing`。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest" test` -> PASS，2 tests，空 `repoRoot` 返回业务级 `repoRoot missing`，有效 `repoRoot` 作为 PowerShell 工作目录执行。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest,RuntimeControlServiceImplTest" test` -> PASS，52 tests。

INFO: 当前 48081 Java 进程命令行显示 `--yudao.runtime-control.repo-root=` 与路径分离，运行态仍需后续重启才能加载修复后的代码；本任务未擅自重启本机服务。

Verification: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest,RuntimeControlServiceImplTest" test` -> PASS，52 tests。

Blockers: 无代码阻塞。当前已经运行的 48081 后端进程仍是修复前启动状态，需要后续重启后才能加载本次代码；本任务未擅自重启本机运行服务。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260608-runtime-console-empty-repo-root\execution-log.md` -> PASS。

CLOSEOUT: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-runtime-console-empty-repo-root --mode preview` -> PASS，keep `task.md`、`execution-log.md`，delete/blocked/warnings 均为 none。
