# Execution Log

BDD: 主分支构建发布测试服 -> Given 后端和前端都位于主工作区 `int_main`，When 执行构建发布，Then 测试服务器运行镜像应来自本次主工作区构建且包含 DCC 分片上传接口。

PRECHECK: ci-cd-skill -> PASS, 已阅读 ci-cd-environment-delivery 及 evidence contract。

PRECHECK: release-docs -> PASS, 已阅读 `docs/server-access.md`、`docs/release-backup-restore.md`、`docs/experience-index.md`。

PRECHECK: target-scope -> PASS, 本次只发布测试服务器 `172.30.30.58`，不要求正式服务器，不操作正式服务器。

PRECHECK: main-worktree -> PASS, 根目录仅保留主 worktree；后端 `ruoyi-vue-pro` 分支为 `int_main`，前端 `yudao-ui-admin-vue3` 分支为 `int_main`。

PRECHECK: working-tree-dirty -> WARN, 后端和前端主工作区存在未提交改动；本次发布包按当前主工作区内容构建。

PRECHECK: dcc-source-endpoints -> PASS, 后端源码存在 `/local-folder-import/sessions`、`/batches`、`/upload-state`、`/chunks`、`/complete`；前端源码存在 `uploadLocalFolderImportChunk`、`upload-state`、`/chunks`。

GREEN: experience-preflight -> PASS, 高风险发布前已完成经验门禁、发布文档和目标范围确认。

## Commands

GREEN: backend-dcc-tests -> PASS, `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.DccBaseSchemaTest,cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileLocalFolderImportControllerTest,cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，Tests run: 46, Failures: 0, Errors: 0, Skipped: 0。

GREEN: frontend-nas-static-tests -> PASS, `node scripts/system-nas-management.test.mjs`，tests 2, pass 2。

RED: build-release-test-only -> FAIL, `publish-int-ruoyi.ps1 -Mode build-release ... -Environment test ...` 未传 `-ProdServerHost` 时失败，原因：脚本仍强制要求正式服务器主机。

GREEN: runtime-control-test-only-tests -> PASS, `mvn -pl yudao-module-infra -am "-Dtest=cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，Tests run: 65, Failures: 0, Errors: 0, Skipped: 0。

GREEN: backend-dcc-tests-after-release-script-fix -> PASS, `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.DccBaseSchemaTest,cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileLocalFolderImportControllerTest,cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，Tests run: 46, Failures: 0, Errors: 0, Skipped: 0。

RED: command-line-build-release -> FAIL, 直接命令行运行 `publish-int-ruoyi.ps1 -Mode build-release` 已停止作为正式构建路径；本次还触发迁移元数据门禁失败，缺少 `sql/mysql/20260512_ai_base_schema.sql` 的 `release-migration:` 元数据。后续必须改用运行控制台按钮触发构建发布包。

BLOCKER: button-build-release -> current runtime-control button path would still fail migration policy gate; sql/mysql has 143 files missing `release-migration:` metadata, first missing file is `20260512_ai_base_schema.sql`.

GREEN: release-migration-metadata-gate -> PASS, added explicit `release-migration:` metadata to 143 legacy SQL files, fixed `dependsOn` format in `20260615_mes_edhr_tail_four_goals.sql`, and `python -X utf8 ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root ruoyi-vue-pro\sql\mysql` returned `status=passed`, `migrationCount=146`.

GREEN: runtime-control-button-command-tests -> PASS, `mvn -pl yudao-module-infra -am "-Dtest=cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`, Tests run: 65, Failures: 0, Errors: 0, Skipped: 0.

GREEN: frontend-nas-static-tests-after-gate -> PASS, `node scripts/system-nas-management.test.mjs`, tests 2, pass 2.

AUTHORIZATION: runtime-control-admin-login -> PASS, 用户明确要求继续构建发布且禁止命令行直接构建发布；测试租户 `aoteman` 登录后运行控制台动态路由 404，说明该账号未注册运行控制台页面。本次切换到 `芋道源码/admin` 仅用于运行控制台按钮触发授权的构建发布 operation。

