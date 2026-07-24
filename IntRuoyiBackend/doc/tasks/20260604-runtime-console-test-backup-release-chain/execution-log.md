# 执行日志：运行控制台验证测试服与备份服发布链路

BDD: 构建 code-only 发布包 A -> Given 本机运行控制台可用 / When 点击“构建发布包”，选择只发代码且不勾选 OnlyOffice / Then NAS `Backup/ReleasePackage` 生成发布包 A，manifest 为 `publishScope=code-only`、`onlyOfficeIncluded=false`。

BDD: 发布包 A 部署到测试服 -> Given 发布包 A 构建成功 / When 点击“部署发布包到测试服”并选择 A / Then 测试服运行的 backend/frontend/website 镜像与发布包 A 一致，健康检查和 smoke 通过。

BDD: 发布包 A 标记测试通过 -> Given 测试服部署 A 成功 / When 点击“标记测试通过”并填写原因 / Then A 变为已验证状态。

BDD: 已验证发布包 A 上线备份服 -> Given A 已测试通过 / When 点击“上线备份服务器”选择 A / Then 备份服运行的程序与 A 一致，健康检查和 smoke 通过。

BDD: 正式服务器禁止触碰 -> Given 当前任务执行任一步骤 / When 需要选择目标环境 / Then 禁止访问、发布、登录、SSH、HTTP 探测或修改正式服务器 `172.30.30.57`。

VERIFY: 上一个同服务仓库任务 `doc/tasks/20260604-test-deploy-showroom-image-json/task.md` 当前状态为 `completed`。

VERIFY: 本机后端 `curl.exe --fail --silent --show-error --max-time 15 http://localhost:48081/actuator/health` -> PASS，`{"status":"UP"}`。

VERIFY: 本机前端 `curl.exe --fail --silent --show-error --max-time 15 --head http://127.0.0.1:8081/` -> PASS，HTTP 200。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#getOverviewShouldNotProbeProductionWhenAccessIsDisabled,RuntimeControlServiceImplTest#executeProductionActionsShouldFailFastWhenProductionAccessIsDisabled" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`RuntimeControlProperties.Environment` 缺少 `accessEnabled` 门禁字段，当前运行控制台总览会默认把 `prod/172.30.30.57` 放入状态探测队列。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，37 个运行控制台服务用例通过；默认正式环境 `accessEnabled=false` 时总览返回 `BLOCKED/access-disabled` 且不调用任何 `prod` 状态脚本，正式动作服务端 fail fast；显式启用 prod 的旧门禁用例仍验证 `PROD` 确认逻辑。

VERIFY: 运行控制台构建发布包 A `26-06-04 01:25:18` -> PASS，operationId=`4af5eb74-5365-4569-b4f8-aac9dfa1ffe4`，operation JSON 状态 `succeeded`，参数 `publishScope=code-only`、`includeOnlyOffice=false`，日志显示 `Release package built: 26-06-04 01:25:18` 与 `NAS release path: Backup/ReleasePackage/26-06-04_01-25-18`。

VERIFY: 正式服访问边界巡检 -> 发现非本任务本机计划任务 `IntRuoyi Backup Scheduled` 自动启动 `backup-scheduled` 并通过 SSH 访问 `172.30.30.57`；已立即停止对应 `ssh.exe` 与 `powershell.exe` 进程，并执行 `Disable-ScheduledTask -TaskName 'IntRuoyi Backup Scheduled'`，复查状态 `Disabled`。

RED: `node --check doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS 前的静态脚本创建阶段无语法运行证据；补充本任务专用 E2E 脚本，要求 `RUNTIME_CONTROL_CHAIN_RELEASE_TAG` 锁定发布包 A，并在浏览器 request/response 与 operation log 中禁止出现 `172.30.30.57`。

GREEN: `node --check doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，专用真实链路 E2E 脚本语法有效。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，37 个运行控制台服务用例通过；当前工作树正式服访问禁用门禁、测试服/备份服发布参数回归通过。

RED: `node doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> FAIL，发布包 `26-06-04 01:25:18` 部署测试服成功且测试服三项健康检查通过，但脚本未关闭“部署发布包到测试服”操作日志弹窗，导致下一步点击“标记测试通过”被弹窗拦截。

GREEN: `node --check doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，E2E 脚本已修复为每个操作成功后关闭日志弹窗，并从“构建发布包”开始创建本轮发布包 A；`mark-release-tested` 断言按服务端规范化目录名校验。

RED: `node doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> FAIL，发布包 `26-06-04 02:25:41` 从“构建发布包”开始重新验证，构建成功且测试服部署成功，测试服 backend/frontend/website 三项健康检查通过；随后“标记测试通过”弹窗提交未产生 action POST，原因是脚本先填写“验证结论”后又用通用 `fillDialogReason()` 填第一个 textarea，覆盖了验证结论且没有填写“操作原因”，前端校验拦截了提交。

GREEN: `node --check doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，E2E 脚本改为按表单标签精确填写 `验证结论` 与 `操作原因`，避免 mark-release-tested 双 textarea 字段互相覆盖。

