# 执行日志：NAS 权限转移恢复真实数据 E2E

BDD: 测试租户完整 NAS 权限恢复 happy path -> Given 测试租户存在可安全转移的小型真实 NAS 文件夹和启用的 DCC 类别 `其他` / When 用户在 `NAS管理` 选择该文件夹并转移到 DCC / Then 系统创建真实转移任务，完成后展示权限快照、恢复预览，并允许显式应用恢复。

BDD: 测试租户缺失前置条件 fail fast -> Given 测试租户缺少安全 NAS 路径、启用的 `其他`、权限管理权限或后端新接口 / When E2E 启动 / Then 用例必须失败并报告缺失前置条件，不得改用芋道源码租户或 mock 数据。

BDD: 测试租户阻断路径可见 -> Given 快照存在未映射主体、DENY ACE、不支持权限或 ACL 采集失败 / When 用户打开权限恢复抽屉 / Then UI 必须展示真实阻断原因，并禁止应用恢复。

BDD: 芋道源码 admin 只读验证 -> Given 芋道源码租户 admin 登录正式或指定验证环境 / When 打开 NAS 管理并检查转移弹窗和权限恢复入口 / Then 页面必须加载真实接口、默认类别规则仍为 `其他`，且用例不得提交转移、保存映射或应用恢复。

RED: 现有测试覆盖审计 -> FAIL，当前仓库只有 `tests/e2e/dcc-nas-permission-restore-static.spec.js` 做源码片段断言，没有 Playwright 浏览器用例覆盖 `/system/nas` 真实路径、测试租户真实 NAS 选择、转移任务、快照、映射、恢复预览、应用恢复和芋道源码 admin 只读验证。

GREEN: `pnpm add -D playwright@1.60.0 --ignore-scripts` -> PASS，前端仓库声明真实浏览器 E2E 所需 Playwright devDependency，避免依赖其他 worktree 或全局环境。

IMPLEMENTATION: 新增 `tests/e2e/dcc-nas-permission-real-data.e2e.js` -> 支持 `--mode=test-write`、`--mode=admin-readonly` 和 `--mode=all`。`test-write` 仅允许测试租户写入，必须显式设置 `NAS_PERMISSION_E2E_ALLOW_TEST_WRITE=1` 与 `NAS_PERMISSION_E2E_TEST_NAS_PATH`；`admin-readonly` 登录芋道源码/admin 后只读检查 NAS 页面、连接测试、目录刷新、转移弹窗默认 `其他`，并阻止 DCC/NAS 权限恢复写请求。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js` -> FAIL，缺少 `--mode` 时 fail fast，避免误触发真实环境写操作。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> FAIL，未设置 `NAS_PERMISSION_E2E_ALLOW_TEST_WRITE=1`，脚本阻止测试租户写入。

RED: `$env:NAS_PERMISSION_E2E_ALLOW_TEST_WRITE='1'; node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> FAIL，缺少 `NAS_PERMISSION_E2E_TEST_NAS_PATH`，脚本拒绝默认选择 NAS 大目录。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，芋道源码/admin 能登录并读取 NAS 配置与目录，但 DCC 文件类别接口没有启用的 `其他`，转移弹窗按要求 fail fast；截图：`output/playwright/dcc-nas-permission-real-data-admin-readonly-1779859584714.png`。

RED: 测试环境只读登录探测 -> FAIL，使用 `http://172.30.30.58:8081`、租户 `测试租户`、用户 `aoteman` 时登录页找不到 `测试租户` 选项；截图：`output/playwright/dcc-nas-permission-real-data-admin-readonly-1779859623603.png`。

IMPLEMENTATION: 登录租户选择兼容真实 `el-select allow-create` 行为 -> 当登录页历史选项没有 `测试租户` 时，脚本直接输入租户名并回车，仍然使用真实前端登录控件，不注入 mock。

IMPLEMENTATION: `/system/nas` 404 诊断 -> E2E 脚本在登录跳转或直接访问返回 404 时立即报告缺少 NAS 管理菜单/路由权限或菜单部署前置条件，避免把权限/部署问题误判成等待元素超时。

