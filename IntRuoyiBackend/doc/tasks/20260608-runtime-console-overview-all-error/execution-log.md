# 执行日志：恢复运行控制台概览状态

BDD: 本机后端健康后概览不全红 -> Given 本机后端 `48081` 正常运行 / When 运维打开运行控制台概览 / Then Local 后端状态不再显示 `错误 unknown / ERROR`。

BDD: 后端不可用时不伪造成功 -> Given 本机后端 `48081` 未监听 / When 查询运行控制台概览 / Then 页面显示错误并保留真实失败原因。

BDD: 远程状态探针在合理预算内返回真实状态 -> Given 远程状态脚本需要超过 20 秒但未超过配置的状态命令超时时间 / When 运维刷新运行控制台概览 / Then 状态卡显示脚本返回的真实 running 状态，而不是固定 20 秒超时导致的 `ERROR unknown`。

INFO: 已检查 `20260608-runtime-build-release-showroom-option` 并记录为 blocked；本任务只恢复本机运行控制台运行态，不访问、不发布、不修改正式服务器。

RED: 用户提供截图 -> FAIL，运行控制台 Local/Test/Production/Backup 各组件均显示 `错误 unknown / ERROR`。

RED: `Invoke-WebRequest -UseBasicParsing -Uri http://127.0.0.1:48081/actuator/health -TimeoutSec 5` -> FAIL，无法连接到远程服务器。

INFO: 重新检查端口后发现 48081 已由 `java.exe` 监听，但启动命令来自 `D:\ProjectPackage\Int\IntRuoyi\worktrees\paichan\ruoyi-vue-pro`，不是主仓库 `int_main`；当前前端入口 `8081` 对接了错误 worktree 后端。

VERIFY: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File show-int-ruoyi-local-status.ps1 -Component backend -Json` -> PASS，直接状态脚本返回 `running / HTTP 200 / listening`；说明脚本本身可用，页面全红主要来自本机运行态混用和页面刷新前旧状态。

VERIFY: API 只读登录 `芋道源码/admin` 后请求 `/infra/runtime-control/overview` -> PASS，Local 全部 running；Test/Production 多数 running；Backup 仍因状态命令超时显示 error。

INFO: Playwright 首次验证脚本过早查找登录输入框失败；前端实际在数秒后正常渲染登录页。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component frontend -WorktreeName int_main` -> PASS，8081 进程命令行指向 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS，48081 进程命令行指向 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260608-151042.jar`，`repo-root=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json` -> PASS，status=`running`，`frontend=HTTP 200; backend=HTTP 200`。

RED: Playwright 打开 `http://localhost:8081/infra/monitors/runtime-control` -> FAIL，Local/Test/Backup 多数恢复，但 Production `intruoyi-frontend`、`intruoyi-backend`、`intruoyi-full` 与 Backup `website-frontend` 间歇显示 `error / ERROR / unknown`，失败原因为 `运行控制台命令执行失败：Command timed out`。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlCommandExecutorImplTest test` -> FAIL，新增测试 `queryStatusShouldUseConfiguredStatusCommandTimeout` 编译失败，缺少 `RuntimeControlProperties#setStatusCommandTimeout(Duration)`；证明状态命令超时仍为硬编码契约。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlCommandExecutorImplTest test` -> PASS，`RuntimeControlCommandExecutorImplTest` 3 tests passed；状态命令使用 `yudao.runtime-control.status-command-timeout`，配置为 200ms 时会按配置超时并暴露 `Command timed out`。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlCommandExecutorImplTest,RuntimeControlServiceImplTest" test` -> PASS，56 tests passed；运行控制台概览并发、生产只读阻断提示、Backup 参数契约未回归。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> PASS，重新打包并启动 `backend-runtime-control-20260608-152739.jar`；48081 进程命令行指向主仓库 `repo-root=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`。

GREEN: `Invoke-RestMethod -Uri http://127.0.0.1:48081/actuator/health -TimeoutSec 20` -> PASS，返回 `{"status":"UP"}`。

GREEN: API 只读登录 `芋道源码/admin` 后请求 `/infra/runtime-control/overview` -> PASS，Local/Test/Production/Backup 共 16 个状态项全部 `running`，errors 数组为空；Production 写动作仍保留未授权阻断提示。

GREEN: Playwright 打开 `http://localhost:8081/infra/monitors/runtime-control` -> PASS，捕获 `/admin-api/infra/runtime-control/overview` 返回 HTTP 200、code=0；16 个状态项全部 `running`，截图 `runtime-console-overview.png` 显示状态卡均为绿色 `运行中`。
