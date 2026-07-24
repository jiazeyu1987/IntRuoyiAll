# 执行日志：DCC 受控下载真实加密网关

## 2026-05-28 启动

- INFO: 上一同仓后端任务 `20260528-nas-transfer-large-task-resume-performance` 为 `completed`。
- INFO: 后端仓库开始前 `git status --short --branch` 显示 `## codex/20260528-dcc-protection-implementation`，无未提交文件。
- BDD: 受控下载生成真实加密产物 -> Given 已发布 DCC 文件、有效下载审计记录和启用的下载加密配置 / When 用户发起受控下载 / Then 网关必须读取源文件、生成 AES-GCM 加密信封、持久化密文文件并返回满足下载加密合同的密文证据。
- BDD: 加密配置缺失或无效失败关闭 -> Given 下载加密配置被显式启用但缺少策略、密钥、密钥编号或密钥长度无效 / When 网关初始化或执行加密 / Then 必须抛出明确异常，不得创建密文产物或返回默认成功。
- BDD: 审计 READY 更新失败不返回密文 -> Given 加密产物已生成但下载记录 READY 更新返回 0 行 / When 下载服务准备返回文件 / Then 服务必须记录失败访问、标记失败并抛出审计失败异常，不得把密文字节返回给用户。
- BDD: 成功审计日志失败不留下已返回证据 -> Given 加密产物已生成且 READY 更新已执行 / When ALLOWED 下载访问日志写入失败 / Then READY 更新必须回滚，失败记录必须清空 `artifact_id`、`cipher_file_ref`、hash 和 `returned_at`，不得返回密文。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `DccAesGcmDownloadEncryptionGateway`、`DccDownloadEncryptionProperties` 和 `DCC_DOWNLOAD_ENCRYPTION_CONFIG_MISSING` 尚未实现，新增 RED 测试无法编译。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 38 tests passed；覆盖真实 AES-GCM 加密、同一明文两次密文不同、解密回原文、返回体不包含明文片段、密文 artifact 通过 `FileService.createFileAndReturnId` 落库、配置缺失/非法 fail-fast、READY 审计更新 0 行时抛 `DCC_DOWNLOAD_AUDIT_RECORD_FAILED` 且不返回密文。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccDownloadEncryptionContractValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests passed；下载加密结果合同校验仍通过。
- REVIEW: Popper first review -> BLOCKED, allowed access log insert=0 已不返回密文，但 READY/returnedAt/artifactId/cipherFileRef/hash 可能在无事务情况下残留；旧 `script/docker` 路径也必须显式要求 `DCC_DOWNLOAD_ENCRYPTION_*`。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#readDownloadFile_failsClosedWhenAllowedAccessLogInsertReturnsZero" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `recordDownloadAccess` 未检查 ALLOWED 下载日志 insert 行数，审计失败可能返回密文。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#readDownloadFile_failsClosedWhenAllowedAccessLogInsertReturnsZero" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test passed；ALLOWED 下载日志 insert=0 时抛 `DCC_DOWNLOAD_AUDIT_RECORD_FAILED`。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#readDownloadFile_failsClosedWhenAllowedAccessLogInsertReturnsZero" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 失败记录清空 READY 证据的测试先暴露 `LambdaUpdateWrapper` 在纯 Mockito 单测中缺少 lambda cache，改用项目既有 `UpdateWrapper` 模式。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#readDownloadFile_failsClosedWhenAllowedAccessLogInsertReturnsZero" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test passed；验证 `TransactionTemplate` rollback 被调用且失败 update wrapper 清空可返回证据字段。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest,DccControlledFileQueryServiceTest,DccDownloadEncryptionContractValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 41 tests passed；覆盖 AES-GCM、配置 fail-fast、READY 更新失败、ALLOWED 日志失败事务回滚、失败记录清空返回证据和合同校验。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS；本地重启、测试服发布脚本、测试服 compose、dev/local YAML 均要求 `DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION`、`DCC_DOWNLOAD_ENCRYPTION_KEY_ID`、`DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY`、`DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY` 且 YAML 无默认密钥值。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS；补充覆盖旧 `script/docker/docker-compose.yml`、`docker.env` 和 `Docker-HOWTO.md` 均显式要求下载加密环境变量且无默认值。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS；本地状态脚本仍暴露 OnlyOffice health，未被本次环境门禁破坏。
- GREEN: `node tests\e2e\dcc-controlled-file-protection.contract.test.js` -> PASS；前端上传 purpose 合同未被破坏。
- GREEN: `node --check tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS；真实 E2E 脚本语法有效。
- BLOCKED: `node tests\e2e\dcc-controlled-file-protection.e2e.js; Write-Output "EXIT:$LASTEXITCODE"` -> EXIT:2, expected reason: 真实测试租户、`DCC_E2E_*` 环境输入、OnlyOffice、截图追溯验收标准和真实 DCC/非 DCC 样本仍缺失；未使用 mock 或 API 快捷路径伪造成功。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260528-dcc-download-encryption-gateway/backend-api-evidence.md` -> PASS, backend API evidence is valid.
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS, server package completed and produced `yudao-server/target/yudao-server.jar`.
- GREEN: `git diff --check` -> PASS，无 whitespace error；仅 CRLF 工作区提示。
- REVIEW: Popper final review -> PASS, READY 残留 blocker 已关闭；建议发布前仍用真实数据库/E2E 再跑一次下载失败审计路径。
- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-dcc-download-encryption-gateway --mode preview` -> BLOCKED, expected reason: cleanup 脚本只归属 task docs，当前代码/脚本改动被识别为非 cleanup pending changes；主 worktree `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 非干净且当前分支不能 ff-only 合入 `int_main`。本轮不执行自动 cleanup/merge/remove worktree。

