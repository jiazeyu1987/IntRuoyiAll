# Execution Log

## Setup
- Worktree: `D:\IntRuoyiWorktree\20260908-route-process-config-ownership-impl`
- Branch: `codex/20260908-route-process-config-ownership-impl`
- Runtime profile: `int_main`, slot `34`, frontend `8209`, backend `48209`

## BDD
- BDD: 路线候选版本维护工序生产配置 -> Given 工艺路线存在候选版本 When 工艺人员维护损耗原因、超产比例、设备映射、设备参数标准 Then 配置写入候选版本快照和版本投影，不再写入生产组长运行配置。
- BDD: 发布与活跃订单冻结 -> Given 路线版本包含生产配置 When 生产工单进入活跃订单 Then 每个工序冻结统一生产配置快照、损耗原因快照、超产比例快照和哈希。
- BDD: 一线运行只读快照 -> Given 活跃订单工序已冻结生产配置 When 一线生产填写报工 Then 只能使用冻结的设备、参数标准和损耗原因，缺失快照直接报错。
- BDD: 识别同步全量覆盖候选 -> Given 导入 JSON 或 Word 识别出设备参数 When 同步到项目 Then 覆盖该项目候选路线版本的全部设备参数配置，不写设备主数据。
- BDD: 删除生产组长工序配置入口 -> Given 生产组长打开工作台 When 查看页签 Then 不再显示“工序配置”，相关写接口不再作为维护入口。

