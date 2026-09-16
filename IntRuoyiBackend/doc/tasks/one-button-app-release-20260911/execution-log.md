# Execution Log

- Task ID: `one-button-app-release-20260911`
- Workspace: `D:\IntRuoyiWorktree\r260911-release-button\a`
- Current status: `in_progress`

## User Request

继续完成维护仓 `doc/tasks/one-button-app-release-20260911` 中的开发验证任务；安装包固定为 without-data / `app-release`，不得包含业务数据、MinIO 快照或全量数据库。

## Experience Preflight

GREEN: experience-preflight -> PASS，已读取本任务维护仓状态、应用仓 `AGENTS.md`、PowerShell/worktree/release backup restore 规则、CI/CD 和数据库 schema 技能合同。本轮先处理 P3 target read-only preflight 缺口，不执行真实发布、数据库写入、MinIO、NAS 上传或正式服操作。

## BDD Scenarios

BDD: 目标数据依赖迁移必须有只读检查 -> Given app-release 发布计划中存在 data/menu/config/permission/seed 或 `requiresTargetPreflight=true` 的 schema 迁移 / When 构建前 target data preflight 运行 / Then 每条待应用迁移必须绑定一个 UTF-8 `.preflight.sql`，并在只读事务中返回精确 `TARGET_PREFLIGHT_PASS:<migrationId>`。

BDD: 结构演练不伪造业务数据 -> Given 迁移依赖测试服已有菜单、角色、业务路线或配置 / When schema-only rehearsal 运行 / Then 结构库只验证结构迁移，真实目标状态由 target preflight 只读查询证明，缺失检查时在 Maven 前阻断。

RED: `python -X utf8 -m pytest -q IntRuoyiBackend\script\tests\test_release_target_preflight_files.py --tb=short --basetemp <app-worktree>\.tmp-pytest-target-preflight-red` -> FAIL，目标预检目录为空，58 个 `*.preflight.sql` 缺失。

GREEN: target-preflight-file-contract -> PASS。已补齐 58 个绑定 `release-target-preflight` 元数据的只读 `SELECT`/`WITH` 检查文件，覆盖迁移表存在性、关键菜单/角色/通知模板/tenant package JSON、业务策略表和受控 schema 依赖。

RED: app-migration-policy-target-preflight-scan -> FAIL，`script\release\release_migration_policy_gate.py` 会把 `sql/mysql/target-preflight/*.preflight.sql` 当普通迁移扫描，并因缺少 `release-migration` 元数据失败。

GREEN: app-migration-policy-target-preflight-scan -> PASS。迁移 policy gate 与 manifest builder 均排除 `target-preflight/` 检查文件，检查文件不进入 required SQL 清单。

RED: requires-target-preflight-manifest-metadata -> FAIL，`release_migration_manifest.py` 不识别 `requiresTargetPreflight`，无法让目标只读门禁字段参与 manifest/plan 合同。

GREEN: requires-target-preflight-manifest-metadata -> PASS。manifest builder 显式解析 `requiresTargetPreflight=true/false`，默认 `false`，非法值 fail fast，并在条目中输出布尔字段。

RED: manifest-validator-fixture-real-bytes -> FAIL，`valid-v1` fixture 的 `backend/app.txt` 在 Windows worktree 中为 CRLF 真实字节，manifest 中 size/hash 仍是旧 LF 值，导致安全扫描被 `PACKAGE_FILE_SHA256_MISMATCH` 抢先拦截。

GREEN: app-preflight-regression -> PASS，`python -X utf8 -m pytest -q --tb=short --basetemp <app-worktree>\.tmp-pytest-app-preflight-final script\tests\test_release_target_preflight_files.py script\tests\test_release_manifest_migration_contract.py script\tests\test_release_migration_metadata.py script\tests\test_release_migration_policy_gate.py script\tests\test_release_preflight_plan.py script\tests\test_release_manifest_validator.py` -> 61 passed。

NOTE: 本轮未执行服务器写入、NAS 上传、Docker 构建、Maven package、MinIO 或正式服动作；测试临时目录清理命令被本机安全策略拦截，提交时仅精确暂存任务文件，临时目录不纳入。

## Workflow app-release scope correction

BDD: workflow 构建动作固定 app-release -> Given 操作者通过三按钮 workflow 生成程序包 / When 后端编排调用底层 `build-release` / Then 服务端固定 `PublishScope=app-release`，客户端不得把旧 `code-only` 或 `with-data` 带入标准 workflow。

