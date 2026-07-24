# 执行日志

## BDD

- BDD: 版本导入预检页面 -> Given 用户导入同名 Word When 后端返回预检结果 Then 页面展示版本号、未生效状态、迁移摘要和阻断项。
- BDD: 审批状态页面 -> Given V2.0 待审批 When 用户查看版本列表 Then 页面显示 V1.0 当前生效、V2.0 待审批且不可生产使用。
- BDD: 二期结构化 diff -> Given 迁移项包含表、工序、字段、签名位、附件规则 When 用户查看差异 Then 页面按分组展示并支持授权确认。
- BDD: 三期治理看板 -> Given 存在多个版本和历史引用 When 用户查看治理页面 Then 页面展示版本影响面、回滚入口和异常巡检结果。
- BDD: 三期附加表单槽位版本化 -> Given 后端返回槽位绑定和快照哈希 When 用户查询治理页 Then 页面展示附加表单槽位版本化、负责人角色和快照信息。
- BDD: 三期受控回滚审批 -> Given 测试租户用户具备回滚申请权限 When 从治理页提交回滚原因、影响面摘要、签核证据和幂等键 Then 页面只调用 rollback/request 并校验返回 `BATCH_RECORD_VERSION / ROLLBACK`。
- BDD: 三期影响面与巡检 -> Given 版本存在执行、路线、规则和迁移阻断项 When 查询治理页 Then 页面展示风险等级、批量历史治理巡检状态、问题摘要和下一步动作。
- BDD: 三期运营指标 -> Given 版本存在待审批、已审批、回滚申请和迁移项 When 查询治理页 Then 页面展示运营指标和治理看板。
- BDD: 三期 admin 只读复验 -> Given `芋道源码/admin` 登录同一路径 When 查询治理页 Then E2E 监听不得出现治理接口 POST/PUT/PATCH/DELETE 写请求。

## RED / GREEN 计划

- RED: 静态契约测试 -> 旧页面缺少版本列表、预检差异、审批状态和 API。
- RED: E2E 登录预检 -> 独立 worktree 启动前应阻塞。
- GREEN: 阶段一页面、API、E2E。
- GREEN: 阶段二页面、API、E2E。
- GREEN: 阶段三页面、API、E2E。
- RED: `node scripts/edhr-version-governance-contract.test.mjs` -> FAIL, 阶段三治理 API 模块、页面、路由、真实 E2E 路径缺失。
- GREEN: `node scripts/edhr-version-governance-contract.test.mjs` -> PASS, eDHR version governance frontend contract。
- GREEN: `node --check tests/e2e/edhr-version-governance-real-flow.e2e.js` -> PASS。
- BLOCKER: `node tests/e2e/edhr-version-governance-real-flow.e2e.js` -> FAIL, 缺少 eDHR 版本治理真实 E2E 登录、版本或回滚申请环境变量，已写入 `version-governance-real-e2e-evidence.md`。

## 经验预检

- INFO: experience-index -> matched `docs/worktree-memory.md`, `docs/login-access.md`, `docs/powershell-memory.md`, `docs/experience/batch-record-form-recognition.md`。
- BLOCKER: experience-preflight -> 前端运行态尚未启动，真实 E2E 前必须确认 `node_modules`、Playwright、代理目标、后端健康检查和官方登录预检。

## 已完成工作

- 创建前端任务文档和执行日志。
- 阶段二子 agent 完成前端结构调研：确认当前“导入 Word”入口、`BatchRecordReportApi.recognizeUploadedRoute`、eDHR 初始化/放行页面样式和真实 E2E 脚本资产。
- 新增阶段二前端设计文档，明确 API、页面组件、错误暴露、只读复验网络门禁。
- 新增阶段二真实 E2E 待启用清单，覆盖结构化 diff、`CONFIRM_REQUIRED`、草稿重传、迁移证据展示四类功能点。
- 阶段三新增版本治理 API 模块，覆盖 summary、impact、inspection、metrics、rollback/request。
- 阶段三新增 `eDHR版本治理` 页面，展示附加表单槽位版本化、受控回滚审批、版本影响面分析、批量历史治理巡检、治理看板和运营指标。
- 阶段三新增剩余路由入口 `pro/feedback/edhr-version-governance`，使用 `mes:pro-batch-record-version:governance-query` 查询权限，回滚按钮使用 `mes:pro-batch-record-version:rollback-request`。
- 阶段三新增真实 E2E 路径，要求测试租户写入回滚申请，admin 只读复验治理接口无写请求；缺少真实参数时 fail fast 并写阻塞证据。