REVIEW: 独立子 agent 覆盖审查 -> NO-GO，真实写入前必须补齐测试写入 API origin guard、转移 POST `templateCategoryId` 真实 `其他` ID 断言、权限快照 items 列表断言；现有测试租户 `/system/nas` 404 与芋道/admin 缺少启用 `其他` 均归类为环境数据/权限前置条件。

IMPLEMENTATION: 真实写入前门禁加固 -> `test-write` 模式新增 `NAS_PERMISSION_E2E_TEST_API_ORIGIN` 后端 origin 拦截，任何 `/admin-api` 请求偏离预期测试后端会被浏览器路由直接 abort；转移创建请求断言 `templateCategoryId` 等于接口返回的启用 `其他` ID，且 `selectedNasPaths` 包含显式安全 NAS 路径；打开恢复抽屉时等待并断言 `/permission-snapshot/items` 列表和表格行。

RED: 测试环境只读 NAS 页面探测 -> FAIL，使用 `http://172.30.30.58:8081`、租户 `测试租户`、用户 `aoteman` 能完成真实登录，但访问 `/system/nas` 返回 404；缺失前置条件为测试租户用户必须具备 NAS 管理菜单与路由权限，或测试环境必须部署对应菜单；截图：`output/playwright/dcc-nas-permission-real-data-admin-readonly-1779860797371.png`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，2026-05-27 复验芋道源码/admin 真实只读路径仍缺少启用的 DCC 类别 `其他`；截图：`output/playwright/dcc-nas-permission-real-data-admin-readonly-1779860851661.png`。

RED: 测试环境只读 NAS 页面复验 -> FAIL，加固 API origin guard 后，`测试租户/aoteman` 仍能登录但 `/system/nas` 返回 404；截图：`output/playwright/dcc-nas-permission-real-data-admin-readonly-1779861316512.png`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，加固 API origin guard 后，芋道源码/admin 仍稳定失败在缺少启用的 DCC 类别 `其他`；截图：`output/playwright/dcc-nas-permission-real-data-admin-readonly-1779861284425.png`。

DIAGNOSTIC: 测试服只读数据库核对 -> PASS，测试服运行正常；`测试租户/aoteman` 对应用户 `system_users.id=113 tenant_id=122`，角色 `111 tenant_admin` 与 `910209 showroom_publicity`。角色 `111` 已有 `dcc:controlled-file:submit`、`dcc:controlled-file:directory:manage`、`dcc:controlled-file:category:manage`、`dcc:controlled-file:access-rule:manage`、`dcc:controlled-file:query`，但缺少 NAS 菜单与权限菜单 `5900/5901/5902/5903`，解释 `/system/nas` 404。

DIAGNOSTIC: 测试租户 DCC 类别只读核对 -> PASS，测试服 `dcc_file_category` 中 `产品技术要求` 仅存在于 `tenant_id=0/1`，`测试租户 tenant_id=122` 没有启用的 `产品技术要求` 源类别，也没有启用的 `其他`；按无 fallback 规则，不能为测试租户创建空 `其他` 模板。

GREEN: `node --check tests\e2e\dcc-nas-permission-real-data.e2e.js` -> PASS。

GREEN: `pnpm exec eslint tests\e2e\dcc-nas-permission-real-data.e2e.js` -> PASS。

GREEN: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS。

GREEN: `pnpm exec eslint src\api\dcc\controlledFile\workflow.ts src\views\system\nas\index.vue src\views\system\nas\components\NasPermissionRestorePanel.vue tests\e2e\dcc-nas-permission-restore-static.spec.js tests\e2e\dcc-nas-permission-real-data.e2e.js` -> PASS。

GREEN: 测试租户前置数据修复演练 -> PASS，在测试租户 `tenant_id=122` 内补齐 NAS 菜单授权、启用的 `产品技术要求` 与启用的 `其他` 类别治理配置；源数据仅读取租户 1，写入仅限测试租户。

GREEN: 测试租户只读 NAS 页面探测 -> PASS，`测试租户/aoteman` 能通过真实登录控件打开 `http://172.30.30.58:8081/system/nas`。