## Evidence
- 2026-09-08: 已读取 `docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`。
- 2026-09-08: 已读取后端、前端、数据库技能约束，按严格 TDD 从数据库/快照结构测试开始。
- RED: `mvn -q -pl yudao-module-mes -am "-Dtest=MesRouteProductionProcessConfigSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 缺少 `MesProRouteProcessLossReasonDO`、活跃订单工序快照缺少统一生产配置字段和迁移 SQL。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesRouteProductionProcessConfigSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- RED: `mvn -q -pl yudao-module-mes -am "-Dtest=MesRouteProductionProcessConfigSchemaTest,MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 路线版本完整快照校验新增 `productionProcessConfigSchemaVersion` 与 `productionProcessConfigs` 后，旧测试快照未补齐新必填配置。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesRouteProductionProcessConfigSchemaTest,MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesRouteProductionProcessConfigSchemaTest,MesProRouteCandidateConfigServiceTest,MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -q -pl yudao-module-mes -am "-DskipTests" compile` -> PASS
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesProRouteVersionPublishProjectionServiceTest,MesProRouteVersionPublishProjectionServiceImplTest,MesProRouteVersionSnapshotHashMigrationTest,MesProRouteVersionSnapshotResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- RED: `mvn -q -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_usesFrozenActiveOrderDeviceAndLossReasonSnapshotsWithoutCurrentProcessConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 一线活跃订单运行配置仍依赖当前生产组长工序设备绑定，未从活跃订单冻结设备和损耗原因快照恢复运行选项。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_usesFrozenActiveOrderDeviceAndLossReasonSnapshotsWithoutCurrentProcessConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesRouteProductionProcessConfigSchemaTest,MesProRouteCandidateConfigServiceTest,MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- RED: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> FAIL, expected reason: 未带 `NODE_OPTIONS=--max-old-space-size=8192` 时前端类型检查内存不足。
- GREEN: `pnpm install` -> PASS, worktree 前端依赖已按 lockfile 安装。
- GREEN: `pnpm ts:check` -> PASS
- GREEN: `node tests\e2e\route-production-config-ownership-static.spec.cjs` -> PASS
- GREEN: `pnpm ts:check` -> PASS，工艺路线流转图生产配置编辑入口类型检查通过。
- GREEN: `node tests\e2e\route-production-config-ownership-static.spec.cjs` -> PASS，覆盖生产组长页签删除、路线生产配置入口、前端路线配置 API、一线活跃订单冻结设备/损耗快照读取。
- GREEN: `mvn -q -pl yudao-module-mes -am "-DskipTests" compile` -> PASS
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRecognitionDeviceSyncServiceTest,MesFrontlineRuntimeConfigServiceTest,MesRouteProductionProcessConfigSchemaTest,MesProRouteCandidateConfigServiceTest,MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，覆盖 JSON/Word 识别同步候选路线版本、冻结快照和一线运行读取。
- GREEN: `node tests\e2e\route-production-config-ownership-static.spec.cjs` -> PASS，新增锁定旧版无页签布局不再加载工序配置接口，且识别同步写入候选路线版本生产配置快照。
- GREEN: `pnpm ts:check` -> PASS，生产组长旧工序配置加载关闭后类型检查通过。
- GREEN: `git diff --check` -> PASS。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`codex/20260908-route-process-config-ownership-impl/int_main` 使用 frontend 8209、backend 48209。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260908-route-process-config-ownership-impl\backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260908-route-process-config-ownership-impl\frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260908-route-process-config-ownership-impl\database-schema-evidence.md` -> PASS。
- RED: `mvn -q -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRecognitionDeviceSyncServiceTest,MesFrontlineRuntimeConfigServiceTest,MesRouteProductionProcessConfigSchemaTest,MesProRouteCandidateConfigServiceTest,MesProRouteVersionWorkflowServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 全模块 testCompile 暴露旧生产组长控制器测试仍引用已迁移删除的 `process-config` 设备接口，报工分配旧测试未提供活跃订单冻结超产比例快照。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderControllerReleaseExceptionTest,MesReportAllocationCommandServiceTest,MesReportAllocationConcurrencyTest,MesReportAllocationFrontlineSnapshotGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，旧控制器测试已同步为生产组长只保留运行类维护入口，报工分配测试已按冻结快照提供超产比例。
- GREEN: `mvn -q -pl yudao-module-mes -am "-Dtest=MesProBatchRecordRecognitionDeviceSyncServiceTest,MesFrontlineRuntimeConfigServiceTest,MesRouteProductionProcessConfigSchemaTest,MesProRouteCandidateConfigServiceTest,MesProRouteVersionWorkflowServiceTest,MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderControllerReleaseExceptionTest,MesReportAllocationCommandServiceTest,MesReportAllocationConcurrencyTest,MesReportAllocationFrontlineSnapshotGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -q -pl yudao-module-mes -am "-DskipTests" compile` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `pnpm build:local` -> PASS。
- GREEN: `node tests\e2e\route-production-config-ownership-static.spec.cjs` -> PASS。
- GREEN: `git diff --check` -> PASS。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`codex/20260908-route-process-config-ownership-impl/int_main` 使用 frontend 8209、backend 48209。
- GREEN: backend/frontend/database evidence validators -> PASS。

## Completed Work
- 新增 `mes_pro_route_process_loss_reason` 路线版本损耗原因投影 DO、Mapper、MySQL 迁移和 H2 测试表结构。
- 活跃订单工序快照增加损耗原因、超产比例、统一生产配置、哈希和迁移来源字段。
- 路线版本完整快照新增 `productionProcessConfigSchemaVersion` 与 `productionProcessConfigs` 必填校验。
- 活跃订单冻结时写入统一生产配置快照；快照来源固定标记 `ROUTE_VERSION`，不再从生产组长运行配置表补齐设备或参数。
- 一线活跃订单运行配置改为读取冻结设备选择、设备参数、损耗原因快照，提交前运行选项不再依赖生产组长当前工序配置。
- 新增路线版本生产工序配置查询与保存 API，前端 API 层已对接。
- 生产组长多模块页签中移除“工序配置”可见入口。
- 工艺路线流转图新增“生产配置”关注字段，可在候选版本中编辑当前路线工序的生产配置 JSON 并保存到路线版本 `productionProcessConfigs`。
- JSON/Word 识别同步改为全量覆盖候选路线版本 `productionProcessConfigs` 中的设备组和参数规则，保留同一候选中的损耗原因和超量比例，不再写生产组长运行配置。
- 生产组长旧版无页签布局不再展示或初始化加载“工序配置”模块，避免旧接口迁移后产生隐藏运行错误。
- 旧控制器测试移除生产组长 `process-config` 设备映射和设备参数接口断言；这些配置现由工艺路线候选版本维护。
- 报工分配旧测试改为显式提供活跃订单冻结超产比例快照，避免用旧 `MesTeamLeaderOverageLimitService` 作为运行时来源。


