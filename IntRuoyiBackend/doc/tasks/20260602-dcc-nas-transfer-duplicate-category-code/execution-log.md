# Execution Log

BDD: NAS 转移文件复用既有分类 -> Given DCC 文件分类表中已存在某个 NAS 目录对应的唯一分类编码 `NASCAT-...` / When 用户在 NAS 管理页签再次转移该目录或目录下文件并需要关联同一分类 / Then 系统应复用已有分类，不应再次插入相同 `code`，也不应吞掉数据库唯一键异常后返回默认成功。

BDD: NAS 连接与目录刷新 -> Given 管理员打开本机 NAS 管理页并输入用户名 `ceshi`、密码 `Kdlyx123` / When 点击测试连接并刷新目录 / Then 测试连接通过，目录树显示 NAS 中的目录，且包含 `1. QMS documents`。

BDD: QMS 目录转移后 DCC 受控浏览一致 -> Given NAS 目录树中存在 `1. QMS documents` 及其所有子目录和文件 / When 管理员选择该目录点击转移并等待任务完成 / Then DCC 受控浏览中 `1. QMS documents` 下的目录结构和文件与 NAS 目录一致。

BDD: 删除父文件夹后可重新完整转移 -> Given `1. QMS documents` 已转移到 DCC 且 DCC 目录管理中可见 / When 管理员在 DCC 目录管理选择 `1. QMS documents`、点击删除父文件夹并确认 / Then `1. QMS documents` 从 DCC 目录中删除；When 管理员再次从 NAS 管理选择 `1. QMS documents` 转移 / Then DCC 受控浏览中的目录结构和文件再次与 NAS 一致。

SETUP: 2026-06-02 建立任务 `20260602-dcc-nas-transfer-duplicate-category-code`；上一后端任务 `20260602-dcc-viewer-token-config-missing` 已标记 `blocked`。

SETUP: 2026-06-02 接续用户目标，将任务范围扩展为 NAS `1. QMS documents` 连接、刷新、转移、DCC 浏览一致、删除父文件夹、再次转移和再次一致的 1-7 闭环；默认仅操作本机环境，不登录测试服或正式服。

RED: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_rebindsExistingNasCategoryCodeAfterDirectoryDeleted test` -> FAIL, 删除父文件夹后遗留的同 `NASCAT-...` 类别未按 code 复用并重新绑定，新转移未产生 `reusedCategoryCount=1`。

GREEN: `node scripts/system-nas-management.test.mjs` -> PASS, NAS 管理页转移确认弹窗挂载在当前转移弹窗内，完成任务不会被刷新后自动恢复，完整 NAS 配置允许刷新目录，缺少配置仍不可刷新。

GREEN: `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileServiceImplTest" test` -> PASS, 36 tests passed；覆盖 SMB 读取改由 `openFile` 判定，以及 Infra 文件对象有界并发删除后批量删库。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccDirectoryAdminServiceImplTest" test` -> PASS, 覆盖 DCC 父目录删除时分批删除受控文件。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, 后端运行 jar 已按当前源码重新打包。

RED: `node output/playwright/20260602-dcc-nas-transfer-full-loop-test-tenant-final/verify-full-loop.mjs` -> FAIL, 使用本机 `测试租户`/`tenant-id=122` 登录后，步骤 1 测试连接通过、步骤 2 刷新目录并选中 `1. QMS documents` 通过、NAS 快照为 51 个目录和 958 个文件、第一次转移任务 24 创建成功；运行中本机后端/对象存储运行态被外部清理或重启打断，E2E 轮询出现 `ECONNREFUSED 127.0.0.1:48081`，任务 24 最终 51 个目录完成、49 个文件完成、909 个文件失败，失败信息为 MinIO `127.0.0.1:9000` connection refused。该结果不能作为成功证据，必须在清理完成后从第 1 步重跑。

BLOCKER: 2026-06-02 10:55 只读检查 -> `GET http://127.0.0.1:48081/actuator/health` 连续 6 次 PASS，MinIO health PASS；但 `docker exec docker-minio-1 du -sh /data/yudao-dcc-e2e` 显示 bucket 仍约 9.2G，其中 `dcc` 约 7.9G、`showroom` 约 1.3G。用户正在清理 `yudao-dcc-e2e/`，当前不得启动新的转移写入；待清理完成后使用 `测试租户` 从步骤 1 重跑完整 1-7。