PARTIAL: 测试租户真实写路径转移任务 -> PASS，已用真实 NAS 小目录创建并完成转移任务 `4/5/6`，覆盖目录选择、默认 `其他`、转移提交真实 ID、任务轮询完成。

RED: 测试租户完整权限恢复写路径 -> FAIL，转移任务 `6` 完成后结果弹窗未显示权限恢复面板，E2E 等待 `刷新权限状态` 按钮超时；影响为权限快照列表、身份映射、恢复预览、应用恢复和恢复后目录规则校验仍没有 GREEN 证据。

STATUS: 当前每个功能点均已有脚本级 E2E 覆盖入口，但只有登录、NAS 读取、目录选择、转移提交和任务完成取得真实路径证据；权限恢复后半段尚未跑通，不能视为每个功能点都有已通过的 E2E。

GREEN: 测试服 DCC NAS ACL 表结构迁移 -> PASS，已在测试库创建 `dcc_nas_acl_snapshot`、`dcc_nas_acl_directory_snapshot`、`dcc_nas_acl_descriptor`、`dcc_nas_acl_ace`、`dcc_nas_acl_identity_mapping`、`dcc_nas_acl_restore_plan`、`dcc_nas_acl_restore_plan_item`、`dcc_nas_acl_restore_log`。

GREEN: NAS 权限快照/恢复后端目标测试 -> PASS，`mvn -pl yudao-module-dcc '-Dtest=DccNasPermissionSchemaTest,DccNasPermissionSnapshotControllerTest,DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreExecutionServiceTest' test` 共 38 个测试通过。

GREEN: 测试服前后端部署 -> PASS，backend/frontend 镜像已切换到 `20260527_nas_acl_e2e`，`/admin-api/dcc/controlled-files/nas-transfer/tasks/{taskId}/permission-snapshot` 已从静态资源 404 变为登录态接口响应。

RED: 测试租户真实写路径快照等待 -> FAIL，任务 `8` 转移完成后 `刷新权限状态` 返回 `snapshotStatus=RUNNING`，旧 E2E 立即断言失败；期望脚本轮询异步快照采集终态后再进入恢复预览。

IMPLEMENTATION: E2E 快照轮询 -> 新增 `waitForPermissionSnapshotCaptured`，在 `snapshotStatus` 为 `WAITING/RUNNING` 时轮询 `/permission-snapshot`，直到 `CAPTURED/SUCCESS` 或明确失败/超时。

RED: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> FAIL，`NasPermissionRestorePanel.vue` 直接调用 `crypto.randomUUID()`，HTTP 测试环境浏览器报 `crypto.randomUUID is not a function`，导致应用恢复按钮无法提交。

GREEN: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS，恢复幂等键改用项目 `generateUUID()` 工具。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionSchemaTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，`dcc_nas_acl_restore_plan.semantic_policy_version` 与 `identity_mapping_version` 为 `varchar(32)`，无法保存 `NAS_SEMANTIC_POLICY_V1` 等真实版本值。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionSchemaTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，版本列已扩展为 `varchar(64)`，测试库也已执行 ALTER。

RED: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> FAIL，初始化 SQL 未包含版本列扩展与恢复 hash 列宽度断言。

GREEN: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> PASS，恢复版本列为 `varchar(64)`，恢复 hash 列为 `varchar(128)`。

RED: 测试租户真实写路径应用恢复 -> FAIL，`dcc_nas_acl_restore_plan_item.expected_after_hash` 为 `char(64)`，真实 `sha256:` 前缀 hash 长度为 71，写入时报 `Data too long for column 'expected_after_hash'`。

GREEN: 测试服恢复 hash 列迁移 -> PASS，`dcc_nas_acl_restore_plan_item.expected_after_hash/actual_after_hash` 与 `dcc_nas_acl_restore_log.before_hash/expected_after_hash/actual_after_hash` 均为 `varchar(128)`。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，`DccNasPermissionRestoreExecutionScheduler` 不存在，恢复计划保持 `READY` 不会被后台执行。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，新增租户维度恢复调度器，每 30 秒处理等待恢复计划。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccDirectoryControllerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，`GET /dcc/directories/{id}/access-rules` 将恢复后的 `subjectType=USER` 转成 `Integer` 时抛出 `Unsupported int format: [USER]`。