GREEN: runtime-control-button-preview -> PASS, 运行控制台“构建发布包”弹窗预览命令为 `-Mode build-release -ReleaseTag 26-06-15 22:23:26 -Component intruoyi -SkipDatabaseSync -SkipMinioSync -TestServerHost 172.30.30.58 -BackupServerHost 172.30.30.59 ...`，不包含 `-ProdServerHost`，不要求正式服务器。

GREEN: runtime-control-button-build-started -> PASS, 已通过运行控制台“确认执行”按钮提交构建发布包 operation `e7c3f75d-d542-4931-bbb2-728ba6c150b2`；运行态命令仍只包含测试服和备份服务器参数，不包含正式服参数。

GREEN: runtime-control-button-build-release -> PASS, operation `e7c3f75d-d542-4931-bbb2-728ba6c150b2` 通过运行控制台按钮完成构建；后端 Maven `BUILD SUCCESS`，前端 Vite `Build successful`，生成镜像标签 `26-06-15_22-23-26`，发布包已上传 NAS `Backup/ReleasePackage/26-06-15_22-23-26`。

GREEN: runtime-control-button-deploy-preview -> PASS, 运行控制台“部署发布包到测试服”弹窗选择发布包 `26-06-15 22:23:26`，预览命令为 `-Mode deploy-release -Environment test -ReleaseTag 26-06-15 22:23:26 -ServerHost 172.30.30.58 ... -TestServerHost 172.30.30.58 -BackupServerHost 172.30.30.59 ...`，目标为测试服，不包含正式服主机参数。

GREEN: runtime-control-button-deploy-started -> PASS, 已通过运行控制台“确认执行”按钮提交测试服部署 operation `e257f932-deac-443d-9673-d60651637f80`；远端 `.env` 已验证 `IMAGE_TAG=26-06-15_22-23-26`。

RED: runtime-control-button-deploy-release -> FAIL, operation `e257f932-deac-443d-9673-d60651637f80` 在测试服部署过程中失败；原因是 `deploy-release` 要求发布包本地缓存存在 `preflight-plan.json`，但运行控制台“构建发布包”链路未生成该文件，导致 required SQL 执行前 fail fast。失败前仅完成镜像/SQL/运行脚本复制、镜像 load、MySQL/Redis 预检查和 `.env` 标签设置，未完成后端/前端 compose 更新验收。

RECOVERY: release-lock -> PASS, 测试服 `infra_release_operation_lock` 中 operation `test-26-06-15 22:23:26` 已从残留 `RUNNING` 标记为 `FAILED`，失败原因记录为缺少 `preflight-plan.json`，避免后续按钮重试被旧锁阻塞。

GREEN: deploy-preflight-plan-tests -> PASS, `python -X utf8 -m pytest script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`，88 passed；已补齐 `release_preflight_plan.py` CLI，并让 `deploy-release` 在 acquired lock 后、required SQL 前按目标测试服已应用迁移生成 `preflight-plan.json`。

GREEN: runtime-control-button-deploy-retry-preview -> PASS, 通过运行控制台按钮重试部署发布包 `26-06-15 22:23:26` 到测试服；预览命令仍为 `-Mode deploy-release -Environment test -ReleaseTag 26-06-15 22:23:26 -ServerHost 172.30.30.58 ... -TestServerHost 172.30.30.58 -BackupServerHost 172.30.30.59 ...`，不包含 `-ProdServerHost`。

RED: runtime-control-button-deploy-retry -> FAIL, operation `b5c2ac62-ab86-46f2-b305-204e9d7585e7` 越过旧的 `preflight-plan.json missing`，但在生成目标 preflight plan 时失败：`Join-Path` 的 `Path` 参数为空，原因是新函数使用了未定义的 `$RepoRoot`，应使用已解析的 `$backendRepo`。