BLOCKER RECHECK: 2026-06-02 automatic continuation -> backend `48081` health PASS，MinIO `9000` health PASS；`/data/yudao-dcc-e2e` 仍约 9.2G，`dcc` 约 7.9G、`showroom` 约 1.3G；测试租户 `tenant_id=122` 中仍存在上次失败残留的未删除 `1. QMS documents`。继续写入会与用户清理 bucket 冲突，必须等待外部清理完成后从第 1 步重跑。

RED: `node output/playwright/20260602-dcc-nas-transfer-full-loop-after-minio-current-clear/verify-full-loop.mjs` -> FAIL, 用户清空 `yudao-dcc-e2e` 当前对象后重跑，步骤 1 连接通过、步骤 2 刷新并选中 `1. QMS documents` 通过，NAS 快照为 51 个目录和 958 个文件，第一次转移任务 25 创建成功；11:12:54 本机后端被 `script\deploy\restart-ruoyi-local-component.ps1 -Component backend` 替换，E2E 轮询 `/nas-transfer/tasks/25` 出现 `ECONNRESET`。任务 25 最终 51 个目录完成、958 个文件失败，失败阶段为 `submit`，错误为 MinIO `127.0.0.1:9000` connection refused。该结果不能作为成功证据。

RECHECK: 2026-06-02 11:24 -> `docker exec docker-minio-1 ... mc ls --recursive local/yudao-dcc-e2e` 返回当前对象数 `0`；后端 `48081` 与 MinIO `9000` 连续健康检查返回 200，最近 10 分钟未发现新的 `restart-ruoyi-local-component.ps1 -Component backend` 事件。准备从第 1 步重新执行完整 1-7 E2E。

BDD: E2E 运行期间阻止本地重启 -> Given DCC NAS 完整闭环 E2E 正在使用测试租户执行真实转移 / When 其他本地进程尝试调用 `restart-ruoyi-local-component.ps1` 重启 backend 或 frontend / Then 重启脚本必须在停止任何监听进程前失败快显，避免中断当前 E2E 写入链路。

RED: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL, 新增运行态重启保护测试失败，重启脚本缺少 `local-runtime-restart.guard` 检查，DCC NAS full-loop E2E 也尚未创建/清理该 guard。

GREEN: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS, 9 passed；本地组件重启脚本会在执行 component 分支前检查 `output/runtime/local-runtime-restart.guard`，DCC NAS full-loop E2E 会在 preflight 前创建 guard 并在写出证据后清理。

GREEN: PowerShell parser check for `script/deploy/restart-ruoyi-local-component.ps1` -> PASS。

GREEN: Manual guard check -> PASS, 放置 `output/runtime/local-runtime-restart.guard` 后调用 `restart-ruoyi-local-component.ps1 -Component backend` 返回 exitCode=1，并输出 `Local runtime restart is blocked by an active guard`，未进入重启流程。

RED: `node output/playwright/20260602-dcc-nas-transfer-full-loop-after-restart-guard/verify-full-loop.mjs` -> FAIL, 测试租户完整 E2E 从步骤 1 重跑，guard 创建、登录、前置清理、NAS 测试连接、刷新目录、选择 `1. QMS documents`、NAS 快照均通过；第一次转移任务 26 完成，创建 51 个目录、958 个文件、失败 0。随后进入 `04-first` DCC 全量一致性比对时，本机后端正在处理受控文件分页查询，E2E Node 进程被外部结束，未写出 `evidence.json`，仅留下第一轮转移完成截图；该结果不能作为成功证据，必须补充异常退出证据落盘和长请求 heartbeat 后从第 1 步重跑。

RED: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL, 新增 E2E evidence/heartbeat 测试失败，脚本尚未在异常退出时同步写出 `evidence.json`，也未在 DCC 受控文件全量分页等待期间周期记录 heartbeat。

