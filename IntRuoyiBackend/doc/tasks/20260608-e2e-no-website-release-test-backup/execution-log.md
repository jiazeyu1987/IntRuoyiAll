# 执行日志：E2E 构建不含展厅发布包并部署测试/备份

BDD: E2E 构建不含展厅和 OnlyOffice 的发布包 -> Given 本机运行控制台加载当前 `int_main` 代码 / When 运维点击“构建发布包”且保持“发布展厅构筑包”和“发布 OnlyOffice”未选中 / Then 构建命令使用 `-Component intruoyi`，发布包 manifest 记录 `includeShowroomBuildPackage=false` 和 `onlyOfficeIncluded=false`。

BDD: E2E 部署发布包到测试服务器 -> Given 发布包构建成功且 manifest 完整 / When 运维通过运行控制台执行“部署发布包到测试服” / Then 测试服健康检查通过，部署记录显示成功。

BDD: E2E 标记测试通过并部署到备份服务器 -> Given 测试服部署成功 / When 运维通过运行控制台标记测试通过并执行“上线备份服务器” / Then 备份服务器健康检查通过，部署记录显示成功，未访问正式服务器。

INFO: `docs\server-access.md` 确认测试服务器 `172.30.30.58`、备份服务器 `172.30.30.59`；正式服务器访问内容被移除且本任务禁止访问正式服务器。

GREEN: 本机运行态预检 -> `show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json` 返回 running，8081/48081 均 HTTP 200。

INFO: 发现 48081 和 8081 曾漂移到 `worktrees\edhr_jimu`；已重启本机后端与前端到主仓库 `int_main`，并在后端启动进程继承 `INTRUOYI_RUNTIME_CONTROL_PROD_HOST=127.0.0.1`，避免运行控制台概览触发正式服状态探测。

GREEN: 本机后端来源校验 -> 48081 进程命令行指向 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260608-152101.jar`，`repo-root=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`，健康检查返回 `{"status":"UP"}`。

GREEN: 本机前端来源校验 -> 8081 进程命令行指向 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\.bin\..\vite\bin\vite.js`，`http://127.0.0.1:8081/` 返回 HTTP 200。

RED: E2E preflight -> FAIL，`runtime-control-e2e-release-test-backup.js` 在登录和提交构建前检测到当前进程环境包含 `EDHR_S3_ENDPOINT=http://172.30.30.57:9000`；按正式服禁用要求立即停止，未提交 `build-release`。

INFO: 发布脚本风险复核 -> `script\deploy\publish-int-ruoyi.ps1` 会读取 `EDHR_S3_*`，在构建/部署阶段运行 `Invoke-EdhrStorageRetentionVerifier`，并写入发布包 `runtime-env/*.env` 与目标运行时 `.env`。若继续使用当前正式 endpoint，会访问正式对象存储并把正式 endpoint 绑定到测试/备份发布包。

RED: 测试服 eDHR S3 配置只读检查 -> FAIL，`ssh root@172.30.30.58 "grep EDHR_S3_ENDPOINT /opt/intruoyi/runtime/.env"` 返回 `EDHR_S3_ENDPOINT=http://172.30.30.57:9000`；继续发布会触发正式对象存储 verifier。

RED: 备份服 eDHR S3 配置只读检查 -> FAIL，`ssh root@172.30.30.59 "grep EDHR_S3_ENDPOINT /opt/intruoyi/runtime/.env"` 返回 `EDHR_S3_ENDPOINT=http://172.30.30.57:9000`；继续发布会触发正式对象存储 verifier。