## 2026-05-28 测试服配置与启动回归

- BDD: Spring 生产构造器必须可装配 -> Given 测试服已配置 `DCC_DOWNLOAD_ENCRYPTION_*` 并加载 DCC 模块 / When Spring 创建 `DccAesGcmDownloadEncryptionGateway` / Then 必须选择生产构造器注入 `FileService`、`DccDownloadEncryptionProperties`、`ObjectMapper`，不得尝试不存在的无参构造器。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest#springContextWiresGatewayThroughProductionConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: Spring 报 `No default constructor found`，复现测试服 backend 重启循环根因。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest#springContextWiresGatewayThroughProductionConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test passed；生产构造器显式 `@Autowired` 后 Spring context 可创建网关 bean。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest,DccControlledFileQueryServiceTest,DccDownloadEncryptionContractValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 42 tests passed。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS。
- CONFIG: 测试服 `172.30.30.58` `/opt/intruoyi/runtime/.env` 已写入测试专用 `DCC_DOWNLOAD_ENCRYPTION_POLICY_VERSION`、`DCC_DOWNLOAD_ENCRYPTION_KEY_ID`、`DCC_DOWNLOAD_ENCRYPTION_BASE64_KEY`、`DCC_DOWNLOAD_ENCRYPTION_ARTIFACT_DIRECTORY`；密钥值只在测试服 `.env` 中保存，未写入 Git、任务文档或日志。
- CONFIG: 已复制当前 `script/deploy/int-ruoyi-test/docker-compose.yml` 到测试服 runtime，backend 启动参数包含四项 `yudao.dcc.download.encryption.*`。
- GREEN: backend-only 镜像部署并重建测试服 `intruoyi-backend` -> PASS；健康检查 `curl.exe -s http://172.30.30.58:48081/actuator/health` 返回 `{"status":"UP"}`。
- GREEN: 测试库执行 `sql/mysql/20260527_dcc_nas_acl_snapshot_restore.sql` -> PASS；补齐 8 张 NAS ACL snapshot/restore 表，最近日志不再出现 `dcc_nas_acl_restore_plan` 缺表错误。
- GREEN: `powershell -ExecutionPolicy Bypass -File .\script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS。
- BLOCKED: `node tests\e2e\dcc-controlled-file-protection.e2e.js` with test env/account and `DCC_E2E_RG01_ENCRYPTION_READY=true` -> EXIT:2, expected reason: 真实 DCC 页面路径、选择器、样本文件、审计校验 URL、OnlyOffice、水印追溯验收标准、审计/普通账号权限等 `DCC_E2E_*` 前置仍未齐备；未用 mock 或接口捷径伪造通过。

## 2026-05-30 int_main 融合验证

- BDD: DCC 保护融合后保留统一发布入口 -> Given `int_main` 已删除旧 test/prod/promote 发布脚本并使用统一 `publish-int-ruoyi.ps1` / When DCC OnlyOffice、viewer token、下载加密和 PDF worker 运行时门禁合入 / Then 统一发布脚本必须 fail-fast 要求所有 DCC 运行时变量、打包 OnlyOffice 镜像、启动并健康检查 OnlyOffice，且不得复活旧发布入口。
- BDD: DCC 错误码融合后不覆盖签名治理和目录删除错误码 -> Given `int_main` 已占用 `1_080_000_094` 到 `1_080_000_107` / When DCC upload、viewer-token、download-encryption 错误码合入 / Then DCC 新错误码必须迁移到未占用号段并由契约测试覆盖。
- BDD: 最新 int_main eDHR 发布门禁不能复活旧发布入口 -> Given 最新 `int_main` 引入 eDHR 受保护存储发布门禁 / When 与 DCC 单入口发布策略融合 / Then eDHR `EDHR_S3_*` fail-fast、verifier、post-import SQL 和运行时环境必须迁入统一 `publish-int-ruoyi.ps1`，旧 test/promote/direct 发布入口不得存在。
- GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_dcc_sql_scripts.py script\tests\test_dcc_controlled_file_protection_sql.py -q` -> PASS, 41 tests passed；统一发布脚本、DCC SQL、OnlyOffice/PDF worker 和审计菜单 SQL 契约通过。
- GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS；下载加密运行时配置测试已迁移到统一发布脚本。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest,DccUploadTicketServiceTest,DccUploadSizePolicyServiceTest,DccUploadTemporaryFileCleanupSchedulerTest,DccControlledFileAccessAuditServiceTest,DccControlledFileAuditQueryServiceTest,DccControlledFilePreviewDownloadApiTest,DccControlledFileQueryServiceTest,DccOnlyOfficeControlledPreviewTest,DccControlledFilePreviewProtectionTest,DccFileDirectLinkAccessGuardTest,DccControlledFileWorkflowServiceImplTest,DccTrainingTaskServiceTest,DccViewerTokenErrorCodeContractTest,DccDownloadPolicyServiceTest,DccAesGcmDownloadEncryptionGatewayTest,DccDownloadEncryptionContractValidatorTest,DccProtectionSchemaTest,DccUploadSizePolicyControllerTest,DccControlledFileAuditControllerTest,DccControlledPreviewAccessServiceTest,DccControlledFileFinalizationServiceImplTest,DccExternalFileReviewServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 209 tests passed。
- GREEN: `mvn -pl yudao-module-infra -am "-Dtest=FileControllerTest,FileServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 29 tests passed。
- GREEN: `mvn -pl yudao-server "-Dtest=UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests passed。
- RED: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_dcc_sql_scripts.py script\tests\test_dcc_controlled_file_protection_sql.py script\tests\test_edhr_archive_business_health_contract.py script\tests\test_edhr_archive_sql.py script\tests\test_edhr_domain_trace_schema_sql.py script\tests\test_edhr_protected_storage_publish_tooling.py script\tests\test_edhr_release_ops_acceptance_contract.py script\tests\test_edhr_storage_retention_contract.py tool\tests\test_edhr_storage_retention_verifier.py -q` -> FAIL, expected reason: 最新 `int_main` 重新引入旧 test/promote 发布入口，且 eDHR 发布合同仍指向缺失的 direct-to-prod wrapper，存在绕过统一 DCC 发布门禁的风险。
- GREEN: `python -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_dcc_sql_scripts.py script\tests\test_dcc_controlled_file_protection_sql.py script\tests\test_edhr_archive_business_health_contract.py script\tests\test_edhr_archive_sql.py script\tests\test_edhr_domain_trace_schema_sql.py script\tests\test_edhr_protected_storage_publish_tooling.py script\tests\test_edhr_release_ops_acceptance_contract.py script\tests\test_edhr_storage_retention_contract.py tool\tests\test_edhr_storage_retention_verifier.py -q` -> PASS, 136 tests passed；eDHR protected storage 发布门禁已迁入统一发布脚本，旧发布入口被移除。
- GREEN: `mvn -pl yudao-module-infra -am "-Dtest=FileControllerTest,FileServiceImplTest,S3FileClientTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 48 tests run, 6 skipped；`FileService` 同时保留 DCC 直链门禁和 eDHR 存储留存接口。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesBatchRecordBaseSchemaTest,MesProBatchRecordDomainTraceControllerTest,MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordExecutionArchiveBusinessHealthCollectorTest,ExecutionArchiveRendererTest,MesProBatchRecordExecutionArchiveContractTest,MesProBatchRecordExecutionArchiveServiceImplTest,MesProBatchRecordExecutionServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 94 tests passed；最新 `int_main` eDHR 归档/追溯业务回归通过。

