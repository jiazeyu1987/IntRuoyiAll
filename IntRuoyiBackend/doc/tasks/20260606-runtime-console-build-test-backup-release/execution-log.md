# Execution Log：运行控制台构建、测试服部署、标记测试通过、上线备份服

BDD: UI 构建发布包 A -> Given 登录本机运行控制台 / When 选择只发代码、勾选 OnlyOffice 并确认构建 / Then NAS 上生成发布包 A。

BDD: UI 部署发布包 A 到测试服 -> Given 发布包 A 已生成 / When 在运行控制台选择部署到测试服 / Then `172.30.30.58` 部署成功并可访问。

BDD: UI 标记测试通过 -> Given 发布包 A 已部署到测试服 / When 填写验证结论并点击标记测试通过 / Then 发布包 A 状态允许上线备份服务器。

BDD: UI 上线备份服务器 -> Given 发布包 A 已标记测试通过 / When 在运行控制台选择上线备份服务器并确认 / Then `172.30.30.59` 部署成功并可访问。

BDD: UI 立即备份测试服 -> Given 发布包 A 已上线备份服务器 / When 在运行控制台选择立即备份且目标为测试服 / Then 生成测试服备份点。

BDD: UI 恢复数据到测试服 -> Given 测试服备份点已生成 / When 在运行控制台选择恢复数据且目标为测试服 / Then 测试服数据恢复成功并健康检查通过。

BDD: 禁止正式服务器变更 -> Given 任意发布步骤 / When 执行运行控制台动作 / Then 不对 `172.30.30.57` 执行发布、重启、写入或验证修改。

## Evidence

GREEN: Playwright login and runtime console navigation -> PASS, local admin UI opened the real runtime console at `http://localhost:8081/infra/monitors/runtime-control`; no formal server operation was submitted.

GREEN: UI `构建发布包` submit -> PASS, operationId `bfaf3f7b-5c8f-46d4-9d24-ce0ca063f0fa`, parameters included `releaseTag=20260606_ui_code_only_onlyoffice_A_1138`, `publishScope=code-only`, `includeOnlyOffice=true`.

GREEN: runtime-control operation log `bfaf3f7b-5c8f-46d4-9d24-ce0ca063f0fa.log` -> PASS, `Release package built: 20260606_ui_code_only_onlyoffice_A_1138` and `NAS release path: Backup/ReleasePackage/20260606_ui_code_only_onlyoffice_A_1138`.

GREEN: OnlyOffice inclusion evidence -> PASS, build log inspected `onlyoffice/documentserver:latest` and exported it in `docker save` together with backend and frontend images.

GREEN: UI `部署发布包到测试服` submit -> PASS, operationId `ba7919f1-913f-4f39-be3f-951b358679c9`, parameters included `releaseTag=20260606_ui_code_only_onlyoffice_A_1138`, environment `test`.

GREEN: runtime-control operation log `ba7919f1-913f-4f39-be3f-951b358679c9.log` -> PASS, `Publish completed for test`; release package A was deployed to `172.30.30.58`.

RED: UI `标记测试通过` dialog -> FAIL, expected blocker reason: dialog showed current test release package as `无` and restore candidate count `可用 0 / 0`; submit button was disabled with `服务端未返回标记测试通过候选，禁止提交`.

RED: `node doc\tasks\20260607-runtime-console-current-release-refresh\scripts\runtime-console-full-goal.e2e.js` -> FAIL before submitting any runtime-control action，登录等待跳转超时；本机后端 `127.0.0.1:48081` 已停止监听，无法完成登录。

RED: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> FAIL，本机重启脚本执行 System NAS menu SQL 探针时报 `Unknown column 'ame' in 'where clause'`。根因：探针 SQL 使用 PowerShell 双引号 here-string，MySQL 反引号字段名 `` `name` `` 被 PowerShell 转义为换行加 `ame`。

RED: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py::test_local_restart_system_nas_menu_probe_uses_literal_powershell_here_string -q` -> FAIL，NAS menu 探针仍使用 `ProbeSql = @"`。

GREEN: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py::test_local_restart_system_nas_menu_probe_uses_literal_powershell_here_string -q` -> PASS，NAS menu 探针改为 literal here-string，避免 PowerShell 反引号转义。

GREEN: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py -q` -> PASS，4 passed。

RED: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> FAIL，literal here-string 修复后仍提示 System NAS menu 探针在应用迁移后不通过；根因继续定位为 `Test-LocalSqlProbe` 仍通过 `mysql -e $Sql` 传多行 SQL，命令行传参对换行、反引号和中文不稳定。

RED: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py::test_local_restart_sql_probe_passes_multiline_sql_through_stdin -q` -> FAIL，`Test-LocalSqlProbe` 尚未通过 stdin 传入多行 SQL。

GREEN: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py -q` -> PASS，5 passed；`Test-LocalSqlProbe` 改为 `Invoke-LocalMySqlCommand -MySqlArguments @('-N', '-B') -InputText $Sql`。