RECOVERY: release-lock-after-retry -> PASS, 测试服 operation `test-26-06-15 22:23:26` 已从残留 `RUNNING` 标为 `FAILED`，失败原因记录为 `$RepoRoot` 为空导致 preflight plan 生成失败。

GREEN: deploy-preflight-plan-tests-after-backendrepo-fix -> PASS, 将 `Write-ReleasePreflightPlan` 的脚本路径根从 `$RepoRoot` 修正为 `$backendRepo`，重新执行 `python -X utf8 -m pytest script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`，88 passed。

BLOCKER: runtime-control-button-deploy-third-attempt -> 运行控制台主面板一度显示候选包 `40` 且测试服当前包为 `26-06-15_22-23-26`，但点击“部署发布包到测试服”后弹窗提示 `Backup/ReleasePackage 中没有可部署发布包`，发布包组合框无数据，“确认执行”按钮禁用。按用户要求禁止直接命令行调用构建发布包，因此不能绕过按钮执行 `publish-int-ruoyi.ps1`。测试服 `infra_release_operation_lock` 已核验为 `FAILED`，无 `RUNNING` 残留锁。

RED: runtime-control-button-deploy-third-submit -> FAIL, operation `e4a8c91a-3ed8-4368-904a-9977fc3856fd` 已通过运行控制台按钮提交，越过 `preflight-plan.json missing` 和 `$RepoRoot` 为空问题；失败于 `Assert-RemoteQuartzSchemaReady`，原因是 `release_preflight_plan.py` 只读取顶层 `manifest.schemaMigrations`，而真实 Manifest v1 将迁移放在 `database.schemaMigrations`，导致生成 `items: []`，required SQL 没有执行。

GREEN: deploy-preflight-manifest-v1-tests -> PASS, 修复 `release_preflight_plan.py` 支持 Manifest v1 `database.schemaMigrations`，并将 checksum 漂移项纳入 APPLY 重跑当前包 required SQL；`python -X utf8 -m pytest script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`，90 passed。用真实发布包生成 preflight：status=`passed`，items=146，APPLY=59，SKIP=87，BLOCKED=0。

GREEN: release-package-candidate-readonly-check -> PASS, NAS 路径 `/mnt/nas/Backup/ReleasePackage/26-06-15_22-23-26` 存在，包含 `release-manifest.json`、`manifest.json`、镜像 tar、`required-sql`、`runtime-env`；`release-manifest.json` 中 `releaseTag=26-06-15 22:23:26`、`packageDirectoryName=26-06-15_22-23-26`、`component=intruoyi`、`includeShowroomBuildPackage=false`、`onlyOfficeIncluded=false`，且 artifacts 包含 sha256。运行控制台 `release-packages` 接口未携带登录态时返回 `code=401`，因此不能用 shell 绕过按钮提交发布。

BLOCKER: runtime-control-button-submit-required -> 当前没有新的运行控制台 operation；最新仍为 `e4a8c91a-3ed8-4368-904a-9977fc3856fd`，状态 `failed`。用户明确禁止命令行直接构建发布，且当前浏览器自动化策略不能代替用户操作运行控制台按钮；必须由已登录运行控制台页面点击“部署发布包到测试服”并选择 `26-06-15 22:23:26` 后才能继续追踪发布。

BDD: Kingdee 同步记录索引迁移可重放 -> Given 测试服发布重试时旧唯一索引可能已被删除且新租户维度唯一索引已存在，When required SQL 被再次执行，Then 脚本应识别当前合法状态并继续；若旧索引和新索引都不存在，则必须 fail fast。

RED: kingdee-tenant-unique-replay-test -> FAIL, `python -X utf8 -m pytest script\tests\test_erp_kingdee_sync_record_tenant_unique_sql.py script\tests\test_mes_puhui_schedule_menu_migration.py script\tests\test_mes_edhr_execution_list_hide_menu_migration.py script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`，旧脚本仅固定 `DROP INDEX`/`ADD UNIQUE KEY`，没有 `INFORMATION_SCHEMA.STATISTICS` 与可重放状态检查。

