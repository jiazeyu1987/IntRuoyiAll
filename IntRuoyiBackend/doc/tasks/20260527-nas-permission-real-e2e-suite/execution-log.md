# 执行日志：NAS 权限恢复真实 E2E 后端修复

BDD: 测试租户恢复计划自动执行 -> Given 测试租户完成 NAS 转移并创建权限恢复计划 / When 后台调度运行 / Then READY 计划会在租户上下文内执行并最终进入 COMPLETED 或明确失败。

BDD: 恢复后的 DCC 目录权限可读取 -> Given 恢复服务写入 `subjectType=USER` 的目录访问规则 / When 前端读取 `/dcc/directories/{id}/access-rules` / Then 接口返回字符串主体类型，不因数字转换报 500。

BDD: 真实版本与 hash 可持久化 -> Given 恢复计划保存语义版本、映射版本和 `sha256:` 前缀 hash / When 写入 MySQL / Then 字段长度足够且初始化 SQL 与测试 schema 一致。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionSchemaTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，恢复计划版本列为 `varchar(32)`，真实版本值写入失败。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionSchemaTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，版本列扩展为 `varchar(64)`。

RED: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> FAIL，初始化 SQL 未包含版本列和 hash 列宽度断言。

GREEN: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> PASS，恢复版本列为 `varchar(64)`，恢复 hash 列为 `varchar(128)`。

RED: 测试租户真实 E2E 应用恢复 -> FAIL，`expected_after_hash` 为 `char(64)`，真实 `sha256:` 前缀 hash 写入时报 `Data too long`。

GREEN: 测试服 MySQL ALTER -> PASS，恢复计划 item 与 log hash 字段均为 `varchar(128)`。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，缺少 `DccNasPermissionRestoreExecutionScheduler`，恢复计划停留 `READY`。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，新增每 30 秒租户维度恢复调度。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccDirectoryControllerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，`subjectType=USER` 转 `Integer` 抛 `Unsupported int format: [USER]`。

GREEN: `mvn -pl yudao-module-dcc -am '-Dtest=DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，目录访问规则请求/响应统一为字符串主体类型。

RED: `python -X utf8 -m pytest script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q` -> FAIL，缺少历史数字主体类型归一 SQL。

GREEN: `python -X utf8 -m pytest script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q` -> PASS，新增幂等 SQL 归一 `1/2/3/4` 到 `USER/DEPT/ROLE/POSITION`。

GREEN: `mvn -f pom.xml -pl yudao-server -am -DskipTests package` -> PASS。

DEPLOY: 测试服部署 -> PASS，backend 镜像 `intruoyi-backend:20260527_nas_acl_e2e_fix5` 已加载并重启，健康检查 `UP`。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=test-write` -> PASS，`taskId=23`、`restoreId=4`、`directories=2`、`rules=47`。

RED: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> FAIL，`芋道源码/admin` 缺少启用的 DCC 类别 `其他`；该阶段不得修改芋道源码租户数据。

GREEN: 后端目标回归 -> PASS，相关 71 个测试通过。

REVIEW: 独立子 agent Aquinas 复核 -> CONDITIONAL GO for int_main，NO-GO for release；指出芋道源码/admin 只读阻塞、身份映射与 blocker 分支缺少真实 GREEN、恢复调度器 catch-and-continue 与 no-fallback 冲突。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，将恢复调度器异常行为改为 fail-fast 的测试先失败，证明当前实现吞掉异常。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPermissionRestoreExecutionSchedulerTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，恢复调度器移除 catch-and-continue，异常直接抛出。

GREEN: `mvn -f pom.xml -pl yudao-server -am -DskipTests package` -> PASS，fail-fast 调度器版本完成后端构建。

DEPLOY: 测试服 backend fail-fast 调度器部署 -> PASS，当前 backend 容器使用 image `sha256:2733dd99c069cda08c2de96ff06073cde175baea2a41c9314a4add0ab82cd17e`，frontend 容器使用 image `sha256:340b5076a00425bc5a4612ac94f5ea8d9546fe8932586b02e5963845013e4a0d`，健康检查 `UP`。

GREEN: 测试租户真实 E2E 复验 -> PASS，`PASS: test-write taskId=24, restoreId=5, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

RED: 芋道源码/admin 只读复验 -> FAIL，缺少启用的 DCC 类别 `其他`，验证阶段不得修改芋道源码租户数据。

GREEN: 后端目标回归复验 -> PASS，相关 71 个测试通过。

STATUS(历史): 后端 no-fallback 风险已消除；admin-readonly 仍为数据前置条件阻塞；当时身份映射保存与 blocker 分支需要专门测试数据，后续已由 `test-mapping` 与 `test-blocker` 复验关闭。

REVIEW: 独立放行报告 -> CONDITIONAL GO for int_main，NO-GO for release；后端目标回归、SQL 断言、调度 fail-fast 与测试租户真实恢复路径已通过，芋道源码/admin 数据前置条件和分支真实 E2E 仍阻塞发布放行。

CLEANUP: `task_closeout.py --mode preview` -> BLOCKED，delete 为 `<none>`；阻塞原因为任务未达到完成/可快进合并状态且当前 worktree 仍有待提交改动。

BDD: 禁用 SID 映射可重新激活 -> Given 同一租户已存在 `INACTIVE` 的 NAS SID 映射 / When 用户保存该 SID 到 DCC 主体 / Then 后端更新原映射并重新置为 `MAPPED`，不得插入重复 SID 行。

RED: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，新增 `saveMapping_reactivatesExistingInactiveSidInsteadOfInsertingDuplicate` 后，当前服务没有调用 `updateById`，会尝试插入重复 `tenant_id + sid_hash`。

GREEN: `mvn -pl yudao-module-dcc -am -Dtest=DccNasPrincipalMappingServiceTest '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，`saveMapping` 改为按 SID hash 查找任意状态旧映射；`MAPPED` 且目标冲突时 fail fast，`INACTIVE` 时更新原行。