GREEN: `mvn -pl yudao-module-dcc -am '-Dtest=DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，目录访问规则请求/响应统一使用字符串主体类型 `USER/DEPT/ROLE/POSITION`。

RED: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> FAIL，前端目录访问规则 API 与主体类型下拉仍使用数字主体类型。

GREEN: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS，前端目录访问规则 API 类型与下拉选项统一使用字符串主体类型。

RED: `python -X utf8 -m pytest script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q` -> FAIL，缺少目录访问规则历史数字主体类型归一迁移。

GREEN: `python -X utf8 -m pytest script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q` -> PASS，新增幂等 SQL 将 `1/2/3/4` 归一为 `USER/DEPT/ROLE/POSITION`；测试库执行后 `dcc_directory_access_rule.subject_type` 仅剩 `USER`，共 `101` 条。

GREEN: 前端构建与静态验证 -> PASS，`node tests\e2e\dcc-nas-permission-restore-static.spec.js`、`pnpm exec eslint src\api\dcc\controlledFile\directories.ts src\api\dcc\controlledFile\workflow.ts src\views\dcc\controlled-file\access-rules\index.vue src\views\dcc\controlled-file\shared\options.ts src\views\system\nas\index.vue src\views\system\nas\components\NasPermissionRestorePanel.vue tests\e2e\dcc-nas-permission-restore-static.spec.js tests\e2e\dcc-nas-permission-real-data.e2e.js`、`node node_modules\vite\bin\vite.js build --mode test` 均通过。

GREEN: 后端构建 -> PASS，`mvn -f pom.xml -pl yudao-server -am -DskipTests package` 通过。

DEPLOY: 测试服部署 -> PASS，前端镜像 `intruoyi-frontend:20260527_nas_acl_e2e_fix5`，backend 镜像 `intruoyi-backend:20260527_nas_acl_e2e_fix5` 已加载并重启；当前 tag `20260527_edhr_fusion_e2e` 对应 frontend image `sha256:340b5076a00425bc5a4612ac94f5ea8d9546fe8932586b02e5963845013e4a0d`，backend image `sha256:3ba97b971875086c0c91a8a9dbcb9404a4aed74e6a3ee43d84b7ec17f318e7f6`；健康检查 `UP`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，环境为 `测试租户/aoteman`、`NAS_PERMISSION_E2E_TEST_NAS_PATH=9. 其他`、`NAS_PERMISSION_E2E_TEST_API_ORIGIN=http://172.30.30.58:48081`，结果 `PASS: test-write taskId=23, restoreId=4, directories=2, rules=47`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，环境为 `http://172.30.30.58:8081`、`芋道源码/admin`，失败于真实 DCC 类别接口未返回启用的 `其他`；截图 `output/playwright/dcc-nas-permission-real-data-admin-readonly-1779885732694.png`。验证阶段不得修改芋道源码租户数据，因此记录为数据前置条件阻塞。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> PASS，环境为 `http://172.30.30.58:8081`、`测试租户/aoteman`，只读路径无 DCC/NAS 权限恢复写请求。

GREEN: 后端目标回归 -> PASS，`mvn -pl yudao-module-dcc -am '-Dtest=DccControlledFileNasTransferServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccNasPermissionSchemaTest,DccNasPermissionSnapshotControllerTest,DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreExecutionSchedulerTest,DccNasPermissionRestoreControllerTest,DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` 共 71 个测试通过。

REVIEW: 独立子 agent Aquinas 复核 -> CONDITIONAL GO for int_main，NO-GO for release；指出 `芋道源码/admin` 只读未 GREEN、身份映射保存与 blocker 分支缺少真实 GREEN、恢复调度器 catch-and-continue 与 no-fallback 口径冲突。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，新增 fail-fast 断言后，当前恢复调度器吞掉租户异常并继续后续租户，不符合本任务 no-fallback 口径。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，恢复调度器移除 catch-and-continue，异常直接抛出。