GREEN: kingdee-tenant-unique-replay-test -> PASS, 将 `20260613_erp_kingdee_sync_record_tenant_unique.sql` 改为过程化迁移：旧索引存在才删除，新索引不存在才添加，旧/新索引都不存在时 `SIGNAL SQLSTATE '45000'`；同一测试命令 `95 passed`。

RECOVERY: release-lock-after-kingdee-failure -> PASS, 测试服 `20260613_erp_kingdee_sync_record_tenant_unique` 残留 `RUNNING` 迁移与 operation `test-26-06-16 01:47:23` 已标记为 `FAILED`，原因记录为旧唯一索引已删除且需使用修复后发布包重试。

GREEN: runtime-control-button-build-after-kingdee-fix -> PASS, 通过运行控制台“构建发布包”按钮提交 operation `8eb3891f-c9f1-4580-8673-3272ae13ea85`，发布标签 `26-06-16 02:11:18`，日志显示后端 Maven 构建、前端 Vite 构建、Docker 镜像构建与 NAS 发布包上传完成，状态 `succeeded`。

RED: runtime-control-button-publish-after-kingdee-fix -> FAIL, 通过运行控制台按钮提交测试服发布 operation `796aff73-b33d-41a0-b129-d911ef8e7e35`，发布标签 `26-06-16 02:11:18`；发布推进到 `20260613_erp_kingdee_sync_record_tenant_unique.sql` 后失败，原因是 MySQL `PREPARE stmt FROM v_sql` 不能直接使用过程局部变量，需改为用户变量。

GREEN: kingdee-tenant-unique-dynamic-ddl-test -> PASS, 将动态 DDL 改为 `SET @kingdee_tenant_unique_sql = v_sql; PREPARE stmt FROM @kingdee_tenant_unique_sql;`，并补充测试禁止 `PREPARE stmt FROM v_sql`；同一发布迁移测试命令 `95 passed`。

RECOVERY: release-lock-after-kingdee-prepare-failure -> PASS, 测试服 `20260613_erp_kingdee_sync_record_tenant_unique` 残留 `RUNNING` 迁移与 operation `test-26-06-16 02:11:18` 已标记为 `FAILED`，原因记录为动态 DDL PREPARE 语法修复后需重建包。

GREEN: runtime-control-button-build-after-kingdee-prepare-fix -> PASS, 通过运行控制台“构建发布包”按钮提交 operation `0de1f8a6-4676-4b1a-a044-14ee077d95e7`，发布标签 `26-06-16 02:31:33`，日志显示 `Release package built: 26-06-16 02:31:33`，NAS 路径 `Backup/ReleasePackage/26-06-16_02-31-33`。

RED: runtime-control-button-publish-after-kingdee-prepare-fix -> FAIL, 通过运行控制台按钮提交测试服发布 operation `acbd4455-c00a-427b-b5d8-c752958edcc9`，发布标签 `26-06-16 02:31:33`；发布越过 Kingdee 租户唯一索引迁移并推进到 DCC 产品可见组迁移，失败于 `20260614_dcc_product_visibility_group.sql`：`Duplicate column name 'scope_type'`，原因是迁移重跑时重复添加已存在字段。

GREEN: dcc-product-visibility-scope-type-replay-test -> PASS, 将 `20260614_dcc_product_visibility_group.sql` 改为通过 `INFORMATION_SCHEMA.COLUMNS` 检查 `dcc_file_category_permission_rule.scope_type`，字段缺失才添加，已存在则输出状态；`python -X utf8 -m pytest script\tests\test_dcc_product_visibility_group_sql.py script\tests\test_erp_kingdee_sync_record_tenant_unique_sql.py script\tests\test_mes_puhui_schedule_menu_migration.py script\tests\test_mes_edhr_execution_list_hide_menu_migration.py script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`，98 passed。

RECOVERY: release-lock-after-product-visibility-failure -> PASS, 测试服 `20260614_dcc_product_visibility_group` 残留 `RUNNING` 迁移与 operation `test-26-06-16 02:31:33` 已标记为 `FAILED`，原因记录为 `scope_type` 重复字段修复后需重建包。

