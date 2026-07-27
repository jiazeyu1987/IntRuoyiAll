# Execution Log

## User Intent

用户要求按照已确认的根因进行设计、开发和验证：流转关系图“批记录表单”必须读取工序设置中对应工序绑定的正式批记录表单，不能读取表单槽位。

## BDD

- BDD: 关系图显示对应工序的正式批记录表单 -> Given 某路线工序在工序设置中绑定正式批记录表单且 `formBindings` 为空，When 在流转关系图选择“批记录表单”并点击该工序，Then 右侧显示正式批记录表单名称和链接，节点状态为已配置。
- BDD: 表单槽位不能替代批记录表单 -> Given 某路线工序只有 `formBindings` 动态表单槽位且没有正式批记录表单，When 在流转关系图选择“批记录表单”，Then 该工序显示“未配置”且节点状态为缺失。
- BDD: 同一基础工序的路线实例保持独立 -> Given 同一 `processId` 在路线中存在两个 `routeProcessId` 且分别绑定不同批记录表单，When 查看关系图，Then 每个节点只显示自己的正式批记录表单。
- BDD: 特殊节点负责人来源不受影响 -> Given 工序开始配置了 `batchRecordAttachmentOwners`，When 查看或执行特殊节点，Then 上传人仍来自工序开始配置，不受批记录表单和表单槽位改动影响。
- BDD: 仅升版批记录表单也生成正式路线候选绑定 -> Given 已存在唯一工艺路线和激活路线版本，且用户只选择升版“批记录表单”未选择重建工艺流程，When Word 导入生成新批记录版本，Then 系统必须生成或更新路线草稿候选版本，并按现有路线工序顺序写入逐 `routeProcessId` 的 `batchRecordReports`，不得修改 `formBindings` 或直接改写生效路线。
- BDD: 批记录升版保持路线关联 -> Given 来源批记录版本已关联工艺路线，When 创建下一批记录版本，Then 新版本必须保留同一 `routeId`，直至显式路线候选结果更新该关联。
- BDD: 导入页明确确认正式绑定候选 -> Given 预检发现当前批记录对应唯一现有路线，且用户勾选“批记录表单”，When 用户确认升版导入，Then 前端必须明确提示将生成路线候选版本，并提交当前路线 ID、激活版本 ID 和确认标记；不能因为未勾选“工艺流程”而跳过正式绑定候选。

## Preflight

- Skill: `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery`。
- Trigger docs: `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- Branch: `int_main`。
- Git state at task start: local branch diverged from `origin/int_main`; existing dirty changes belonged to concurrent tasks and were preserved before implementation.
- Experience index: present; applicable gates copied to `task.md`.
- Baseline commit `698d6ba3` preserved the initial concurrent workspace changes.
- Follow-up baseline commit `a6714535` preserved concurrent closeout updates that appeared after the first baseline.
- Merge commit `97ecf51a` integrated `origin/int_main`, including the formal three-source terminology contract.

## Milestone Updates

- M1 completed: dirty workspace preserved, remote terminology contract integrated, merge conflicts limited to an unrelated completed task and resolved using its pushed completion evidence.
- M2 in progress: adding backend draft-snapshot regression tests and a focused frontend three-source isolation contract before production changes.

## Root Cause Evidence

- `MesProRouteFlowConfigServiceImpl.normalizeCandidateUseConfigSnapshot` currently calls `processConfig.setBatchRecordReports(Collections.emptyList())`.
- The same save path sets `batchRecordBindingSnapshotExplicit=true`; draft reads then select the emptied snapshot before current bindings.
- `getRouteVersionSnapshotFlowProcessConfigList` loads report metadata only from current bindings whenever the candidate is readable, so explicit snapshot reports cannot resolve their names.
- `RouteFlowGraphDesigner.vue` currently merges `selectedRecordBindings` and `selectedLegacyBatchRecords` for the `batchRecordFormNames` value, links, and node status.
- 路线 `922119` 的 V18 草稿 14 个 `batchUseConfigs` 均为 `batchRecordReports=[]`，当前正式绑定表和路线工序旧平铺字段也均为空；同时当前批准的批记录版本 `130 / V14.0` 有 15 张报表但 `route_id` 已为空。
- `confirmWordImportUpgradeSelections` 仅在选中工艺流程产品时设置 `routeUpgradeConfirmed` 和 `expectedRouteId/expectedRouteVersionId`；只勾选“批记录表单”不会生成路线候选。
- `MesProBatchRecordReportServiceImpl#createPrecheckVersion` 只写 `sourceRouteId`，没有继承来源版本 `routeId`。
- `MesProBatchRecordReportServiceImpl` 仅在 `routeRebuildRequested=true` 时调用路线生成服务；批记录单独升版不会持久化逐工序正式绑定。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 2 个回归用例按预期失败：
  - 草稿保存后的 `batchRecordReports=[]`，证明正式批记录表单被清空。
  - 同一 `processId` 的两个 `routeProcessId` 可读到各自报表 ID，但报表名称为 `null`，证明显式草稿快照元数据未加载。