BLOCKED: 缺少非正式 eDHR protected storage/Object Lock 配置 -> IMPACT，未构建发布包，未部署测试服，未部署备份服；需要提供测试/备份可验证的 `EDHR_S3_*` 配置或先完成非正式 eDHR Object Lock 存储准备任务。

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-e2e-no-website-release-test-backup --mode preview` -> PASS，keep=`task.md`、`execution-log.md`，delete candidate=`runtime-control-e2e-release-test-backup.js`；任务未完成且为 blocked，未执行 apply、未删除文件、未提交。

BDD: 目标环境绑定 eDHR 配置 -> Given 构建发布包需要写入 `runtime-env/test.env` 与 `runtime-env/backup.env` / When 操作员提供 `EDHR_S3_TEST_*` 和 `EDHR_S3_BACKUP_*` / Then 发布包必须把测试服和备份服分别绑定到对应非正式 protected storage，不得继承正式 `EDHR_S3_ENDPOINT`。

RED: 非正式 eDHR 配置阻塞根因 -> FAIL，当前脚本只能从通用 `EDHR_S3_*` 写入 `runtime-env/*.env`，无法让测试服和备份服使用不同非正式 endpoint。

RED: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> FAIL，新增 `test_release_runtime_env_can_bind_edhr_storage_per_target_environment` 失败，原因是 `publish-int-ruoyi.ps1` 尚无 `Resolve-TargetPublishRuntimeValue` / `Get-TargetSpecificEdhrEnvName`，也无 `EDHR_S3_TEST_*`、`EDHR_S3_BACKUP_*`、`EDHR_S3_PROD_*` 解析。

GREEN: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS，5 passed；发布脚本已支持 `EDHR_S3_TEST_*`、`EDHR_S3_BACKUP_*`、`EDHR_S3_PROD_*` 目标环境绑定解析。

GREEN: PowerShell parser check -> PASS，`publish-int-ruoyi.ps1` 无语法解析错误。

INFO: 备份服非正式 eDHR bucket 准备 -> 在 `172.30.30.59` 的 `intruoyi-minio` 内创建 `edhr-retention-verifier-20260608`，启用 versioning 与 Object Lock。

GREEN: 测试服 eDHR Object Lock verifier -> PASS，endpoint `http://172.30.30.58:9000`，bucket `edhr-retention-verifier-20260530`，versioning/ObjectLock/retention/legal hold/delete denied/readback 全部通过。

GREEN: 备份服 eDHR Object Lock verifier -> PASS，endpoint `http://172.30.30.59:9000`，bucket `edhr-retention-verifier-20260608`，versioning/ObjectLock/retention/legal hold/delete denied/readback 全部通过。

GREEN: 本机运行态复检 -> `show-int-ruoyi-local-status.ps1 -WorktreeName int_main -Json` 返回 running，8081/48081 均 HTTP 200。

GREEN: E2E preflight -> PASS，Playwright 登录本机 `http://localhost:8081` 后调用真实 preview；构建参数包含 `-Component intruoyi`、`-SkipDatabaseSync`、`-SkipMinioSync`，不包含 `-IncludeOnlyOffice`，且 `-ProdServerHost 127.0.0.1`。

RED: E2E full -> FAIL before submit，弹窗中未找到 `发布展厅构筑包` 复选框；未提交 `build-release`。根因是 8081 Vite 进程漂移到 `worktrees\paichan`，不是当前主仓库前端。

GREEN: 本机前端来源修正 -> `restart-int-ruoyi-local.ps1 -Component frontend -WorktreeName int_main` 后，8081 命令行指向 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\.bin\..\vite\bin\vite.js`，HTTP 200。

RED: E2E full -> FAIL before submit，登录阶段未等待用户名输入框渲染完成即填表，未提交任何运行控制台动作。

INFO: E2E 脚本稳定性修正 -> 登录流程增加 `input[placeholder="请输入用户名"]` 可见等待，只调整测试脚本等待时机，不修改产品逻辑。

BDD: 恢复候选接口有界返回 -> Given NAS 中存在大量历史备份点 / When 运行控制台加载恢复候选用于“标记测试通过” / Then 后端只扫描最近 30 个备份点，接口快速返回可用或阻断原因，不因旧备份无限枚举导致前端超时。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> FAIL，新增 `listRestoreCandidatesShouldScanOnlyRecentBackupPoints` 失败，原因是恢复候选列表返回 35 个历史备份点而不是最近 30 个。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> PASS，14 tests passed；恢复候选列表已限制扫描最近 30 个备份点。

GREEN: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS，10 passed；不含 Website/OnlyOffice 的发布工具逻辑保持通过。

GREEN: PowerShell parser check -> PASS，`publish-int-ruoyi.ps1` 无语法解析错误。

RED: E2E resume7 -> FAIL，页面“标记测试通过”弹窗仍显示恢复集候选 `可用 0 / 0`；直接 API 诊断确认 30 个恢复候选仍会让运行控制台防呆加载接近或超过前端超时。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> FAIL，更新 `listRestoreCandidatesShouldScanOnlyRecentBackupPoints` 为最近 5 个恢复点后，当前实现仍返回 10 个。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> PASS，14 tests passed；恢复候选列表已限制扫描最近 5 个备份点。

GREEN: 本地 restore-candidates API -> PASS，真实登录后调用 `GET /infra/runtime-control/restore-candidates` 返回 HTTP 200/code=0，耗时 26304ms，返回 5 个候选且均为 `AVAILABLE`。

GREEN: 本地 overview API -> PASS，真实登录后调用 `GET /infra/runtime-control/overview` 返回 HTTP 200/code=0，耗时 60149ms，测试服 `intruoyi-full.currentReleaseTag=26-06-08_16-11-25`。

RED: E2E resume8 -> FAIL，`loadCandidates()` 中 `rollback-candidates` 与 `restore-candidates` 并发访问 NAS，任一候选接口超时后前端 catch 会同时清空两个候选列表，导致“标记测试通过”仍显示恢复集候选 `可用 0 / 0`。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest test` -> FAIL，新增 `listRollbackCandidatesShouldScanOnlyRecentReleasePackages` 失败，原因是回滚候选列表返回 10 个发布包而不是最近 5 个。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRollbackCandidateServiceImplTest test` -> PASS，16 tests passed；回滚候选列表已限制扫描最近 5 个发布包。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> PASS，14 tests passed；恢复候选列表保持最近 5 个恢复点。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS；默认 4GB 堆内存运行 `pnpm ts:check` 曾 OOM 退出 134，加大到项目构建同等 8192MB 后通过。

GREEN: 前端候选加载顺序验证 -> PASS，真实登录后按页面新顺序调用 `restore-candidates` 再调用 `rollback-candidates`，restore 耗时 50926ms 返回 5 个候选，rollback 耗时 23755ms 返回 5 个候选；每个请求均低于 70000ms 超时，且恢复候选先写入页面状态。

REGRESSION: `mvn -pl yudao-module-infra -Dtest=RuntimeRestoreCandidateServiceImplTest test` -> PASS，14 tests passed。

REGRESSION: `python -X utf8 -m pytest script/tests/test_edhr_protected_storage_publish_tooling.py -q` -> PASS，10 passed。

REGRESSION: PowerShell parser check -> PASS，`publish-int-ruoyi.ps1` 无语法解析错误。

BDD: 发布包列表有界返回 -> Given NAS 中存在大量历史发布包 / When 运行控制台加载“标记测试通过”和“上线备份服务器”候选 / Then 后端只扫描最近 5 个发布包，接口快速返回候选，不因旧发布包无限枚举阻塞 E2E。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#getReleasePackagesShouldScanOnlyRecentReleasePackageDirectories test` -> FAIL，新增测试期望最近 5 个发布包但当前实现返回 10 个。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#getReleasePackagesShouldScanOnlyRecentReleasePackageDirectories test` -> PASS，1 test passed；发布包列表已限制扫描最近 5 个目录。

REGRESSION: `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest' test` -> PASS，84 tests passed；发布包列表、回滚候选、恢复候选有界扫描逻辑均通过。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#getOverviewShouldQuerySameEnvironmentComponentsSequentially test` -> FAIL，当前 overview 会并发探测同一环境下多个组件，测试捕获到 same environment overlap。

GREEN: `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest#getOverviewShouldQuerySameEnvironmentComponentsSequentially+getOverviewShouldQueryStatusesConcurrently' test` -> PASS，2 tests passed；overview 改为环境之间并发、同一环境内组件顺序探测。

REGRESSION: `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlCommandExecutorImplTest' test` -> PASS，88 tests passed；运行控制台动作、发布包列表、候选列表、命令执行器相关测试均通过。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#getOverviewShouldLimitConcurrentEnvironmentStatusProbes test` -> FAIL，overview 环境级状态探测最大并发超过 2，真实 E2E 中会让测试服状态脚本随机命令超时。

GREEN: `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest#getOverviewShouldLimitConcurrentEnvironmentStatusProbes+getOverviewShouldQuerySameEnvironmentComponentsSequentially+getOverviewShouldQueryStatusesConcurrently' test` -> PASS，3 tests passed；overview 现在最多同时探测 2 个环境，且同一环境内组件顺序探测。

REGRESSION: `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest,RuntimeRollbackCandidateServiceImplTest,RuntimeRestoreCandidateServiceImplTest,RuntimeControlCommandExecutorImplTest' test` -> PASS，89 tests passed。

GREEN: 本地 overview API 复检 -> PASS，真实登录后调用 `GET /infra/runtime-control/overview` 耗时 46588ms，测试服 `intruoyi-full`、`intruoyi-backend`、`intruoyi-frontend`、`website-frontend` 均返回 `currentReleaseTag=26-06-08_16-11-25`。

INFO: 停止漂移本地进程 -> 发现旧 `worktrees\paichan` 后端曾拉起正式服状态探测脚本；已停止该本地后端进程，后续仅保留主仓库 `int_main` 的 48081 后端。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS；运行控制台前端改为打开依赖当前测试服版本的弹窗时单独刷新 overview 矩阵，并把运维接口 timeout 提升到 120000ms。

GREEN: `node --check runtime-control-e2e-release-test-backup.js` -> PASS；E2E 脚本修正为读取“当前测试服发布包” disabled input value、等待阻断提示消失、等待操作按钮可用，并可复用已标记 tested 的发布包。

GREEN: E2E resume19 -> PASS，`runtime-control-e2e-release-test-backup-result.json` 写入完成；`BUILD_OPERATION=reused-existing-package`，`TEST_DEPLOY_OPERATION=1c94e294-a818-4037-ba7c-d8d008dc90ba`，`MARK_TESTED_OPERATION=reused-mark-release-tested`，`BACKUP_DEPLOY_OPERATION=104f6a98-62ef-44d0-b494-9550e873b4a5`。

GREEN: 发布包 manifest 校验 -> PASS，发布包 `26-06-08_16-11-25` 为 `component=intruoyi`，`includeShowroomBuildPackage=false`，`onlyOfficeIncluded=false`，`checksumPresent=true`，`tested=true`，并绑定恢复集 `restore:20260608-153128`。

GREEN: 测试服最终核验 -> PASS，`172.30.30.58:/opt/intruoyi/runtime/.env` 为 `IMAGE_TAG=26-06-08_16-11-25`；`intruoyi-backend=intruoyi-backend:26-06-08_16-11-25`，`intruoyi-frontend=intruoyi-frontend:26-06-08_16-11-25`，健康检查 HTTP 200/UP。

GREEN: 备份服最终核验 -> PASS，`172.30.30.59:/opt/intruoyi/runtime/.env` 为 `IMAGE_TAG=26-06-08_16-11-25`；`intruoyi-backend=intruoyi-backend:26-06-08_16-11-25`，`intruoyi-frontend=intruoyi-frontend:26-06-08_16-11-25`，健康检查 HTTP 200/UP。