DEPLOY: 测试服 backend fail-fast 调度器部署 -> PASS，当前测试服 tag `20260527_edhr_business_flow_repair` 的 backend 容器使用 image `sha256:2733dd99c069cda08c2de96ff06073cde175baea2a41c9314a4add0ab82cd17e`，frontend 容器使用 image `sha256:340b5076a00425bc5a4612ac94f5ea8d9546fe8932586b02e5963845013e4a0d`，健康检查 `UP`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，部署后复验结果 `PASS: test-write taskId=24, restoreId=5, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

GREEN: 测试租户只读复验 -> PASS，`PASS: admin-readonly baseUrl=http://172.30.30.58:8081`。

RED: 芋道源码/admin 只读复验 -> FAIL，`DCC file categories are missing active "其他"; transfer dialog must fail fast`；截图 `output/playwright/dcc-nas-permission-real-data-admin-readonly-1779887788977.png`。

GREEN: 前端目标静态/ESLint -> PASS，`node tests\e2e\dcc-nas-permission-restore-static.spec.js` 与 `pnpm exec eslint ... dcc-nas-permission-real-data.e2e.js` 均通过。

GREEN: 后端目标回归复验 -> PASS，71 个相关测试通过。

STATUS(历史): 测试租户 happy path 真实 E2E 已 GREEN；当时身份映射保存分支与 blocker/禁用应用恢复分支因真实数据 `unmapped=0, blockers=0` 尚未真实 GREEN；该缺口已由后续 `test-mapping` 和 `test-blocker` 复验关闭。

REVIEW: 独立放行报告 -> CONDITIONAL GO for int_main，NO-GO for release；已在 `verification-report.md` 记录测试租户真实写路径、测试租户只读、芋道源码/admin 阻塞、身份映射保存分支缺口和 blocker/禁用恢复分支缺口。

CLEANUP: `task_closeout.py --mode preview` -> BLOCKED，delete 为 `<none>`；阻塞原因为任务未达到完成/可快进合并状态且当前 worktree 仍有待提交改动。

BDD: 身份映射保存真实分支 -> Given 测试租户完成真实 NAS 转移且当前任务存在一个被置为未映射的 SID / When 用户在权限恢复抽屉选择真实 DCC 用户并保存映射 / Then 后端重新激活同 SID 的真实映射记录，恢复预览允许应用恢复，提交时带真实映射结果。

BDD: blocker 禁用应用恢复真实分支 -> Given 测试租户完成真实 NAS 转移且当前任务存在一个克隆 descriptor 上的 DENY ACE / When 用户打开恢复预览 / Then UI 展示 blocker code，`应用恢复` 不可用，并且浏览器不得提交恢复写请求。

RED: SMB 真实 NAS fixture 创建 -> FAIL，账号 `ceshi` 可访问 `\\172.30.30.4\质量体系文件\9. 其他`，但在该路径创建专用测试子目录返回 Access Denied；影响为不能通过 NAS 写 ACL 制造专用分支数据，后续仅允许在测试租户 DB 中对当前任务快照做显式 fixture。

RED: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> FAIL，新增断言要求真实 E2E 脚本包含 `test-mapping`、`test-blocker`、DB fixture 显式开关和分支输出；当前脚本缺少分支模式。

