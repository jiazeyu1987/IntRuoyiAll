# Execution Log：Smart Release Phase 1 实现与本地真实 E2E 门禁

## 2026-06-05

- BDD: Manifest v1 缺字段失败 -> Given 发布包包含 `manifest.json` 但缺少必填字段 / When 运行 manifest validator / Then 输出结构化 failed 结果和 errorCode，不修改构建或部署结果。
- BDD: 构建 intake 只读报告 schema/data/resource 变化 -> Given 本机仓库和本地只读数据库存在变化 / When 运行 release-intake report-only / Then 输出 change-set、schema-change-report、data-change-manifest、resource-reference-manifest、intake-result，不写数据库、不同步资源。
- BDD: 部署 precheck 按逻辑环境校验目标 -> Given 发布包 manifest 只允许 `test` / When 选择 `backup` 执行 deploy-precheck report-only / Then 输出 target mismatch 报告，不推断或写死服务器 IP，不执行远程写操作。
- BDD: 本地真实 E2E 门禁 -> Given 功能点涉及 Runtime Control 或用户路径 / When 执行验收 / Then 先用本机测试租户 `测试租户/aoteman` 调试，再用本机 `芋道源码/admin` 复核；失败返回测试租户修复，不连接外部服务器。

## 前置检查

- RED: `Invoke-WebRequest http://localhost:48080/admin-api/system/auth/get-permission-info` -> FAIL, 本地后端不可连接，真实租户 E2E 暂时阻塞。
- GREEN: `Invoke-WebRequest http://localhost:48081/actuator/health` -> PASS, 当前 worktree 后端端口为 `48081`。
- GREEN: `Invoke-WebRequest http://localhost:8081/login?redirect=/infra/monitors/runtime-control` -> PASS, 本地前端入口可访问。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS, 本机 `芋道源码/admin` Runtime Control 只读复核通过，未提交写操作。
- GREEN: `docker exec -e MYSQL_PWD=123456 int-ruoyi-mysql mysql -uroot -D ruoyi-vue-pro --batch --raw --skip-column-names -e "<readonly probes>"` -> PASS, 本机真实库只读探测可读：information_schema columns=7112，infra_file=12503，system_menu=1451。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py -q` -> PASS, 26 passed。
- RED: `run-release-intake.ps1` live mode smoke with wrong credential env names -> FAIL, `INTAKE_DB_CREDENTIAL_ENV_MISSING`，按配置 fail fast。
- GREEN: `run-release-intake.ps1` live mode smoke with `INTRUOYI_INTAKE_TEST_USER/PASSWORD` -> PASS, 本机真实库只读 report-only：status=warning，schemaDriftCount=465，resourceReferenceCount=12503，warnings 包含 `INTAKE_DATA_BASELINE_NOT_BOUND`。
- BDD: publish script report-only hook 不改变 legacy 行为 -> Given `-EnableSmartReleaseReport` 或 `INTRUOYI_SMART_RELEASE_REPORT_ONLY=1` / When build-release 或 deploy-release 执行到 report-only hook / Then 只写报告，不改变 legacy build/deploy 返回码，不提前访问 NAS/SSH/远程服务器。
- Review: M4 explorer 建议 build hook 位于 `Write-ReleaseManifest` 之后、`Copy-ReleasePackageToNas` 之前；deploy hook 位于 `Apply-ReleaseRuntimeEnvPackage` 之后、`Require-Command ssh/scp` 之前。M4 必须等待 M3 precheck 工具主审通过后再接入。

## M1：Manifest v1 schema 与 validator

- BDD: Manifest v1 validator 结构化校验 -> Given 发布包可能缺少 `manifest.json`、包含非法 JSON、缺少必填字段、声明 smart-release 但无 baseline、包含未声明/缺失/sha256 不匹配文件、硬编码目标 IP、密钥模式、legacy v0 manifest 或合法 v1 manifest / When 运行 `validate-release-manifest.ps1` / Then 输出 UTF-8 结构化 JSON，包含 `status`、`mode`、`errors`、`warnings`、`checks`、`code`、`scope`、`path`、`message`、`impact`、`nextStep`，且失败场景不伪装成功。
- RED: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q` -> FAIL, 12 failed；预期原因：`script/release/validate-release-manifest.ps1` 尚不存在，PowerShell 报告 `The argument ... validate-release-manifest.ps1 to the -File parameter does not exist`，validator 不能输出结构化 JSON。

