# 任务：电子签名治理增强代码实施

## 目标

在当前独立 worktree `codex/20260528-signature-governance-docs` 中，按已放行文档实现电子签名治理增强代码。主 agent 作为 reviewer，使用多个子 agent 分工开发，并持续 review / 修复 / 复测，直到所有子 agent 的工作完全符合文档要求。

本任务必须覆盖四个功能点：

- 长期防篡改留存证据：MinIO Object Lock / WORM、归档保留策略、恢复演练证明签名证据和归档长期可恢复。
- 签名周期性审阅：签名权限、锁定、失败记录、异常签名证据由质量人员定期复核页面或报表。
- CSV/质量体系材料：URS/FRS、风险评估、IQ/OQ/PQ、追溯矩阵、电子签名 SOP、培训记录、变更控制。
- 跨模块统一策略：DCC、eDHR、Showroom、IntAuth 的签名链路由统一策略源约束，避免规则漂移。

## 输入文档

- `doc/tasks/20260528-signature-governance-docs/docs/system/retention-recovery-design.md`
- `doc/tasks/20260528-signature-governance-docs/docs/system/signature-periodic-review-design.md`
- `doc/tasks/20260528-signature-governance-docs/docs/quality/csv-quality-system-package.md`
- `doc/tasks/20260528-signature-governance-docs/docs/system/cross-module-signature-policy.md`
- `doc/tasks/20260528-signature-governance-docs/docs/system/backend-api-design.md`
- `doc/tasks/20260528-signature-governance-docs/docs/system/frontend-design.md`
- `doc/tasks/20260528-signature-governance-docs/docs/acceptance/bdd-scenarios.md`

## 范围

- 后端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\ruoyi-vue-pro`
- 前端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260528-signature-governance-docs\yudao-ui-admin-vue3`
- 分支：`codex/20260528-signature-governance-docs`
- 代码、测试、E2E 和任务证据都必须在本任务中更新。

## 非范围

- 不触碰正式数据、正式 MinIO bucket、live 审核矩阵或生产恢复动作。
- 不引入 mock success、fallback、静默降级或默认成功。
- 不在缺少 Object Lock、恢复演练环境、质量 owner、统一策略源或真实测试租户数据时宣称生产放行。

## 前置任务检查

- 文档任务 `doc/tasks/20260528-signature-governance-docs/task.md` 已完成并提交。
- 前端配套任务 `yudao-ui-admin-vue3/doc/tasks/20260528-signature-governance-docs/task.md` 已完成并提交。

## 里程碑

- [x] M1：创建代码实施任务文档骨架。
- [x] M2：planner 子 agent 完成 request-analysis / PRD，主 reviewer 审查通过。
- [x] M3：decomposer 子 agent 完成 dev-plan / test-plan，主 reviewer 审查通过。
- [x] M4：按任务图启动开发子 agent，所有改动遵守 disjoint write scope。
- [x] M5：每个功能点都有后端测试、前端验证和 E2E 测试用例。
- [x] M6：独立 tester 或主 reviewer 复测全部 E2E、回归测试和文档放行标准。
- [x] M7：收尾清理预览、提交本任务直接改动。

## 放行标准

只有同时满足以下条件才可放行：

1. 四个功能点均有可运行代码，且行为完全符合已放行文档。
2. 每个功能点均有 E2E 测试用例；缺少真实前置条件时，E2E 必须证明 fail-fast 阻断路径，不能用 mock 成功替代。
3. 所有生产代码改动均先有 RED，再有 GREEN，再有 REGRESSION 证据，并记录在 `execution-log.md`。
4. 主 reviewer 确认每个子 agent 的代码没有越界改动、隐藏副作用、fallback 或接口漂移。
5. 后端、前端、E2E 和任务收尾验证均通过；若任一必需环境缺失，任务不得完成，必须记录 blocker 和影响。

## 预期验证

- 后端 Maven 定向测试和受影响模块回归测试。
- 前端类型检查、静态测试和受影响页面检查。
- 每个功能点至少一条 E2E：retention/recovery、periodic review、CSV package、cross-module policy。
- `task-closeout-cleanup` preview。

## 当前状态