GREEN: `node --check tests\e2e\dcc-nas-permission-real-data.e2e.js` 与 `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS，真实 E2E 脚本新增 `test-mapping`、`test-blocker`，DB fixture 需要 `NAS_PERMISSION_E2E_ALLOW_DB_FIXTURE=1` 与 `NAS_PERMISSION_E2E_FIXTURE_MYSQL_PASSWORD`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> FAIL，当前测试服被切到非 NAS ACL 镜像，页面缺少 `刷新权限状态`。

DEPLOY: 测试服镜像固定 -> PASS，当前运行 tag 重新指向 NAS ACL frontend `sha256:340b5076a00425bc5a4612ac94f5ea8d9546fe8932586b02e5963845013e4a0d` 与 backend fix7 `sha256:f07215e41d818e117835ac185b5a63c93e864882c7b18f190d42d0eac423e5c4`，健康检查 `UP`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> FAIL，保存未映射 SID 时后端返回 500，MySQL 唯一键 `uk_dcc_nas_acl_identity_sid` 冲突；根因是后端只按 `MAPPED` 查重，未重新激活同 SID 的 `INACTIVE` 映射。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> PASS，后端修复并部署后结果 `PASS: test-mapping taskId=27, restoreId=6, directories=2, rules=47, unmapped=2, savedMappings=2, blockers=0`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-blocker` -> FAIL，blocker code 在摘要和明细中各出现一次，Playwright 严格文本定位匹配两个元素。

GREEN: `node --check tests\e2e\dcc-nas-permission-real-data.e2e.js` 与 `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS，blocker code 断言改为 drawer 内至少一个可见匹配。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-blocker` -> FAIL，按钮定位使用 `.el-drawer:visible` 时未找到可见 `应用恢复`，截图确认 UI 正确显示禁用按钮。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-blocker` -> PASS，按钮定位改为 role/name，结果 `PASS: test-blocker taskId=31, unmapped=0, blockers=3`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> FAIL，旧 blocker fixture 直接修改归一化 `dcc_nas_acl_ace`，导致后续真实任务捕获到 `DCC_NAS_ACL_DENY_UNSUPPORTED`，证明 fixture 污染测试租户共享 descriptor。

RED: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> FAIL，新增断言要求 blocker fixture 必须 `INSERT INTO dcc_nas_acl_descriptor` 并 `UPDATE dcc_nas_acl_directory_snapshot ds`，当前脚本缺少克隆式 fixture。

GREEN: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS，blocker fixture 改为克隆当前任务 descriptor/ACE 并只重定向当前任务的 directory snapshot；旧 fixture 污染已在测试租户清理，`cleanup_affected=3`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，清理后结果 `PASS: test-write taskId=33, restoreId=7, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-blocker` -> PASS，克隆式 fixture 结果 `PASS: test-blocker taskId=34, unmapped=0, blockers=1`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，blocker 后无污染复验结果 `PASS: test-write taskId=35, restoreId=8, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> PASS，最终身份映射分支复验结果 `PASS: test-mapping taskId=36, restoreId=9, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`。

GREEN: blocker fixture 非污染验证 -> PASS，测试库查询 `old_direct_fixture_deny=0`、`cloned_fixture_descriptors=1`；task `34` 的根路径有 `deny_count=1`，后续 task `35/36` 对同路径均为 `deny_count=0`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，芋道源码/admin 仍缺少启用的 DCC 类别 `其他`；截图 `output/playwright/dcc-nas-permission-real-data-admin-readonly-1779891788206.png`；验证阶段不得修改芋道源码租户数据。

GREEN: 前端目标静态/ESLint -> PASS，`node --check tests\e2e\dcc-nas-permission-real-data.e2e.js`、`node tests\e2e\dcc-nas-permission-restore-static.spec.js`、`pnpm exec eslint tests\e2e\dcc-nas-permission-real-data.e2e.js tests\e2e\dcc-nas-permission-restore-static.spec.js` 均通过。

CLEANUP: `task_closeout.py --task-id 20260527-nas-permission-real-e2e-suite --mode preview` -> BLOCKED，delete 为 `<none>`；阻塞原因为当前任务仍受芋道源码/admin 数据前置条件限制，且 linked worktree 不能 fast-forward merge 到 `int_main`。

RED: 芋道源码/admin 最终只读复验 -> FAIL，`DCC file categories are missing active "其他"; transfer dialog must fail fast`；截图 `output/playwright/dcc-nas-permission-real-data-admin-readonly-1779892890843.png`；未修改芋道源码租户数据。