## 验证证据

- GREEN: phase3-frontend-contract -> `node scripts/edhr-version-governance-contract.test.mjs` -> PASS。
- GREEN: phase3-e2e-syntax -> `node --check tests/e2e/edhr-version-governance-real-flow.e2e.js` -> PASS。
- BLOCKER: phase3-real-e2e -> `node tests/e2e/edhr-version-governance-real-flow.e2e.js` -> FAIL，缺少 `EDHR_VERSION_GOVERNANCE_E2E_*` 本地运行态、租户账号、版本 ID、回滚目标版本 ID 和签核证据哈希；详见 `doc/tasks/20260708-edhr-version-implementation/version-governance-real-e2e-evidence.md`。

## 剩余阻塞

- 待确认前端 worktree 依赖是否完整。
- 待确认后端 `48096` 启动成功后再执行登录和 E2E。
- BLOCKER: phase2-wait-phase1 -> 阶段一版本快照、迁移证据、审批门禁 API 未合入前，不能启用阶段二页面写入入口。
- TODO(PHASE2_WAIT_PHASE1): 阶段一版本契约合入前，阶段二前端只保留页面/API/E2E 路径设计。
- BLOCKER: phase2-real-e2e -> 未启动本地 `8096/48096` 运行态，真实 E2E 仅完成路径设计，不能声明通过。
- BLOCKER: phase3-real-e2e -> 需要主控启动本地 `8096/48096`，提供或创建测试租户真实批记录定义、当前版本、可回滚旧版本、签核证据哈希，并确认 admin 账号只读访问权限后重跑 Playwright。
- REVIEW: phase3-menu-sql -> 当前子 agent 添加的是 remaining 路由入口；是否需要补 `system_menu` SQL 和测试租户角色绑定，需由主控结合阶段一/二菜单迁移统一放行。
- REVIEW: phase3-selector-risk -> 真实 E2E 使用 Element Plus 表单输入顺序定位，首轮本地页面验证后如选择器不稳定，应改为更明确的 data-testid 或 label 定位，但不得新增仅供测试的业务控件。

## 当前状态

- blocked: 阶段三前端契约和 E2E 脚本语法已通过；真实 E2E 因本地运行态和真实测试数据参数缺失阻塞。

## 运行态与登录预检 - 2026-07-08 20:59:24

- GREEN: runtime-ownership -> PASS，后端 http://127.0.0.1:48096/actuator/health 返回 {"status":"UP"}，前端 http://127.0.0.1:8096 返回 200；进程命令行均指向 D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version。
- GREEN: experience-preflight -> PASS，官方 login-preflight.mjs 使用本机测试租户 测试租户/aoteman/111111 成功进入 /mes/pro/feedback/edhr-version-governance。
- BLOCKER: admin-readonly-login -> 芋道源码/admin/111111 官方登录预检失败，接口返回账号密码不正确；按登录门禁不得猜测密码、不得静默切换账号、不得修改 admin 租户数据。影响：芋道源码/admin 最终只读复验暂不能完成。

## 主控 review 修复 - 2026-07-09

