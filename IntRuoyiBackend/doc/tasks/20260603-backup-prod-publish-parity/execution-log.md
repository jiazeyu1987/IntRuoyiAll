# Execution Log

BDD: 备份服发布复用正式服发布管线 -> Given 运维人员选择已测试通过的 NAS 发布包 / When 分别执行 `promote-prod` 与 `promote-backup` / Then 两者均调用 `publish-int-ruoyi.ps1 -Mode deploy-release`，均要求 `PROD` 确认与 tested gate，差异仅来自环境画像。

BDD: 备份服带数据发布不得依赖正式服历史 MinIO 容器名 -> Given 备份服环境不存在 `ragflow_compose-minio-1` / When 执行带数据 `deploy-release -Environment backup` / Then 脚本使用备份服环境声明的 MinIO 凭据来源并 fail fast，不读取未声明的 RagFlow 容器。

BDD: 正式服与备份服发布包恢复门禁一致 -> Given 发布包包含 MySQL dump 与 MinIO `yudao` 快照 / When 部署到 prod 或 backup / Then 两个环境都执行数据库导入、MinIO 同步、`infra_file_config.id=28` 目标重绑定、后端容器 MinIO 可达校验、前端/后端/Website 健康检查和对应环境发布历史记录。

BDD: 只发代码语义不被备份服特例破坏 -> Given 发布包或操作明确为 code-only / When 部署到 prod 或 backup / Then 两个环境都不恢复 MySQL 或 MinIO，但仍执行应用镜像加载、运行环境写入、服务启动与健康检查，且不会为跳过数据同步而读取目标 MinIO 凭据。

INFO: 2026-06-03 建立任务包，确认前序后端任务 `20260603-restore-data-guide-alignment` 已为 `blocked`。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "remote_minio or manifest_scope" -q` -> FAIL, 3 个脚本契约测试失败：`RemoteMinioContainer` 仍有全局 RagFlow 默认，`deploy-release` 未读取 `release-manifest.json` 的 `publishScope`，远端 MinIO 凭据读取未受 `SkipMinioSync` 控制。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#executePromoteProdShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard+executePromoteBackupShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 运行控制台实际发布参数未包含 `-RemoteMinioContainer`；prod/backup 仍只传服务器、发布目录、数据盘等环境事实。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#executePromoteProdShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard+executePromoteBackupShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, prod deploy-release 传入既有 MinIO 源；backup 在显式配置测试值时传入环境画像中的 `-RemoteMinioContainer`，默认不再继承 RagFlow。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "remote_minio or manifest_scope" -q` -> PASS, 3 passed，发布脚本不再使用全局 RagFlow 默认，deploy-release 使用 manifest scope，code-only/skip-minio 不读取远端 MinIO。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k "backup or remote_minio or deploy_release" -q` -> PASS, 4 passed。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_scripts.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS, 66 passed。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 32 passed。

GREEN: PowerShell parser for `script/deploy/publish-int-ruoyi.ps1` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260603-backup-prod-publish-parity\bug-regression-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence docs\environments\20260603-backup-prod-publish-parity-ci-cd-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence docs\recovery\20260603-backup-prod-publish-parity.md` -> PASS。

BLOCKER: 远程备份服 with-data 发布验证未执行，因为当前没有用户明确授权操作 `172.30.30.59`。影响：本地代码与脚本契约已验证，但备份服真实 MinIO 容器名/凭据来源仍需授权后只读检查、用户提供或环境配置补齐。

INFO: 最终实现不猜测备份服 MinIO 容器名；backup 默认 `remoteMinioContainer` 为空，避免继承正式服历史 RagFlow 容器。code-only 包不需要该值；with-data 包若未通过参数或环境画像提供该值，会在发布脚本读取远端 MinIO 前 fail fast。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-backup-prod-publish-parity --mode preview` -> PASS, `status: ready`，无 blocked/warnings；preview 默认只保留 `task.md` 与 `execution-log.md`，会删除任务包内额外证据文件，因此本轮未执行 apply。

INFO: 用户明确授权检查备份服 `172.30.30.59` 实际情况；本轮只执行只读 SSH / Docker / HTTP / MySQL 非敏感字段检查，未发布、未重启、未创建或修改远端容器。

GREEN: backup status read-only check -> PASS, `show-int-ruoyi-remote-status.ps1 -ServerHost 172.30.30.59 ... -Component full -Json` 返回 `status=running`，当前发布包 `26-06-01_18-17-51`，backend/frontend/pdfWorker/OnlyOffice 均 HTTP 200。

BLOCKER: backup target MinIO check -> FAIL as expected, 备用服 compose 服务仅有 `mysql/onlyoffice/redis/backend/frontend/website`；`docker ps -a` 未发现 MinIO/RagFlow/S3 类容器；`ragflow_compose-minio-1=missing`、`intruoyi-minio=missing`；主机 `127.0.0.1:9000/minio/health/live` 不可达。

BLOCKER: backup backend MinIO reachability -> FAIL, `docker exec intruoyi-backend curl http://host.docker.internal:9000/minio/health/live` 失败，说明当前后端容器无法访问发布脚本要求绑定的目标 MinIO endpoint。