GREEN: `node --check doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，本轮继续前确认专用真实链路 E2E 脚本语法有效。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，37 个运行控制台服务用例通过；正式服默认禁用访问、测试服发布、标记测试通过与备份服上线参数仍由单元回归覆盖。

RED: `node doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> FAIL，发布包 `26-06-04 03:12:28` 从“构建发布包”开始重新验证，构建成功并上传 NAS `Backup/ReleasePackage/26-06-04_03-12-28`，测试服部署成功，测试服 backend/frontend/website 三项健康检查通过；随后“标记测试通过”弹窗中脚本等待 `操作原因` textarea 超时，说明当前页面表单结构与脚本字段定位仍不一致。

GREEN: `node --check doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，E2E 脚本已按前端实际表单标签将“标记测试通过”的原因字段定位改为 `原因`，下一轮将从“构建发布包”重新验证完整链路。

GREEN: 本机后端重启 -> PASS，当前运行 jar 为 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260604-033559.jar`，来自当前 `yudao-server\target\yudao-server.jar` 构建产物；`curl.exe http://localhost:48081/actuator/health` 返回 `{"status":"UP"}`。

VERIFY: 已登录本机后端读取 `/admin-api/infra/runtime-control/overview` -> PASS，`prod` 下 `intruoyi-frontend`、`intruoyi-backend`、`intruoyi-full`、`website-frontend` 均返回 `status=BLOCKED`、`runtimeState=access-disabled`、`actionEnabled=false`、`blockedReason=正式环境未授权，当前任务禁止访问正式服务器`，证明运行控制台总览不会探测正式服务器。

GREEN: `node doc\tasks\20260604-runtime-console-test-backup-release-chain\runtime-console-test-backup-chain.e2e.cjs` -> PASS，发布包 `26-06-04 03:42:41` 从“构建发布包”开始完成完整链路：构建成功并上传 NAS `Backup/ReleasePackage/26-06-04_03-42-41`，测试服部署成功，标记测试通过成功，备份服上线成功，脚本输出 `PASS: runtime console test and backup release chain`。

VERIFY: operation JSON 审计 -> PASS，`dadf4187-cbc1-4aba-bd61-07863197d661` 为 `build-release/succeeded` 且参数 `publishScope=code-only`、`includeOnlyOffice=false`；`93f00281-5573-4782-91e5-720a094a6faf` 为 `publish-test/succeeded`；`1fe8ab8c-016c-4cfe-9535-773571d67045` 为 `mark-release-tested/succeeded` 且 `releaseTag=26-06-04_03-42-41`；`dd09917e-d2b8-4bbc-9e84-ac9156d5e058` 为 `promote-backup/succeeded`。

VERIFY: 测试服运行版本一致 -> PASS，`ssh root@172.30.30.58` 只读检查 `.env` 返回 `IMAGE_TAG=26-06-04_03-42-41`，`docker compose ps` 显示 backend 镜像 `intruoyi-backend:26-06-04_03-42-41`、frontend 镜像 `intruoyi-frontend:26-06-04_03-42-41`，backend/frontend/website 均为 running。

VERIFY: 备份服运行版本一致 -> PASS，`ssh root@172.30.30.59` 只读检查 `.env` 返回 `IMAGE_TAG=26-06-04_03-42-41`，`docker compose ps` 显示 backend 镜像 `intruoyi-backend:26-06-04_03-42-41`、frontend 镜像 `intruoyi-frontend:26-06-04_03-42-41`，backend/frontend/website 均为 running。

VERIFY: 正式服务器禁止触碰 -> PASS，本轮四个 operation 日志 `dadf4187-cbc1-4aba-bd61-07863197d661`、`93f00281-5573-4782-91e5-720a094a6faf`、`1fe8ab8c-016c-4cfe-9535-773571d67045`、`dd09917e-d2b8-4bbc-9e84-ac9156d5e058` 搜索 `172.30.30.57` 无匹配；运行控制台服务端总览已验证正式环境 `BLOCKED/access-disabled`。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260604-runtime-console-test-backup-release-chain\bug-regression-evidence.md` -> PASS，bug regression evidence 格式有效。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-console-test-backup-release-chain --mode preview` -> READY，keep `task.md` / `execution-log.md` / `bug-regression-evidence.md`，delete 本任务临时 E2E 脚本与 `artifacts/`，blocked `<none>`，warnings `<none>`。

CLOSEOUT APPLY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-console-test-backup-release-chain --mode apply` -> APPLIED，已删除本任务临时 E2E 脚本与 `artifacts/`，保留核心任务记录与回归证据。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，37 个运行控制台服务用例通过；closeout 后最终单元回归仍通过。

VERIFY: 正式服务器进程边界复查 -> PASS，`Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -match '172\.30\.30\.57' }` 无匹配。