GREEN: runtime-control-button-build-after-product-visibility-fix -> PASS, 通过运行控制台“构建发布包”按钮提交 operation `9503cb2b-b022-4311-8a90-071042e956cb`，发布标签 `26-06-16 02:52:54`，日志显示 `Release package built: 26-06-16 02:52:54`，NAS 路径 `Backup/ReleasePackage/26-06-16_02-52-54`。

GREEN: runtime-control-button-publish-after-product-visibility-fix -> PASS, 通过运行控制台按钮提交测试服发布 operation `c91ab375-924f-493e-8cf8-55739d778dd1`，发布标签 `26-06-16 02:52:54`；发布完成，后端健康检查、前端首页、PDF worker MIME 验证均通过，运行态镜像为 `intruoyi-backend:26-06-16_02-52-54` / `intruoyi-frontend:26-06-16_02-52-54`。

RED: test-server-login-before-captcha-config-fix -> FAIL, Playwright 打开 `http://172.30.30.58:8081/system/nas` 并用 `测试租户/aoteman` 登录，登录接口返回 `captchaVerification: 验证码不能为空`；运行态核验显示后端 `SPRING_PROFILES_ACTIVE=dev` 且验证码开启，而前端测试构建未显示验证码控件，前后端登录配置不一致。

GREEN: test-compose-captcha-consistency-test -> PASS, 在测试服 compose 后端启动参数加入 `--yudao.captcha.enable=false`，与前端测试构建保持一致；`python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_dcc_product_visibility_group_sql.py script\tests\test_erp_kingdee_sync_record_tenant_unique_sql.py script\tests\test_mes_puhui_schedule_menu_migration.py script\tests\test_mes_edhr_execution_list_hide_menu_migration.py script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`，182 passed。

GREEN: runtime-control-button-build-after-captcha-config-fix -> PASS, 通过运行控制台“构建发布包”按钮提交 operation `c7d69e38-aaa2-4bdb-a90a-0f9d676b5043`，发布标签 `26-06-16 03:21:37`，日志显示 `Release package built: 26-06-16 03:21:37`，NAS 路径 `Backup/ReleasePackage/26-06-16_03-21-37`。

GREEN: runtime-control-button-publish-after-captcha-config-fix -> PASS, 通过运行控制台按钮提交测试服发布 operation `cc54dd9a-d13a-4842-94ff-10b5d5cda188`，发布标签 `26-06-16 03:21:37`；发布完成，后端健康检查、前端首页、PDF worker MIME 验证均通过。运行态 `docker compose config` 已确认后端启动参数包含 `--yudao.captcha.enable=false`。

GREEN: nas-local-folder-import-e2e-diagnostic -> PASS, 使用真实页面和真实目录 `E:\Downloads\3.DMR` 复核本地文件夹导入链路；页面先创建了上传任务 `14`，后端任务状态保持 `UPLOADING`，但 10 分钟内未出现任何 `/chunks` 请求，`uploadChunkCount` 始终为 `0`。当前证据说明卡点不在登录或会话创建，而在确认导入后未进入分片上传阶段，需继续沿前端分片启动链路排查。

BDD: local-folder-import-session-resume -> Given 用户刷新页面或重新选择同一本地目录且已有 `LOCAL_FOLDER/UPLOADING` 上传任务，When 页面再次创建本地导入 session，Then 后端应返回同一上传任务以便前端继续执行 `upload-state -> chunks`，而不是用 `nas transfer task already active` 阻断续传。