- RED: `node tests/e2e/mes-route-flow-batch-record-form-source-static.spec.js` -> FAIL，当前不存在只读取 `selectedLegacyBatchRecords` 的正式批记录表单专用构建函数。

## 2026-07-28 Verification And Runtime Evidence

- Delegation review: 侧线程同步要求最终证据必须覆盖正式链路，不得只用手工 API 保存 V19 证明完成；同时指出可疑回归区间 `d083d962` 和 `229fad52`。已纳入根因和验证记录。
- DB schema: 本地 Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro` 执行 `20260727_mes_route_version_snapshot_mediumtext.sql` 后，`SHOW COLUMNS FROM mes_pro_route_version LIKE "route_snapshot_json"` 返回 `mediumtext`。
- Runtime API before repair: V19 `routeVersionId=390`，`flow-config?routeId=922119&useType=BATCH&routeVersionId=390` 返回 `rows=14`、`reportRows=0`、`totalReports=0`、`formBindingRows=4`。
- Runtime API formal save: 使用登录态正式接口 `/mes/pro/route/flow-config/batch-record/save`，按 V14.0 `batchRecordVersionId=130` 的 MAIN 报表 `sourceTableIndex=2..15` 映射到 V19 14 个路线工序；保留 `formBindings`，未直接 SQL 写业务绑定。
- Runtime API after repair: V19 返回 `rows=14`、`reportRows=14`、`totalReports=14`、`formBindingRows=4`；前三行示例为 `粗洗工序生产记录`、`精洗工序生产记录`、`清洗工序生产记录`。
- Real E2E: `http://127.0.0.1:8086` 真实页面打开 `RT000028` V19 草稿流转关系图，点击“批记录表单”后，粗洗、精洗、大包装工序均显示正式生产记录名称，且 `MES` 写请求数为 `0`。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_route_version_snapshot_mediumtext_sql.py -q` -> PASS, `3 passed`。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-route-flow-batch-record-form-source-static.spec.js` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-batch-record-import-formal-route-binding-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionLifecycleSchemaTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordRouteGovernanceContractTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `154` tests。
- GREEN: `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS, `BUILD SUCCESS`。
- REGRESSION BLOCKER (unrelated): `pnpm ts:check` still fails in `src/views/form-center/template/index.vue` for missing template workspace/simulation properties.
- REGRESSION BLOCKER (unrelated): `pnpm build:local` still fails in `src/views/form-center/template/index.vue (624:3): Invalid end tag`.
- GREEN: `experience-preflight` -> PASS，已将“批记录表单正式逐工序来源、只升版批记录表单也生成路线候选、`route_snapshot_json` 需支持大快照、前端不得用表单槽位补空”的长期经验合并到 `docs/backend-development.md`、`docs/frontend-development.md`、`docs/e2e-rules.md` 和 `docs/experience-index.md`。
- GREEN: `git diff --check` -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`frontend 8086, backend 48086`。
- GIT: implementation commit `e95b2449 fix: preserve formal batch record route bindings`，文件清单见 `git show --name-status --oneline e95b2449`。
- Merge: `origin/int_main` 合入当前分支时 `MesProBatchRecordRouteGenerationServiceImpl.java` 与主线重建路线保留连接信息逻辑冲突；处理结果为“只升版批记录表单”继续生成 DRAFT 候选，“勾选工艺流程重建”按主线要求重建当前路线并映射保留 `next_process_id/link_type/prepare_time/wait_time/color_code/key_flag` 与人工边。
- RED: 合并后 `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionLifecycleSchemaTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordRouteGovernanceContractTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，主线新增用例 `recognizeUploadedRoute_whenUpgradingRoute_preservesStableProcessConnectionInfo` 暴露 `applyExistingRouteRebuild` 未生效。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#recognizeUploadedRoute_whenUpgradingRoute_preservesStableProcessConnectionInfo" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: 合并后 `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionLifecycleSchemaTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordRouteGovernanceContractTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`154` tests。
- GREEN: 合并后 `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS，`BUILD SUCCESS`。
- GIT: merge commit `ffbdbd49 merge: sync int_main before batch record binding closeout`；`git merge-base --is-ancestor int_main HEAD` 与 `git merge-base --is-ancestor origin/int_main HEAD` 均返回 `0`。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260727-route-flow-batch-record-form-source --mode preview` -> BLOCKED；keep 包含 `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`、`database-schema-evidence.md`，delete 为空，唯一 blocker 为主 worktree `E:\IntRuoyi` 存在外部脏改动，不能接收 ff-only merge。

## Blockers

- Closeout apply / worktree removal blocked by dirty main worktree `E:\IntRuoyi`。只读 `git status --short --branch` 显示外部改动包括 `MesProBatchRecordReportLayoutCalibrator.java`、`MesProBatchRecordRouteBRecognizer.java`、`IntRuoyiFronted/package.json`、`src/views/mes/pro/task/calendar/index.vue` 以及若干其它任务目录；本任务未修改这些主 worktree 文件。