## 2026-05-30 本地验证门禁

- BDD: 本地 DCC 保护闭环必须覆盖泄密与丢失风险 -> Given 用户要求不发布到服务器且只做本地验证 / When 本地 Docker MySQL、Redis、MinIO、OnlyOffice、主仓后端 `48081` 和主前端 `8081` 启动 / Then 受控直链阻断、OnlyOffice 只读、上传大小策略、临时文件清理、水印追溯、下载加密、审计证据和非 DCC 回归必须通过真实浏览器 E2E。
- CONFIG: 按用户要求跳过服务器发布；停止旧 SSH 隧道后，后端改连本地 Docker `127.0.0.1:23306/26379`，未把测试服作为验证依赖。
- CONFIG: 本地执行 `sql/mysql/20260528_dcc_controlled_file_protection.sql`、`sql/mysql/20260529_dcc_audit_menu_permission.sql` -> PASS；补齐本地库 DCC 保护表、审计字段和审计菜单权限。
- CONFIG: 本地测试租户 `122` 插入上传大小策略 `CODEX_E2E_SYSTEM_SOURCE_LOCAL_20260530`，scope=`CATEGORY_PURPOSE`，category=`906101`，purpose=`SOURCE`，maxBytes=`20000`；用于让 16KB 样本通过、46KB 样本触发超限，不影响生产配置。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`；`Invoke-WebRequest http://127.0.0.1:8081/` -> 200。
- GREEN: local MinIO Object Lock verifier -> PASS；本地 bucket `edhr-local-verifier-20260530` 生成 versionId，COMPLIANCE retention 与 legal hold ON，删除被拒绝，可按版本读取。
- RED: first local full `node tests\e2e\dcc-controlled-file-protection.e2e.js` -> FAIL at `TC-E2E-007`, expected reason: 本地测试租户缺少 `906101/SOURCE` 上传大小策略，超限路径只能产生 `DCC_UPLOAD_SIZE_POLICY_MISSING`，审计 API 查不到 `DCC_UPLOAD_SIZE_EXCEEDED`。
- GREEN: selected local `DCC_E2E_CASES=TC-E2E-007..TC-E2E-017` -> TC-E2E-007 至 TC-E2E-016 PASS；TC-E2E-017 因只运行后半段而按设计失败，证明汇总用例要求同轮完整执行。
- GREEN: full local `node tests\e2e\dcc-controlled-file-protection.e2e.js` with `DCC_E2E_BASE_URL=http://127.0.0.1:8081` and `DCC_E2E_API_BASE_URL=http://127.0.0.1:48081` -> PASS，TC-E2E-001 至 TC-E2E-017 全部通过；证据 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\test-results\dcc-controlled-file-protection\summary.json`。
- REVIEW: independent verification gate -> PASS for local scope；服务器发布不在本轮范围内。