## M2：build-release intake report-only

- BDD: build-release 本地 schema 手工变化被报告 -> Given baseline manifest 的 `system_menu` 无 `icon` 字段且本地 schema fingerprint 多出该字段 / When 运行 `run-release-intake.ps1 -Mode report-only` / Then `schema-change-report.json` 输出 `added-column`，`intake-result.json` 标记 `warning` 且不写数据库。
- BDD: build-release 必要数据变化进入分类报告 -> Given data ownership registry 拥有 `system_menu` / When fixture data changes 包含菜单权限变化 / Then `data-change-manifest.json` 输出 `required-data`。
- BDD: build-release 未归类数据变化不进入发布包 -> Given fixture data changes 包含未被 registry 覆盖的 `dcc_controlled_file` / When 运行 intake / Then 输出 `unclassified-local-change`。
- BDD: build-release 新增文件引用进入资源引用报告 -> Given fixture resource rows 包含 `infra_file.url` / When 运行 intake / Then `resource-reference-manifest.json` 输出 bucket、object key、domain 和 `resourcePreparedStatus=unknown`，不连接 MinIO。
- BDD: registry 无效失败 -> Given registry 缺少 `tenantScope` 且 owned/forbidden 字段重叠 / When 运行 intake / Then `intake-result.json` 结构化失败，code=`INTAKE_DATA_REGISTRY_INVALID`。
- BDD: 本地数据库配置缺失失败 -> Given 未传 `LocalDatabaseConfigPath` / When 运行 intake / Then `intake-result.json` 结构化失败，code=`INTAKE_DB_CONFIG_MISSING`，不伪造真实成功。
- RED: `python -X utf8 -m pytest script/tests/test_release_intake_report_only.py -q` -> FAIL, 6 failed；预期原因：`script/release/run-release-intake.ps1` 尚不存在，PowerShell `-File` 参数无法找到入口脚本。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_intake_report_only.py -q` -> PASS, 6 passed in 5.40s。

## M1 补充验证证据

- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q` -> PASS, 12 passed in 5.23s。
- RED: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q` -> FAIL, 15 failed / 1 passed；预期原因：主审退回后新增 `-Mode report-only`、`-OutputPath`、资源引用 `127.0.0.1:9000` 不阻断、目标 `172.30.30.58` 阻断和 schema 契约测试，当前 `validate-release-manifest.ps1` 不支持 `-Mode` 参数，PowerShell 报告 `A parameter cannot be found that matches parameter name 'Mode'`，因此 OutputPath 结构化结果也未生成。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q` -> PASS, 16 passed in 7.01s；已支持 `-PackagePath`、`-Mode report-only`、`-OutputPath`，OutputPath 写入结构化 JSON，stdout 输出摘要；资源引用 `127.0.0.1:9000` 不触发目标 IP blocker，`targetRequirements` / `deployContract` 写入 `172.30.30.58` 会失败。
- RED: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q` -> FAIL, 1 failed / 16 passed；预期原因：新增脚本/模块文本断言后发现 `ReleaseManifestValidator.psm1` 内仍硬编码 `172.30.30.57`、`172.30.30.58`、`172.30.30.59` forbidden list，违反目标 IP 不写死的长期要求。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py -q` -> PASS, 17 passed in 7.99s；已移除 validator 脚本/模块中的固定服务器 IP 字符串，`targetRequirements` / `deployContract` 继续用通用 IPv4 regex 拒绝具体 IP，resource-reference 文件中的本机/存储 profile 域名不触发目标 IP blocker。

## M2 主审退回修复：本机 Docker MySQL 只读 live mode