GREEN: `python -m pytest script\tests\test_runtime_control_scripts.py::test_local_restart_backend_routes_mysql_and_redis_through_unshadowed_docker_loopback -q` -> PASS。

RED: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> FAIL，stdin 修复后 NAS menu 探针仍在 `powershell.exe` 下返回 0；临时诊断确认 `powershell.exe` 读取 UTF-8 no-BOM `.ps1` 时会破坏探针 SQL 内中文字符串。

GREEN: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py -q` -> PASS，7 passed；NAS menu 探针中的中文菜单名改为 MySQL `_utf8mb4 0x...` 字面量，并保留 UTF-8 stdin bytes 写入。

GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File doc\tasks\20260606-runtime-console-build-test-backup-release\scripts\debug-local-sql-probe.ps1` -> PASS，临时诊断显示 `StdOut=[1]`、`ProbePassed=True`。

RED: `python -m pytest script\tests\test_runtime_control_scripts.py::test_remote_status_script_exposes_current_release_package_from_runtime_env -q` -> FAIL，远端状态脚本仅在 `-Component full` 时读取运行环境 `IMAGE_TAG`，导致前端概览在非 full 状态刷新时拿不到当前测试服发布包。

GREEN: `python -m pytest script\tests\test_runtime_control_scripts.py::test_remote_status_script_exposes_current_release_package_from_runtime_env -q` -> PASS，`show-int-ruoyi-remote-status.ps1` 改为无条件读取远端 `IMAGE_TAG` 并输出 `currentReleaseTag`。

GREEN: `python -m pytest script\tests\test_runtime_control_scripts.py -q` -> PASS，14 passed。

GREEN: `python -m pytest script\tests\test_restart_int_ruoyi_local_schema.py script\tests\test_runtime_control_scripts.py -q` -> PASS，21 passed。

## Final Restarted UI Verification

GREEN: `node doc\tasks\20260607-runtime-console-current-release-refresh\scripts\runtime-console-full-goal.e2e.js` with `RUNTIME_CONTROL_FULL_GOAL_ALLOW=1` -> PASS，完整从步骤 1 重新执行，ReleaseTag A 为 `20260607_ui_code_only_onlyoffice_A_043314`。

GREEN: UI `构建发布包` submit -> PASS，operationId `f45a3095-a28c-423c-94cf-e2257e2120f5`，参数包含 `releaseTag=20260607_ui_code_only_onlyoffice_A_043314`、`publishScope=code-only`、`includeOnlyOffice=true`。

GREEN: runtime-control operation log `f45a3095-a28c-423c-94cf-e2257e2120f5.log` -> PASS，`Release package built: 20260607_ui_code_only_onlyoffice_A_043314` and `NAS release path: Backup/ReleasePackage/20260607_ui_code_only_onlyoffice_A_043314`。

GREEN: OnlyOffice inclusion evidence -> PASS，build log inspected `onlyoffice/documentserver:latest` and exported it in `docker save` together with backend and frontend images。

GREEN: UI `部署发布包到测试服` submit -> PASS，operationId `c035f04c-f8db-4555-b9fc-c6c0b3307056`，发布包 A 部署到测试服务器 `172.30.30.58`。

GREEN: UI `标记测试通过` submit -> PASS，operationId `404bea05-1954-40c1-b7a1-0fdadd8e9e30`，验证结论为 `验证结论：20260607_ui_code_only_onlyoffice_A_043314 已通过测试服部署与健康检查，可上线备份服务器`。

GREEN: UI `上线备份服务器` submit -> PASS，operationId `41455001-0c1e-44e0-8ef1-9dc4d7ab6cf6`，发布包 A 部署到备份服务器 `172.30.30.59`。

GREEN: UI `立即备份` 选择测试服 -> PASS，operationId `599de693-0aec-414d-9362-cc3b37a1971f`，生成测试服备份点 `20260607-050200`。

GREEN: UI `恢复数据` 选择测试服 -> PASS，operationId `21336b5c-a5b2-4bd0-8e65-f61536e37f41`，恢复点 `20260607-050200`，参数 `targetEnvironment=test`。

GREEN: independent operation status sweep -> PASS，以上 6 个 operation JSON 均为 `status=succeeded`。

GREEN: independent HTTP health sweep -> PASS，`172.30.30.58` 与 `172.30.30.59` 的 `48081/actuator/health`、`8081/`、`8080/healthcheck`、`8083/` 均返回 HTTP 200。

GREEN: production boundary assertion -> PASS，UI 驱动记录 `No promote-prod action and no targetEnvironment=prod were submitted by this run.`；本次任务未对正式服务器 `172.30.30.57` 提交发布、重启、写入或恢复动作。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260606-runtime-console-build-test-backup-release --mode preview` -> PASS，`status: ready`，`blocked: <none>`；仅执行预览，未删除本地验证证据。