- 状态：completed
- 当前阶段：task-specific commit ready。
- 已完成：SG-GOV-00 共享契约、后端错误码、前端共享权限码与 blocker 类型基线已完成 RED/GREEN。
- 已完成：SG-GOV-F1/F2/F3/F4 后端服务契约、API/controller 契约、前端 API client、电子签名治理工作台、四条真实 E2E 脚本均已完成 RED/GREEN。
- 已完成：长期留存不再信任请求布尔值；`SignatureGovernanceRetentionServiceImpl` 只有在唯一服务端 verifier 明确确认 MinIO/S3 Object Lock、bucket versioning、default retention、object version、content SHA-256 与 metadata 后才返回 `READY/RECORDED/PASSED`。
- 已完成：真实 MinIO bucket `signature-governance-e2e-20260528` 中已准备 DCC evidence、eDHR archive、recovery rehearsal 三类 retained object，并通过四条真实 Playwright E2E 验证。
- 已完成：周期审阅页面与 E2E 提交真实 review projection source；缺 owner、period、rule、source table/id/hash/action/meaning 时 fail-fast。
- 已完成：CSV release gate 页面与 E2E 提交材料、追溯、培训、变更控制、QA approval；缺任一真实样本时 fail-fast。
- 已完成：跨模块统一策略通过 `signature.governance.policy.modules.*` 配置作为当前统一策略源；DCC、eDHR、Showroom、IntAuth 四个模块均返回 `moduleStatuses`，全部确认后 `ready=true/status=READY`。
- 已完成：新增菜单权限 seed `sql/mysql/20260528_signature_governance_menu.sql`，为 `signature-governance:*` 权限写入菜单并授予已有 DCC 电子签名父菜单角色；测试库已执行并清理权限缓存，`tenant_admin` 角色确认 8 个签名治理权限。
- 已完成：`task-closeout-cleanup` preview 已执行；preview 确认 runtime 日志、`LOG_FILE_IS_UNDEFINED` 为清理候选，本轮已按安全路径校验手动删除临时产物。
- 已完成：最终 reviewer 子 agent Round 4 放行，`final_decision: pass`。
- 已完成：本任务直接改动已准备随 task-specific commit 提交。

## 当前 release decision

- 结论：GO for task-specific commit。
- 实现验证：PASS。后端定向回归、MES adapter、Showroom adapter、server package、前端静态/类型检查和四条真实 Playwright E2E 均已通过。
- 收尾状态：`task-closeout-cleanup` preview 已执行，临时产物已删除；最终 reviewer 子 agent 已放行；本文件随 task-specific commit 一并提交。worktree 快进合并/删除被主 worktree dirty 和当前分支不能 fast-forward merge into `int_main` 阻塞，不在本文档中声明已完成。
- superseded：2026-05-28 11:29 的 NO-GO、12:43 的 backend endpoint blocker、缺少真实 E2E runtime inputs、缺少真实 retention/recovery 样本、缺少 CSV release gate 数据和缺少统一策略权威源，均已由 2026-05-28 15:25 的最终 PASS 证据取代。

## 进度记录

### 2026-05-28 09:27 +08:00

- Planner 子 agent 完成 `request-analysis.md` 与 `prd.md`，主 reviewer 审查通过。
- Decomposer 子 agent 完成 `dev-plan.md` 与 `test-plan.md`，主 reviewer 审查通过。
- 主 reviewer 完成 SG-GOV-00 最小共享契约：
  - 后端：签名治理错误码、模块枚举、blocker 模型、权限码。
  - 前端：`src/api/signature-governance/shared.ts` 共享模块码、blocker 类型和权限码。
  - 验证：后端定向 Maven 测试、前端共享契约静态测试、前端 `npm run ts:check` 均通过。

### 2026-05-28 10:01 +08:00

- Worker Sartre 完成 F1 留存恢复服务契约，主 reviewer 要求补充 sourceType mismatch RED/GREEN 后通过。
- Worker Parfit/Aristotle 完成 F4 统一策略 policy/adapter 服务契约；主 reviewer 发现并修复 `PolicyServiceImpl` 过早注册 Spring `@Service` 的启动副作用风险。
- 验证：
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernanceRetention* test` PASS，9 tests。
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernancePolicy* test` PASS，8 tests。
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*DccControlledFileSignatureEvidence*,*SignatureGovernanceRetention* test` PASS，14 tests。
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernance* test` PASS，21 tests。
- 未放行项：API/controller、前端页面、四个功能点真实 E2E、F2 周期审阅、F3 CSV/质量包仍待实施。

### 2026-05-28 10:27 +08:00