- REVIEW: independent-agents -> FAIL，阶段一、阶段二、阶段三审查均判定不可放行；主要阻塞为 admin 只读复验、阶段二确认门禁/草稿重传真实链路、阶段三真实行为契约与真实 E2E。
- GREEN: runtime-ownership -> PASS，`48096` Java 进程和 `8096` Vite 进程均来自 `D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version`。
- GREEN: login-preflight-test-tenant-minimal -> PASS，使用系统 Chrome 真实登录 `测试租户/aoteman/111111` 进入 `/mes/pro/feedback/edhr-version-governance`。
- RED: phase2-review -> FAIL，`CONFIRM_REQUIRED` 确认后仍被旧 `countBlockingItems` / 巡检指标口径计为阻断。
- GREEN: phase2-confirm-gate-contract -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseTwoMigrationContractTest,MesProBatchRecordVersionPhaseThreeGovernanceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，12 tests / 0 failures / 0 errors；已将阻断口径统一为 `BLOCKER + 未确认 CONFIRM_REQUIRED`。
- BLOCKER: admin-readonly-login -> `芋道源码/admin/111111` 仍登录失败；未猜测密码、未切换账号、未修改 admin 租户数据。
- BLOCKER: phase2-real-e2e -> 草稿重传仍需接入真实 Word 文件上传/解析/迁移证据重建，阶段二真实 E2E 不可声明通过。
- BLOCKER: phase3-real-e2e -> 仍需真实版本治理数据、回滚目标版本、签核证据哈希、admin 只读网络写请求断言和数据库前后只读比对。
## 阶段二 review 修复复验 - 2026-07-09 00:39:23

- RED: phase2-draft-reupload-contract -> FAIL，旧契约仍允许 sourceFileName/sourceFileSha256 元数据式草稿重传，且服务实现残留 buildReuploadVersionNo 合成版本路径。
- GREEN: phase2-draft-reupload-contract -> PASS，草稿重传改为 multipart file + productNames + remark，后端复用 recognizeUploadedRoute 执行真实 Word 导入，前端使用 FormData 和 request.upload，禁止手工填写 SHA/文件名。
- GREEN: phase2-phase3-contract-rerun -> node scripts\edhr-version-governance-contract.test.mjs -> PASS；mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordVersionPhaseTwoMigrationContractTest,MesProBatchRecordVersionPhaseThreeGovernanceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，Tests run: 13, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKER: admin-readonly-login -> 芋道源码/admin/111111 仍为最终只读复验硬前置；未猜测密码、未切换账号、未修改 admin 租户数据。

## 阶段三真实 E2E 推进与阻塞 - 2026-07-09 01:23

- GREEN: phase3-menu-permission -> 后端迁移已补 `eDHR版本治理` 页面菜单和 `governance-query / confirm / import / rollback-request` 按钮权限；本地测试租户角色已绑定，页面按钮由真实权限显示。
- RED: phase3-real-e2e-selector -> FAIL，初版 E2E 在治理数据异步重载前填写目标版本，实际提交仍为当前版本 `17`；已修复为等待全部治理接口返回后再填写回滚表单，并断言 POST 请求体 `targetVersionId=15`。
- GREEN: phase3-real-e2e-test-tenant-write -> `pnpm e2e:edhr:version-governance` 已完成测试租户真实写入段：从页面查询 definitionId=12/versionId=17，提交回滚到 targetVersionId=15，后端落库 `EDHR-CHANGE-20260709012217`，状态 `DRAFT`，事件 `CREATE -> DRAFT`。
- GREEN: phase3-frontend-contract-rerun -> `pnpm e2e:edhr:version-governance:check` -> PASS；`node --check tests/e2e/edhr-version-governance-real-flow.e2e.js` -> PASS。
- BLOCKER: admin-readonly-login-final -> 官方登录预检 `芋道源码/admin/111111` 失败，接口返回账号密码不正确；按门禁未猜测密码、未切换账号、未修改 admin 租户数据。
- BLOCKER: release-gate -> `芋道源码/admin` 只读复验未完成，不能声明三阶段全部完成，不能提交、合并或删除 worktree。

## 继续任务阻塞复核 - 2026-07-09