RED: local-folder-import-session-resume-test -> FAIL, `mvn -pl yudao-module-dcc -am "-Dtest=cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，新增用例期望同用户同根目录复用已有 `UPLOADING` 任务，旧实现返回 `nas transfer task already active: 18`。

GREEN: local-folder-import-session-resume-test -> PASS, `createLocalFolderImportSession` 已改为同用户、同根目录、`LOCAL_FOLDER/UPLOADING` 时返回现有任务；同一 Maven 命令通过，`Tests run: 27, Failures: 0, Errors: 0, Skipped: 0`。

GREEN: commit-precheck-whitespace -> PASS, `git diff --check` 仅提示 Git LF/CRLF 触碰提示，无空白错误。

GREEN: commit-precheck-python -> PASS, `python -X utf8 -m pytest script/tests/test_dcc_product_visibility_group_sql.py script/tests/test_erp_kingdee_sync_record_tenant_unique_sql.py script/tests/test_mes_edhr_execution_list_hide_menu_migration.py script/tests/test_mes_puhui_schedule_menu_migration.py script/tests/test_publish_int_ruoyi_to_test_tooling.py script/tests/test_release_preflight_plan.py -q`，99 passed。

GREEN: commit-precheck-maven -> PASS, `mvn --% -pl yudao-module-infra,yudao-module-dcc -Dtest=RuntimeControlServiceImplTest,DccControlledFileNasTransferServiceTest -DfailIfNoTests=false test`，Tests run: 92, Failures: 0, Errors: 0。

GREEN: commit-precheck-staged -> PASS, `git diff --cached --check` 通过，暂存范围 159 个文件，未包含 `runtime/` 运行期产物。

RED: backend-commit-without-tdd-task-dir -> FAIL, `git commit -m "任务: 完成测试服发布链路修复"` 被提交钩子阻断，原因是未设置 `TDD_TASK_DIR`。

BDD: backup-nas-transfer-task-16-fail-cleanup -> Given 备份服务器 DCC 本地文件夹导入任务 16 残留为 UPLOADING 并阻塞新导入，When 用户明确要求停掉任务 16、标记失败并清理剩余，Then 仅对备份服 task_id=16 执行定点失败标记和剩余项清理核验，不影响正式服和其他任务。
GREEN: experience-preflight -> PASS, 2026-06-16 13:19:46 已按门禁读取 docs/server-access.md、docs/release-backup-restore.md、docs/experience-index.md；目标环境 backup，目标主机 172.30.30.59，授权范围仅 DCC NAS transfer task 16 失败标记与剩余清理，正式服不操作。
GREEN: backup-nas-transfer-task-16-fail-cleanup -> PASS, 备份服 task_id=16 已从 UPLOADING 标记为 FAILED；task 明细中剩余的 1805 个 DIRECTORY 和 9331 个 FILE 项已统一标记为 FAILED，当前活跃任务数为 0。后端最近日志仅保留 13:17:49 的 nas transfer task already active: 16 失败记录，未再产生新的 active task。

GREEN: backup-nas-transfer-task-17-fail-cleanup -> PASS, 备份服 task_id=17 已从 UPLOADING 标记为 FAILED；该任务 uploaded_file_count=0、uploaded_total_bytes=0，未发现需额外失败化的明细计数输出，当前活跃任务数为 0。

BDD: nas-test-package-cleanup -> Given NAS 上 BackupPackage 全部为测试备份包且 ReleasePackage 只需保留 26-06-16_03-21-37，When 用户明确要求删除测试备份包和其他构建包，Then 清理命令必须硬校验根目录并只删除目标目录的一层子项，不删除 NAS 根目录、ReleasePackage 根目录或保留版本。
GREEN: experience-preflight -> PASS, 2026-06-16 14:54:19 已确认本次授权范围为 NAS 测试备份包清理与发布包保留清理；目标路径 /mnt/nas/Backup/BackupPackage 与 /mnt/nas/Backup/ReleasePackage；保留构建包 26-06-16_03-21-37；正式服不操作。
GREEN: nas-test-package-cleanup -> PASS, 已清理 /mnt/nas/Backup/BackupPackage 的 136 个直接子项，清理后剩余 0；已清理 /mnt/nas/Backup/ReleasePackage 中除 26-06-16_03-21-37 外的 79 个直接子项，清理后仅剩 26-06-16_03-21-37，且保留包 manifest.json 与 release-manifest.json 存在。