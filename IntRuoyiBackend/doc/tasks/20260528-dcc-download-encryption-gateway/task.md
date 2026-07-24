# 任务：DCC 受控下载真实加密网关

## 任务目标

- 为 DCC 受控下载接入真实 AES-GCM 加密网关，下载接口只返回加密产物，不读取或返回明文。
- 加密配置、密钥、源文件、加密证据或审计 READY 更新缺失时必须失败关闭，不得 fallback、不得默认成功。
- 补齐后端 TDD 证据，作为根任务 `20260528-dcc-release-gate-fill` 的 RG-01/RG-06 工程实现输入。

## 前置任务状态

- 上一同仓后端任务 `20260528-nas-transfer-large-task-resume-performance` 状态为 `completed`。
- 当前 worktree 后端仓库在本任务开始前为干净状态。

## BDD 场景

- BDD: 受控下载生成真实加密产物 -> Given 已发布 DCC 文件、有效下载审计记录和启用的下载加密配置 / When 用户发起受控下载 / Then 网关必须读取源文件、生成 AES-GCM 加密信封、持久化密文文件并返回满足下载加密合同的密文证据。
- BDD: 加密配置缺失或无效失败关闭 -> Given 下载加密配置被显式启用但缺少策略、密钥、密钥编号或密钥长度无效 / When 网关初始化或执行加密 / Then 必须抛出明确异常，不得创建密文产物或返回默认成功。
- BDD: 审计 READY 更新失败不返回密文 -> Given 加密产物已生成但下载记录 READY 更新返回 0 行 / When 下载服务准备返回文件 / Then 服务必须记录失败访问、标记失败并抛出审计失败异常，不得把密文字节返回给用户。
- BDD: 成功审计日志失败不留下已返回证据 -> Given 加密产物已生成且 READY 更新已执行 / When ALLOWED 下载访问日志写入失败 / Then READY 更新必须回滚，失败记录必须清空 `artifact_id`、`cipher_file_ref`、hash 和 `returned_at`，不得返回密文。

## 里程碑

- [x] M1：创建任务文档，确认上一任务状态和当前 worktree 状态。
- [x] M2：写入 RED 测试，覆盖真实网关与审计更新失败。
- [x] M3：实现 AES-GCM 下载加密网关、配置门禁和审计更新失败关闭。
- [x] M4：运行目标后端测试与合同验证。
- [x] M5：更新根任务补齐报告，完成 review 门禁和提交。

## 预期验证

- RED/GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest,DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccDownloadEncryptionContractValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260528-dcc-download-encryption-gateway/backend-api-evidence.md`

## 当前状态

- 状态：completed
- 当前阶段：后端实现、review 门禁、运行配置和本地全量 E2E 验证已完成；按用户 2026-05-30 指示，本轮不发布到服务器。

## 当前验证

- `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest#springContextWiresGatewayThroughProductionConstructor" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> RED FAIL then GREEN PASS, fixed Spring constructor wiring regression.
- `mvn -pl yudao-module-dcc -am "-Dtest=DccAesGcmDownloadEncryptionGatewayTest,DccControlledFileQueryServiceTest,DccDownloadEncryptionContractValidatorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 42 tests passed.
- `powershell -ExecutionPolicy Bypass -File .\script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS.
- `powershell -ExecutionPolicy Bypass -File .\script\tests\test_restart_ruoyi_script_onlyoffice.ps1` -> PASS.
- `node tests\e2e\dcc-controlled-file-protection.contract.test.js` -> PASS.
- `node --check tests\e2e\dcc-controlled-file-protection.e2e.js` -> PASS.
- `node tests\e2e\dcc-controlled-file-protection.e2e.js; Write-Output "EXIT:$LASTEXITCODE"` -> EXIT:2, expected BLOCKED because real test tenant, runtime env and E2E input prerequisites are missing.
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260528-dcc-download-encryption-gateway/backend-api-evidence.md` -> PASS.
- `mvn -pl yudao-server -am -DskipTests package` -> PASS.
- 测试服 `172.30.30.58` backend-only deploy -> PASS；`http://172.30.30.58:48081/actuator/health` -> `{"status":"UP"}`；`DCC_DOWNLOAD_ENCRYPTION_*` 已配置到测试服 `.env` 和 compose，密钥未写入 Git。
- Popper reviewer final result -> PASS, current blocker closed; nonblocking residual risk is real database/E2E failure-path verification before production release.
- 2026-05-30 int_main 融合验证 -> PASS：统一发布脚本/SQL/eDHR 契约 136 PASS，下载加密运行时 PowerShell 配置测试 PASS，DCC 目标 Maven 209 PASS，infra 文件门禁和 eDHR 存储留存 48 PASS/6 skipped，server 上传限制 2 PASS，MES eDHR 归档/追溯 94 PASS。
- 2026-05-30 本地验证 -> PASS：Docker 本地 MySQL/Redis/MinIO/OnlyOffice 启动，主仓后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`，主前端 `http://127.0.0.1:8081/` 返回 200；本地测试租户补齐 `CATEGORY_PURPOSE/906101/SOURCE/20000` 上传大小策略；`node tests\e2e\dcc-controlled-file-protection.e2e.js` 在本地 `8081 -> 48081` 完整通过 TC-E2E-001 至 TC-E2E-017。

## Cleanup Keep

- `doc/tasks/20260528-dcc-download-encryption-gateway/verification-report.md`