SUBAGENT: Pauli 前端只读复核 -> CONDITIONAL GO；指出 `test-mapping` 直接修改共享 `dcc_nas_acl_identity_mapping` 有失败后污染风险、芋道源码/admin 未 GREEN、subagent-driven 记录不足、`pnpm-lock.yaml` 需要确认仅包含本任务必要变更。

SUBAGENT: Archimedes 后端只读复核 -> CONDITIONAL GO；确认 `INACTIVE` SID 重新激活逻辑自洽，后端 release 阻塞仍是芋道源码/admin 缺少启用的 `其他`。

RED: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> FAIL，新增静态断言禁止 `test-mapping` fixture 直接 `UPDATE dcc_nas_acl_identity_mapping m`，当前脚本失败。

GREEN: `node tests\e2e\dcc-nas-permission-restore-static.spec.js` -> PASS，`test-mapping` fixture 改为克隆当前任务 descriptor/ACE 并注入任务专属合成 SID，不再修改共享 identity mapping。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> FAIL，克隆式 mapping fixture 中 `@e2e_mapping_sid_hash` 与表字段 collation 不一致，MySQL 报 `Illegal mix of collations`。

GREEN: 前端静态/ESLint -> PASS，显式将合成 SID/hash 转为 `utf8mb4_unicode_ci` 后，`node --check`、静态断言和 ESLint 通过。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> FAIL，ACE 中合成 SID hash 为 MySQL `SHA2()` 小写，而后端保存映射为大写 hash，恢复预览仍报 `DCC_NAS_PRINCIPAL_UNMAPPED`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-mapping` -> PASS，将 fixture SID hash 改为大写后，结果 `PASS: test-mapping taskId=39, restoreId=10, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，mapping fixture 后续非污染复验结果 `PASS: test-write taskId=40, restoreId=11, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

GREEN: mapping fixture 非污染 DB 复验 -> PASS，`inactive_old_mapping_fixture_rows=0`、`mapping_clone_descriptors=2`；task `39` 有 `synthetic_sid_count=1`，task `40` 同路径 `synthetic_sid_count=0` 且 `deny_count=0`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-blocker` -> PASS，最终脚本下 blocker 分支结果 `PASS: test-blocker taskId=41, unmapped=0, blockers=1`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，blocker fixture 后续非污染复验结果 `PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

GREEN: `pnpm-lock.yaml` 差异收窄 -> PASS，移除非必要 direct `graceful-fs`，以原始 lockfile 为基线仅加入 `playwright@1.60.0`、`playwright-core@1.60.0` 和 Playwright optional `fsevents@2.3.2`，lockfile diff 从全量 registry churn 收窄为 `+29`。

GREEN: 前端最终静态/ESLint -> PASS，`node --check tests\e2e\dcc-nas-permission-real-data.e2e.js`、`node tests\e2e\dcc-nas-permission-restore-static.spec.js`、`pnpm exec eslint tests\e2e\dcc-nas-permission-real-data.e2e.js tests\e2e\dcc-nas-permission-restore-static.spec.js` 均通过。

GREEN: 芋道源码/admin 数据前置条件解除 -> PASS，后端运行库补齐任务已在测试服为 `tenant_id=1` 创建唯一启用的 DCC 类别 `其他`，治理规则数量与 `产品技术要求` 一致。

GREEN: 芋道源码/admin 只读 E2E -> PASS，`node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` 输出 `PASS: admin-readonly baseUrl=http://172.30.30.58:8081`，只读 guard 未发现转移、映射或恢复写请求。

GREEN: 最终前端回归 -> PASS，`node --check tests\e2e\dcc-nas-permission-real-data.e2e.js`、`node tests\e2e\dcc-nas-permission-restore-static.spec.js`、`pnpm exec eslint tests\e2e\dcc-nas-permission-real-data.e2e.js tests\e2e\dcc-nas-permission-restore-static.spec.js src\api\dcc\controlledFile\directories.ts src\api\dcc\controlledFile\workflow.ts src\views\dcc\controlled-file\access-rules\index.vue src\views\dcc\controlled-file\shared\options.ts src\views\system\nas\index.vue src\views\system\nas\components\NasPermissionRestorePanel.vue` 均通过。