- BDD: explicit snapshot mode 保持可用 -> Given 显式传入 `LocalSchemaFingerprintPath`、`LocalDataChangeRowsPath`、`ResourceRowsPath` / When 运行 intake / Then 使用 snapshot 文件输出 schema/data/resource 报告，原 6 个测试保持通过。
- BDD: local readonly docker mysql mode 缺容器配置失败 -> Given 未传 snapshot 且 `LocalDatabaseConfigPath` 缺少 `dockerContainer` / When 运行 intake / Then `intake-result.json` 结构化失败，code=`INTAKE_DB_DOCKER_CONTAINER_MISSING`。
- BDD: local readonly docker mysql mode 缺凭据环境变量失败 -> Given `usernameEnv` 或 `passwordEnv` 未设置 / When 运行 intake / Then `intake-result.json` 结构化失败，code=`INTAKE_DB_CREDENTIAL_ENV_MISSING`，不连接 Docker/MySQL。
- BDD: local readonly docker mysql mode 只读采集真实结构 -> Given 未传 snapshot 且本机 Docker MySQL 配置完整 / When 运行 intake / Then 调用 `information_schema.tables/columns/statistics/views` 和 `infra_file` SELECT，不出现 DROP/INSERT/UPDATE/DELETE，不访问 ssh/scp/172.30.30.*，输出 `local-schema-fingerprint.json`、`data-change-manifest.json`、`resource-reference-manifest.json`。
- BDD: live data baseline 未绑定不伪造无变化 -> Given registry 已校验但 Phase 1 尚无 baseline data snapshot / When live mode 运行 / Then `data-change-manifest.json` 输出 registry coverage summary、`changes=[]`、`liveDataChangeMode=not-yet-bound-to-baseline`，`intake-result.json` 输出 warning `INTAKE_DATA_BASELINE_NOT_BOUND`。
- RED: `python -X utf8 -m pytest script/tests/test_release_intake_report_only.py -q` -> FAIL, 3 failed / 6 passed；预期原因：主审退回后新增 live mode 测试，当前实现仍要求 snapshot，且入口不支持 `-DockerCliPath`。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_intake_report_only.py -q` -> PASS, 9 passed in 6.81s。
- GREEN: 本机真实 smoke `powershell.exe -NoProfile -ExecutionPolicy Bypass -File script\release\run-release-intake.ps1 -RepoRoot . -BaselineManifestPath script\tests\fixtures\release-intake\baseline-manifest.json -LocalDatabaseConfigPath script\tests\fixtures\release-intake\db-config.live.json -DataOwnershipRegistryPath script\tests\fixtures\release-intake\data-ownership-registry.valid.json -OutputDir <temp>\intake -Mode report-only`，环境变量 `INTRUOYI_INTAKE_TEST_USER=root`、`INTRUOYI_INTAKE_TEST_PASSWORD=123456` -> PASS, exit=0, status=warning, schemaDriftCount=465, resourceReferenceCount=12503, warnings=1；只读 Docker MySQL SELECT，未写数据库，未连接 MinIO，未访问外部服务器。

## M3：deploy-release precheck report-only

- BDD: deploy-precheck 成功报告不改变部署行为 -> Given Manifest v1 发布包、匹配的逻辑环境和服务器侧 target config / When 运行 `run-deploy-precheck-report.ps1 -Mode report-only` / Then 输出 `deploy-precheck-result.json`，包含 `changesDeployExitCode=false`，不连接远程、不执行 SQL、不同步 MinIO。
- BDD: deploy-precheck 目标配置失败结构化报告 -> Given target config 缺失、JSON 非法、schemaVersion 不支持或 environmentCode 与命令环境不一致 / When 运行 deploy-precheck report-only / Then 输出对应 `DEPLOY_TARGET_*` failed error，不推断默认目标，不写死服务器地址。
- BDD: deploy-precheck manifest 与目标能力不匹配结构化报告 -> Given manifest 目标环境、docker profile、storage profile 或 artifact cache profile 与 target config 不兼容 / When 运行 deploy-precheck report-only / Then 输出对应 failed error，只报告风险。
- BDD: deploy-precheck artifact 与 resource gate 本地校验 -> Given artifact 缺失、sha256 不匹配、cache profile 不可用或 resource delta proof 未验证 / When 运行 deploy-precheck report-only / Then 输出对应 failed error，不读取远端对象，不同步资源。
- BDD: deploy-precheck legacy 与禁止项扫描 -> Given legacy 包或 manifest 的 `targetRequirements` / `deployContract` 包含具体目标 IP / When 运行 deploy-precheck report-only / Then legacy 只输出 warning，硬编码目标 IP 输出 failed error；脚本、模块和 fixture 不包含外部服务器写操作命令或固定服务器 IP。
- RED: `python -X utf8 -m pytest script/tests/test_release_deploy_precheck_report_only.py -q` -> FAIL, 18 failed；预期原因：`script/release/run-deploy-precheck-report.ps1` 与 `script/release/templates/deploy-target-config.schema.json` 尚不存在，PowerShell `-File` 参数无法找到入口脚本，precheck 不能写出 `deploy-precheck-result.json`。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_deploy_precheck_report_only.py -q` -> PASS, 18 passed in 11.41s；已覆盖 target config 缺失/非法 JSON/schema 不支持/环境不合法/环境不匹配、manifest 目标不匹配、storage/docker/cache/artifact/resource gate、legacy warning、硬编码目标 IP 失败，并断言脚本、模块和 fixture 不包含外部服务器写操作命令或固定服务器 IP。