- GREEN: runtime-state-recheck -> PASS，`D:\ProjectPackage\Int\IntRuoyiWorktrees\edhr_version` 前后端 worktree 均存在，后端 `http://127.0.0.1:48096/actuator/health` 返回 `{"status":"UP"}`，前端 `8096` 与后端 `48096` 均有监听进程。
- BLOCKER: admin-readonly-login-recheck -> 官方登录预检 `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8096 --tenant 芋道源码 --username admin --password 111111 --target-path /mes/pro/feedback/edhr-version-governance --timeout 90000` -> FAIL，退出码 1，HTTP 200 登录失败，账号密码不正确。
- INFO: admin-account-readonly-db-check -> 本地库只读核对 `system_users.username='admin'`：`tenant_id=1` 账号存在、启用、未删除，所属租户 `芋道源码` 启用、未删除；`tenant_id=122` 同名账号密码为空，不作为最终 `芋道源码/admin` 只读复验账号。
- BLOCKER: release-gate-recheck -> 当前仍缺少可通过真实登录页的本机 `芋道源码/admin` 凭据或用户明确授权的本地 admin 租户账号修复；按门禁未猜测密码、未切换账号、未修改 admin 租户数据，因此不能提交、不能融合进 `int_main`，不能删除 `edhr_version` worktree。

## admin 只读门禁恢复 - 2026-07-09

- GREEN: admin-readonly-login-admin123 -> 用户提供本机 `芋道源码/admin` 密码 `admin123` 后，官方登录预检 `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://127.0.0.1:8096 --tenant 芋道源码 --username admin --password admin123 --target-path /mes/pro/feedback/edhr-version-governance --timeout 90000` -> PASS，真实登录已进入目标页。
- RED: phase3-admin-query-e2e -> `pnpm e2e:edhr:version-governance` -> FAIL，admin 登录已通过，但治理查询脚本等待测试租户版本接口超时；诊断确认 admin 租户页面可进入且无治理写请求，测试租户版本 ID 在 admin 租户下按租户隔离显示 `版本信息未生成`。
- GREEN: phase3-admin-readonly-e2e -> `pnpm e2e:edhr:version-governance:check` -> PASS；`pnpm e2e:edhr:version-governance` -> PASS，测试租户真实页面创建受控回滚申请 `EDHR-CHANGE-20260709081905`，`芋道源码/admin` 真实登录同一路径并验证治理写请求数 `0`。
- REVIEW: phase2-real-e2e-gap -> 阶段二任务文档仍记录 `phase2-real-e2e` 为阻塞，且当前已知证据只覆盖确认门禁契约和草稿重传 multipart 契约；融合前必须补齐阶段二真实页面 E2E 或明确记录不可放行原因。

## Phase 2 真实 E2E 补齐 - 2026-07-09

- BDD: phase2 structured migration -> Given 测试租户通过页面生成已批准 V1 / When aoteman 导入同名真实 Word 升级 / Then 生成六类结构化迁移差异且未确认 `CONFIRM_REQUIRED` 阻断审批。
- BDD: phase2 draft reupload -> Given V2 处于 `PRECHECK_FAILED` / When aoteman 在治理页选择真实 Word 和真实产品名称重新上传 / Then 旧版本作废，新版本生成，并重建迁移证据。
- BDD: phase2 confirm migration -> Given 重传新版本存在 `CONFIRM_REQUIRED` / When aoteman 填写确认意见并授权确认 / Then 页面显示确认审计，审批就绪为是。
- BDD: phase2 admin readonly -> Given `芋道源码/admin` 登录同一路径 / When 查询测试租户生成的定义和版本 / Then 不发送治理 POST/PUT/PATCH/DELETE 写请求。
- RED: phase2-status-flow-db-test -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenUpgrade_createsStructuredPhaseTwoMigrationDiffAndBlocksUntilConfirmed" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected `<6>` but was `<1>`，真实升级未生成六类迁移差异。
- GREEN: phase2-status-flow-db-test -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent+recognizeUploadedRoute_whenUpgrade_createsStructuredPhaseTwoMigrationDiffAndBlocksUntilConfirmed,MesProBatchRecordVersionPhaseTwoMigrationContractTest#confirmRequiredMigrationControlsPrecheckStatusUntilAuthorized" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- RED: phase2-real-e2e-first-run -> `pnpm e2e:edhr:batch-version-phase2` -> FAIL，产品下拉浮层拦截导入确认按钮，真实页面未完成长链路。
- RED: phase2-real-e2e-multipart-assert -> `pnpm e2e:edhr:batch-version-phase2` -> FAIL，草稿重传业务成功但脚本用 `postData()` 断言 multipart 文件字段不稳定。
- GREEN: phase2-real-e2e -> `pnpm e2e:edhr:batch-version-phase2:check` -> PASS；`EDHR_PHASE2_ADMIN_PASSWORD=<provided> pnpm e2e:edhr:batch-version-phase2` -> PASS，batchRecordName=`E2E-PHASE2-1783559122925`，definitionId=`14`，v1=`21`，v2Voided=`22`，v3=`23`，adminWriteCount=`0`。
- BLOCKED: historical-phase2-gate -> 先前 `phase2-real-e2e` 阻塞已由本节 GREEN 解除；保留此 marker 作为 release 覆盖门禁的历史阻塞证据，当前不再阻塞。