- Worker Meitner 完成 F2 周期审阅服务契约，主 reviewer 复跑通过。
- Worker Lagrange 完成 F3 CSV/质量体系材料包服务契约，主 reviewer 复跑通过，并确认 CSV 包返回集合不可变。
- 验证：
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernanceReview* test` PASS，5 tests。
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernanceCsv* test` PASS，9 tests。
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernance* test` PASS，35 tests。
- 未放行项：四个功能点的 API/controller、前端页面、真实 E2E 仍未完成；当前只是服务契约层通过。

### 2026-05-28 10:41 +08:00

- 主 reviewer 完成后端 API/controller 契约：
  - `POST /signature-governance/retention/precheck`
  - `POST /signature-governance/periodic-review/batches`
  - `POST /signature-governance/csv/packages/{releaseId}/release-gate`
  - `GET /signature-governance/policies/current`
- Reviewer 追加 Bean 注册 RED/GREEN：`SignatureGovernanceReviewServiceImpl` 和 `SignatureGovernanceCsvServiceImpl` 注册为 Spring service；`SignatureGovernancePolicyServiceImpl` 继续保持未注册，避免缺少统一策略权威源时产生启动副作用或伪可用状态。
- 证据文档：`backend-api-evidence.md`。
- 验证：
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=SignatureGovernanceControllerTest test` PASS，6 tests。
  - `mvn --% -f pom.xml -pl yudao-module-dcc -Dtest=*SignatureGovernance* test` PASS，41 tests。
- 未放行项：前端页面、真实 E2E、策略源持久化/API、真实 Object Lock 与恢复演练证据仍待完成。

### 2026-05-28 11:24 +08:00

- Frontend API worker Raman 完成并提交 `d6e611e45 任务: 实现电子签名治理API客户端`，覆盖四个后端入口，`shared.ts` 未漂移。
- 主 reviewer 完成并提交 `ff5144b1b 任务: 实现电子签名治理工作台`，提供 `/signature-governance` 隐藏路由和长期留存、周期审阅、CSV质量包、统一策略四个页签。
- 主 reviewer 完成并提交 `2ffdb65a8 任务: 建立签名治理真实E2E门禁`，四个 E2E 文件与共享 helper 已建立。
- 前端验证：
  - `node scripts\signature-governance-shared-contract.test.mjs` PASS。
  - `node scripts\signature-governance-page-contract.test.mjs` PASS，2 tests。
  - `npm run ts:check` PASS。
  - `node tests\e2e\signature-governance-e2e-static.spec.js` PASS。
  - `node --check tests\e2e\signature-governance-real-flow-helper.js` 和四个 E2E 入口 PASS。
- E2E 阻塞：
  - `node tests\e2e\signature-governance-policy.e2e.js` FAIL fast，缺少 `SIGNATURE_GOVERNANCE_E2E_BASE_URL`、`SIGNATURE_GOVERNANCE_E2E_TENANT`、`SIGNATURE_GOVERNANCE_E2E_USERNAME`、`SIGNATURE_GOVERNANCE_E2E_PASSWORD`。
  - 影响：真实 Playwright E2E 未放行，整体验收和收尾清理不得执行为完成。

### 2026-05-28 11:29 +08:00

- 主 reviewer 按 `independent-verification-gate` 完成独立门禁报告：`verification-report.md`。
- 历史门禁结论：NO-GO；该结论已被 2026-05-28 15:25 的最终 PASS 证据 superseded。
- 已确认：后端契约、前端 API/工作台、静态 E2E 门禁均有可追溯证据。
- 未放行原因：真实 Playwright E2E、真实 Object Lock/WORM 与恢复演练证据、真实 CSV release gate 数据、统一策略权威源仍缺失。

### 2026-05-28 12:33 +08:00

- 按用户要求用 `admin/admin123` 尝试当前本地路径：
  - 当前 worktree 前端启动在 `http://127.0.0.1:18098`，后端使用已在线 `http://127.0.0.1:48098`。
  - 默认租户登录成功并进入 `/signature-governance`，但统一策略接口返回 `No static resource admin-api/signature-governance/policies/current`，说明本地后端端口不是当前后端 worktree 代码。
  - 显式要求非生产 `测试租户` 后，E2E helper fail-fast：`Login tenant mismatch: expected 测试租户, actual 芋道源码`。
- Reviewer 追加 E2E helper 租户校验 RED/GREEN，避免后续配置测试租户却实际使用默认租户。

### 2026-05-28 12:43 +08:00

- 按用户授权使用测试租户账号继续真实路径尝试：
  - `SIGNATURE_GOVERNANCE_E2E_TENANT=测试租户`
  - `SIGNATURE_GOVERNANCE_E2E_USERNAME=aoteman`
  - `SIGNATURE_GOVERNANCE_E2E_PASSWORD=admin123`
- Reviewer 修复 E2E helper 的租户读取范围，改为读取可见 Element Plus 租户选择控件，避免隐藏表单值干扰。
- 验证：
  - `node tests\e2e\signature-governance-e2e-static.spec.js` PASS。
  - `node --check tests\e2e\signature-governance-real-flow-helper.js` PASS。
  - `node tests\e2e\signature-governance-policy.e2e.js` 通过测试租户选择与账号输入，随后在后端请求处失败：`No static resource admin-api/signature-governance/policies/current`。