BLOCKER: backup file config readback -> FAIL, `infra_file_config.id=28` 非敏感字段为 `endpoint=http://host.docker.internal:9000`、`domain=http://172.30.30.58:9000/yudao`、`bucket=yudao`，accessKey/accessSecret 均存在但未打印。影响：备份服当前文件配置仍指向测试服域，且本机 9000 无服务，with-data 备份发布缺少真实目标 MinIO 服务/凭据来源。

INFO: 用户已明确授权补齐备份服环境前置条件；备份服 `172.30.30.59` 已创建真实 MinIO 容器 `intruoyi-minio`，数据目录 `/mnt/intruoyi-data/runtime-data/minio`，bucket `yudao` 已创建并配置匿名下载；敏感凭据只写入远端 root-only 环境文件，未写入仓库或任务文档。

GREEN: backup target MinIO provision/readiness -> PASS, `intruoyi-minio` 运行中，主机 `http://127.0.0.1:9000/minio/health/live` 可达，后端容器访问 `http://host.docker.internal:9000/minio/health/live` 可达，`/data/yudao` 存在。

RED: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -k remote_restart_blocks -q` -> FAIL, 远端重启脚本仍默认 `RemoteMinioContainer='ragflow_compose-minio-1'`，人工 backend/full 重启入口还保留历史 RagFlow 隐式默认。

GREEN: `python -X utf8 -m pytest script/tests/test_runtime_control_scripts.py -k remote_restart_blocks -q` -> PASS, 远端重启脚本默认 `RemoteMinioContainer=''`，backend/full 重启缺少 `-RemoteMinioContainer` 时先 fail fast。

RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k release_package_embeds_runtime_env -q` -> FAIL, `Get-RemoteRuntimeEnvMap` 调用了缺失的 `Test-RemoteFileExists`，真实备份发布在写入远端 compose 环境文件阶段中断。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k release_package_embeds_runtime_env -q` -> PASS；PowerShell parser for `script/deploy/publish-int-ruoyi.ps1` -> PASS，已补齐 `Test-RemoteFileExists` 并保证定义早于调用。

BLOCKER: backup deploy-release `26-06-02 20:13:57` first rerun -> FAIL, 已通过 NAS 下载、eDHR Object Lock 与远端数据盘检查，随后暴露脚本缺陷 `Test-RemoteFileExists` 缺失；该缺陷已按 RED/GREEN 修复。

BLOCKER: backup deploy-release `26-06-02 20:13:57` second rerun -> FAIL at final showroom image smoke. 发布通过 NAS 下载、eDHR Object Lock、远端数据盘、镜像加载、required SQL、服务启动、后端/前端/OnlyOffice/Website 健康检查与后端 MinIO 可达检查；最终 `http://172.30.30.59:8081/admin-api/infra/file/28/get/showroom/product/cover/20260527/product-product_001-cover.png` 返回 HTTP 200 但 Content-Type 为 `application/json`，不是 `image/*`。

BLOCKER: release package inventory -> FAIL, NAS 当前最新正式服历史包 `20260603_website_assets_cache_immutable` 有 `prod-latest.json` 和 `tested.json`，但 manifest 标记 `publishScope=with-data` 且缺少 `ruoyi-vue-pro-current.sql` 与 `minio/yudao`；已测 code-only 包 `26-06-02 20:13:57` 不含数据快照，无法为新建备份 MinIO 提供对象基线。未获用户明确批准前，不修改 NAS 发布审计记录、不跳过 smoke、不无 tested gate 部署旧 with-data 包。

GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_runtime_control_scripts.py script/tests/test_runtime_control_ops_scripts.py -q` -> PASS, 67 passed。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 32 passed。

GREEN: PowerShell parser for `script/deploy/publish-int-ruoyi.ps1` and `script/deploy/restart-int-ruoyi-remote.ps1` -> PASS。

GREEN: evidence validators -> PASS, bug regression evidence、CI/CD environment evidence、backup disaster recovery evidence 均通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-backup-prod-publish-parity --mode preview` -> PASS, `status: ready`，无 blocked/warnings；未执行 apply，因为当前任务处于阻塞且证据文件仍需保留。