## M5：Runtime Control Smart Release report-only 接入

- BDD: 运行控制台构建命令显式传递 Smart Release report-only -> Given 测试租户在运行控制台选择构建发布包并启用 Smart Release report-only / When 后端构造 build-release 命令 / Then 参数包含 `-EnableSmartReleaseReport`、基础镜像参数和来自服务端配置的 `-TestServerHost/-ProdServerHost/-BackupServerHost`，不写死目标 IP。
- BDD: 运行控制台部署预览不触发远程动作 -> Given 测试租户在运行控制台选择部署发布包到测试服并启用 Smart Release report-only / When 点击命令预览 / Then 真实后端预览接口返回 `-EnableSmartReleaseReport`、`-ServerHost` 和目标 host 参数，且不提交 `/infra/runtime-control/actions` 执行动作、不触发 SSH/SCP/真实发布。
- BDD: 芋道源码管理员只读复核 -> Given `芋道源码/admin` 登录本机运行控制台 / When 查看同一入口和配置 / Then Runtime Control 只读信息可见，且不调用写 runtime-control 端点。
- RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> FAIL, testCompile 失败；预期原因：`RuntimeControlActionReqVO` 尚无 `enableSmartReleaseReport` 字段，`RuntimeControlServiceImpl` 尚无 `previewAction` 接口。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 49 tests；新增覆盖 build-release 参数拼接、Smart Release report-only 标识、基础镜像参数、服务端配置 host、publish-test 预览接口不调用执行器。
- RED: `node yudao-ui-admin-vue3/tests/e2e/runtime-control-smart-release-report-only.e2e.js` -> FAIL, 本机旧后端返回 `No static resource admin-api/infra/runtime-control/actions/preview`；原因是 48081 仍运行旧 jar，未加载新增预览接口。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS；本机重新启动 48081 后端到新 `yudao-server.jar`，`/actuator/health` 返回 `UP`。
- GREEN: `node yudao-ui-admin-vue3/tests/e2e/runtime-control-smart-release-report-only.e2e.js` -> PASS；测试租户 `测试租户/aoteman/admin123` 真实登录，构建和部署弹窗提供 report-only 开关，预览接口返回 `-EnableSmartReleaseReport`，未提交执行动作。
- GREEN: `node yudao-ui-admin-vue3/tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS；`芋道源码/admin/admin123` 只读复核通过，ownerRows=21、rollbackCandidates=26、restoreCandidates=10、probes=12、capacityStatus=WARN、incidents=0，未调用写 runtime-control 端点。

## M5 主审退回修复：Smart Release report-only 不默认启用

- BDD: Smart Release report-only 必须显式启用 -> Given 测试租户打开构建或部署弹窗 / When 未手动勾选 Smart Release report-only / Then `enableSmartReleaseReport` 保持 false，不改变旧构建/部署默认行为；When 手动勾选并点击预览 / Then 真实预览接口返回 `-EnableSmartReleaseReport`，且不提交 `/infra/runtime-control/actions` 执行动作。
- RED: `node yudao-ui-admin-vue3/tests/e2e/runtime-control-smart-release-report-only.e2e.js` -> FAIL, 预期原因：主审退回后 E2E 改为断言默认未勾选，当前前端打开构建弹窗时 `.el-checkbox` 仍包含 `is-checked`。
- GREEN: `node yudao-ui-admin-vue3/tests/e2e/runtime-control-smart-release-report-only.e2e.js` -> PASS；构建/部署弹窗默认未勾选 Smart Release report-only，E2E 手动勾选后预览返回 `-EnableSmartReleaseReport`，未提交执行动作。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 49 tests。
- GREEN: `node yudao-ui-admin-vue3/tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS；`芋道源码/admin/admin123` 只读复核通过，未调用写 runtime-control 端点。