- 结论：测试租户账号 blocker 已解除；当前剩余主要阻塞是被测后端 endpoint 未运行当前后端 worktree 的签名治理 API。

### 2026-05-28 14:13 +08:00

- Backend worker 修复 Round 1 reviewer 阻塞项：retention receipt/recovery 成功状态必须由服务端 verifier 明确确认。
- 变更点：
  - `SignatureGovernanceRetentionVerificationService` 增加 receipt 与 recovery rehearsal 验证契约。
  - `SignatureGovernanceRetentionServiceImpl` 在 DCC receipt、eDHR receipt、recovery rehearsal 字段校验通过后继续执行服务端 verifier；verifier 缺失、返回 null 或返回 blocker 均 fail-closed 为 `BLOCKED`。
  - `SignatureGovernanceRetentionServiceTest` 增加无 verifier 与 verifier blocker 回归测试，证明请求字段和布尔值不能驱动成功。
- 验证：
  - RED: `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionServiceTest test` FAIL，旧行为返回 `RECORDED/PASSED`。
  - GREEN: `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionServiceTest test` PASS，15 tests。
  - GREEN: `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionServiceTest,SignatureGovernanceControllerTest test` PASS，24 tests。
- 历史剩余阻塞：真实 Object Lock/WORM、真实恢复演练环境、真实 E2E、CSV release gate 数据与统一策略权威源当时仍未完成，任务当时保持 `in_progress/NO-GO`；该状态已被 2026-05-28 15:25 的最终 PASS 证据 superseded。

### 2026-05-28 14:31 +08:00

- Backend worker 接入真实 MinIO/S3 Object Lock/WORM retention verifier：
  - 新增 `SignatureGovernanceRetentionObjectStoreVerificationService`，receipt/recovery 不再信任请求布尔值或请求 hash，必须读取服务端对象版本、Object Lock retention、对象内容 SHA-256 与 metadata。
  - 新增 `SignatureGovernanceRetentionS3ObjectStore`，通过 AWS S3 SDK/MinIO S3 API 查询 bucket versioning、Object Lock configuration、default retention、object retention、object metadata 与 object content。
  - 新增 `SignatureGovernanceRetentionS3Configuration` 与 `SignatureGovernanceRetentionS3Properties`，仅在 `signature.governance.retention.s3.enabled=true` 时注册 verifier；缺 endpoint/bucket/region/accessKey/secretKey 启动即失败。
  - DCC receipt 校验 metadata `sourceType/sourceId/auditEventId/evidenceHash`；eDHR receipt 校验 `sourceType/sourceId/auditEventId/archiveSha256/signatureHash`；recovery 校验 `backupId/recoveryRuntime/ownerReviewed/reportWritten/auditWritten/sourceType` 与 domain hash metadata。
- 验证：
  - RED: `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionObjectStoreVerificationServiceTest test` FAIL，生产 verifier/object-store 类缺失。
  - GREEN: `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionObjectStoreVerificationServiceTest test` PASS，9 tests。
  - GREEN: `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionServiceTest,SignatureGovernanceRetentionObjectStoreVerificationServiceTest,SignatureGovernanceControllerTest test` PASS，33 tests。
- 说明：用户提供的本机 MinIO bucket `signature-governance-e2e-20260528` 可作为后续 live verification 输入；本轮未运行真实 E2E，未改前端，未提交。

### 2026-05-28 15:25 +08:00