## 最终任务范围门禁 - 2026-07-09

- GREEN: task-closeout-cleanup-preview -> PASS，已清理 `.env.edhr_version` 与 `tests/e2e/output` 临时产物；保留 `task.md`、`execution-log.md` 与真实 E2E evidence。
- GREEN: scoped-batch-version-coverage -> PASS，三阶段升版 source/API/E2E/evidence token 覆盖检查通过。
- GREEN: frontend-contracts -> PASS，`pnpm e2e:edhr:batch-version-phase1:check`、`pnpm e2e:edhr:batch-version-phase2:check`、`pnpm e2e:edhr:version-governance:check` 均通过。
- GREEN: backend-contracts-db -> PASS，`mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenSameNameAndRouteExistsWithUpgrade_createsPendingVersionSnapshotWithoutMutatingCurrent+recognizeUploadedRoute_whenUpgrade_createsStructuredPhaseTwoMigrationDiffAndBlocksUntilConfirmed,MesProBatchRecordVersionPhaseTwoMigrationContractTest,MesProBatchRecordVersionPhaseThreeGovernanceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，16 tests / 0 failures / 0 errors。
- BLOCKER: full-release-coverage-existing-gaps -> `pnpm e2e:edhr:release:check` 仍因既有全量 eDHR 覆盖矩阵历史缺口失败；本次新增三阶段升版 scoped coverage 已通过，未用该失败项覆盖或跳过本任务真实 E2E。

## 融合门禁审计 - 2026-07-09

- GREEN: frontend-task-commit -> PASS，`codex/edhr_version` 前端提交 `0cafa8c56`，工作区干净。
- GREEN: backend-task-commit -> PASS，`codex/edhr_version` 后端提交 `e952894902`，工作区干净；新增 SQL 契约门禁 `python -X utf8 -m pytest script/tests/test_mes_batch_record_version_sql.py -q` -> PASS，6 tests。
- BLOCKER: frontend-int-main-merge-tree -> FAIL，`git merge-tree --write-tree int_main codex/edhr_version` 退出码 `1`，`src/api/mes/pro/scheduleorder/index.ts` 内容冲突。
- BLOCKER: frontend-main-dirty-overlap -> FAIL，主工作区前端脏改 `89` 个文件，与本任务重叠 `src/api/mes/pro/scheduleorder/index.ts`、`src/views/mes/pro/batchrecordtemplate/index.vue`。
- BLOCKER: backend-main-dirty-overlap -> FAIL，后端 merge-tree 退出码 `0`，但主工作区后端脏改 `71` 个文件，与本任务重叠 `yudao-module-mes/src/test/resources/sql/create_tables.sql`。
- BLOCKER: merge-cleanup -> 按 worktree 门禁，重叠归因/处理前不得融合 `int_main`，不得删除 `edhr_version` worktree。


## 合并后收尾 - 2026-07-09 09:51:50

- GREEN: frontend-int-main-merge -> PASS，`git merge --no-ff codex/edhr_version` 生成 merge commit `2cb8413a8`。
- GREEN: frontend-overlap-restore -> PASS，恢复本地重叠脏改后手工解析 `src/views/mes/pro/batchrecordtemplate/index.vue`，保留 `resolveWordImportRouteKey` 与 `edhr-batch-version-phase1-panel`。
- GREEN: frontend-post-merge-contracts -> PASS，三项前端任务范围契约检查均通过。