GREEN: `mvn -f pom.xml -pl yudao-server -am -DskipTests package` -> PASS，包含禁用映射重新激活修复。

DEPLOY: 测试服 backend 映射修复部署 -> PASS，backend fix7 image `sha256:f07215e41d818e117835ac185b5a63c93e864882c7b18f190d42d0eac423e5c4` 已加载并运行，健康检查 `UP`。

GREEN: 测试租户身份映射真实 E2E -> PASS，`PASS: test-mapping taskId=36, restoreId=9, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`。

GREEN: 测试租户 blocker 真实 E2E -> PASS，前端克隆式 fixture 结果 `PASS: test-blocker taskId=34, unmapped=0, blockers=1`。

GREEN: 测试租户完整写路径最终复验 -> PASS，`PASS: test-write taskId=35, restoreId=8, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

RED: 芋道源码/admin 只读复验 -> FAIL，缺少启用的 DCC 类别 `其他`；验证阶段不得修改芋道源码租户数据。

GREEN: 后端目标回归最终复验 -> PASS，`mvn -pl yudao-module-dcc -am '-Dtest=DccNasPrincipalMappingServiceTest,DccControlledFileNasTransferServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccNasPermissionSchemaTest,DccNasPermissionSnapshotControllerTest,DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreExecutionSchedulerTest,DccNasPermissionRestoreControllerTest,DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` 共 80 个测试通过。

CLEANUP: `task_closeout.py --task-id 20260527-nas-permission-real-e2e-suite --mode preview` -> BLOCKED，delete 为 `<none>`；阻塞原因为当前任务仍受芋道源码/admin 数据前置条件限制，且 linked worktree 不能 fast-forward merge 到 `int_main`。

RED: 芋道源码/admin 最终只读复验 -> FAIL，`DCC file categories are missing active "其他"; transfer dialog must fail fast`；前端截图 `output/playwright/dcc-nas-permission-real-data-admin-readonly-1779892890843.png`；未修改芋道源码租户数据。

SUBAGENT: Pauli 前端只读复核 -> CONDITIONAL GO；指出 `test-mapping` 直接修改共享 `dcc_nas_acl_identity_mapping` 有失败后污染风险。主 reviewer 已在前端 E2E 中改为克隆当前任务 descriptor/ACE 并复验通过。

SUBAGENT: Archimedes 后端只读复核 -> CONDITIONAL GO；确认 `INACTIVE` SID 重新激活逻辑自洽，release 阻塞仍是芋道源码/admin 缺少启用的 `其他`。

GREEN: 测试租户身份映射真实 E2E 最终复验 -> PASS，前端克隆式 mapping fixture 结果 `PASS: test-mapping taskId=39, restoreId=10, directories=2, rules=47, unmapped=1, savedMappings=1, blockers=0`。

GREEN: 测试租户完整写路径最终复验 -> PASS，mapping fixture 后续 `PASS: test-write taskId=40, restoreId=11, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`，blocker fixture 后续 `PASS: test-write taskId=42, restoreId=12, directories=2, rules=47, unmapped=0, savedMappings=0, blockers=0`。

GREEN: 测试租户 blocker 真实 E2E 最终复验 -> PASS，`PASS: test-blocker taskId=41, unmapped=0, blockers=1`。

GREEN: 芋道源码/admin 数据前置条件解除 -> PASS，`doc/tasks/20260527-dcc-other-category-runtime-apply/` 已在测试服为 `tenant_id=1` 补齐唯一启用的 DCC 类别 `其他`，治理规则数量与 `产品技术要求` 对齐。

GREEN: 芋道源码/admin 只读 E2E -> PASS，`node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` 输出 `PASS: admin-readonly baseUrl=http://172.30.30.58:8081`，只读 guard 未发现转移、映射或恢复写请求。

GREEN: 最终后端回归 -> PASS，`node script\tests\dcc-other-template-sql.test.mjs`、`python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py script\tests\test_dcc_directory_access_rule_subject_type_contract_sql.py -q`、`mvn -pl yudao-module-dcc -am "-Dtest=DccNasPrincipalMappingServiceTest,DccControlledFileNasTransferServiceTest,DccNasPermissionSnapshotCaptureServiceImplTest,DccNasPermissionSchemaTest,DccNasPermissionSnapshotControllerTest,DccNasPermissionSnapshotQueryServiceImplTest,DccNasPermissionRestoreServiceTest,DccNasPermissionRestoreExecutionServiceTest,DccNasPermissionRestoreExecutionSchedulerTest,DccNasPermissionRestoreControllerTest,DccDirectoryControllerTest,DccDirectoryAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 均通过，Maven 共 80 个测试通过。