- 主 reviewer 启动本后端 worktree `yudao-server`，端口 `48198`，连接测试 MySQL `127.0.0.1:23306`、Redis `127.0.0.1:26379`、MinIO `http://127.0.0.1:9000` bucket `signature-governance-e2e-20260528`。
- 测试租户使用 `测试租户 / aoteman / admin123`，仅操作测试租户数据；芋道源码/admin 仅用于早期诊断，不作为放行 E2E 证据。
- 测试库执行 `sql/mysql/20260528_signature_governance_menu.sql`，为 `signature-governance:*` 8 个权限创建菜单并授予已有 DCC 电子签名父菜单角色；随后清理 Redis 中相关 permission/menu-role 缓存，避免旧空权限缓存继续导致 Access Denied。
- 真实 MinIO/Object Lock 样本：
  - DCC evidence：sourceId `302`，objectKey `dcc/signature-302.txt`，versionId `41c1a0b9-7851-4079-b3b0-ac2032589d0f`，content SHA-256 `368e96d0705d38d8e55e9b1f51e4f45aa716acbf6983dd6293b2d87f7d55fd7d`，retain-until `2026-06-27T06:18:00.000Z`。
  - eDHR archive：sourceId `9`，objectKey `edhr/archive-9.txt`，versionId `ced9a1ca-7d1f-4a6f-b718-88628402baa1`，content SHA-256 `0028470a082b948eba3bd7d06148f7f28efea2db717f742f609de7c1d0a61521`，retain-until `2026-06-27T06:18:00.000Z`。
  - Recovery rehearsal：backupId `backup-sg-20260528-001`，runtime `isolated-minio-restore-20260528`，objectKey `recovery/dcc-signature-302.txt`，versionId `517fcdf1-c83b-4c46-ab2e-6d7ba3627b52`，content SHA-256 `6c8c867f30f6e019d4f4854e9fafa6726bcd533bf8f15a4f0898d8cb3124993d`。
- 最终验证通过：
  - `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceRetentionServiceTest,SignatureGovernanceRetentionObjectStoreVerificationServiceTest,SignatureGovernanceControllerTest,ConfigurableSignatureGovernancePolicySourceProviderTest,SignatureGovernancePolicyIntAuthAdapterTest,SignatureGovernancePolicyServiceTest test` PASS，47 tests。
  - `mvn --% -pl yudao-module-mes -am -Dtest=MesEdhrSignatureGovernanceAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test` PASS，2 tests。
  - `mvn --% -pl yudao-module-showroom -am -Dtest=ShowroomSignatureGovernanceAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test` PASS，2 tests。
  - `mvn --% -pl yudao-module-dcc -Dtest=SignatureGovernanceControllerTest test` PASS，10 tests。
  - `mvn --% -pl yudao-server -am -DskipTests package` PASS。
  - 前端四条真实 Playwright E2E：retention/recovery、periodic review、CSV package、policy 均 PASS。
- 剩余动作仅为独立 reviewer 子 agent 放行、任务清理预览和提交。

### 2026-05-28 15:55 +08:00

- 执行 `task-closeout-cleanup` preview：
  - 后端 preview 保留核心任务证据，识别删除候选 `doc/tasks/20260528-signature-governance-implementation/runtime/` 与 `LOG_FILE_IS_UNDEFINED`。
  - 前端 preview 保留核心任务证据，识别删除候选 `doc/tasks/20260528-signature-governance-implementation/runtime/` 与 `test-results/signature-governance/`。
  - apply/快进合并清理被阻塞：当前 worktree 尚有本任务未提交改动、主 worktree dirty，且当前分支不能 fast-forward merge into `int_main`。
- 已按 preview 候选和路径安全校验删除临时 runtime 日志、`LOG_FILE_IS_UNDEFINED`、E2E 临时 result/debug 文件。
- 收尾状态：清理预览完成；最终 reviewer 放行与本任务提交仍待完成；worktree 合并/删除需等主 worktree 清洁且可快进后再执行。

### 2026-05-28 16:25 +08:00

- Review-fix-loop Round 4 最终 reviewer 放行：
  - 报告：`D:\ProjectPackage\Int\IntRuoyi\.review-fix-loop\runs\20260528T060130Z-e2f2b7\review\report-round-4.md`
  - 结论：`final_decision: pass`
  - 逻辑层、易用性层、UI 层均为 `pass`，无 blocking issues。
- 本任务代码、测试、E2E 与证据链满足放行条件，进入 task-specific commit。
- worktree 快进合并/删除仍未声明完成，原因保持为主 worktree dirty 与当前分支不能 fast-forward merge into `int_main`。

## Cleanup Keep

- `doc/tasks/20260528-signature-governance-implementation/task.md`
- `doc/tasks/20260528-signature-governance-implementation/execution-log.md`
- `doc/tasks/20260528-signature-governance-implementation/test-report.md`
- `doc/tasks/20260528-signature-governance-implementation/verification-report.md`
- `doc/tasks/20260528-signature-governance-implementation/backend-api-evidence.md`
- `doc/tasks/20260528-signature-governance-implementation/request-analysis.md`
- `doc/tasks/20260528-signature-governance-implementation/prd.md`
- `doc/tasks/20260528-signature-governance-implementation/dev-plan.md`
- `doc/tasks/20260528-signature-governance-implementation/test-plan.md`
- `doc/tasks/20260528-signature-governance-implementation/task-state.json`

## Cleanup Candidates

- `LOG_FILE_IS_UNDEFINED`
- `doc/tasks/20260528-signature-governance-implementation/runtime/`