RED: `mvn -pl yudao-module-infra -Dtest=ReleaseWorkflowOrchestratorTest#buildButtonDispatchesOneServerOwnedAppReleaseOperation test` -> FAIL，测试期望 `ReleaseWorkflowContract.PUBLISH_SCOPE` 即 `app-release`，实际仍为 `code-only`。

GREEN: 同一 Maven 单测 -> PASS；`mvn -pl yudao-module-infra -Dtest=ReleaseWorkflow*Test test` -> PASS，25 tests；workflow package 静态扫描确认不再存在 `setPublishScope("code-only")`。

NOTE: 本修正只修改本机 app worktree 中的 workflow 契约与测试；未启动服务、未发布、未访问服务器、NAS、数据库、MinIO 或正式服。

## P3 C07 Regression Unblocker

BDD: infra 全量回归必须保持可运行 -> Given 发布 workflow 已收紧为 `app-release` / `manifest.json` / 已测试证据合同 / When 运行主程序 infra 模块全量测试 / Then 旧测试 fixture 不能继续使用 `release-manifest.json` 或缺失测试服验收证据，且 codegen 快照必须与现有生成器输出一致。

RED: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` -> FAIL，首次 546 tests 中 12 failures、2 errors；runtime-control 两个错误分别为 rollback fixture 缺 `manifest.json.packageId`、promotion fixture 缺 `testOperationEvidence`。

GREEN: runtime-control fixture contract -> PASS，`mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=RuntimeControlHighRiskActionContractTest,RuntimeOpsResponsibilityServiceImplTest" test` 通过 14 tests。

RED: app-infra-c07-after-runtime-fixture-fix -> FAIL，`mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` 仍有 `CodegenEngineVue2Test` / `CodegenEngineVue3Test` 12 个 stale snapshot mismatch；确认本任务未改 codegen 生产逻辑。

GREEN: codegen snapshot contract -> PASS，使用既有 `-Dcodegen.regenerate=true` 只刷新 `CodegenEngineVue2Test` / `CodegenEngineVue3Test` 对应测试资源后，`mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=CodegenEngineVue2Test,CodegenEngineVue3Test" test` 通过 12 tests。

GREEN: app-infra-c07-regression -> PASS，`mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-infra test` 通过 546 tests、0 failures、0 errors、10 skipped；未执行服务器写入、数据库写入、NAS 上传、Docker build、MinIO 或正式服动作。

## R28 Balloon XLSX Route Cleanup Target Contract

BDD: 已规范化路线不应被旧清理迁移阻断 -> Given 测试服 `ROUTE-XLSX-00001/00002` 当前活跃工序总数已为后续绑定迁移要求的 49，且 `ROUTE-XLSX-00002` 第 26 道为有效 `Z2620` 而不是旧无效 `B320` / When 发布执行 `20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.sql` / Then 迁移应输出 already-normalized 结果并不修改业务数据；target preflight 必须在构建前验证 legacy cleanup 或 already-normalized 两种合法目标状态。

RED: `python -X utf8 -m pytest -q script\tests\test_mes_balloon_xlsx_route_00002_invalid_process_cleanup_sql.py script\tests\test_release_target_preflight_files.py --basetemp ..\.tmp-r28-balloon-cleanup-red` -> FAIL，新增测试证明迁移缺少 already-normalized 分支，且 target preflight 只检查弱条件，无法提前覆盖 `B320/Z2620/49` 目标合同。

GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_balloon_xlsx_route_00002_invalid_process_cleanup_sql.py script\tests\test_release_target_preflight_files.py --basetemp ..\.tmp-r28-balloon-cleanup-green` -> PASS，11 passed；`python -X utf8 -m pytest -q script\tests\test_release_preflight_plan.py script\tests\test_release_manifest_migration_contract.py script\tests\test_release_migration_policy_gate.py --basetemp ..\.tmp-r28-balloon-cleanup-regression` -> PASS，40 passed。

GREEN: remote-target-preflight-r28-fix -> PASS，只读执行 `20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.preflight.sql` 到测试服业务库，返回 `TARGET_PREFLIGHT_PASS:20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup`；查询证据显示 `ROUTE-XLSX-00001=23`、`ROUTE-XLSX-00002=26`、总数 `49`、`B320 target=0`、`Z2620 target=1`。

NOTE: 本修复只修改应用仓迁移 SQL、target preflight SQL 和对应静态合同测试；没有手工修改测试库业务数据，没有执行正式服、审查服或 MinIO 数据操作。R28 发布包已判废，下一轮必须使用新 releaseTag。

## R42 C00 Backfill Collation Regression

USER_AUTHORIZATION: 用户继续并明确授权；范围覆盖本机应用仓 SQL 修复、测试、任务分支提交，以及后续使用新 tag 重新生成 without-data/app-release 程序包并发布测试服；不覆盖正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步或全量数据库同步。

RED: deploy-release-r42-c00-backfill-collation -> FAIL，测试服执行 required SQL `20260812_mes_pqc_dcc_qa_c00_backfill.sql` 时失败：`ERROR 1267 (HY000) at line 1353: Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT) for operation '<>'`。release operation lock 已释放为 FAILED，尚未切换测试服 `.env` 到 R42，未执行正式服或 MinIO 数据同步。

ROOT_CAUSE: `c00_backfill_task_rule_candidate` 使用 `CREATE TEMPORARY TABLE ... AS SELECT CASE ...`，`expected_rule_key` 继承当前数据库默认 `utf8mb4_0900_ai_ci`；测试服旧表 `mes_pqc_inspection_task.inspection_rule_key/submitted_content_hash` 为 `utf8mb4_unicode_ci`。脚本在 `< >` 与 `<=>` 比较时形成两个隐式排序规则比较，导致部署期失败。

BDD: C00 回填临时文本列必须匹配旧表排序规则 -> Given 测试服旧业务表文本列为 `utf8mb4_unicode_ci` 且数据库默认可能为 `utf8mb4_0900_ai_ci` / When C00 backfill 生成临时候选和 approved manifest 文本列 / Then 所有会与旧表文本列比较的临时列必须显式声明 `CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`，不得依赖数据库默认排序规则。

RED: `python -X utf8 -m pytest -q script\tests\test_mes_pqc_dcc_qa_c00_backfill_sql.py --basetemp .tmp-r43-c00-collation-red` -> FAIL，2 failed；新增合同测试证明 `c00_backfill_task_rule_candidate` 仍为 CTAS 且 `piece_detail_sha256/submitted_content_hash` 未显式绑定旧表排序规则。

GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_pqc_dcc_qa_c00_backfill_sql.py --basetemp .tmp-r43-c00-collation-green` -> PASS，2 passed。实现将 `c00_backfill_task_rule_candidate` 改为显式建表后插入，并把 approved manifest hash 文本列固定为 `utf8mb4_unicode_ci`。

REGRESSION: `python -X utf8 -m pytest -q script\tests\test_mes_pqc_dcc_qa_c00_backfill_sql.py script\tests\test_release_target_preflight_files.py script\tests\test_release_preflight_plan.py --basetemp .tmp-r43-c00-collation-regression` -> PASS，26 passed。

NEXT: R42 判废不复用；提交应用仓修复后必须使用新 releaseTag `release-20260915-one-button-app-r43`，source freeze 固定维护仓当前 clean HEAD 与应用仓新提交，重新生成 without-data/app-release 程序包并发布测试服。

## P2/P3 scheduler heartbeat and Jimu target-preflight continuation

BDD: 后台自动推进按钮 workflow -> Given 底层 release operation 已经从 `running` 变为 `succeeded` 或 `failed` / When scheduler 运行且操作者没有手动刷新页面 / Then workflow 必须自动 reconcile 到 READY/FAILED/TEST_DEPLOYED 等真实状态，不能依赖用户轮询触发阶段推进。

BDD: 长构建日志仍推进不得被 heartbeat 误判 -> Given workflow heartbeat 已超过阈值但底层 operation 仍为 `running` 且 operation log 有新的 mtime / When recovery scheduler 执行 / Then workflow heartbeat 使用日志 mtime 刷新，不取消底层进程；只有日志和 operation 均无可见推进时才 fail-closed recovery。

RED: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=ReleaseWorkflowOrchestratorTest" test` -> FAIL，新增测试要求 `ReleaseWorkflowOrchestrator.reconcileActiveWorkflows(Instant)`，旧实现没有后台自动 reconcile 入口。

GREEN: scheduler-heartbeat-fix -> PASS。新增 `ReleaseWorkflowRecoveryScheduler`，每 30 秒先 `reconcileActiveWorkflows(now)` 再 `recoverStaleWorkflows(now)`；orchestrator 对终态 operation 自动推进 workflow，对 running operation 用日志最后修改时间刷新 heartbeat，对不可读取日志抛出 `RELEASE_WORKFLOW_OPERATION_LOG_INSPECTION_FAILED`，不吞异常。

GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-infra "-Dtest=ReleaseWorkflowOrchestratorTest,ReleaseWorkflowRecoverySchedulerTest,ReleaseWorkflowRecoveryTest,ReleaseWorkflowSourceBindingTest,ReleaseWorkflowStoreTest" test` -> PASS，19 tests，0 failures，0 errors。

BDD: R53 Jimu 目标语义必须前移 -> Given 旧表单模板迁移依赖 `jimu_schema_json` / `recognized_schema_json` 生成 Jimu layout / When target readonly preflight 运行 / Then 必须检查 `sheetLayoutJson` object、rows/cols、待迁移 binding 唯一性、已绑定报表存在性和 release migration 状态，不能只检查表/列存在。

GREEN: `python -X utf8 -m pytest -q script\tests\test_mes_old_form_template_binding_switch_sql.py script\tests\test_release_target_preflight_files.py --basetemp .tmp-current-jimu-preflight` -> PASS，12 tests。

STATUS: R61 本地目录只有 required-sql 与 build-preflight 证据，缺少 `manifest.json` 和镜像包；本机无 `release-20260915-one-button-app-r61` 的发布/Maven/Docker 进程。R61 判定为中断半成品，不复用、不发布。

## R73 Application Release Tooling Contract Sync

USER_AUTHORIZATION: 用户要求继续修复，目标仍是通用、长期可用的一键按钮发布功能；范围覆盖本机应用仓脚本/测试修复、提交和后续新 releaseTag 测试服 app-release 发布验证，不覆盖正式服、审查服、`mark-tested`、`promote-prod`、`promote-backup`、MinIO 数据同步或全量数据库复制。

BDD: 应用发布脚本必须跟随通用 migration metadata 合同 -> Given 应用仓 SQL 使用 `requiresTargetPreflight/applyOrder/sessionProfile/approvedHook/resultAssertion`、evidence-only 类型和 rollback-only 元数据 / When build-release 自动发现并执行应用仓发布合同测试 / Then 应用侧 `publish-int-ruoyi.ps1` 必须解析、校验并投影这些字段，且排除 rollback/evidence-only SQL，不得把旧脚本合同作为 required SQL 阻塞点。

RED: r73-application-tooling-contract -> FAIL。R73 `build-release` 在昂贵 Maven package、Docker、NAS 上传或测试服写入前停止于应用仓自动发现测试：`test_publish_dockerfiles_point_at_current_workspace_artifacts` 仍断言旧 backend Dockerfile CMD，`test_build_release_backend_e2e_fails_fast_without_internal_backend_runtime_base_config` 因应用脚本不识别 `requiresTargetPreflight`、随后把 rollback-only SQL 当 required SQL 而提前失败。

GREEN: r73-application-tooling-contract -> PASS。应用仓 `script/deploy/publish-int-ruoyi.ps1` 已同步通用 metadata 解析与 manifest 投影，支持 `requiresTargetPreflight/applyOrder/sessionProfile/approvedHook/resultAssertion`，并排除 rollback-only 与 evidence-only SQL；测试更新为当前 `INTRUOYI_EXTRA_ARGS` Docker runtime 合同。

REGRESSION: `python -X utf8 -m pytest -q script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_publish_dockerfiles_point_at_current_workspace_artifacts script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_accepts_release_migration_metadata_types_used_by_policy_gate script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_preserves_extended_release_migration_metadata_in_manifest script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_publish_script_excludes_rollback_and_evidence_only_sql_from_required_migrations script\tests\test_publish_int_ruoyi_to_test_tooling.py::test_build_release_backend_e2e_fails_fast_without_internal_backend_runtime_base_config --basetemp .tmp-r73-app-tooling-green2` -> PASS，5 passed。

REGRESSION: `python -X utf8 -m pytest -q script\tests\test_dcc_fvm_matrix_retain_other_completion_sql.py script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_release_target_preflight_files.py --basetemp .tmp-r73-app-tooling-regression` -> PASS，135 passed。

REGRESSION: `python -X utf8 -m pytest -q script\tests\test_release_manifest_migration_contract.py script\tests\test_release_migration_policy_gate.py script\tests\test_release_preflight_plan.py script\tests\test_release_manifest_validator.py --basetemp .tmp-r73-app-release-regression` -> PASS，58 passed；PowerShell AST for `script\deploy\publish-int-ruoyi.ps1` -> PASS。

COMMIT: app release tooling contract sync -> `27f2ceccec7e`。

RESULT: R73 判废且不得复用；失败前未形成完整发布包、未上传 NAS、未写测试服。下一轮必须使用全新 releaseTag `release-20260916-one-button-app-r74`，source freeze 绑定应用仓 `27f2ceccec7e` 之后的 clean HEAD 与维护仓新记录提交后的 clean HEAD。

## 2026-09-16 one-button app-release R74 planner ordering sync

- RED: build-release-r74-schema-rehearsal-order -> FAIL，R74 在 schema rehearsal 中先执行 `20260624_dcc_view_matrix_independent_seed.sql`，随后才计划执行 `20260624_dcc_view_matrix_test_tenant_prereq.sql`，违反 `applyOrder=10/20` 的声明式顺序，未生成 READY 包、NAS 上传或测试服写入。
- ROOT_CAUSE: 应用仓 `script/release/release_preflight_plan.py` 与维护仓 planner 同源缺口一致，依赖拓扑 ready queue 仅按 manifest 原始顺序取项，没有把 `applyOrder` 作为同层优先级；这会让按钮发布在 build preflight、schema rehearsal 与 deploy preflight 之间产生顺序语义漂移。
- BDD: 应用仓 deploy preflight 与维护仓 build preflight 顺序一致 -> Given 迁移声明 `applyOrder` 且依赖关系不冲突 / When 应用仓生成 release preflight plan / Then planner items 必须按 `applyOrder` 排序，并且 dependsOn 始终优先于 applyOrder。
- GREEN: app-planner-ordering-sync -> PASS。应用仓 `script/release/release_preflight_plan.py` 的 ready queue 优先键改为 `applyOrder -> originalOrder`，`script/tests/test_release_preflight_plan.py` 新增 applyOrder 排序与 dependsOn 优先回归。
- REGRESSION: `python -X utf8 -m pytest -q script\tests\test_release_preflight_plan.py script\tests\test_dcc_view_matrix_independent_seed_sql.py script\tests\test_release_target_preflight_files.py --basetemp .tmp-r74-app-preflight-regression` -> PASS，35 passed；`python -X utf8 -m py_compile script\release\release_preflight_plan.py` 与 `git diff --check` -> PASS。
- RESULT: 应用仓需提交本修复后供 R75 source freeze 使用；R74 已判废，不得复用或补包。

## 2026-09-16 one-button app-release R79 route-snapshot runner exit-code contract

- BDD: 路线快照 hook 的进程结果必须只由命令结果决定 -> Given route-snapshot backfill 命令返回 `EXIT_READY` 且 Spring 容器存在无关的非零 `ExitCodeGenerator` / When 发布 hook 启动 runner / Then runner 必须把命令结果 `EXIT_READY` 传给进程退出消费者，不能由 Spring 的聚合退出码覆盖；命令返回非 READY 时仍必须非零失败并暴露报告路径。
- RED: `deploy-release-r79-route-snapshot-runner-exit-code` -> FAIL。真实测试服证据显示 `/var/lib/docker/intruoyi-releases/release-20260916-one-button-app-r79/runtime-reports/route-snapshot-identity-backfill.json` 为 `status=READY`、`blockerCount=0`，但 route runner SSH 返回 exit 1，operation lock=FAILED；版本切换前 `.env` 和实际 backend/frontend 镜像仍为旧 tag。
- ROOT_CAUSE: `MesProRouteVersionSnapshotMigrationRunner` 调用 `SpringApplication.exit(applicationContext, () -> EXIT_READY)`，Spring 会聚合容器中无关的 `ExitCodeGenerator`，使 READY 命令结果被覆盖为 1。该问题不是目标数据缺失，也不能通过放宽发布脚本的非零检查掩盖。
- GREEN: `mvn -f D:\IntRuoyiWorktree\r260911-release-button\a\IntRuoyiBackend\pom.xml -pl yudao-module-mes clean test "-Dtest=MesProRouteVersionSnapshotMigrationCommandTest"` -> PASS，6 tests，0 failures，0 errors；实现直接把 `command.run(...)` 的结果传给 `processExit`，非 READY 继续抛出明确异常。
- REGRESSION: 新增无关 `ExitCodeGenerator` 场景回归，确保 READY 结果不被 Spring 聚合退出码改写；未引入 fallback、未吞异常、未放宽 deploy hook 检查。
- RESULT: R79 判废且不得复用；应用修复提交后使用全新 `release-20260916-one-button-app-r80`，source freeze 必须绑定新的应用提交与维护仓记录提交，继续执行测试服 `app-release/without-data` publish-test。