## 2026-09-08 Authorized Real E2E And Closeout
- AUTHORIZATION: 用户授权执行真实页面 E2E，并在通过后提交和推送当前 worktree 分支。
- RUNTIME: 48209 后端 PID 64812 归属当前 worktree，health 为 `UP`；8209 前端 HTTP 200。
- MIGRATION: 已对本地运行库执行正式迁移 `IntRuoyiBackend\sql\mysql\20260908_mes_route_production_process_config_snapshot.sql`；回读确认 `mes_pro_route_process_loss_reason` 存在，活跃订单工序快照 7 个新增生产配置冻结字段存在。
- GREEN: `node --check doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS。
- GREEN: `node doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS，真实页面使用 `http://127.0.0.1:8209`、租户 `芋道源码`、账号 `admin`；生产组长页签不再显示 `工序配置`，工艺路线候选版本编辑页可见 `生产配置` 配置项，相关请求 HTTP 200，consoleErrors/pageErrors 均为空。
- GREEN: `git diff --check` -> PASS。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。

## 2026-09-08 Final E2E Rerun Before Push
- RED: `node doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> FAIL, expected reason: 任务目录一次性 E2E 脚本直接 `require('playwright')`，Node 按脚本目录解析依赖，未使用 `IntRuoyiFronted\node_modules\playwright`。
- GREEN: `node --check doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS，脚本已改为从前端依赖目录显式加载 Playwright。
- GREEN: `node doc\tasks\20260908-route-process-config-ownership-impl\route-production-config-real.e2e.cjs` -> PASS，真实页面 `http://127.0.0.1:8209`、租户 `芋道源码`、账号 `admin`；生产组长页签为 `人员管理 / 报工管理 / 报工历史 / 活跃订单池`，不显示 `工序配置`；工艺路线首行 `RT000028-IDI / 按压式球囊扩充压力泵`，进入候选版本 `V13 草稿` 后可见 `生产配置`；相关请求 HTTP 200，consoleErrors/pageErrors 均为空。
- GREEN: `git diff --check` -> PASS。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`codex/20260908-route-process-config-ownership-impl/int_main` 使用 frontend 8209、backend 48209。
- EXPERIENCE: 已按 project-experience-consolidation 检查长期经验归宿；`docs\e2e-rules.md` 已有“任务目录下 E2E 脚本必须显式从 IntRuoyiFronted/node_modules/playwright 加载”的规则，本次不新增长期经验文档。

## 2026-09-08 Commit Push And Cleanup Preview
- COMMIT: `53668905fad0fb32daecf5ccaaba176cee7092c8` / `feat: migrate production process config to route version`，包含路线版本生产配置迁移实现、前后端测试、静态 E2E 合同和保留任务文档。
- PUSH: `git push origin codex/20260908-route-process-config-ownership-impl` -> PASS，远端 `refs/heads/codex/20260908-route-process-config-ownership-impl` 指向 `53668905fad0fb32daecf5ccaaba176cee7092c8`。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260908-route-process-config-ownership-impl --mode preview` -> BLOCKED；保留 `task.md`、`execution-log.md`、`verification-report.md`，候选删除 task-local evidence / E2E 临时脚本 / e2e-artifacts。
- BLOCKER: cleanup apply 暂不执行，因为主工作区 `E:\IntRuoyi` 有其它任务脏改动，且当前分支不能 fast-forward merge 到 `int_main`；为避免影响并行任务，保持当前 worktree 和端口槽位不清理。