## M4：发布脚本 Smart Release report-only 接入

- BDD: build-release 可选 report-only 生成包内报告 -> Given `-EnableSmartReleaseReport` 或 `INTRUOYI_SMART_RELEASE_REPORT_ONLY=1` 且传入 baseline、local database config、data ownership registry / When `build-release` 写出 `release-manifest.json` / Then 在发布包 `smart-release-report` 下运行 manifest validation 与 release intake，写出 `manifest-validation-result.json` 和 `intake/intake-result.json`，不访问 ssh/scp，不执行 docker pull 或 apt-get。
- BDD: legacy 发布流程默认不接入 Smart Release -> Given 未传 `-EnableSmartReleaseReport` 且未设置 `INTRUOYI_SMART_RELEASE_REPORT_ONLY=1` / When 执行原 `direct`、`build-release`、`deploy-release` 或 `mark-tested` 流程 / Then 不调用 Smart Release report-only 脚本，不改变 Mode 枚举。
- BDD: deploy-release report-only 本地 precheck 后停止真实部署 -> Given `deploy-release` 已定位本地发布包且传入 `SmartReleaseTargetConfigPath` / When Smart Release report-only 开启 / Then 在进入 ssh/scp、远程 docker 或远程 SQL 前运行 `run-deploy-precheck-report.ps1 -Mode report-only`，报告写入包内 `smart-release-report/deploy-precheck-result.json`；precheck 成功也返回 `SMART_RELEASE_REPORT_ONLY_DEPLOY_STOP`，不伪装部署成功。
- RED: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, 6 failed / 63 passed；预期原因：`publish-int-ruoyi.ps1` 尚无 Smart Release report-only 开关、环境变量、build/deploy hook、稳定报告路径和 deploy precheck 停止逻辑。
- GREEN: `python -X utf8 -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 69 passed in 2.54s；已断言开关/环境变量、Mode 枚举不变、build hook 位于 manifest 之后 NAS 上传之前、deploy hook 位于 ssh/scp 前、报告脚本传参清晰且不包含 docker pull/apt-get fallback。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py script/tests/test_release_deploy_precheck_report_only.py -q` -> PASS, 44 passed in 29.66s。
- GREEN: PowerShell AST parse + `Get-Command script\deploy\publish-int-ruoyi.ps1` parameter check -> PASS, `publish-int-ruoyi.ps1 syntax and parameters OK`。

## 主审补充：release-intake 禁止远端引用不绑定公司网段

- BDD: live intake 远端引用禁止项使用通用规则 -> Given release-intake live mode 只允许本机 Docker MySQL 只读查询 / When 检查 SQL 和 Docker 命令文本 / Then 禁止 ssh/scp 与任意 IPv4 字面量，不在实现里写死 `172.30.30.*` 公司网段。
- RED: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, 1 failed / 113 passed；预期原因：新增 guard 测试先失败，`test_release_intake_live_mode_uses_generic_remote_reference_guard` 最初插入位置错误导致 `ROOT` / `schema_fingerprint` / `output_dir` 未定义。
- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 114 passed in 24.00s；`script/release` 静态扫描未发现 `172.30.30.57/58/59` 或 `172.30.30.`。

## 主线程最终验证

- GREEN: `python -X utf8 -m pytest script/tests/test_release_manifest_validator.py script/tests/test_release_intake_report_only.py script/tests/test_release_deploy_precheck_report_only.py script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 114 passed。
- GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS, 49 tests。
- GREEN: `node yudao-ui-admin-vue3\tests\e2e\runtime-control-smart-release-report-only.e2e.js`，环境 `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081`、`RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://127.0.0.1:48081`、`测试租户/aoteman/admin123` -> PASS；构建/部署弹窗默认未勾选，手动勾选后预览返回 `-EnableSmartReleaseReport`，未提交执行动作。
- GREEN: `node yudao-ui-admin-vue3\tests\e2e\runtime-control-yudao-admin-readonly.e2e.js`，环境 `RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081`、`芋道源码/admin/admin123` -> PASS；只读复核通过，未调用写 runtime-control 端点。