GREEN: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS, 11 passed；DCC NAS full-loop E2E 在 `process.exit`、`uncaughtException`、`unhandledRejection` 场景同步写 evidence，并在 `04/07` DCC 受控文件分页比对期间每 30 秒记录 heartbeat。

GREEN: `D:\Programs\node.exe --check yudao-ui-admin-vue3/output/playwright/20260602-dcc-nas-transfer-full-loop-after-openfile-read-fix/verify-full-loop.mjs` -> PASS。

GREEN: `python -m pytest script/tests/test_system_nas_menu_sql.py -q` -> PASS, 1 passed；提交前确认系统 NAS 菜单 SQL 结构测试通过。

GREEN: `mvn -pl yudao-module-infra -Dtest=NasBrowserServiceImplTest test` -> PASS, 11 tests passed；提交前确认 NAS 文件读取判定相关 Infra 回归通过。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileQueryServiceTest,DccControlledFileWorkflowServiceImplTest" test` -> PASS, 121 tests passed；提交前确认 DCC NAS 转移、查询和工作流相关回归通过。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-dcc-nas-transfer-duplicate-category-code --mode preview` -> PASS, status ready, delete/blocked/warnings 均为空。

CHECKPOINT: 完整 1-7 Playwright E2E 尚未在本次提交前重新跑通；当前提交仅保存已通过单元/静态回归覆盖的源码与测试改动，后续仍需从步骤 1 重跑真实用户路径。

RED: `node output/playwright/20260602-dcc-nas-transfer-full-loop-after-token-cleanup-heartbeats/verify-full-loop.mjs` -> FAIL, guard 创建和 preflight 成功后，登录响应成功但 E2E 校验发现页面 localStorage 中的 access/refresh token 与登录响应 token 不一致，脚本按 fail-fast 写出 `evidence.json` 并停止。进一步确认该断言过严：前端可能在登录落地后立即刷新 token，真实用户路径应以后续页面会话 token 为准，同时记录它是否与登录响应一致。

RED: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL, E2E token 逻辑尚未同步页面当前 token，也未记录 `accessTokenMatchesLoginResponse`。

GREEN: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS, 12 passed；E2E 登录后会读取真实页面会话 access/refresh token、记录是否等于登录响应，并将后续 API 校验 token 同步为页面当前 token。缺少页面 token 时仍然失败快显。

GREEN: `D:\Programs\node.exe --check yudao-ui-admin-vue3/output/playwright/20260602-dcc-nas-transfer-full-loop-after-openfile-read-fix/verify-full-loop.mjs` -> PASS。

RED: `node output/playwright/20260602-dcc-nas-transfer-full-loop-after-evidence-heartbeat/verify-full-loop.mjs` -> FAIL, 重跑后 guard 与登录成功，但前置清理阶段长时间无 `00-preclean` 进展日志；同期后端日志出现残留 Playwright 浏览器使用旧 refreshToken 反复请求 `/system/auth/refresh-token` 并返回“无效的刷新令牌”。该轮被人工停止，不能作为成功证据；需要清理残留浏览器进程，并让 E2E 登录后校验页面 token，一旦页面使用非本次登录 token 立即失败，同时给目录树和删除父文件夹等待加 heartbeat。

RED: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> FAIL, 新增登录 token 校验、前置清理目录树 heartbeat、删除预检/删除提交 heartbeat 断言失败。

GREEN: `python -m pytest script/tests/test_restart_ruoyi_bat_runtime_config.py -q` -> PASS, 12 passed；E2E 登录后会校验页面 access/refresh token 与本次登录响应一致，前置清理和删除父文件夹路径会记录 `delete-start`、目录树 heartbeat、active-transfer precheck heartbeat、delete-subtree heartbeat。

GREEN: `D:\Programs\node.exe --check yudao-ui-admin-vue3/output/playwright/20260602-dcc-nas-transfer-full-loop-after-openfile-read-fix/verify-full-loop.mjs` -> PASS。
