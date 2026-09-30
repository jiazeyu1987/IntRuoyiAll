# Verification Report

## 2026-09-27 Final E2E Verification Addendum

- **真实生命周期矩阵通过**：上传免培训、升版免培训、上传带培训、升版带培训、作废五条真实 Playwright 页面链路均已完成。每条链路的业务动作均由真实前端页面完成；没有用 API/DB 代替上传、检出/检入、审批、培训记录、分发或文控签名。
- **上传带培训**：文件 `2054545668044084023`，`needTraining=true`，真实页面完成会签、批准、培训记录上传、手工分发、文控审核，最终 `ACTIVE`；培训记录使用任务目录中的 `test-signature-admin-20260926.png` 作为测试环境任务材料，提交响应业务码 `0`。
- **升版带培训**：文件 `2054545668044084024`，B/1 真实检入时 checkbox 保持 `needTraining=true`；真实页面完成会签、批准、培训记录上传、手工分发和文控审核，最终 B/1 `ACTIVE`、A/1 `SUPERSEDED`、master 指向 B/1，发布/盖章件均可用。只读 Playwright 复核：`needTraining=true`、`currentActiveVersionNo=B/1`、B/1 `ACTIVE`、A/1 `SUPERSEDED`。
- **作废**：文件 `2054545668044084022` 真实作废闭环 PASS，详情显示 `OBSOLETE` 和作废原因；统一审批中心作废待办动作投影为 `APPROVE/REJECT`，`DccApprovalTaskAdapterTest` 25/25 PASS。
- **代码回归**：最终 BPM+DCC 定向组合回归 294 tests，0 failures、0 errors、0 skipped；前端证据索引 52/52 PASS；额外审批中心/作废/路线静态契约 7/7 PASS；`pnpm ts:check` PASS；子 Agent 修复了作废服务一次路线解析一致性和两条过期静态合同。
- **剩余门禁**：任务仍保持 `in_progress`，仅剩授权 Git commit/push 和 cleanup closeout；本报告不把它们误标为 E2E 失败。

## 2026-09-25 Permission Merge and B/1 Runtime Addendum

- 修复了合并菜单后的真实权限断链：独立 `DCC访问规则` 菜单已退役，目录授权接口与前端按钮原先仍只识别 `dcc:controlled-file:access-rule:manage`；现在四个目录授权端点接受旧权限或 `dcc:controlled-file:category:manage`，前端新增/保存按钮使用新的文控权限入口。
- TDD 证据：`DccDirectoryControllerTest` 先以旧注解取得 RED，修复后 11 tests PASS；`dcc-permission-tabs-merge-static.spec.js`、`dcc-permission-unified-list-template-static.spec.js`、`dcc-access-rule-menu-retire-static.spec.js` 和 `pnpm ts:check` 均 PASS。
- 真实运行态：任务专用后端 `48062` 重建并恢复 health `UP`，OnlyOffice `8080/healthcheck` healthy；共享 `8061/48061` 未触碰。
- 真实 UI：目录 `质量管理/4.Ohter` 的 `USER=admin` 内容查看规则通过文控权限页面保存并回读成功，未直接写库。
- B/1 状态：只读数据库确认 `2054545668044084018` 为 `ACTIVE`、A/1 `2054545668044084017` 为 `SUPERSEDED`，发布与盖章文件均存在；真实项目代码页面已为 admin 创建 `SELECTED_FILES` assignment，随后真实受控浏览页面可见 B/1 `ACTIVE`、版本 `B/1`、发布与盖章文件均已生成。该页面验证通过，但不替代其余完整三流程 E2E 与 Git 收尾门禁。
- 新增运行态错误语义修复：对象存储中缺失签名图片时，route readiness 不再把 `NoSuchKeyException` 暴露为 HTTP 500，而是返回业务码 0、`ready=false` 和 `APPROVER_SIGNATURE_IMAGE_INVALID`。定向 `DccControlledFileRouteReadinessServiceTest` 4 tests PASS；真实上传预检已验证系统异常消失，剩余 blocker 精确收敛为签名图片材料缺失。
- 真实 UI 已将两个任务部门负责人切换为 `zhaojie/赵杰`，并通过用户页面补齐“项目经理”系统岗位；其 `wenkong` 角色和既有电子签名授权保留，但对象存储中的签名图仍无效，因此完整上传 E2E 继续按业务资格 blocker 停止，不用伪造图片绕过。

## 2026-09-23 Continuation Addendum

- 配置保存已完成：租户 1/分类 `908710` 的 `NEW`、`REVISION`、`OBSOLETE` 路线均已保存；会签矩阵指定任务专用生产部和质量部，两个部门的唯一负责人均为管理员用户 `1`。
- 代码回归已通过：`DccThreeWorkflowBpmnMigrationTest` 3 tests，0 failures/errors/skips。
- 真实数据库迁移已完成并重复执行验证：三套流程的两个租户均启用 v2、停用 v1；v2 的批准/文控审核节点为候选策略 `34`，会签节点为 `35`；重复执行无重复记录。
- 真实前端上传已通过：管理员通过 Playwright 页面提交培训选项为 `true` 的新上传，业务码 `0`，受控文件 ID `2054545668044084015`，运行态状态 `PENDING_MATRIX_REVIEW`，首个待办为 `MATRIX_REVIEW`，流程定义为 `dcc-controlled-file-upload:2`。
- 真实页面第一项会签已通过并推进到批准；旧运行进程在批准后进入 receiveTask 等待阶段时错误返回“受控文件任务阶段与固定 DCC 流程不一致”。源码已修复该等待阶段状态同步缺陷，并通过新增回归测试及 148 项组合回归；当前 PID `50264` 仍是修复前进程，未按运行限制重启。
- 因此此前“候选人为空”和“负责人/路线未配置”已不再是当前阻塞。当前仍未声称完整 E2E 通过：需要加载修复后构建后继续批准、培训、分发、文控审核、升版、作废及权限/历史页面链路；任务状态保持 `in_progress`。

## 2026-09-23 Current Verification State

- Design-document evidence wording was reconciled: the acceptance plan now records partial real-page attempts without calling them a full E2E pass, and the technical design explicitly limits the recorded migration/Flowable evidence to the approved local Docker test database, not production. Documentation verifier passed (5 files, 13 links, 22 source files, 35 anchors); TDD compliance and scoped whitespace checks passed.
- Backend BPM+DCC core aggregation was freshly rerun: 201 tests passed, 0 failures/errors/skips, all 23 Maven reactor modules succeeded.
- Frontend static contracts: 52 scripts passed; `pnpm ts:check` passed. A fresh `pnpm build:local` invocation did not complete observably: it remained silent while concurrent Vite build processes existed and was interrupted. Do not treat that attempt as a pass.
- Frontend production build rerun: after confirming the competing build processes had ended, `pnpm build:local` completed with exit code 0 and `Build successful. Please see dist directory`. Vite CJS deprecation and stale Browserslist database warnings remain non-fatal.
- Frontend source-contract rerun: all 52 scripts indexed in `frontend-evidence-index.json` passed against the current source; `pnpm ts:check` passed. This remains development evidence, not real-page E2E.
- Backend core aggregation rerun on 2026-09-23: after an environment-only `JAVA_HOME` failure before tests, the repository JDK 17 rerun passed with BPM 14 + DCC 187 = 201 tests, 0 failures/errors/skips, and 23 Maven modules SUCCESS.
- Existing local runtime was checked read-only: frontend `8061` and backend `48061` responded with HTTP 200. Neither process was restarted.
- Real admin-page configuration was saved and then checked read-only: tenant 1/category `908710` has active `NEW` version 5, `REVISION` version 4, and `OBSOLETE` version 4. Each route's `MATRIX_REVIEW` stage references exactly task-owned departments `910333` and `910334`; both departments resolve to the single configured owner user `1` (`admin`/`瑛泰管理员`). The previous route/owner configuration blocker is cleared.
- Earlier authorized DB migration and formal Flowable deployment-table evidence remains documented in the dated execution records. The route and department-owner saves in this continuation were performed through the real admin UI; database inspection was read-only.
- Full Playwright business-flow acceptance remains incomplete. The admin account now completed the real upload preview and submit path successfully after the v2 BPMN migration, but the remaining approval, training, distribution, document-control, revision, obsolete, permission and history paths have not all been completed. No API or database write was used as a substitute for those page actions.
- Git closeout was not performed in this continuation. Task status remains `in_progress`; cleanup, commit, push, and completion are not claimed.

## Scope

本轮实现并验证 P1 的第一批代码切片：

- `candidateSourceType=DEPT` 可解析部门负责人。
- 缺负责人、部门缺失或重复返回时失败，不创建部分有效结果。
- 同一负责人负责多个部门时，解析结果保留重复 userId，避免在会签义务进入 BPM 前被 `Set` 合并。
- BPM 三个新流程 key 纳入 DCC 严格候选校验。
- BPM 通用审批/驳回守卫识别三个新流程 key，避免绕过 DCC 签名入口。
- 后端路线保存 VO/API 支持多部门 `candidateSourceIds`，并保留首个 `candidateSourceId` 投影。
- 前端路线表单支持 `DEPT` 候选来源的部门多选，列表和预览按部门名称回显。
- DCC 审批中心适配器识别 upload/revision/obsolete 三个新 key，并保留旧 approval、旧 FormCenter obsolete、external review 兼容查询。
- DCC native 状态监听识别旧 approval、upload、revision，继续排除 obsolete/FormCenter 作废，避免重复效果执行。
- 前端 workflow API 暴露三新 key 常量；工作台直接 BPM 查询补入 upload/revision/obsolete，作废流程使用 BPM 响应 `businessObjectId` 定位受控文件，不把 `FORM_ACTION` businessKey 当文件 ID。
- 工作稿送审支持 `needTraining` 进入后端幂等 payload、文件记录和 BPM 变量；REVISION 工作稿送审选择 revision processDefinitionKey。前端工作稿送审请求传递当前版本 `needTraining`。
- 升版检入弹窗支持选择 `needTraining`，后端新 WORKING 版本按检入请求保存该值，后续工作稿送审沿用该版本事实。
- 路线主表增加 `actionType`；上传、升版、作废可分别按 NEW/REVISION/OBSOLETE 读取独立激活路线，缺 action 路线时报错，不 fallback 到 legacy 路线。
- 上传/升版创建审批实例时按流程 key 映射 actionType；NEW/REVISION action 路线首节点为 MATRIX_REVIEW 时，文件初始状态进入 `PENDING_MATRIX_REVIEW`。
- BPM 发起变量按实际首节点拆分：`startUserSelectAssignees` 只放首节点会签，`approveUserSelectAssignees` 放批准与文控审核后续节点。
- 管理端路线保存、分页、预览和前端路线配置表单支持 `actionType`；LEGACY 使用旧四阶段，NEW/REVISION/OBSOLETE 使用三节点 action 策略且首节点必须是 DEPT 会签。
- 作废发起入口在请求未显式传 `startUserSelectAssignees` 时按 OBSOLETE actionType 解析路线，不再由原文件的 upload/revision/legacy processDefinitionKey 决定作废候选。
- 作废请求合同显式拒绝 `needTraining`，即使全局 JSON mapper 允许未知字段，也不能把作废请求中的培训选项静默吞掉。
- 上传/升版提交合同显式拒绝 `selectedSignoffUserIds`，即使旧 VO 仍暂时暴露该字段，也不能让客户端手选会签人；三流程会签人只能由 action 路线矩阵部门和任务创建时部门负责人配置解析。
- 三套独立 BPMN seed migration 完成静态合同验证：upload/revision 包含会签、批准、`needTraining` 条件培训、分发、文控审核；obsolete 包含会签、批准、文控审核且不包含培训或分发。
- 三流程 BPMN seed migration 通过 release migration policy gate，依赖闭包为 `20260513_dcc_base_schema` -> `20260921_dcc_category_approval_route_action_type` -> `20260922_dcc_three_workflow_bpmn_seed`。
- 逐部门义务快照落库：新增 `dcc_controlled_file_task_assignee_snapshot`、DO/Mapper、base/test schema 和增量 migration；上传/升版提交时对 DEPT 节点保存每部门一条义务快照，同一负责人负责多个部门时保留多条 `departmentId/obligationId`。
- 义务快照 migration 通过 release migration policy gate，依赖闭包延伸到 `20260922_dcc_task_assignee_snapshot`。
- BPM 多实例创建侧保留 DCC 同人多部门候选列表：DCC 三个新流程在 `START_USER_SELECT` / `APPROVE_USER_SELECT` 节点读取流程变量原始 List，`[A, A]` 不再被公共 `Set` 合并；并行/串行多实例 behavior 改为使用 List collection variable。
- BPM runtime 任务与 DCC 逐部门义务快照完成绑定切片：DCC 启动流程时传入 stage -> obligationId 列表；BPM 多实例任务创建时按 `loopCounter` 写入 task local obligation 变量；DCC 审批校验时按 `obligationId` 将 `bpmTaskId/nodeInstanceId` 回写到对应义务快照，且更新数量不是 1 时 fail fast。
- 上传/升版培训与分发等待节点运行态连接：BPMN seed 中 `TRAINING`/`DISTRIBUTION` 改为 receiveTask；DCC 上传培训记录完成后触发 `TRAINING` 并进入待分发；DCC 手动分发完成后触发 `DISTRIBUTION` 并进入文控审核；未勾选培训时分发放行不要求培训确认，旧公共流程保持原行为。
- 文控审核终态闭环单元级验证：upload/revision 新流程从 `PENDING_DOC_CONTROL_REVIEW` 审核完成后可进入既有 ordinary finalization 并受控生效；obsolete 流程 key 由 FormCenter action/审批定义使用，DCC 通用 listener 不消费 obsolete key，`FORM_ACTION:*` businessKey 仍被 listener 排除，作废效果只由 `DccControlledFileObsoleteFormEffectExecutor` 执行。

## Commands

- Initial RED/environment: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest" test` -> FAIL, `mvn` not found on PATH.
- Initial RED/environment: repository Maven without `JAVA_HOME` -> FAIL, `The JAVA_HOME environment variable is not defined correctly`.
- Initial RED/command-boundary: repository Maven with `-am` and specified tests but without `surefire.failIfNoSpecifiedTests=false` -> FAIL in upstream modules with no matching test.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm -am "-Dtest=BpmDccRequiredCandidatesTest,BpmTaskExternalSignatureGuardTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- GREEN: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs`.
- Initial RED/environment: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` -> FAIL, Node heap out of memory.
- GREEN: `pnpm ts:check`.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest#checkinRealSourceCreatesWorkingA2AndLeavesA1FormalPointerUnchanged" test` -> FAIL at test compile because `DccControlledFileCheckinReqVO` had no `needTraining`.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileQueryServiceTest#checkinRealSourceCreatesWorkingA2AndLeavesA1FormalPointerUnchanged,DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables" test` -> PASS, 2 tests.
- GREEN: `pnpm ts:check`.
- Initial RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest,DccControlledFileStatusListenerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, null processDefinitionKey caused NPE in FormCenter obsolete key check.
- GREEN: same Maven command after null-key fix.
- GREEN: `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs`.
- GREEN: `pnpm ts:check`.
- RED/baseline: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceConvertTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before target test because upstream `yudao-module-infra` test compile references missing `FileServiceImpl.updateUploadTime(...)`.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm "-Dtest=BpmProcessInstanceConvertTest" test` -> FAIL, new objectId projection test exposed missing explicit `businessKey` mapping from `HistoricProcessInstance`.
- GREEN: same BPM convert test command after explicit `businessKey` and `businessObjectId` mapping.
- GREEN: `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs`.
- GREEN: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs`.
- GREEN: `pnpm ts:check`.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables" test` -> FAIL at test compile because `DccControlledFileSubmitIterationReqVO` had no `needTraining`.
- RED/test-fixture: same target test after implementation -> FAIL because the new fixture lacked the current ACTIVE baseline required by existing revision-advance validation.
- GREEN: same target DCC workflow test after fixture completion.
- RED/frontend: `pnpm ts:check` -> FAIL because `ControlledFileBrowserVersion` did not expose `needTraining`.
- GREEN: `pnpm ts:check`.
- PASS: scoped `git diff --check` for changed source, tests and task docs.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_usesUploadActionRouteAndStartsAtMatrixReview" test` -> FAIL at test compile because `DccCategoryApprovalRouteDO` had no `actionType` and mapper had no action-specific selector.
- GREEN: same upload action route target after adding route action type, action route policy, first-node status mapping and fixture completion -> PASS, 1 test.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest#submitControlledFile_usesUploadActionRouteAndStartsAtMatrixReview" test` -> PASS, 12 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables,DccControlledFileQueryServiceTest#checkinRealSourceCreatesWorkingA2AndLeavesA1FormalPointerUnchanged" test` -> PASS, 2 tests.
- RED/regression: broad DCC regression initially failed 11 workflow tests, then 4, then 1 because legacy-only fixtures and old assertions still expected the previous common route shape.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 176 tests.
- PASS: repository `git diff --check` -> no whitespace errors; output contained only existing LF/CRLF warnings from the dirty worktree.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest#testSaveRoute_actionTypeNewPersistsActionRouteAndKeepsLegacyActive,DccApprovalRouteAdminServiceImplTest#testPreviewRoute_actionTypeNewUsesActionRouteNotLegacy,DccApprovalRouteAdminServiceImplTest#testSaveRoute_actionTypeSignoffRejectsUserCandidate" test` -> FAIL at test compile because route save/preview VO had no `actionType`.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest" test` -> PASS, 29 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest" test` -> PASS, 139 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 179 tests.
- GREEN: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs` -> PASS after actionType static contract extension.
- GREEN: `pnpm ts:check` -> PASS.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileObsoleteServiceTest#obsoleteControlledFile_derivesAssigneesFromObsoleteActionRouteRegardlessOriginalProcessKey" test` -> FAIL at test compile because `DccControlledFileApprovalRouteAssigneeResolver` had no actionType-specific `resolveStartUserSelectAssignees(...)`.
- GREEN: same obsolete target test after adding actionType overload and using OBSOLETE actionType in the obsolete entry -> PASS, 1 test.
- RED/regression: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileObsoleteServiceTest" test` -> FAIL 1 old fixture still stubbed the legacy two-argument resolver.
- GREEN: same obsolete service class after fixture update -> PASS, 8 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileObsoleteServiceTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 187 tests.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccThreeWorkflowBpmnMigrationTest" test` -> FAIL before migration implementation because `sql/mysql/20260922_dcc_three_workflow_bpmn_seed.sql` did not exist.
- GREEN: same `DccThreeWorkflowBpmnMigrationTest` command after adding the migration -> PASS, 1 test.
- RED/release-gate: `run-release-migration-policy-gate.py --sql-file 20260922_dcc_three_workflow_bpmn_seed.sql` -> FAIL because dependency `20260921_dcc_category_approval_route_action_type` was missing from the input set.
- RED/release-gate: policy gate with actionType migration included -> FAIL because `20260921_dcc_category_approval_route_action_type.sql` lacked release metadata.
- GREEN: `run-release-migration-policy-gate.py --sql-file 20260513_dcc_base_schema.sql --sql-file 20260921_dcc_category_approval_route_action_type.sql --sql-file 20260922_dcc_three_workflow_bpmn_seed.sql --output doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-three-workflows.json` -> PASS, `migrationCount=3`.
- GREEN: surefire report check for combined DCC regression -> PASS, 188 tests across `DccThreeWorkflowBpmnMigrationTest`, `DccControlledFileApprovalRouteAssigneeResolverTest`, `DccControlledFileObsoleteServiceTest`, `DccApprovalRouteAdminServiceImplTest`, `DccControlledFileWorkflowServiceImplTest`.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup" test` -> FAIL at test compile because `DccControlledFileTaskAssigneeSnapshotDO/Mapper` did not exist.
- GREEN: same target after adding task assignee snapshot DO/Mapper/schema and submit-time persistence -> PASS, 1 test.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables,DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccTaskAssigneeSnapshotMigrationTest" test` -> PASS, 3 tests.
- RED/broad-schema: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccBaseSchemaTest#mysqlSchemaShouldCoverEveryDccDoTableAndColumn" test` -> FAIL in the existing broad runtime schema destructiveness scan before reaching this slice's schema assertions.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccTaskAssigneeSnapshotMigrationTest" test` -> PASS, 2 tests.
- GREEN: `run-release-migration-policy-gate.py --sql-file 20260513_dcc_base_schema.sql --sql-file 20260921_dcc_category_approval_route_action_type.sql --sql-file 20260922_dcc_three_workflow_bpmn_seed.sql --sql-file 20260922_dcc_task_assignee_snapshot.sql --output doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json` -> PASS, `migrationCount=4`.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccTaskAssigneeSnapshotMigrationTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 153 tests.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm "-Dtest=BpmDccRequiredCandidatesTest#dccMultiInstanceCandidateListPreservesDepartmentObligationsWithSameLeader" test` -> FAIL at test compile because `BpmTaskCandidateInvoker.calculateUserListByTask(...)` did not exist.
- GREEN: same BPM target after adding list-preserving multi-instance candidate calculation -> PASS, 1 test.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm "-Dtest=BpmDccRequiredCandidatesTest" test` -> PASS, 5 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccControlledFileApprovalRouteAssigneeResolverTest,DccTaskAssigneeSnapshotMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 13 tests.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm "-Dtest=BpmUserTaskActivityBehaviorTest#recordDccTaskObligationLocalVariables_usesMultiInstanceLoopCounter" test` -> FAIL at test compile because DCC obligation constants and `recordDccTaskObligationLocalVariables(...)` did not exist.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_bindsDepartmentObligationSnapshotFromBpmTaskLocalVariable" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before implementation because the BPM compile RED above exposed the missing DCC obligation variable contract.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm "-Dtest=BpmUserTaskActivityBehaviorTest#recordDccTaskObligationLocalVariables_usesMultiInstanceLoopCounter" test` -> PASS, 1 test.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_bindsDepartmentObligationSnapshotFromBpmTaskLocalVariable" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 1 test.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm "-Dtest=BpmUserTaskActivityBehaviorTest,BpmDccRequiredCandidatesTest" test` -> PASS, 6 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#approveTask_bindsDepartmentObligationSnapshotFromBpmTaskLocalVariable,DccControlledFileWorkflowServiceImplTest#submitControlledFile_persistsDepartmentObligationSnapshotsWithoutUserDedup,DccTaskAssigneeSnapshotMigrationTest,DccThreeWorkflowBpmnMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 15 tests.
- RED/environment: a parallel Maven attempt failed while concurrently compiling/reading BPM generated MapStruct sources; sequential rerun passed, so this is recorded as a local generated-sources collision rather than product behavior.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest,DccControlledFileWorkflowServiceImplTest#uploadTrainingRecord_newUploadProcessTriggersTrainingReceiveTaskAndMovesToDistribution,DccControlledFileFinalizationServiceImplTest#releaseManualDistribution_newUploadProcessTriggersDistributionReceiveTaskAndMovesToDocControlReview" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, exposing auto-completing BPMN serviceTasks, training record status going to doc-control approval, and distribution still using the old activation path.
- GREEN: same receiveTask/training/distribution target after implementation -> PASS, 3 tests.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 191 tests.
- RED/environment: release migration policy gate attempts with `.runtime` Python, Windows `python`, and `py` failed because this workspace has no repo-local Python and no working launcher.
- RED/command-boundary: bundled Python gate run failed until adding required `--sql-root sql/mysql`.
- GREEN: `run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file 20260513_dcc_base_schema.sql --sql-file 20260921_dcc_category_approval_route_action_type.sql --sql-file 20260922_dcc_three_workflow_bpmn_seed.sql --sql-file 20260922_dcc_task_assignee_snapshot.sql --output migration-policy-gate-task-assignee-snapshot.json` using Codex bundled Python -> PASS, `migrationCount=4`.
- RED/product: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileStatusListenerTest#onApplicationEventDelegatesNativeObsoleteProcess,DccControlledFileFinalizationServiceImplTest#handleProcessInstanceStatusChanged_uploadDocControlReviewActivatesControlledFile" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL in the initial hypothesis run; it exposed upload/revision `PENDING_DOC_CONTROL_REVIEW` finalization, while the obsolete-listener expectation was later rejected after rechecking FormCenter effect ownership.
- GREEN: same文控审核终态目标 after implementation -> PASS, 2 tests; the obsolete-listener part of that initial hypothesis was corrected by the follow-up RED/GREEN below.
- GREEN: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 198 tests.
- RED/product correction: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileStatusListenerTest#onApplicationEventIgnoresNativeObsoleteProcessBecauseFormCenterOwnsTheEffect" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL because obsolete was incorrectly in `NATIVE_FINALIZATION_KEYS`.
- GREEN correction: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileStatusListenerTest#onApplicationEventIgnoresNativeObsoleteProcessBecauseFormCenterOwnsTheEffect,DccControlledFileStatusListenerTest#onApplicationEventIgnoresObsoleteFormCenterProcess,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 40 tests.
- GREEN regression correction: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 236 tests.
- GREEN/current-state frontend: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs` -> PASS.
- GREEN/current-state frontend: `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs` -> PASS.
- GREEN/current-state frontend: `node tests/e2e/dcc-upload-training-checkbox-static.spec.cjs` -> exit 0; script produced no stdout.
- GREEN/current-state frontend: `pnpm ts:check` -> PASS.
- GREEN/current-state backend: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 236 tests.
- GREEN/current-state BPM: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-bpm -am "-Dtest=BpmDccRequiredCandidatesTest,BpmUserTaskActivityBehaviorTest,BpmTaskExternalSignatureGuardTest,BpmProcessInstanceConvertTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests.
- RED/current-state command-boundary: `run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file 20260513_dcc_base_schema.sql ...` -> FAIL, current script version resolved `--sql-file` against the working directory and reported missing file.
- GREEN/current-state migration gate: `run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260513_dcc_base_schema.sql --sql-file sql/mysql/20260921_dcc_category_approval_route_action_type.sql --sql-file sql/mysql/20260922_dcc_three_workflow_bpmn_seed.sql --sql-file sql/mysql/20260922_dcc_task_assignee_snapshot.sql --output doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json` using Codex bundled Python -> PASS, `migrationCount=4`.
- RED/current-state broad DCC: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 426 tests run with 42 errors and 2 failures, all traced to `DccControlledFileQueryServiceTest` missing the newly required `attachmentService` mock before the intended assertions could execute.
- GREEN/current-state query service: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 149 tests.
- GREEN/current-state broad DCC: same broad DCC command after completing the query test fixture -> PASS, 426 tests.
- RED/current-state frontend static: `node tests/e2e/dcc-admin-full-config-route-static.spec.js` -> FAIL because the static contract still read SQL from the old `ruoyi-vue-pro` workspace path.
- GREEN/current-state frontend static: after correcting the static contract path to `IntRuoyiBackend/sql/mysql/20260630_dcc_admin_full_config_menu.sql`, `node tests/e2e/dcc-admin-full-config-route-static.spec.js` -> PASS.
- GREEN/current-state frontend static: `node tests/e2e/dcc-working-iteration-submit-static.spec.js` -> PASS.
- GREEN/current-state frontend static: `node tests/e2e/dcc-distribution-training-workbench-static.spec.js` -> PASS.
- GREEN/current-state frontend static: `node tests/e2e/dcc-obsolete-form-center-static.spec.js` -> PASS.
- GREEN/current-state frontend static: `node tests/e2e/dcc-obsolete-form-center-entry-static.spec.js` -> exit 0; script produced no stdout.
- GREEN/current-state frontend static: `node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js` -> PASS.
- GREEN/current-state frontend: `pnpm ts:check` -> PASS after the static-contract path fix.
- GREEN/current-state backend adjacent: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 85 tests.
- RED/current-state broad with dependencies: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before BPM/DCC execution, because `yudao-module-system` test `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` requires missing local file `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js`.
- RED/current-state DCC full module: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc test` -> FAIL, 2064 tests run, 19 failures, 11 errors. Non-task failures remain in registration-certificate/project-code/product-catalog/schema broad suites; task-adjacent failures exposed stale actionType-aware route-readiness and working-iteration test fixtures plus route effective-time boundary.
- GREEN/current-state task fixture repair: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccWorkingIterationSubmissionServiceTest,DccControlledFileRouteReadinessServiceTest,DccApprovalRouteAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 38 tests.
- GREEN/current-state expanded DCC task regression: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileRouteReadinessServiceTest,DccWorkingIterationSubmissionServiceTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 435 tests.
- GREEN/current-state frontend static refresh: `node tests/e2e/dcc-route-department-candidate-static.spec.cjs` -> PASS.
- GREEN/current-state frontend static refresh: `node tests/e2e/dcc-three-workflow-process-keys-static.spec.cjs` -> PASS.
- GREEN/current-state frontend static refresh: `node tests/e2e/dcc-upload-training-checkbox-static.spec.cjs` -> exit 0; script produced no stdout.
- RED/current-state command-boundary refresh: `run-release-migration-policy-gate.py --sql-root sql/mysql --sql-file sql/mysql/20260513_dcc_base_schema.sql ...` from repository root -> FAIL because the current double-directory workspace has no root-level `sql/mysql`.
- GREEN/current-state migration gate refresh: `run-release-migration-policy-gate.py --sql-root IntRuoyiBackend/sql/mysql --sql-file IntRuoyiBackend/sql/mysql/20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend/sql/mysql/20260921_dcc_category_approval_route_action_type.sql --sql-file IntRuoyiBackend/sql/mysql/20260922_dcc_three_workflow_bpmn_seed.sql --sql-file IntRuoyiBackend/sql/mysql/20260922_dcc_task_assignee_snapshot.sql --output doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json` using Codex bundled Python -> PASS, `migrationCount=4`.
- GREEN/current-state frontend refresh: `pnpm ts:check` -> PASS.
- GREEN/current-state frontend static refresh: `node tests/e2e/dcc-working-iteration-submit-static.spec.js`, `node tests/e2e/dcc-distribution-training-workbench-static.spec.js`, `node tests/e2e/dcc-obsolete-form-center-static.spec.js`, `node tests/e2e/dcc-obsolete-form-center-entry-static.spec.js`, `node tests/e2e/dcc-admin-full-config-route-static.spec.js`, `node tests/e2e/dcc-controlled-file-routes-list-display-static.spec.js` -> PASS/exit 0.
- PASS/current-state whitespace: `git diff --check` -> no whitespace errors; output contained only existing LF/CRLF warnings from the dirty worktree.
- GREEN/current-state matrix admin: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 17 tests.
- GREEN/current-state admin config package: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 16 tests.
- GREEN/current-state upload/publication/version adjacent: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileUploadApiTest,DccControlledFilePublicationFlowTest,DccControlledFileVersionNumberAllocationTest,DccApprovalVersionBindingTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 41 tests.
- GREEN/current-state upload/source/preview adjacent: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccSourceUploadContextTest,DccControlledFileUploadNameOptionQueryServiceTest,DccControlledFileUploadNameOptionApiTest,DccControlledFilePlatformAdapterTest,DccOnlyOfficeControlledPreviewTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 20 tests.
- PASS/current-state whitespace after documentation update: `git diff --check` -> no whitespace errors; LF/CRLF warnings only.
- RED/current-state frontend static: `node tests/e2e/dcc-approval-task-view-mode-static.spec.js` -> FAIL because the old contract still required the retired DCC dedicated task page view switch instead of the unified approval center redirect.
- RED/current-state frontend static: `node tests/e2e/dcc-approval-task-summary-static.spec.js` -> FAIL because the old contract used a brittle `<el-table>` indentation anchor and expected inline DCC module checks in the template.
- RED/current-state frontend static: `node tests/e2e/dcc-approval-publish-transition-static.spec.cjs` -> FAIL because the old contract still expected `待文控发布`, contradicting the current three-workflow design that final doc-control review is followed by system activation rather than another manual publish approval.
- GREEN/current-state frontend static: after narrowing those contracts to current behavior, `node tests/e2e/dcc-approval-task-view-mode-static.spec.js`, `node tests/e2e/dcc-approval-task-summary-static.spec.js`, `node tests/e2e/dcc-approval-publish-transition-static.spec.cjs`, `node tests/e2e/dcc-approval-center-handling-entry-static.spec.js`, `node tests/e2e/dcc-upload-current-version-static.spec.js`, `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js`, `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`, `node tests/e2e/dcc-upload-category-permission-static.spec.js`, `node tests/e2e/dcc-upload-controlled-save-closed-loop-static.spec.js`, `node tests/e2e/dcc-upload-onlyoffice-document-url-static.spec.js`, `node tests/e2e/form-center-dcc-upload-embedded-static.spec.js`, `node tests/e2e/form-center-bpm-dcc-approval-bypass-static.spec.js` -> PASS.
- GREEN/current-state frontend: `pnpm ts:check` -> PASS.
- PASS/current-state whitespace after frontend static/doc update: `git diff --check` -> no whitespace errors; LF/CRLF warnings only.
- GREEN/current-state approval/finalization adjacent backend: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccApprovalTaskTimelineAdapterTest,DccApprovalPrintTemplateServiceTest,DccControlledContentAdapterTest,DccControlledFilePublishFormEffectExecutorTest,DccControlledFilePublishServiceTest,DccWorkingSubmissionConditionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 41 tests.
- GREEN/current-state signature/approval adjacent backend: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccApprovalReasonValidationTest,DccSignatureBindingSchemaTest,DccControlledFileSignatureServiceTest,DccControlledFileSignatureEvidenceServiceTest,DccControlledFileSignatureBindingServiceTest,DccFrozenApprovalSignaturesTest,DccApprovalParticipantPostValidatorTest,DccElectronicSignatureAuthorizationAuditServiceTest,DccElectronicSignatureAuthorizationServiceTest,DccElectronicSignatureManagementServiceTest,DccElectronicSignatureImageServiceImplTest,DccElectronicSignatureFailureAuditServiceTest,DccElectronicSignatureAuthorizationControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 98 tests.
- PASS/current-state whitespace after signature/approval doc update: `git diff --check` -> no whitespace errors; LF/CRLF warnings only.
- GREEN/current-state approval/action/preview backend adjacent: `$env:JAVA_HOME='C:\IntRuoyiAll-int_main\.runtime\tools\jdk-17'; $env:PATH="$env:JAVA_HOME\bin;$env:PATH"; & 'C:\IntRuoyiAll-int_main\.runtime\tools\apache-maven-3.9.11\bin\mvn.cmd' -pl yudao-module-dcc -am "-Dtest=DccControlledFileTaskActionApiTest,DccControlledFilePendingActionGuardTest,DccOrdinaryApprovalRemovedActionsTest,DccRevisionSourceSelectionTest,DccControlledFileVersionPolicyTest,DccControlledFilePreviewDownloadApiTest,DccControlledFilePreviewProtectionTest,DccControlledPreviewAccessServiceTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFileViewMatrixAccessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 55 tests.
- RED/current-state frontend static: `dcc-detail-distribution-summary-static.spec.js`、`dcc-detail-route-snapshot-summary-static.spec.js`、`dcc-browser-finalization-retry-entry-static.spec.js`、`dcc-detail-handling-summary-static.spec.js`、`dcc-approval-task-load-error-context-static.spec.js` first failed on stale static anchors or stale current-code expressions; contracts were narrowed to current source boundaries without weakening business checks.
- RED/current-state frontend product: `node tests/e2e/dcc-doc-control-path-confirmation-static.spec.js` -> FAIL because the final DCC approval dialog did not render the `存入路径确认` field, despite the API/helper contract already carrying `confirmedDirectoryId`.
- GREEN/current-state frontend static: after adding the final doc-control confirmed-directory selector and updating stale static contracts, `node tests/e2e/dcc-detail-distribution-summary-static.spec.js`, `node tests/e2e/dcc-detail-route-snapshot-summary-static.spec.js`, `node tests/e2e/dcc-browser-finalization-retry-entry-static.spec.js`, `node tests/e2e/dcc-detail-training-summary-static.spec.js`, `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js`, `node tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js`, `node tests/e2e/dcc-detail-lifecycle-timeline-static.spec.js`, `node tests/e2e/dcc-detail-handling-summary-static.spec.js`, `node tests/e2e/dcc-detail-approval-render-safety-static.spec.js`, `node tests/e2e/dcc-approval-task-load-error-context-static.spec.js`, `node tests/e2e/dcc-doc-control-path-confirmation-static.spec.js` -> PASS.
- GREEN/current-state frontend: `pnpm ts:check` -> PASS after the doc-control confirmed-directory UI and payload update.
- PASS/current-state whitespace after backend/frontend/doc update: `git diff --check` -> no whitespace errors; LF/CRLF warnings only.

## Results

- `DccControlledFileApprovalRouteAssigneeResolverTest`: 11 tests, 0 failures, 0 errors, 0 skipped.
- `DccApprovalRouteAdminServiceImplTest`: 29 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileObsoleteServiceTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileWorkflowServiceImplTest`: 142 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileFinalizationServiceImplTest`: 49 tests, 0 failures, 0 errors, 0 skipped.
- `DccThreeWorkflowBpmnMigrationTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccTaskAssigneeSnapshotMigrationTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `BpmDccRequiredCandidatesTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
- `BpmUserTaskActivityBehaviorTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `BpmTaskExternalSignatureGuardTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
- `DccApprovalTaskAdapterTest`: 22 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileStatusListenerTest`: 6 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileObsoleteFormEffectExecutorTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
- `BpmProcessInstanceConvertTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_persistsNeedTrainingAndPassesItToBpmVariables`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileQueryServiceTest`: 149 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileRouteReadinessServiceTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccWorkingIterationSubmissionServiceTest`: 7 tests, 0 failures, 0 errors, 0 skipped.
- `DccTrainingAssignmentAckServiceTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccTrainingConcurrentAcknowledgementTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccTrainingTaskServiceTest`: 6 tests, 0 failures, 0 errors, 0 skipped.
- `DccDistributionReceiptServiceImplTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
- `DccDistributionTaskServiceImplTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccPaperDistributionAckServiceTest`: 9 tests, 0 failures, 0 errors, 0 skipped.
- `DccCategoryTrainingRuleAdminServiceImplTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `DccCategoryDistributionRuleAdminServiceImplTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
- `DccCategoryApprovalMatrixAdminServiceImplTest`: 17 tests, 0 failures, 0 errors, 0 skipped.
- `DccAdminFullConfigPackageServiceTest`: 11 tests, 0 failures, 0 errors, 0 skipped.
- `DccFileCategoryControllerConfigPackageContractTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileUploadApiTest`: 34 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePublicationFlowTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileVersionNumberAllocationTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccApprovalVersionBindingTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePlatformAdapterTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileUploadNameOptionApiTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileUploadNameOptionQueryServiceTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `DccOnlyOfficeControlledPreviewTest`: 7 tests, 0 failures, 0 errors, 0 skipped.
- `DccSourceUploadContextTest`: 7 tests, 0 failures, 0 errors, 0 skipped.
- `BpmMessageServiceImplTest`: 18 tests, 0 failures, 0 errors, 0 skipped.
- `BpmTaskConvertTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `BpmTaskServiceImplApprovalFilterTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `BpmProcessInstanceServiceImplTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `BpmNativeApprovalTaskProviderTest`: 35 tests, 0 failures, 0 errors, 0 skipped.
- `DccApprovalTaskTimelineAdapterTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccApprovalPrintTemplateServiceTest`: 10 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledContentAdapterTest`: 11 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePublishFormEffectExecutorTest`: 7 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePublishServiceTest`: 10 tests, 0 failures, 0 errors, 0 skipped.
- `DccWorkingSubmissionConditionTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccApprovalReasonValidationTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccSignatureBindingSchemaTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileSignatureServiceTest`: 13 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileSignatureEvidenceServiceTest`: 5 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileSignatureBindingServiceTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
- `DccFrozenApprovalSignaturesTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `DccApprovalParticipantPostValidatorTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccElectronicSignatureAuthorizationAuditServiceTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccElectronicSignatureAuthorizationServiceTest`: 13 tests, 0 failures, 0 errors, 0 skipped.
- `DccElectronicSignatureManagementServiceTest`: 39 tests, 0 failures, 0 errors, 0 skipped.
- `DccElectronicSignatureImageServiceImplTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccElectronicSignatureFailureAuditServiceTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccElectronicSignatureAuthorizationControllerTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileTaskActionApiTest`: 7 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePendingActionGuardTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
- `DccOrdinaryApprovalRemovedActionsTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `DccRevisionSourceSelectionTest`: 1 test, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileVersionPolicyTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePreviewDownloadApiTest`: 12 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFilePreviewProtectionTest`: 8 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledPreviewAccessServiceTest`: 3 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileReviewMatrixAccessServiceTest`: 4 tests, 0 failures, 0 errors, 0 skipped.
- `DccControlledFileViewMatrixAccessServiceTest`: 11 tests, 0 failures, 0 errors, 0 skipped.
- Current-state training/distribution/finalization adjacent regression: 85 tests, 0 failures, 0 errors, 0 skipped.
- Current-state broad DCC regression including query service: 426 tests, 0 failures, 0 errors, 0 skipped.
- Current-state expanded DCC task regression including route-readiness and working-iteration submission: 435 tests, 0 failures, 0 errors, 0 skipped.
- `dcc-route-department-candidate-static.spec.cjs`: PASS.
- `dcc-three-workflow-process-keys-static.spec.cjs`: PASS.
- `dcc-upload-training-checkbox-static.spec.cjs`: exit 0, no stdout.
- `dcc-working-iteration-submit-static.spec.js`: PASS.
- `dcc-distribution-training-workbench-static.spec.js`: PASS.
- `dcc-obsolete-form-center-static.spec.js`: PASS.
- `dcc-obsolete-form-center-entry-static.spec.js`: exit 0, no stdout.
- `dcc-admin-full-config-route-static.spec.js`: PASS.
- `dcc-controlled-file-routes-list-display-static.spec.js`: PASS.
- `dcc-approval-task-view-mode-static.spec.js`: PASS.
- `dcc-approval-task-summary-static.spec.js`: PASS.
- `dcc-approval-publish-transition-static.spec.cjs`: PASS, 2 tests.
- `dcc-approval-center-handling-entry-static.spec.js`: PASS.
- `dcc-upload-current-version-static.spec.js`: PASS.
- `dcc-upload-project-taxonomy-revision-static.spec.js`: PASS.
- `dcc-upload-category-taxonomy-binding-static.spec.js`: PASS.
- `dcc-upload-category-permission-static.spec.js`: PASS.
- `dcc-upload-controlled-save-closed-loop-static.spec.js`: PASS.
- `dcc-upload-onlyoffice-document-url-static.spec.js`: PASS.
- `form-center-dcc-upload-embedded-static.spec.js`: PASS.
- `form-center-bpm-dcc-approval-bypass-static.spec.js`: PASS.
- `dcc-detail-distribution-summary-static.spec.js`: PASS.
- `dcc-detail-route-snapshot-summary-static.spec.js`: PASS.
- `dcc-browser-finalization-retry-entry-static.spec.js`: PASS.
- `dcc-detail-training-summary-static.spec.js`: PASS.
- `dcc-detail-signature-view-mode-static.spec.js`: PASS.
- `dcc-detail-signature-evidence-nonblocking-static.spec.js`: PASS.
- `dcc-detail-lifecycle-timeline-static.spec.js`: PASS.
- `dcc-detail-handling-summary-static.spec.js`: PASS.
- `dcc-detail-approval-render-safety-static.spec.js`: PASS.
- `dcc-approval-task-load-error-context-static.spec.js`: PASS.
- `dcc-doc-control-path-confirmation-static.spec.js`: PASS.
- `dcc-detail-publication-followup-static.spec.js`: PASS.
- `dcc-detail-version-successor-summary-static.spec.js`: PASS.
- `dcc-detail-approval-own-task-without-process-query-static.spec.js`: PASS.
- `dcc-bpm-dcc-approval-viewer-static.spec.js`: PASS.
- `bpm-dcc-approval-preview-pane-static.spec.js`: PASS.
- `dcc-review-matrix-tab-static.spec.js`: PASS.
- `dcc-view-matrix-preview-by-dept-static.spec.js`: PASS.
- `dcc-view-matrix-independent-source-static.spec.js`: PASS.
- `dcc-signature-record-summary-static.spec.js`: PASS.
- `dcc-signature-view-mode-static.spec.js`: PASS.
- `dcc-training-summary-static.spec.js`: PASS.
- `dcc-training-execution-summary-static.spec.js`: PASS.
- `dcc-distribution-toolbar-context-static.spec.js`: PASS.
- `dcc-doc-control-department-distribution-static.spec.js`: PASS.
- `dcc-publish-form-center-static.spec.js`: PASS.
- `dcc-approval-reason-required-static.spec.cjs`: PASS.
- `dcc-workbench-static.spec.js`: PASS.
- `dcc-workbench-file-context-static.spec.js`: PASS.
- `dcc-workbench-entry-static.spec.js`: PASS.
- `dcc-route-summary-static.spec.js`: PASS.
- `dcc-route-operations-static.spec.js`: PASS.
- `dcc-training-ux-prechecks-static.spec.cjs`: PASS.
- `dcc-training-rules-context-static.spec.js`: PASS.
- `dcc-training-mine-workbench-toolbar-static.spec.js`: PASS.
- `dcc-training-mine-unified-list-template-static.spec.js`: exit 0, no stdout.
- `dcc-distribution-category-autoload-static.spec.js`: PASS.
- `dcc-controlled-viewer-permission-static.spec.js`: PASS.
- `dcc-permission-distribution-training-tab-static.spec.js`: PASS.
- `dcc-permission-distribution-training-split-tabs-static.spec.js`: PASS.
- `pnpm ts:check`: PASS.
- Current-state submission/action adjacent backend regression: `DccApprovalTaskAdapterTest` 22, `DccApprovalReasonValidationTest` 1, `DccControlledFilePendingActionGuardTest` 2, `DccControlledFileTaskActionApiTest` 7, `DccOrdinaryApprovalRemovedActionsTest` 4, `DccWorkingIterationSubmissionServiceTest` 7, `DccWorkingSubmissionConditionTest` 1 -> PASS, 44 tests, 0 failures, 0 errors, 0 skipped.
- Core DCC frontend detail/upload/workbench static suite:
  `dcc-approval-upload-view-static.spec.js`,
  `dcc-browser-state-consistency-static.spec.js`,
  `dcc-controlled-file-state-machine-static.spec.js`,
  `dcc-controlled-content-matrix-real-flow-contract-static.spec.js`,
  `dcc-detail-applicant-rework-static.spec.js`,
  `dcc-publication-notify-navigation-static.spec.js`,
  `dcc-release-impact-workbench-static.spec.js`,
  `dcc-readiness-capability-contract-static.spec.js`,
  `dcc-upload-browser-tab-cache-static.spec.js`,
  `dcc-upload-explicit-tenant-http-contract-static.spec.js`,
  `dcc-upload-governance-ux-static.spec.js`,
  `dcc-upload-layout-static.spec.js`,
  `dcc-upload-name-version-autofill-static.spec.js`,
  `dcc-upload-product-autofill-static.spec.js`,
  `dcc-upload-temporary-status-timestamp-static.spec.js` -> PASS.
- `pnpm ts:check` after core DCC frontend detail/upload/workbench fixes: PASS.
- Upload ticket/message/publication/FormCenter adjacent backend regression:
  `DccControlledFileFormEffectExecutorTest` 5,
  `DccControlledFileMessageOutboxTest` 2,
  `DccControlledFileObsoleteFormEffectExecutorTest` 8,
  `DccControlledFilePublicationFlowTest` 2,
  `DccPublicationFollowupTransactionIntegrationTest` 4,
  `DccUploadTicketServiceTest` 27 -> PASS, 48 tests, 0 failures, 0 errors, 0 skipped.
- Replay/notification/logical-identity adjacent backend regression:
  `DccLogicalIdentityConcurrencyTest` 1,
  `DccMessageDeliveryIdempotencyTest` 2,
  `DccMessageDeliveryTransactionIntegrationTest` 3,
  `DccPublicationFollowupQueryServiceTest` 8,
  `DccPublicationFollowupServiceTest` 10,
  `DccPublicationFollowupStatusServiceTest` 1,
  `DccPublicationNotificationDispatchOrchestratorTest` 3,
  `DccPublicationNotificationPostCommitSchedulerTest` 2,
  `DccPublicationNotificationServiceTest` 4,
  `DccPublicationNotificationTransactionIntegrationTest` 7 -> PASS, 41 tests, 0 failures, 0 errors, 0 skipped.
- History/approval-list/log-timeline adjacent backend regression:
  `BpmProcessInstanceControllerVisibilityContractTest` 3,
  `DccApprovalTaskAdapterTest` 22,
  `DccApprovalTaskTimelineAdapterTest` 2,
  `DccControlledFileAuditControllerTest` 1,
  `DccControlledFileLogControllerTest` 1,
  `DccControlledFileAuditQueryServiceTest` 3,
  `DccControlledFileLogQueryServiceTest` 4 -> PASS, 36 tests, 0 failures, 0 errors, 0 skipped.
- FormCenter/DCC obsolete owner adjacent backend regression:
  `BusinessApprovalBpmEventListenerTest` 3,
  `BusinessApprovalEffectExecutorRegistryTest` 4,
  `FormCenterBpmEventBridgeTest` 7,
  `FormCenterRuntimeBpmCallbackTest` 21,
  `FormCenterTemplateObsoleteRuntimeTest` 4,
  `FormBpmBindingServiceTest` 3,
  `FormTemplateObsoleteBusinessApprovalEffectExecutorTest` 9,
  `DccFormCenterPolicyMigrationTest` 1,
  `DccControlledFileObsoleteFormEffectExecutorTest` 8,
  `DccControlledFileObsoleteServiceTest` 8,
  `DccObsoleteFileStorageServiceTest` 2 -> PASS, 70 tests, 0 failures, 0 errors, 0 skipped.
- RED/GREEN BPM candidate strategy adjacent regression:
  first run of `BpmTaskCandidateDeptLeaderStrategyTest,BpmTaskCandidateDeptLeaderMultiStrategyTest,BpmTaskCandidateDeptMemberStrategyTest,BpmTaskCandidateStartUserDeptLeaderStrategyTest,BpmTaskCandidateStartUserDeptLeaderMultiStrategyTest,BpmTaskCandidateStartUserSelectStrategyTest,BpmTaskCandidateMixedStrategyTest,BpmTaskCandidateInvokerTest,BpmDccRequiredCandidatesTest,BpmUserTaskActivityBehaviorTest` failed in `BpmTaskCandidateInvokerTest#testCalculateUsersByTask_none` and `#testCalculateUsersByTask_some` because ordinary legacy tasks with null `processDefinitionKey` hit `Set.of(...).contains(null)` inside the DCC process-key guard.
- BPM candidate strategy adjacent backend regression after null-key guard:
  `BpmUserTaskActivityBehaviorTest` 1,
  `BpmDccRequiredCandidatesTest` 5,
  `BpmTaskCandidateInvokerTest` 5,
  `BpmTaskCandidateDeptLeaderMultiStrategyTest` 1,
  `BpmTaskCandidateDeptLeaderStrategyTest` 1,
  `BpmTaskCandidateDeptMemberStrategyTest` 1,
  `BpmTaskCandidateStartUserDeptLeaderMultiStrategyTest` 2,
  `BpmTaskCandidateStartUserDeptLeaderStrategyTest` 2,
  `BpmTaskCandidateStartUserSelectStrategyTest` 2,
  `BpmTaskCandidateMixedStrategyTest` 5 -> PASS, 25 tests, 0 failures, 0 errors, 0 skipped.
- Cross-module current-state regression:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  BPM side: `FormCenterBpmEventBridgeTest` 7, `FormCenterRuntimeBpmCallbackTest` 21, `BpmUserTaskActivityBehaviorTest` 1, `BpmDccRequiredCandidatesTest` 5, `BpmTaskCandidateInvokerTest` 5 -> PASS, 39 tests, 0 failures, 0 errors, 0 skipped.
  DCC side: `DccTaskAssigneeSnapshotMigrationTest` 1, `DccThreeWorkflowBpmnMigrationTest` 1, `DccControlledFileObsoleteFormEffectExecutorTest` 8, `DccControlledFileObsoleteServiceTest` 8, `DccControlledFileWorkflowServiceImplTest` 142 -> PASS, 160 tests, 0 failures, 0 errors, 0 skipped.
- Frontend build current-state regression:
  `pnpm build:local` -> PASS. Output ended with `Build successful. Please see dist directory`; warning output was limited to Vite CJS API deprecation and stale Browserslist data.
- Backend package current-state regression:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-DskipTests" package` -> PASS, 23 reactor modules SUCCESS, final `BUILD SUCCESS`, total time 02:37. Tests were intentionally skipped in this package command because test execution is covered by the dedicated Maven regression commands above.
- Isolated Flowable engine BPMN regression:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` first failed at testCompile because Flowable `ProcessEngine` is not `AutoCloseable`; after changing the test fixture to close the engine in `finally`, the same command passed.
  `DccThreeWorkflowBpmnMigrationTest`: 2 tests, 0 failures, 0 errors, 0 skipped. The new test extracts the upload/revision/obsolete BPMN XML directly from `20260922_dcc_three_workflow_bpmn_seed.sql`, deploys all three definitions to a real in-memory Flowable engine, runs upload and revision with `needTraining=true` and `needTraining=false`, triggers TRAINING/DISTRIBUTION receive tasks as applicable, runs obsolete without TRAINING/DISTRIBUTION, and verifies each instance ends after DOC_CONTROL_REVIEW.
- Isolated Flowable engine BPMN stop-point regression:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `DccThreeWorkflowBpmnMigrationTest`: 2 tests, 0 failures, 0 errors, 0 skipped.
  The Flowable test now asserts runtime stop points in addition to eventual completion: upload/revision must expose exactly one active user task at MATRIX_REVIEW and MATRIX_APPROVAL, must wait at TRAINING only when `needTraining=true`, must wait at DISTRIBUTION before DOC_CONTROL_REVIEW, and obsolete must go directly from MATRIX_APPROVAL to DOC_CONTROL_REVIEW without TRAINING/DISTRIBUTION.
- Cross-module current-state regression after isolated Flowable test:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  BPM side: `FormCenterBpmEventBridgeTest` 7, `FormCenterRuntimeBpmCallbackTest` 21, `BpmUserTaskActivityBehaviorTest` 1, `BpmDccRequiredCandidatesTest` 5, `BpmTaskCandidateInvokerTest` 5 -> PASS, 39 tests, 0 failures, 0 errors, 0 skipped.
  DCC side: `DccTaskAssigneeSnapshotMigrationTest` 1, `DccThreeWorkflowBpmnMigrationTest` 2, `DccControlledFileObsoleteFormEffectExecutorTest` 8, `DccControlledFileObsoleteServiceTest` 8, `DccControlledFileWorkflowServiceImplTest` 142 -> PASS, 161 tests, 0 failures, 0 errors, 0 skipped.
- RED/GREEN obsolete request `needTraining` rejection:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest#obsoleteRequestJsonRejectsNeedTrainingEvenWhenGlobalMapperAllowsUnknownFields" "-Dsurefire.failIfNoSpecifiedTests=false" test` first failed because the lenient global `JsonUtils` mapper ignored `needTraining`; after adding explicit write-only field rejection in `DccControlledFileObsoleteReqVO`, the same target passed.
  `DccControlledFileObsoleteServiceTest#obsoleteRequestJsonRejectsNeedTrainingEvenWhenGlobalMapperAllowsUnknownFields`: 1 test, 0 failures, 0 errors, 0 skipped.
- Obsolete service current-state regression after request contract fix:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  `DccControlledFileObsoleteServiceTest`: 9 tests, 0 failures, 0 errors, 0 skipped.
- Cross-module current-state regression after obsolete request contract fix:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  BPM side: `FormCenterBpmEventBridgeTest` 7, `FormCenterRuntimeBpmCallbackTest` 21, `BpmUserTaskActivityBehaviorTest` 1, `BpmDccRequiredCandidatesTest` 5, `BpmTaskCandidateInvokerTest` 5 -> PASS, 39 tests, 0 failures, 0 errors, 0 skipped.
  DCC side: `DccTaskAssigneeSnapshotMigrationTest` 1, `DccThreeWorkflowBpmnMigrationTest` 2, `DccControlledFileObsoleteFormEffectExecutorTest` 8, `DccControlledFileObsoleteServiceTest` 9, `DccControlledFileWorkflowServiceImplTest` 142 -> PASS, 162 tests, 0 failures, 0 errors, 0 skipped.
- RED/GREEN upload/revision manual signoff rejection:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest#submitWorkingIteration_rejectsClientSelectedSignoffUsersForThreeWorkflow,DccControlledFileWorkflowServiceImplTest#submitControlledFile_rejectsClientSelectedSignoffUsersForUploadWorkflow" "-Dsurefire.failIfNoSpecifiedTests=false" test` first failed because upload accepted `selectedSignoffUserIds` and working-iteration submission did not reject it at the deprecated-field boundary; after adding the three-workflow guard, the same target passed.
  Target result after fix: 2 tests, 0 failures, 0 errors, 0 skipped.
- Workflow service current-state regression after manual signoff rejection:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  `DccControlledFileWorkflowServiceImplTest`: 144 tests, 0 failures, 0 errors, 0 skipped.
- Focused BPM+DCC cross-module current-state regression after manual signoff rejection:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  BPM side: `BpmDccRequiredCandidatesTest` 5 tests, 0 failures, 0 errors, 0 skipped.
  DCC side: `DccThreeWorkflowBpmnMigrationTest` 2, `DccControlledFileObsoleteServiceTest` 9, `DccControlledFileWorkflowServiceImplTest` 144 -> PASS, 155 tests, 0 failures, 0 errors, 0 skipped.
- RED/GREEN frontend deprecated signoff field removal:
  `dcc-readiness-capability-contract-static.spec.js` and `dcc-upload-governance-ux-static.spec.js` first failed because the old static contracts still required `selectedSignoffUserIds`; after removing the deprecated field from upload readiness, new upload submitter and working-iteration submit payloads, `dcc-upload-training-checkbox-static.spec.cjs`, `dcc-working-iteration-submit-static.spec.js`, `dcc-readiness-capability-contract-static.spec.js`, and `dcc-upload-governance-ux-static.spec.js` all PASS.
- Frontend typing after deprecated signoff field removal:
  `pnpm ts:check` -> PASS.
- RED/GREEN route readiness actionType and manual signoff rejection:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest#evaluate_actionRouteRejectsDeprecatedManualSignoffUsers" "-Dsurefire.failIfNoSpecifiedTests=false" test` first failed because action route readiness still entered the deprecated selected-signoff overlay path. After adding `actionType` to `/route-preview`, passing it through workflow service/readiness service, and rejecting non-empty `selectedSignoffUserIds` for NEW/REVISION/OBSOLETE readiness, the same target passed.
  Target result after fix: 1 test, 0 failures, 0 errors, 0 skipped.
- Readiness/workflow current-state regression after actionType readiness:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  `DccControlledFileRouteReadinessServiceTest` 3, `DccControlledFileWorkflowServiceImplTest` 144 -> PASS, 147 tests, 0 failures, 0 errors, 0 skipped.
- Frontend readiness actionType static/typing:
  `dcc-readiness-capability-contract-static.spec.js`, `dcc-upload-governance-ux-static.spec.js`, `dcc-upload-training-checkbox-static.spec.cjs` -> PASS.
  `pnpm ts:check` -> PASS.
- Focused BPM+DCC cross-module current-state regression after actionType readiness:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccThreeWorkflowBpmnMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS.
  BPM side: `BpmDccRequiredCandidatesTest` 5 tests, 0 failures, 0 errors, 0 skipped.
  DCC side: `DccThreeWorkflowBpmnMigrationTest` 2, `DccControlledFileObsoleteServiceTest` 9, `DccControlledFileRouteReadinessServiceTest` 3, `DccControlledFileWorkflowServiceImplTest` 144 -> PASS, 158 tests, 0 failures, 0 errors, 0 skipped.
- Current-state frontend three-workflow static combination:
  `dcc-route-department-candidate-static.spec.cjs`, `dcc-three-workflow-process-keys-static.spec.cjs`, `dcc-upload-training-checkbox-static.spec.cjs`, `dcc-working-iteration-submit-static.spec.js`, `dcc-distribution-training-workbench-static.spec.js`, `dcc-obsolete-form-center-static.spec.js`, `dcc-obsolete-form-center-entry-static.spec.js`, `dcc-admin-full-config-route-static.spec.js`, `dcc-controlled-file-routes-list-display-static.spec.js` -> PASS in one PowerShell loop.
- Migration policy gate refresh:
  `python --version` failed because the PATH entry is the WindowsApps placeholder launcher. Reran with `C:\Users\D01020\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe`.
  `run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260921_dcc_category_approval_route_action_type.sql --sql-file IntRuoyiBackend\sql\mysql\20260922_dcc_three_workflow_bpmn_seed.sql --sql-file IntRuoyiBackend\sql\mysql\20260922_dcc_task_assignee_snapshot.sql --output doc\tasks\20260921-dcc-three-workflows-implementation\migration-policy-gate-task-assignee-snapshot.json` -> PASS, `status=passed`, `migrationCount=4`.
- RED/GREEN working-iteration REVISION readiness:
  `node tests/e2e/dcc-working-iteration-submit-static.spec.js` first failed because the browser working-iteration submit path did not precheck `REVISION` route readiness. After adding `checkControlledFileRouteReadiness({ categoryId: row.categoryId, actionType: 'REVISION' })` before `submitControlledFileWorkingIteration(...)`, the same static test passed.
  Related frontend static group also passed: `dcc-readiness-capability-contract-static.spec.js`, `dcc-upload-governance-ux-static.spec.js`, `dcc-upload-training-checkbox-static.spec.cjs`, `dcc-three-workflow-process-keys-static.spec.cjs`.
  `pnpm ts:check` -> PASS.
- Evidence asset visibility:
  `git check-ignore -v -- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccThreeWorkflowBpmnMigrationTest.java doc/tasks/20260921-dcc-three-workflows-implementation/task.md doc/tasks/20260921-dcc-three-workflows-implementation/execution-log.md doc/tasks/20260921-dcc-three-workflows-implementation/verification-report.md doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-three-workflows.json doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json` -> no output, exit 1, meaning these evidence assets are not ignored.
  `git status --short -- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccThreeWorkflowBpmnMigrationTest.java doc/tasks/20260921-dcc-three-workflows-implementation` shows the test and task directory as untracked, so they are visible for a future authorized commit.
- Current-state frontend upload/project/checkin static refresh:
  `dcc-upload-optimization-static.spec.js`,
  `dcc-project-code-recognition-static.spec.js`,
  `dcc-static-022-remark-only-checkin-static.spec.cjs` -> PASS.
- `dcc-loss-order-form-center-chain-static.spec.js`: not counted as current DCC three-workflow evidence; the script points to missing historical assets `scripts/dcc-loss-order-form-center-e2e-chain.mjs`, `doc/tasks/20260719-loss-order-form-center-e2e/`, and `doc/tasks/20260719-loss-order-form-center-test-chain/`.
- `pnpm ts:check` after frontend upload/project/checkin static refresh: PASS.
- `migration-policy-gate-three-workflows.json`: PASS, 3 migrations.
- `migration-policy-gate-task-assignee-snapshot.json`: PASS, 4 migrations.
- `git diff --check`: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after BPM/approval-center evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after workbench/route/training/distribution/permission evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after submission/action adjacent evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after core DCC frontend detail/upload/workbench evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after frontend upload/project/checkin refresh and historical loss-order chain boundary documentation: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after replay/notification/logical-identity evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after history/approval-list/log-timeline evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after FormCenter/DCC obsolete owner evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after BPM candidate strategy null-key fix and evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after cross-module BPM+DCC regression evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after frontend build evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after backend package evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- `git diff --check` after obsolete request `needTraining` rejection and evidence refresh: PASS, no whitespace errors; LF/CRLF warnings only.
- RED/GREEN obsolete route-controlled assignees:
  `DccControlledFileObsoleteServiceTest` was updated to assert that request-provided `startUserSelectAssignees` cannot override the OBSOLETE action route. After changing `DccControlledFileObsoleteServiceImpl` to always derive `startUserSelectAssignees` from `resolveStartUserSelectAssignees(file, userId, OBSOLETE)`, the focused backend test passed.
  Command: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 9 tests, 0 failures, 0 errors, 0 skipped.
- Static obsolete assignee contract:
  `node yudao-module-dcc/src/test/js/dcc-obsolete-route-assignees-static.spec.cjs` -> PASS.
- Frontend static/typing refresh after obsolete assignee contract:
  `dcc-working-iteration-submit-static.spec.js`, `dcc-readiness-capability-contract-static.spec.js`, `dcc-upload-governance-ux-static.spec.js`, `dcc-upload-training-checkbox-static.spec.cjs`, `dcc-three-workflow-process-keys-static.spec.cjs`, `dcc-obsolete-form-center-static.spec.js` -> PASS.
  `pnpm ts:check` -> PASS.
- BPM+DCC combination after obsolete assignee contract:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileRouteReadinessServiceTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccThreeWorkflowBpmnMigrationTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS.
  BPM side: 39 tests, 0 failures, 0 errors, 0 skipped. DCC side: 166 tests, 0 failures, 0 errors, 0 skipped. Total: 205 tests, 0 failures, 0 errors, 0 skipped.
- Expanded DCC task regression after obsolete assignee contract:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccControlledFileWorkflowServiceImplTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileRouteReadinessServiceTest,DccWorkingIterationSubmissionServiceTest,DccApprovalRouteAdminServiceImplTest,DccControlledFileQueryServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileObsoleteServiceTest,DccApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, 440 tests, 0 failures, 0 errors, 0 skipped.
- Backend package after latest obsolete/revision readiness changes:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-DskipTests" package` using repository `.runtime` Maven/JDK 17 -> PASS, 23 reactor modules SUCCESS. This is compile/package evidence only; tests were intentionally skipped and are covered by the test commands above.
- Frontend production build after latest browser revision readiness change:
  `pnpm build:local` -> PASS, `Build successful. Please see dist directory`. Warnings observed: Vite CJS API deprecated warning and stale Browserslist/caniuse-lite data.
- Broad frontend DCC static regression after latest obsolete/revision readiness changes:
  `dcc-route-department-candidate-static.spec.cjs`, `dcc-three-workflow-process-keys-static.spec.cjs`, `dcc-upload-training-checkbox-static.spec.cjs`, `dcc-working-iteration-submit-static.spec.js`, `dcc-distribution-training-workbench-static.spec.js`, `dcc-obsolete-form-center-static.spec.js`, `dcc-obsolete-form-center-entry-static.spec.js`, `dcc-admin-full-config-route-static.spec.js`, `dcc-controlled-file-routes-list-display-static.spec.js`, `dcc-approval-task-view-mode-static.spec.js`, `dcc-approval-task-summary-static.spec.js`, `dcc-approval-upload-view-static.spec.js`, `dcc-approval-publish-transition-static.spec.cjs`, `dcc-workbench-static.spec.js`, `dcc-workbench-entry-static.spec.js`, `dcc-training-mine-workbench-toolbar-static.spec.js`, `dcc-training-summary-static.spec.js`, `dcc-distribution-toolbar-context-static.spec.js`, `dcc-permission-distribution-training-split-tabs-static.spec.js`, `form-center-dcc-upload-embedded-static.spec.js` -> PASS in one PowerShell loop.
- Backend governance-entry regression after latest obsolete/revision readiness changes:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS.
  `DccFileCategoryControllerConfigPackageContractTest` 5, `DccAdminFullConfigPackageServiceTest` 11, `DccCategoryApprovalMatrixAdminServiceImplTest` 17, `DccControlledFileApprovalRouteAssigneeResolverTest` 11, `DccControlledFileObsoleteServiceTest` 9, `DccControlledFileRouteReadinessServiceTest` 3 -> PASS, total 56 tests, 0 failures, 0 errors, 0 skipped.
- Task status structure check:
  PowerShell check confirmed `doc/tasks/20260921-dcc-three-workflows-implementation/task.md` has `in_progress` as the first non-empty line under `## Current Status`.
- Migration/model current-state refresh:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccFormCenterPolicyMigrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS.
  `DccFormCenterPolicyMigrationTest` 1, `DccTaskAssigneeSnapshotMigrationTest` 1, `DccThreeWorkflowBpmnMigrationTest` 2 -> PASS, total 4 tests, 0 failures, 0 errors, 0 skipped.
- Migration policy gate current-state refresh:
  `run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260921_dcc_category_approval_route_action_type.sql --sql-file IntRuoyiBackend\sql\mysql\20260922_dcc_three_workflow_bpmn_seed.sql --sql-file IntRuoyiBackend\sql\mysql\20260922_dcc_task_assignee_snapshot.sql --output doc\tasks\20260921-dcc-three-workflows-implementation\migration-policy-gate-task-assignee-snapshot.json` -> PASS, `status=passed`, `migrationCount=4`.
- Cross-module BPM+DCC core regression current-state refresh:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS.
  BPM side: 39 tests, 0 failures, 0 errors, 0 skipped. DCC side: 219 tests, 0 failures, 0 errors, 0 skipped. Total: 258 tests, 0 failures, 0 errors, 0 skipped.
- Evidence asset visibility current-state refresh:
  `git status --short --untracked-files=all -- doc/tasks/20260921-dcc-three-workflows-implementation IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-obsolete-route-assignees-static.spec.cjs` shows the task docs, two migration policy gate JSON files, and the obsolete route assignee static contract as untracked assets.
  `git check-ignore -v -- doc/tasks/20260921-dcc-three-workflows-implementation/task.md doc/tasks/20260921-dcc-three-workflows-implementation/execution-log.md doc/tasks/20260921-dcc-three-workflows-implementation/verification-report.md doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-three-workflows.json doc/tasks/20260921-dcc-three-workflows-implementation/migration-policy-gate-task-assignee-snapshot.json IntRuoyiBackend/yudao-module-dcc/src/test/js/dcc-obsolete-route-assignees-static.spec.cjs` -> no output, exit code 1; these evidence assets are not ignored and can be included if Git submission is authorized.
- TDD compliance gate current-state refresh:
  First attempt using `C:\IntRuoyiAll-int_main\.runtime\tools\python\python.exe` failed because that Python path does not exist. Rerun with Codex bundled Python and explicit task-relevant paths passed: `verify_tdd_compliance.py --repo C:\IntRuoyiAll-int_main --task-dir doc/tasks/20260921-dcc-three-workflows-implementation --paths <DCC obsolete service/test, browser revision readiness, static contract>` -> `TDD compliance passed.`
  The execution log now includes ASCII `RED:` and `GREEN:` evidence lines so the strict TDD verifier can parse the existing RED/GREEN record.
- Broad schema gate current-state boundary:
  Initial `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` failed with 5 failures from broad destructive-operation matching. The gate has since been repaired and rerun: `DccBaseSchemaTest` now PASS, 35 tests, 0 failures, 0 errors, 0 skipped. The repair keeps business-table `DROP TABLE`/`TRUNCATE TABLE` and `DELETE FROM dcc_` blocked, while excluding SQL comments, `DROP TEMPORARY TABLE`, and `TRUNCATE TABLE tmp_*`; it also includes SQL files that define DCC tables even if the filename lacks `_dcc_`, and supports unquoted SQL identifiers. During the repair the gate exposed and fixed a real schema drift: `dcc_controlled_file_source_governance_batch` lacked the DCC base `tenant_id` column. Added `20260922_dcc_source_governance_batch_tenant_id.sql` and refreshed policy evidence.
- Source-governance batch tenant migration policy gate:
  `run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file 20260513_dcc_base_schema.sql --sql-file 20260811_dcc_source_ownership.sql --sql-file 20260905_dcc_source_governance.sql --sql-file 20260922_dcc_source_governance_batch_tenant_id.sql --output doc\tasks\20260921-dcc-three-workflows-implementation\migration-policy-gate-source-governance-batch-tenant.json` -> PASS, `status=passed`, `migrationCount=4`.
- Schema-inclusive cross-module BPM+DCC core regression:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,BpmTaskCandidateInvokerTest,BpmUserTaskActivityBehaviorTest,DccBaseSchemaTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileStatusListenerTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccControlledFileObsoleteServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS.
  BPM side: 39 tests, 0 failures, 0 errors, 0 skipped. DCC side: 254 tests, 0 failures, 0 errors, 0 skipped. Total: 293 tests, 0 failures, 0 errors, 0 skipped. This run includes the repaired `DccBaseSchemaTest` broad schema gate in the same command as the BPM/DCC core workflow regression.
- Current-state document gates after evidence refresh:
  Task status and `Cleanup Keep` structure check -> PASS; `verify_tdd_compliance.py` from `IntRuoyiBackend\tool\verify_tdd_compliance.py` using Codex bundled Python -> PASS, `TDD compliance passed.`; `git diff --check` -> PASS with no whitespace errors, only existing LF/CRLF warnings.
- Source-governance batch tenant static SQL contract:
  Added `IntRuoyiBackend/script/tests/test_dcc_source_governance_batch_tenant_id_sql.py` to lock the new tenant migration metadata, MySQL idempotence guard (`information_schema.COLUMNS`, `PREPARE`/`EXECUTE`), absence of `ADD COLUMN IF NOT EXISTS`, and schema-only behavior with no business table DML. `python -m pytest ...` could not run because the bundled Python has no `pytest` module; direct Python import/call of the three `test_*` functions passed, 3 tests.
- Source-governance schema regression:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccSourceOwnershipSchemaTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, `DccBaseSchemaTest` 35 tests + `DccSourceOwnershipSchemaTest` 5 tests, total 40 tests, 0 failures, 0 errors, 0 skipped.
- Migration/schema contract combination:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,DccFormCenterPolicyMigrationTest,DccSourceOwnershipSchemaTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 44 tests, 0 failures, 0 errors, 0 skipped. This same round covers `DccBaseSchemaTest` 35, `DccFormCenterPolicyMigrationTest` 1, `DccSourceOwnershipSchemaTest` 5, `DccTaskAssigneeSnapshotMigrationTest` 1, and `DccThreeWorkflowBpmnMigrationTest` 2.
- Frontend static and type current-state refresh:
  14 DCC three-workflow/workbench/readiness static scripts passed in one loop from `IntRuoyiFronted`: route department candidate, three workflow process keys, upload training checkbox, working iteration submit, distribution/training workbench, obsolete FormCenter, obsolete entry, admin full config route, routes list display, readiness capability, upload governance UX, workbench, workbench entry, and training mine toolbar. `pnpm ts:check` also passed using the repository script and 8 GB Node heap.
- Backend governance entrypoint current-state refresh:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileRouteReadinessServiceTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccControlledFileObsoleteServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 56 tests, 0 failures, 0 errors, 0 skipped. This round covers route readiness, department matrix responsible-user configuration, admin full config package, route assignee resolution, and obsolete submission deriving assignees from the OBSOLETE action route.
- Cleanup preview current-state gate:
  `task_closeout.py --task-id 20260921-dcc-three-workflows-implementation --mode preview` using Codex bundled Python -> BLOCKED, exit code 1, because task status is still `in_progress`; the tool requires `ready_for_closeout` or `completed`. This is expected while real DB/Flowable/E2E/Git closeout evidence is missing; no cleanup apply was run.
- BPM/approval-center adjacent current-state refresh:
  `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest,BpmTaskConvertTest,BpmTaskServiceImplApprovalFilterTest,BpmProcessInstanceServiceImplTest,BpmNativeApprovalTaskProviderTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 62 tests, 0 failures, 0 errors, 0 skipped. This round covers BPM messages, task conversion, approval filtering, process-instance projection, and the native approval task provider used by the unified approval center.
- Training/distribution/finalization adjacent current-state refresh:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccTrainingTaskServiceTest,DccDistributionReceiptServiceImplTest,DccDistributionTaskServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccControlledFileFinalizationServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 85 tests, 0 failures, 0 errors, 0 skipped. This round covers training acknowledgement, concurrent acknowledgement, training tasks, electronic and paper distribution, distribution receipts, category training/distribution rules, and final doc-control activation/finalization service behavior.
- History/approval list/audit log timeline adjacent current-state refresh:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmProcessInstanceControllerVisibilityContractTest,DccApprovalTaskAdapterTest,DccApprovalTaskTimelineAdapterTest,DccControlledFileAuditControllerTest,DccControlledFileLogControllerTest,DccControlledFileAuditQueryServiceTest,DccControlledFileLogQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 36 tests, 0 failures, 0 errors, 0 skipped. This round covers BPM process-instance visibility, DCC approval task adaptation, approval timeline projection, audit/log controllers, and audit/log query services used around approval-center and history-list surfaces.
- Documentation gate refresh after history/approval-list evidence:
  PowerShell task status/cleanup keep structure check -> PASS, confirming `task.md` still has `in_progress` as the first non-empty line under `## Current Status` and lists all three migration policy JSON files in `Cleanup Keep`.
  `verify_tdd_compliance.py` using Codex bundled Python -> PASS with `TDD compliance passed.`
  `git diff --check` -> PASS, no whitespace errors; LF/CRLF warnings only.
- Permission/preview/download/signature backend current-state refresh:
  `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFilePreviewDownloadApiTest,DccControlledFilePreviewProtectionTest,DccControlledPreviewAccessServiceTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFileViewMatrixAccessServiceTest,DccDirectoryAccessPermissionServiceTest,DccCategoryPermissionAdminServiceImplTest,DccControlledFileCategoryPermissionSupportTest,DccElectronicSignatureAuthorizationServiceTest,DccElectronicSignatureAuthorizationControllerTest,DccElectronicSignatureManagementServiceTest,DccElectronicSignatureImageServiceImplTest,DccElectronicSignatureFailureAuditServiceTest,DccDownloadPolicyServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 111 tests, 0 failures, 0 errors, 0 skipped. This round covers controlled preview/download API boundaries, preview protection, controlled preview access, review/view matrix access services, directory access permission, category permission administration, category permission support, electronic signature authorization/management/image/failure audit, and download policy behavior.
- Permission/detail/signature/view-matrix frontend static current-state refresh:
  Selected DCC static loop first failed at `dcc-view-matrix-unified-source-static.spec.js` because the stale contract still asserted independent view-matrix preview tokens inside `CategoryReviewMatrixTable.vue`. The contract was updated to the current split boundary: review matrix reverse lookup remains in `CategoryReviewMatrixTable.vue`, while view matrix table/preview/users/risks/reverse lookup are asserted from `CategoryViewMatrixTable.vue` and `CategoryViewMatrixUserLookupDialog.vue`.
  After the contract update, `node tests/e2e/dcc-view-matrix-unified-source-static.spec.js` -> PASS. The selected 26-script loop also -> PASS, covering approval upload/view mode/summary, browser state consistency, controlled viewer permission, controlled-file state projection, doc-control path confirmation, detail signature/route/handling/distribution summaries, review matrix, permission distribution/training tabs, training summary, signature view/records, and view-matrix contracts. `pnpm ts:check` -> PASS.
- Documentation gate refresh after permission/preview/signature evidence:
  PowerShell task status/cleanup keep structure check -> PASS, confirming `task.md` still has `in_progress` as the first non-empty line under `## Current Status` and lists all three migration policy JSON files in `Cleanup Keep`.
  `verify_tdd_compliance.py` using Codex bundled Python -> PASS with `TDD compliance passed.`
  `git diff --check` -> PASS, no whitespace errors; LF/CRLF warnings only.
- Obsolete/FormCenter/withdraw-resubmit/idempotent-replay backend current-state refresh:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccControlledFileObsoleteServiceTest,DccControlledFileObsoleteFormEffectExecutorTest,DccControlledFileWorkflowServiceImplTest,DccPublicationNotificationTransactionIntegrationTest,DccPublicationNotificationServiceTest,DccPublicationNotificationPostCommitSchedulerTest,DccPublicationNotificationDispatchOrchestratorTest,DccPublicationFollowupTransactionIntegrationTest,DccPublicationFollowupStatusServiceTest,DccPublicationFollowupServiceTest,DccPublicationFollowupQueryServiceTest,DccMessageDeliveryIdempotencyTest,DccControlledFileMessageReplayServiceTest,FormCenterRuntimeBpmCallbackTest,FormCenterBpmEventBridgeTest,FormEffectOrchestratorTest,FormCenterRepositoryBoundaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 208 tests, 0 failures, 0 errors, 0 skipped.
  BPM side: `FormCenterBpmEventBridgeTest` 7, `FormCenterRuntimeBpmCallbackTest` 21, `FormCenterRepositoryBoundaryTest` 5, `FormEffectOrchestratorTest` 5 -> PASS, total 38 tests.
  DCC side: `DccControlledFileMessageReplayServiceTest` 6, `DccControlledFileObsoleteFormEffectExecutorTest` 8, `DccControlledFileObsoleteServiceTest` 9, `DccControlledFileWorkflowServiceImplTest` 144, `DccMessageDeliveryIdempotencyTest` 2, `DccPublicationFollowupQueryServiceTest` 8, `DccPublicationFollowupServiceTest` 10, `DccPublicationFollowupStatusServiceTest` 1, `DccPublicationFollowupTransactionIntegrationTest` 4, `DccPublicationNotificationDispatchOrchestratorTest` 3, `DccPublicationNotificationPostCommitSchedulerTest` 2, `DccPublicationNotificationServiceTest` 4, `DccPublicationNotificationTransactionIntegrationTest` 7 -> PASS, total 170 tests.
  This round strengthens development-time coverage around BDD-22 to BDD-24: obsolete effect ownership, FormCenter callback/effect boundaries, workflow withdraw/resubmit behavior, message replay/idempotency, publication notification retry, and followup transaction behavior. It still does not replace real page obsolete flow, withdraw/resubmit E2E, stale receipt replay, same-key conflict, or effect-failure runtime replay evidence.
- Documentation gate refresh after obsolete/FormCenter/withdraw-resubmit evidence:
  PowerShell task status/cleanup keep structure check -> PASS, confirming `task.md` still has `in_progress` as the first non-empty line under `## Current Status` and lists all three migration policy JSON files in `Cleanup Keep`.
  `verify_tdd_compliance.py` using Codex bundled Python -> PASS with `TDD compliance passed.`
  `git diff --check` -> PASS, no whitespace errors; LF/CRLF warnings only.
- BDD-25 concurrency/idempotency backend current-state refresh:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=FormCenterRuntimeIdempotencyLookupTest,DccTrainingConcurrentAcknowledgementTest,DccPublicationNotificationTransactionIntegrationTest,DccMessageDeliveryTransactionIntegrationTest,DccImpactAssessmentTransactionIntegrationTest,DccPublicationFollowupTransactionIntegrationTest,DccWorkingSubmissionConditionTest,DccMessageDeliveryIdempotencyTest,DccControlledFileMessageReplayServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 36 tests, 0 failures, 0 errors, 0 skipped.
  BPM side: `FormCenterRuntimeIdempotencyLookupTest` 10 tests -> PASS.
  DCC side: `DccControlledFileMessageReplayServiceTest` 6, `DccImpactAssessmentTransactionIntegrationTest` 2, `DccMessageDeliveryIdempotencyTest` 2, `DccMessageDeliveryTransactionIntegrationTest` 3, `DccPublicationFollowupTransactionIntegrationTest` 4, `DccPublicationNotificationTransactionIntegrationTest` 7, `DccTrainingConcurrentAcknowledgementTest` 1, `DccWorkingSubmissionConditionTest` 1 -> PASS, total 26 tests.
  This round strengthens development-time coverage for FormCenter idempotent lookup, last-two-user training acknowledgement, publication notification concurrent serialization, message-delivery transaction behavior, impact-assessment transaction behavior, publication-followup transaction behavior, working-iteration submission/checkout race protection, message idempotency, and replay. It still does not replace real MySQL transaction interleaving, revision/obsolete competition under a real service, or page-level concurrency E2E.
- Documentation gate refresh after BDD-25 concurrency/idempotency evidence:
  PowerShell task status/cleanup keep structure check -> PASS, confirming `task.md` still has `in_progress` as the first non-empty line under `## Current Status` and lists all three migration policy JSON files in `Cleanup Keep`.
  `verify_tdd_compliance.py` using Codex bundled Python -> PASS with `TDD compliance passed.`
  `git diff --check` -> PASS, no whitespace errors; LF/CRLF warnings only.
- BDD-30 config-change/frozen-plan backend current-state refresh:
  `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=BpmDccRequiredCandidatesTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccCategoryApprovalMatrixAdminServiceImplTest,DccTrainingTaskServiceTest,DccTrainingAssignmentAckServiceTest,DccTrainingConcurrentAcknowledgementTest,DccDistributionTaskServiceImplTest,DccDistributionReceiptServiceImplTest,DccPaperDistributionAckServiceTest,DccCategoryTrainingRuleAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` using repository `.runtime` Maven/JDK 17 -> PASS, total 69 tests, 0 failures, 0 errors, 0 skipped.
  BPM side: `BpmDccRequiredCandidatesTest` 5 tests -> PASS.
  DCC side: `DccCategoryApprovalMatrixAdminServiceImplTest` 17, `DccCategoryDistributionRuleAdminServiceImplTest` 5, `DccCategoryTrainingRuleAdminServiceImplTest` 4, `DccControlledFileApprovalRouteAssigneeResolverTest` 11, `DccDistributionReceiptServiceImplTest` 5, `DccDistributionTaskServiceImplTest` 3, `DccPaperDistributionAckServiceTest` 9, `DccTrainingAssignmentAckServiceTest` 3, `DccTrainingConcurrentAcknowledgementTest` 1, `DccTrainingTaskServiceTest` 6 -> PASS, total 64 tests.
  This round strengthens development-time service-layer coverage around strict DCC candidate contracts, matrix responsible-user preview/blocking, route assignee resolution, training/distribution rule governance, training task/acknowledgement behavior, and electronic/paper distribution task/receipt handling. It still does not replace real page/runtime evidence for post-submit matrix or responsible-user changes and frozen-plan invalid-recipient blocking.
- Documentation gate refresh after BDD-30 config-change/frozen-plan evidence:
  PowerShell task status/cleanup keep structure check -> PASS, confirming `task.md` still has `in_progress` as the first non-empty line under `## Current Status` and lists all three migration policy JSON files in `Cleanup Keep`.
  `verify_tdd_compliance.py` using Codex bundled Python -> PASS with `TDD compliance passed.`
  `git diff --check` -> PASS, no whitespace errors; LF/CRLF warnings only.

## Evidence Boundary

已执行用户授权范围内的真实数据库迁移、正式 BPMN 部署表只读读取、服务运行验证、管理端配置保存和真实页面 Playwright；最终不声称 `docs/dcc-three-workflows/acceptance.md` 的 30 项 BDD 已全部完成。开发期证据和本轮真实证据共同证明：迁移、部署、服务健康和任务专用部门/三动作路线配置已通过；完整业务页面验收目前停在任务专用测试账号的首次登录改密前置条件，未通过 API/数据库写入绕过该 blocker。

## Authorized Final Validation

- 真实数据库：本机 Docker MySQL `int-ruoyi-mysql`，`127.0.0.1:23306`，数据库 `ruoyi-vue-pro`。action route schema、三流程 BPMN seed、task assignee snapshot、source governance baseline、source governance batch tenant 五份迁移首次执行成功；全部迁移第二次重复执行成功。BPMN seed 首次暴露 MySQL 8.0 collation 比较错误，修正为显式二进制比较后重跑通过。
- 正式 Flowable：只读读取 `ACT_RE_DEPLOYMENT`、`ACT_RE_PROCDEF`、`ACT_GE_BYTEARRAY`、`ACT_PROCDEF_INFO` 通过。三个 key `dcc-controlled-file-upload`、`dcc-controlled-file-revision`、`dcc-controlled-file-obsolete` 在 tenant 1、tenant 122 各有一条部署流程定义版本；upload/revision 节点包含会签、批准、培训条件、培训、分发、文控审核，obsolete 只包含会签、批准、文控审核。
- 服务运行：当前 `int_qms` 后端使用 `output\runtime\int_qms` 稳定运行包，`http://127.0.0.1:48061/actuator/health` 返回 `{"status":"UP"}`；前端 `http://127.0.0.1:8061/` 返回 HTTP 200。8081/48081 的 `int_main` 服务未被操作。
- 真实 Playwright 上传/升版前置页面：此前已通过真实页面完成登录、任务自有 PDF 预览、ACTIVE 版本检出和 WORKING 工作稿生成；本轮管理端已补齐对应 `NEW`/`REVISION` 路线和部门负责人配置，尚未重新执行正式提交。
- 真实 Playwright 作废：尚未进入作废业务动作；需要先完成任务专用测试账号的真实登录前置条件。
- 当前 E2E blocker：任务专用测试用户在真实登录页被要求先完成首次创建/重置后的密码变更。未通过数据库/API 修改密码、重置账号或模拟业务成功。
- Git/cleanup：Git 权限虽已授权，但完整真实页面验收尚未通过，按仓库规则未执行 cleanup apply、commit 或 push；任务状态为 `in_progress`，不是 `completed`。

前端已移除新上传、上传 route readiness 和工作稿升版送审请求中的废弃 `selectedSignoffUserIds` 字段；API 类型保留可选字段只作为后端拒绝越权输入的兼容边界。外部评审参与人链路仍使用自身参与人合同，不作为本次 DCC 三流程会签矩阵移除范围。

上传 route readiness 现在显式传 `actionType=NEW`，服务端按 actionType 解析对应 action route；NEW/REVISION/OBSOLETE readiness 请求若携带非空 `selectedSignoffUserIds` 会被拒绝。legacy/null actionType 仍保留旧兼容预览路径，避免扩大影响到非本次三流程入口。

`dcc-loss-order-form-center-chain-static.spec.js` 当前失败边界是历史损耗单 FormCenter 链路资产缺失：package 脚本仍引用 `scripts/dcc-loss-order-form-center-e2e-chain.mjs`，但仓库内该 runner 以及对应 20260719 历史任务目录均不存在。该脚本不作为本任务三流程代码通过/失败证据，且未用空脚本或模拟 E2E 资产绕过。

## Completion Audit

- 已有强证据：当前工作区通过三流程相关前端静态合同、补充的工作稿送审/培训分发/作废 FormCenter/路线入口静态合同、审批中心/上传入口/FormCenter 相邻静态合同、详情页/工作台/文控确认相邻静态合同、详情页/上传页/工作台核心前端静态合同、上传优化/项目代码识别/备注检入相邻静态合同、发布后续/矩阵/签名/培训/分发/文控下发范围相邻静态合同、工作台/路线/培训/分发/权限相邻静态合同、`pnpm ts:check`、`pnpm build:local`、后端 BPM+DCC 依赖闭包 Maven package、DCC 435 个当前任务后端回归、培训/分发/受控生效相邻 85 个后端回归、审批中心/最终生效相邻 41 个后端回归、审批签名/授权/理由相邻 98 个后端回归、审批动作/预览保护/矩阵视图相邻 55 个后端回归、提交/撤回/再提交与审批动作相邻 44 个后端回归、上传票据/消息/发布后续/FormCenter 预检相邻 48 个后端回归、重放/通知/逻辑身份相邻 41 个后端回归、历史/审批列表/日志时间线相邻 33 个后端回归、FormCenter/DCC 作废 owner 相邻 70 个后端回归、矩阵管理 17 个后端回归、管理端全量配置包 16 个后端回归、上传/发布/版本绑定相邻 41 个后端回归、上传/来源/预览相邻 20 个后端回归、BPM 17 个相邻回归、BPM 候选策略 25 个相邻回归、BPM/统一审批中心相邻 62 个回归、BPM+DCC 跨模块组合 200 个回归（含 seed BPMN 隔离 Flowable 内存引擎部署/路径推进/逐节点停靠）、三份 release migration policy gate JSON、机器可读 BDD 覆盖矩阵、证据资产可见性审计和 `git diff --check`；这些证明开发期代码路径、前端构建入口、后端模块打包边界、静态 BPMN seed、receiveTask 连接、部门义务快照、BPM task local 绑定、候选人严格合同、FormCenter 作废 owner 边界、query service 当前夹具完整性、route-readiness/working-iteration actionType 合同、培训/分发相邻服务、审批时间线/打印模板/统一受控内容状态转换合同、审批签名/授权/理由合同、文控最终确认目录与下发范围前端提交合同、详情页后端 actionProjection 二次门禁、上传 readiness 硬门禁、影响评估版本选择、上传名称版本递增、产品编号 DCC 项目代码来源、审批动作/预览保护合同、工作稿送审/撤回/再提交合同、上传临时票据清理/校验、消息 outbox、发布后续事务、FormCenter 上传/作废预检合同、通知/后续重放幂等相邻合同、逻辑身份并发相邻合同、旧/新 key 审批适配、FORM_ACTION 定位、日志审计与时间线相邻合同、FormCenter 作废运行态/BPM 回调桥/业务审批 effect owner 相邻合同、矩阵负责人预览/阻断合同、BPM 部门候选/发起人选择/混合候选/普通空 key 兼容合同、管理端配置包合同、统一审批中心 DCC 入口/摘要/处理合同、工作台入口/文件上下文/审批理由/路线 actionType/我的培训统一列表/分发规则懒加载/权限页拆分合同、BPM 任务投影/审批过滤/原生 provider 相邻合同、上传入口治理、来源上传上下文/名称选项/受控预览相邻合同、发布失败/版本绑定相邻合同和 upload/revision 文控审核终态闭环未在当前源码中回退。
- 最新补充强证据：作废请求 `needTraining` 输入拒绝合同已完成 RED/GREEN，`DccControlledFileObsoleteServiceTest` 更新为 9 tests PASS；作废请求合同纳入后，BPM+DCC 跨模块组合回归更新为 201 tests PASS，其中 BPM 39 tests、DCC 162 tests，0 failures，0 errors，0 skipped。
- 最新补充强证据：上传/升版提交手选会签人拒绝合同已完成 RED/GREEN，`DccControlledFileWorkflowServiceImplTest` 更新为 144 tests PASS；本轮 focused BPM+DCC 跨模块回归 160 tests PASS，其中 BPM 5 tests、DCC 155 tests，0 failures，0 errors，0 skipped。该证据覆盖废弃 `selectedSignoffUserIds` 字段不能越过部门矩阵/负责人配置的提交边界。
- 最新补充强证据：前端新上传、上传 readiness 和工作稿升版送审已不再发送废弃 `selectedSignoffUserIds` 字段；旧静态合同先 RED，更新后四个相关静态脚本 PASS，`pnpm ts:check` PASS。该证据覆盖“前端移除手选会签人”的开发期合同，但仍不替代真实页面 E2E。
- 最新补充强证据：route readiness 已按 `actionType=NEW` 解析上传 action route，并在 NEW/REVISION/OBSOLETE readiness 边界拒绝废弃手选会签人字段；目标 RED/GREEN 完成，readiness/workflow 147 tests PASS，前端 readiness actionType 静态合同和 `pnpm ts:check` PASS。该证据覆盖预检入口不再按 legacy 或客户端人员覆盖来判断上传路线。
- 最新补充强证据：actionType readiness 合同纳入后，focused BPM+DCC 跨模块回归 163 tests PASS，其中 BPM 5 tests、DCC 158 tests，0 failures，0 errors，0 skipped。该证据把 BPM 候选人、三流程 BPMN seed、作废请求、route readiness 和工作流提交合同放在同一轮编译/执行中验证。
- 最新补充强证据：前端三流程 9 个静态合同同轮 PASS；四个三流程相关 SQL 依赖闭包迁移发布策略门禁重新 PASS，`migrationCount=4`，证据 JSON 已刷新。该证据仍是开发期静态/发布策略证据，不替代真实 DB migration 执行。
- 最新补充强证据：工作稿升版送审现在在前端提交前显式预检 `REVISION` action route readiness，且不发送废弃手选会签人字段；静态合同先 RED 后 GREEN，相关前端静态组和 `pnpm ts:check` PASS。该证据覆盖升版入口 actionType 预检消费端合同，但仍不替代真实页面 E2E。
- 最新补充强证据：作废发起不再信任请求提供的 `startUserSelectAssignees`，始终按 OBSOLETE action route 解析候选人；后端 focused `DccControlledFileObsoleteServiceTest` 9 tests PASS，新增静态合同 PASS，并纳入 BPM+DCC 组合 205 tests PASS 和 expanded DCC 任务回归 440 tests PASS。该证据覆盖作废入口“路线定节点/部门负责人定人”的服务端边界，但仍不替代真实运行态 Flowable/E2E。
- 最新补充交付面证据：BPM+DCC 依赖闭包后端 package PASS，前端 `pnpm build:local` PASS。该证据确认最新代码可编译打包和前端生产构建，但不替代真实服务启动、真实 DB migration、正式 Flowable 部署或页面 E2E。
- 最新补充强证据：20 个 DCC/相邻前端静态脚本同轮 PASS，覆盖部门候选、三流程 key、上传/升版 readiness、培训/分发工作台、作废 FormCenter、管理端路线、审批中心、工作台、我的培训、分发上下文、权限页拆分和 FormCenter 上传嵌入。该证据扩大前端合同回归面，但仍不替代真实页面 E2E。
- 最新补充强证据：后端治理入口组合回归 56 tests PASS，覆盖 route readiness、矩阵负责人配置、配置包、部门负责人解析和作废服务 OBSOLETE 路线收口。该证据加强“矩阵决定部门、负责人配置决定人员、作废不能信任请求候选人”的后端入口证明，但仍不替代真实 DB/Flowable/E2E。
- 最新补充强证据：迁移/模型组合测试 4 tests PASS，release migration policy gate 刷新 PASS 且 `migrationCount=4`；该证据覆盖三流程 BPMN seed、逐部门义务快照 migration 和 FormCenter 作废 policy migration 的开发期合同，但仍不替代真实数据库首次/重复执行、正式 Flowable 部署表读取或真实运行态流程实例。
- 最新补充强证据：跨模块 BPM+DCC 核心组合回归 258 tests PASS，其中 BPM 39 tests、DCC 219 tests，覆盖候选严格合同、BPM 多实例任务行为、FormCenter 回调桥、三流程 BPMN/migration、作废入口、受控生效、状态监听和工作流服务。该证据扩大当前源码回归面，但仍不替代真实数据库迁移、正式 Flowable 部署表读取、真实页面 E2E 或服务运行验收。
- 最新补充资产审计：任务文档、三份 migration policy gate JSON、机器可读 BDD 覆盖矩阵和新增作废静态合同脚本均可被 Git 看见且未被 `.gitignore` 忽略；但当前未获 Git add/commit/push 授权，因此只记录提交前清单，不执行提交。
- 最新补充 cleanup 准备：`task.md` 已新增 `Cleanup Keep`，显式保留三份 migration policy gate JSON 和 `acceptance-coverage-matrix.json`，避免后续任务清理把迁移策略证据或完成度审计矩阵当作可删除临时产物；该准备不改变当前 `in_progress` 状态。
- 最新补充 TDD 门禁证据：任务相关生产/测试路径通过 `verify_tdd_compliance.py`，并修正执行日志中的机器可读 RED/GREEN 证据格式；该证据增强提交前门禁准备，但不替代真实 DB/Flowable/E2E/Git 收尾。
- 最新补充强证据：`DccBaseSchemaTest` broad schema gate 已从此前 35 tests / 5 failures 修复为 35 tests PASS。修复过程中补齐了 `dcc_controlled_file_source_governance_batch.tenant_id` 的正式幂等迁移与建表基线，并让 schema gate 正确处理临时表清理、DCC 表 SQL 文件发现和无反引号 SQL 标识符；新增迁移的 release migration policy gate 也已 PASS，`migrationCount=4`。
- 最新补充强证据：修复后的 `DccBaseSchemaTest` 已纳入 BPM+DCC 跨模块核心回归同轮执行，组合命令 PASS，BPM 39 tests + DCC 254 tests，合计 293 tests，0 failures，0 errors，0 skipped。该证据证明 schema gate 修复与三流程核心、作废入口、受控生效、FormCenter 回调和 BPM 候选合同在同一编译/测试轮次内兼容。
- 最新补充强证据：DCC migration/schema 合同组合命令 PASS，合计 44 tests，覆盖 broad schema gate、source-governance schema、三流程 BPMN seed 隔离 Flowable 路径、逐部门义务快照 migration 和 FormCenter obsolete policy migration。该证据把本轮新增 source-governance tenant 修复与三流程迁移合同放在同一 DCC 测试轮次内验证；仍不替代真实数据库首次/重复执行或正式 Flowable 部署表读取。
- 最新补充强证据：前端三流程/工作台/readiness 静态合同 14 scripts 同轮 PASS，`pnpm ts:check` PASS。该证据刷新了当前前端源码对部门候选、三流程 key、培训 checkbox、升版送审预检、培训/分发工作台、作废 FormCenter、管理端 action route、工作台入口和上传治理 UX 的静态合同覆盖；仍不替代真实页面 E2E。
- 最新补充强证据：后端治理入口当前态组合回归 56 tests PASS，覆盖 route readiness、矩阵负责人配置、管理端配置包、部门负责人解析和作废 OBSOLETE 路线收口。该证据再次确认“会签矩阵决定哪些部门、部门负责人配置决定谁来签、作废不能信任请求候选人”的服务端入口边界；仍不替代真实 DB/正式 Flowable/真实页面 E2E。
- 最新补充收尾门禁证据：cleanup preview 已按工具规则拒绝当前 `in_progress` 状态，未执行 apply。该证据说明当前还不能进入 closeout 清理阶段，也不能将任务伪标为完成。
- 最新补充强证据：BPM/统一审批中心相邻当前态回归 62 tests PASS，覆盖 BPM 消息、任务转换、审批过滤、流程实例投影和原生审批任务 provider。该证据刷新统一审批中心相邻边界，但仍不替代真实页面审批中心 E2E 或正式 Flowable 运行态部署证据。
- 最新补充强证据：培训/分发/文控生效相邻当前态回归 85 tests PASS，覆盖培训确认、并发确认、培训任务、电子/纸质分发、分发回执、分类培训/分发规则和最终文控生效服务层边界。该证据刷新 upload/revision 培训与分发相邻服务层证明，但仍不替代真实页面培训/分发签收和正式运行态 Flowable 证据。
- 最新补充强证据：历史/审批列表/日志时间线相邻当前态回归 36 tests PASS，覆盖 BPM 流程实例可见性、DCC 审批适配、审批时间线、审计/日志 controller 和审计/日志查询相邻边界。该证据刷新统一审批中心和历史列表相邻服务证明，但仍不替代真实页面审批中心/历史列表 E2E。
- 最新补充门禁证据：补写上述历史/审批列表证据后，任务状态/cleanup keep 结构检查、TDD 合规检查和 `git diff --check` 均 PASS；`git diff --check` 仅输出当前工作区既有 LF/CRLF warning，无 whitespace error。
- 最新补充强证据：权限/预览下载/签名授权相邻后端回归 111 tests PASS，覆盖预览下载 API、预览保护、查看/审阅矩阵访问、目录访问、类别权限、电子签名授权/管理/图片/失败审计和下载策略。该证据刷新 BDD-26 相关服务端权限边界，但仍不替代真实多账号页面访问和下载 E2E。
- 最新补充强证据：前端审批中心/详情/权限/签名/审阅矩阵/查看矩阵 26 个静态脚本 PASS，`pnpm ts:check` PASS；其中 `dcc-view-matrix-unified-source-static.spec.js` 先 RED 于旧审阅/查看矩阵合并口径，修正为当前拆分组件边界后 GREEN。该证据刷新 BDD-26 至 BDD-28 的前端静态入口覆盖，但仍不替代真实页面 E2E。
- 最新补充门禁证据：补写权限/预览/签名/查看矩阵证据后，任务状态/cleanup keep 结构检查、TDD 合规检查和 `git diff --check` 均 PASS；`git diff --check` 仅输出当前工作区既有 LF/CRLF warning，无 whitespace error。
- 最新补充强证据：作废/FormCenter/撤回重提/幂等重放相邻后端回归 208 tests PASS，覆盖 FormCenter BPM 回调桥、effect orchestrator、repository boundary、DCC 作废服务、作废 effect executor、workflow 撤回/重提、发布通知/后续事务、消息重放与幂等。该证据刷新 BDD-22 至 BDD-24 的开发期服务层覆盖，但仍不替代真实作废页面流程、撤回/重提、同键冲突、旧回执迟到和效果失败重放 E2E。
- 最新补充门禁证据：补写作废/FormCenter/撤回重提/幂等重放证据后，任务状态/cleanup keep 结构检查、TDD 合规检查和 `git diff --check` 均 PASS；`git diff --check` 仅输出当前工作区既有 LF/CRLF warning，无 whitespace error。
- 最新补充强证据：BDD-25 并发/幂等相邻后端回归 36 tests PASS，覆盖 FormCenter 幂等查找、培训最后两人并发确认、发布通知并发串行化、消息投递事务、影响评估事务、发布后续事务、工作稿提交/检出并发抢占、消息投递幂等和重放。该证据刷新 BDD-25 的开发期覆盖，但仍不替代真实 MySQL 事务交错、升版/作废竞争和页面级并发 E2E。
- 最新补充门禁证据：补写 BDD-25 并发/幂等证据后，任务状态/cleanup keep 结构检查、TDD 合规检查和 `git diff --check` 均 PASS；`git diff --check` 仅输出当前工作区既有 LF/CRLF warning，无 whitespace error。
- 最新补充强证据：BDD-30 配置变更/冻结计划失效相邻后端回归 69 tests PASS，覆盖 DCC 严格候选、矩阵负责人预览/阻断、路线负责人解析、培训/分发规则、培训任务/确认、电子/纸质分发任务与签收。该证据刷新 BDD-30 的开发期服务层覆盖，但仍不替代真实提交后矩阵/负责人变更、冻结计划人员失效页面和运行态验证。
- 最新补充门禁证据：补写 BDD-30 配置变更/冻结计划失效证据后，任务状态/cleanup keep 结构检查、TDD 合规检查和 `git diff --check` 均 PASS；`git diff --check` 仅输出当前工作区既有 LF/CRLF warning，无 whitespace error。
- 最新补充强证据：BDD-29 迁移发布策略三份 JSON 重新刷新且均 PASS：三流程 seed 闭包 `migrationCount=3`，逐部门义务快照闭包 `migrationCount=4`，source-governance batch tenant 闭包 `migrationCount=4`。首次短文件名参数运行失败是脚本路径解析边界，随后已用完整 SQL 路径覆盖为通过结果。
- 最新补充强证据：BDD-29 DCC migration/schema 组合命令再次 PASS，`DccBaseSchemaTest` 35、`DccFormCenterPolicyMigrationTest` 1、`DccSourceOwnershipSchemaTest` 5、`DccTaskAssigneeSnapshotMigrationTest` 1、`DccThreeWorkflowBpmnMigrationTest` 2，合计 44 tests，0 failures/errors/skipped。该证据刷新三流程 BPMN seed、逐部门义务快照、FormCenter obsolete policy、source-governance tenant schema 和 broad schema gate 的当前开发期覆盖；仍不替代真实 DB migration 首次/重复执行或正式 Flowable `ACT_*` 部署表读取。
- 最新补充门禁证据：补写 BDD-29 迁移/部署静态证据后，任务状态/cleanup keep 结构检查、TDD 合规检查和 `git diff --check` 均 PASS；`git diff --check` 仅输出当前工作区既有 LF/CRLF warning，无 whitespace error。
- 最新补充重复任务记录门禁审计：当前同主题目录仅有 `doc/tasks/20260921-dcc-three-workflows-design/` 与 `doc/tasks/20260921-dcc-three-workflows-implementation/`；design 任务首状态为 `blocked`，implementation 任务首状态为 `in_progress`。design 是前置开发文档任务，implementation 是当前代码实施验证任务；未发现第二个同范围实施任务仍处于 `pending/in_progress`。
- 最新补充强证据：提交/动作守卫相邻后端回归 44 tests PASS，覆盖工作稿升版提交、提交条件、待办动作守卫、普通审批动作移除、DCC 审批适配、任务动作 API 和审批原因校验。该证据刷新 BDD-15/BDD-20/BDD-23 的开发期服务层边界，但仍不替代真实页面办理、撤回/重提和正式 Flowable 运行态证据。
- 最新补充强证据：BPM 候选策略当前态回归 25 tests PASS，覆盖普通部门负责人/成员/申请人选择/混合候选策略、DCC 三流程严格候选、多实例任务行为以及 null/blank legacy process key 边界。该证据刷新 BDD-14/BDD-15 的开发期 BPM 层覆盖，但仍不替代正式 Flowable 运行态实例或真实页面办理。
- 最新补充强证据：前端三流程/工作台/readiness 当前态静态合同 14 scripts PASS，`pnpm ts:check` PASS。该证据刷新部门候选、三流程 key、上传培训 checkbox、工作稿升版送审预检、培训/分发工作台、作废 FormCenter、管理端 action route、路线列表、readiness、上传治理 UX、工作台入口和我的培训工具栏的当前源码合同；仍不替代真实页面 E2E、真实服务启动、真实 DB migration 或正式 Flowable 运行态证据。
- 最新补充交付面证据：后端 BPM+DCC 依赖闭包 `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-DskipTests" package` PASS，23 个 reactor 模块全部 SUCCESS；前端 `pnpm build:local` PASS，生成 `dist`。该证据确认当前源码可完成后端编译打包和前端生产构建，但后端 package 跳过测试，且两项不替代真实服务启动、真实 DB migration、正式 Flowable `ACT_*` 部署表读取或真实页面 E2E。
- 最新补充提交前审计证据：当前分支/状态审计显示 `int_qms...origin/int_qms`，暂存区为空，工作区仍存在大量已修改和未跟踪文件；本任务三份文档和三份 migration policy JSON 未被 `.gitignore` 忽略并可被 Git 看见。同主题任务目录复核仍只有设计任务 `blocked` 和当前实施任务 `in_progress`，没有第二个同范围实施任务处于 `pending/in_progress`。该证据支持后续收尾清单准备，但不等于已获 Git add/commit/push 授权。
- 最新补充强证据：按 `task.md` Expected Verification 聚合重跑 BPM+DCC P1 回归 201 tests PASS，其中 BPM 14 tests、DCC 187 tests，覆盖部门负责人解析、同人多部门义务、路线 actionType 管理、上传/升版工作流提交、逐部门义务快照 migration、三流程 BPMN seed 隔离 Flowable 部署/路径、DCC 严格候选、多实例任务行为和 BPM 外部签名守卫。该证据增强当前源码的开发期聚合证明，但仍不替代真实数据库迁移、正式 Flowable 部署表读取、真实页面 E2E、服务运行或 Git 收尾。
- 最新补充强证据：正常流程链路聚合回归 140 tests PASS，其中 BPM 38 tests、DCC 102 tests，覆盖 FormCenter BPM 回调桥、effect orchestrator、repository boundary、培训确认/并发确认/培训任务、电子/纸质分发、分发回执、分类培训/分发规则、文控生效、作废 effect executor 和作废服务。该证据补强上传/升版培训分支、分发、文控生效与作废 owner 的开发期组合证明，但仍不替代真实数据库迁移、正式 Flowable 部署表读取、真实页面 E2E、服务运行或 Git 收尾。
- 最新补充强证据：核心开发期聚合回归再次 PASS，BPM 14 tests + DCC 187 tests，合计 201 tests，0 failures/errors/skipped，23 个 reactor modules SUCCESS。该证据刷新部门负责人解析、同人多部门义务、路线 actionType 管理、上传/升版提交、逐部门义务快照 migration、三流程 BPMN 隔离 Flowable 路径、DCC 严格候选、多实例任务行为和外部签名守卫的当前源码证明；仍不替代真实 DB、正式 Flowable 部署表、真实页面 E2E、服务运行或 Git 收尾。
- 最新补充广域风险证据：尝试 `mvn -pl yudao-module-dcc -am test` 时，上游 `yudao-module-system` 因 `InvoiceVoucherPrintAssistantErpConfigBridgeContractTest` 缺少本机外部文件 `C:\ProjectPackage\erp-invoice-voucher-print-assistant\server.js` 失败，DCC 模块未执行；改跑 `-Dtest=Dcc*Test,*Dcc*Test` 后，DCC 模块执行 1988 tests，但注册证、产品目录、项目码相邻域产生 14 failures + 4 errors。该证据证明当前 DCC 模块广域回归不能作为通过门禁；三流程相关定向/相邻测试仍以已通过的 201、140、44、69、56、25 等组合命令为准。
- 最新补充文档状态复核证据：`docs/dcc-three-workflows/README.md`、`technical-design.md`、`acceptance.md`、`review-report.md` 已同步为设计基线口径，并明确当前实施状态、剩余 blocker 和验证证据以本实施任务报告为准。文档验证器 `node doc/tasks/20260921-dcc-three-workflows-design/verify-docs.cjs` PASS；开发前状态旧措辞 `rg` 检查无命中；文档范围 `git diff --check` PASS。
- 最新补充广域失败归因证据：解析当前 DCC surefire XML，失败类总数 11，其中 registrationcertificate 9 类、productcatalog 1 类、projectcode 1 类；三流程/受控文件相关失败类过滤结果为 0。`20260920-dcc-project-product-approval-implementation` 的验证报告明确“旧项目代码和产品目录直接新增服务入口拒绝绕过审批”，与当前 product/project 直接创建失败方向一致，说明该部分是相邻任务规则与旧回归口径未完全收口；registrationcertificate 失败也集中在独立登记证合同。该证据进一步限定广域 blocker 边界，但不把 DCC 广域回归改写为通过。
- 最新补充验收文档口径证据：`acceptance.md` 顶部状态已同步为最终验收与 TDD 计划，并指向本实施任务作为当前证据和 blocker 来源；同时保留文档/静态/单元证据不能替代真实 DB、正式 Flowable 与真实页面 E2E 的边界。
- 最新补充源码锚点审计证据：目标 `rg` 静态追踪已复核三流程 key、upload/revision 的 `needTraining` BPMN 网关、`TRAINING` 与 `DISTRIBUTION` receiveTask、作废拒绝 `needTraining`、逐部门义务快照表/DO/Mapper/保存与 BPM task-local 绑定、以及 actionType 路线和部门负责人解析链路均在当前源码中存在。该证据降低“文档/测试名与源码脱节”的风险，但仍是静态开发期证据，不替代真实数据库、正式 Flowable `ACT_*`、真实页面 E2E 或服务运行态。
- 证据不足：`acceptance.md` 要求的完整上传/升版/作废审批节点办理、培训/分发签收、负责人变更冻结、并发和历史列表页面 E2E 尚无当前运行证据；本轮真实 Playwright 在登录前置条件处阻断，不能把管理端配置保存或管理员页面动作误判为完整业务验收。
- 收尾限制：数据库、正式 Flowable、服务运行和管理端配置已按当前轮授权执行；由于任务专用测试账号首次改密前置条件未完成，仓库规则不允许把任务标记为 `completed`，因此未执行 cleanup apply、commit 或 push。
- 任务目录审计：`doc/tasks/20260921-dcc-three-workflows-design/` 是前置开发文档任务，当前为 `blocked`；`doc/tasks/20260921-dcc-three-workflows-implementation/` 是本轮代码实施任务，当前为 `in_progress`，当前 blocker 是任务专用 Playwright 账号首次改密前置条件和由此未完成的真实页面 E2E。两者职责不同，不属于重复记录；本报告只作为实施任务证据，不覆盖设计任务的 Git push 收尾。
- 文档结构审计：实施任务的 `task.md` 包含目标、里程碑、预期验证、BDD、当前状态、设计约束检查和验证摘要；任务目录下 `task.md`、`execution-log.md`、`verification-report.md`、三份 migration policy gate JSON 以及 `acceptance-coverage-matrix.json` 未被 `.gitignore` 忽略。该结构审计仅证明任务资产可追踪，不替代真实 DB/BPM/E2E/Git 收尾门禁。

## Acceptance Coverage Audit

- BDD-01 至 BDD-05（三套正常流程与培训分支）：开发期证据已覆盖 upload/revision/obsolete 三个 key、upload/revision 的 `needTraining` 网关与 receiveTask、作废无培训/分发、文控审核后受控生效或作废 effect owner 边界；最新 140 tests 正常链路聚合已把培训、分发、文控生效、作废服务和 FormCenter 回调放在同轮回归中验证；仍缺真实页面按完整人工节点走完五条路径、真实培训/分发签收数据和正式 Flowable 部署实例证据。
- BDD-06 至 BDD-15（部门矩阵、唯一负责人、快照、同人多部门和越权输入）：后端 resolver、route readiness、矩阵管理、BPM 候选和义务快照测试已覆盖矩阵只定部门、任务创建时解析负责人、同负责人多部门不去重、废弃手选会签人拒绝和缺配置 fail fast；仍缺真实租户主数据页面配置、负责人变更前后真实待办办理、停用/跨租户/签名资格等页面级组合验证。
- BDD-16 至 BDD-21（升版、分发必经、培训/分发聚合、文控终态和失败重试）：已有后端单元/相邻回归覆盖升版 `REVISION` readiness、检入培训值、培训完成触发 `TRAINING`、手动分发触发 `DISTRIBUTION`、文控审核终态 finalization 和部分发布失败/版本绑定相邻合同；仍缺真实前端工作稿送审、多人培训时长、电子/纸质/混合分发签收、存储失败后重试的端到端运行证据。
- BDD-22 至 BDD-24（作废 FormCenter owner、撤回/重提、幂等）：已有 FormCenter/DCC 作废 owner、obsolete policy migration、作废请求拒绝培训字段、作废发起只走 OBSOLETE action route 和若干幂等/重放相邻回归；仍缺真实作废页面流程、撤回/重提、旧回执迟到、效果失败重放和同键冲突的真实运行态证据。
- BDD-25（并发）：已有 36 tests 当前态并发/幂等相邻回归覆盖 FormCenter 幂等查找、培训最后两人并发确认、发布通知并发串行化、消息投递事务、影响评估事务、发布后续事务、工作稿提交/检出并发抢占、消息投递幂等和重放；但 `acceptance.md` 要求真实事务交错、升版/作废竞争等证据，当前未执行真实 MySQL/真实服务并发测试，不能判定完成。
- BDD-26 至 BDD-28（权限、页面一致性、历史列表）：前端静态合同和后端审批中心/工作台/时间线/权限相邻测试已覆盖多个入口的字段、key、定位和只读投影合同；仍缺真实多账号页面访问、签名下载权限、长部门名 UI、历史列表无重复待办等 E2E 证据。
- BDD-29（迁移首次/重复执行与切换窗口）：目标数据库首次/重复迁移和正式 Flowable `ACT_*` 部署表读取已通过；仍缺旧在途记录阻断启用以及完整真实页面流程的运行版本核验证据。
- BDD-30（提交后配置变化和冻结计划失效）：已有任务创建时负责人快照、作废/上传/升版不信任客户端候选人，以及 69 tests 当前态相邻回归覆盖 DCC 严格候选、矩阵负责人预览/阻断、路线负责人解析、培训/分发规则、培训任务/确认、电子/纸质分发任务与签收；仍缺真实提交后矩阵/负责人变更、冻结分发计划人员失效的页面和运行态验证。

结论：当前代码开发验证已经显著覆盖服务端、前端静态、迁移静态和隔离 BPM 引擎层，但 `acceptance.md` 的最终验收仍必须补真实 DB、正式 Flowable、真实页面 E2E、真实并发和 Git 收尾证据；因此当前状态保持 `in_progress`。

机器可读审计：`acceptance-coverage-matrix.json` 已按 BDD-01 至 BDD-30 逐条记录开发期证据、缺失的最终验收证据、状态和完成结论。当前完整性检查 PASS：30 条 BDD 全部存在，所有条目均为 `development_verified_final_blocked` / `not_complete`，且 5 个全局最终 blocker 仍保留；该文件用于后续 closeout 前复核，不能作为完成证据替代真实 DB、正式 Flowable、真实页面 E2E、真实并发或 Git 收尾。

机器可读审计刷新：`acceptance-coverage-matrix.json` 中作废相关 BDD 的 `FormCenter/DCC obsolete owner` 证据计数已从旧的 70 tests 更新为当前回读验证后的 71 tests；矩阵仍保持 30 条 BDD、全部 `not_complete`，不改变最终验收 blocker。

机器可读审计刷新：`acceptance-coverage-matrix.json` 中作废/FormCenter/撤回重提相关 BDD 的证据计数已从旧的 208 tests 更新为当前回读验证后的 246 tests，对应 `obsoleteFormCenterReplayAggregation`；矩阵仍保持 30 条 BDD、全部 `not_complete`，不改变最终验收 blocker。

证据资产清单：`evidence-assets-manifest.json` 已记录任务文档、三份 migration policy gate JSON 和 `acceptance-coverage-matrix.json` 的 Git 可见性、忽略状态与 SHA-256。该清单用于后续提交/清理前核对证据文件完整性，不能作为真实 DB、正式 Flowable、真实页面 E2E、真实并发或 Git 收尾的替代证据。

源码锚点资产：`source-anchor-audit.json` 已将三流程 key、培训网关、作废拒绝培训字段、逐部门义务快照、BPM obligation task-local 绑定和部门负责人解析链路的源码路径与行号固化为机器可读证据，并已加入 `Cleanup Keep`。当前路径/行号完整性校验 PASS，7 个 anchors 和 14 个 evidence items 均指向存在文件和有效行号；该文件只证明当前源码锚点存在，不替代真实 DB、正式 Flowable、真实页面 E2E、真实并发或 Git 收尾。

源码锚点语义校验：`source-anchor-audit.json` 对应源码窗口的 14 组语义 token 校验 PASS，命中三流程 key、`needTraining`、`receiveTask`、作废培训拒绝、snapshot 字段、`obligationId`、`loopCounter`、`actionType`、`candidateSourceIds` 和 `leaderUserId` 等关键字。该结果增强静态源码证据可信度，但仍不替代真实运行态验收。

Cleanup 保留一致性：`Cleanup Keep` 与 manifest 一致性门禁已执行。首次严格一一比对 RED 于 manifest 自身未列入自身 assets；修正为允许 `evidence-assets-manifest.json` 作为 keep-only 自引用例外后 PASS，当前 6 个 `Cleanup Keep` 路径、8 个 manifest assets 和实际文件存在性一致。该证据只证明任务证据资产保留策略一致，不替代最终运行态验收。

测试证据索引：`test-evidence-index.json` 已从当前 surefire XML 抽取核心聚合与正常流程聚合的类级计数。核心聚合 201 tests PASS，正常流程聚合 140 tests PASS，所有记录均为 0 failures/errors/skipped。该索引用于 closeout 前复核开发期测试证据，不替代真实 DB、正式 Flowable、真实页面 E2E、服务运行或 Git 收尾。

提交与动作守卫证据索引：`test-evidence-index.json` 新增 `submissionActionGuardAggregation`，逐类记录 44 个 surefire tests，覆盖工作稿升版提交、提交条件、待办动作守卫、普通审批动作移除、DCC 审批适配、任务动作 API 和审批原因校验；该聚合全部 0 failures/errors/skipped。它补强 BDD-15/BDD-20/BDD-23 的开发期服务层证据，但仍不替代真实页面办理、正式 Flowable 运行态、真实数据库迁移、服务运行验证或 Git 收尾。

BPM 候选策略证据索引：`test-evidence-index.json` 新增 `bpmCandidateStrategyAggregation`，逐类记录 25 个 surefire tests，覆盖普通部门负责人/成员/申请人选择/混合候选策略、DCC 严格候选合同、多实例任务行为以及 null/blank legacy process key 边界；该聚合全部 0 failures/errors/skipped，并已挂接到 `evidence-coverage-crosscheck.json` 的 `department-owner-signoff` 分组。它补强 BDD-06 至 BDD-15 的 BPM 层开发期证明，但仍不替代真实 Flowable 运行态实例、真实负责人变更后待办或页面办理证据。

并发与幂等证据索引：`test-evidence-index.json` 新增 `concurrencyIdempotencyAggregation`，逐类记录 36 个 surefire tests，覆盖 FormCenter 幂等查找、培训并发确认、发布通知/后续事务、消息投递事务/幂等、影响评估事务、工作稿提交并发条件和受控文件消息重放；该聚合全部 0 failures/errors/skipped，并已挂接到 `evidence-coverage-crosscheck.json` 的 `concurrency` 分组。它补强 BDD-25 的开发期服务层证据，但仍不替代真实 MySQL 事务交错、真实升版/作废竞争或页面级并发 E2E。

作废 FormCenter/重放证据索引：`test-evidence-index.json` 新增 `obsoleteFormCenterReplayAggregation`，逐类记录当前 surefire XML 中的 246 个 tests，其中 BPM FormCenter 38 tests、DCC 作废/重放/通知/workflow 208 tests；覆盖作废服务与效果执行器、上传/升版 workflow service 守卫、FormCenter BPM 回调/事件桥/仓储/效果编排、发布通知与后续事务、消息投递幂等和受控文件消息重放；该聚合全部 0 failures/errors/skipped，并已挂接到 `evidence-coverage-crosscheck.json` 的 `obsolete-formcenter-idempotency` 分组。它补强 BDD-22 至 BDD-24 的开发期服务层证据，但仍不替代真实作废页面、撤回重提、旧回调重放、同键冲突或服务运行态证据。

FormCenter/DCC 作废所有权证据索引：`formCenterObsoleteOwnershipAggregation` 已收录 11 个 surefire XML、71 tests、0 failures/errors/skipped，并挂接到 `obsolete-formcenter-idempotency` 交叉覆盖组；覆盖 FormCenter 作废 runtime、BPM 回调桥、业务审批 effect executor/registry/binding、DCC FormCenter policy migration、作废 effect executor、作废服务和作废存储边界。该聚合补强作废单独流程和效果所有权边界的开发态证据，但仍不替代真实作废页面流、正式 Flowable 运行态或真实 DB 迁移。

迁移/schema 证据索引：`test-evidence-index.json` 新增 `migrationSchemaAggregation`，逐类记录 44 个 surefire tests，覆盖 broad DCC schema、FormCenter obsolete policy migration、source ownership schema、逐部门义务快照 migration 和三流程 BPMN migration；该聚合全部 0 failures/errors/skipped，并已挂接到 `evidence-coverage-crosscheck.json` 的 `migration-and-deployment` 分组。它补强 BDD-29 的开发期迁移/schema/隔离 BPMN 证据，但仍不替代真实数据库首次/重复执行或正式 Flowable `ACT_*` 部署表读取。

配置变更/冻结计划证据索引：`test-evidence-index.json` 新增 `configChangeFrozenPlanAggregation`，逐类记录 69 个 surefire tests，覆盖 DCC 严格候选、矩阵负责人预览/阻断、路线负责人解析、培训/分发规则、培训任务/确认、电子/纸质分发任务与签收；该聚合全部 0 failures/errors/skipped，并已挂接到 `evidence-coverage-crosscheck.json` 的 `configuration-change-and-frozen-plan` 分组。它补强 BDD-30 的开发期服务层证据，但仍不替代真实提交后矩阵/负责人变更、冻结分发计划人员失效或页面运行态验证。

治理入口证据索引：`test-evidence-index.json` 新增 `governanceEntrypointAggregation`，逐类记录 56 个 surefire tests，覆盖 route readiness、分类审批矩阵配置、管理端全量配置包合同、部门负责人路线解析和作废 OBSOLETE 路线入口收口；该聚合全部 0 failures/errors/skipped，并已挂接到 `department-owner-signoff` 与 `configuration-change-and-frozen-plan` 分组。它补强配置入口层面的开发期证据，但仍不替代真实租户页面配置、真实负责人变更或运行态 E2E。

培训/分发/文控生效证据索引：`trainingDistributionFinalizationAggregation` 已收录 9 个 surefire XML、85 tests、0 failures/errors/skipped，并挂接到 `revision-training-distribution-finalization` 交叉覆盖组，用于补强 BDD-16..BDD-21 的开发态证据：培训任务、培训确认、并发确认、电子/纸质分发、分发回执、类别培训/分发规则和文控受控生效。

流程历史/状态推进相邻证据索引：`workflowHistoryStatusAggregation` 已收录 8 个 surefire XML、24 tests、0 failures/errors/skipped，并挂接到普通流程、升版培训分发终态、权限/UI/历史交叉覆盖组，用于补强时间线适配、审计/日志入口、日志查询、受控文件状态监听、外部文件审阅状态监听、升版来源选择和上传上下文。

BPM/统一审批中心投影证据索引：`bpmApprovalCenterProjectionAggregation` 已收录 5 个 surefire XML、62 tests、0 failures/errors/skipped，并挂接到普通流程与权限/UI/历史交叉覆盖组；覆盖 BPM 原生审批任务 provider、任务转换、审批过滤、流程实例投影和消息服务，用于补强三动作在统一审批中心查询/投影侧的开发态证据。

历史/审批列表/日志时间线证据索引：`historyApprovalListTimelineAggregation` 已收录 7 个 surefire XML、36 tests、0 failures/errors/skipped，并挂接到权限/UI/历史交叉覆盖组；覆盖 BPM 审批中心流程实例可见性、DCC 审批任务适配、时间线适配、审计入口、日志入口、审计查询和日志查询。该聚合补齐验收矩阵中 `history/approval-list/log timeline 36 tests` 的机器可追溯来源，但仍不替代真实多账号页面历史/审批中心 E2E。

测试证据索引门禁：`test-evidence-index.json` 已逐项回读 surefire XML 并验证 1298 个 indexed tests 的类级计数；manifest hash、Cleanup Keep 规则口径一致性、任务状态、TDD 合规、`git diff --check`、尾随空白扫描和 Git 可见性均 PASS。Git 可见性审计显示 13 个 manifest 证据资产均为未跟踪但可见，`git check-ignore -v` 无输出/退出码 1，说明未被忽略。

历史/审批列表/日志时间线门禁刷新：重新执行包含审计查询的 7 类组合并通过，修正该聚合为 36 tests；`test-evidence-index.json` 17 组 / 1298 indexed tests 全部能回读对应 surefire XML 且计数一致；`evidence-coverage-crosscheck.json` 8 组引用全部解析；`final-evidence-readiness.json` 摘要与测试索引一致；manifest 13 个资产哈希 PASS；TDD 合规、尾随空白、任务状态和 scoped `git diff --check` 均 PASS。任务仍保持 `in_progress`，因为真实 DB/Flowable/E2E/runtime/Git 最终门禁尚未执行。

矩阵/负责人配置侧回归：按 `acceptance.md` 点名的部门/路线单元测试补跑 `DccControlledFileRouteReadinessServiceTest,DccCategoryApprovalMatrixAdminServiceImplTest`，20 tests PASS，0 failures/errors/skipped。该证据补强矩阵仅部门、负责人预览、缺负责人阻断和路线 readiness 当前态开发验证，并已纳入 `test-evidence-index.json`；仍不替代真实租户页面配置、真实负责人变更和真实待办办理 E2E。

负责人解析与矩阵访问相邻回归：补跑 `DccApprovalPositionRuntimeResolverTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFileViewMatrixAccessServiceTest,DccCategoryViewMatrixAdminServiceImplTest`，28 tests PASS，0 failures/errors/skipped。该证据补强 BDD-26 至 BDD-28 的开发期后端覆盖，包括部门负责人运行时解析、审阅/查看矩阵访问和查看矩阵管理预览；仍不替代真实多账号页面访问、长部门名 UI、审批中心/历史列表 E2E 或真实权限配置。

前端合同索引：三流程/工作台/readiness 相关 14 个前端静态脚本当前态重跑 PASS，`pnpm ts:check` PASS。新增 `frontend-evidence-index.json` 逐项记录脚本、覆盖点和类型检查命令；该证据补强 checkbox、工作稿字段、部门候选、三 key、readiness、工作台、培训/分发和作废入口的开发期前端静态覆盖，但仍不替代真实页面 E2E、真实账号权限或历史列表回看。

前端生产构建索引：`frontend-evidence-index.json#productionBuild` 已记录 `pnpm build:local` 当前态 PASS，输出包含 Vite CJS deprecation 与 stale browserslist warning 后以 `Build successful. Please see dist directory` 结束。该证据证明当前前端可完成本地生产构建，但仍不替代真实页面 E2E、真实账号权限或运行态服务验证。

上传/文控/详情前端静态索引：`frontend-evidence-index.json#uploadDocControlDetailStaticContracts` 已记录 12 个当前态 PASS 的静态脚本，覆盖上传当前版本、项目/类别 taxonomy、上传权限、受控保存闭环、OnlyOffice 文档 URL、FormCenter 上传嵌入、FormCenter/DCC 审批绕过边界、文控路径确认、最终生效重试入口、培训摘要和办理摘要。该证据补强前端源代码合同，但仍不替代真实页面 E2E。

验收证据交叉审计：新增 `evidence-coverage-crosscheck.json`，将 BDD-01 至 BDD-30 合并为 8 个验收分组，并逐组链接到已保留的后端测试索引、前端证据索引、源码锚点和 migration policy gate 资产。当前 8 个分组均存在开发期证据资产，且全部保持 `not_complete`，用于避免证据链条脱节，不替代真实 DB、正式 Flowable、真实页面 E2E、服务运行或 Git 收尾。

收尾预检：本机 `task-closeout-cleanup` preview 已按当前脚本实际参数重跑，结果 BLOCKED：任务状态仍为 `in_progress`，脚本要求 `ready_for_closeout` 或 `completed`。该结果证明当前不能进入 cleanup apply 或完成态；未执行任何清理删除动作。

Git 收尾预检：新增 `git-closeout-preflight.json`，记录当前分支 `int_qms...origin/int_qms`、暂存区为空、同主题任务目录状态，以及本任务 12 个证据资产均可被 Git 看见且未被 `.gitignore` 忽略。该文件只是提交前审计，不代表已经执行 Git add/commit/push。

最终验收准备清单：新增 `final-evidence-readiness.json`，逐项记录真实 DB migration、正式 Flowable ACT_* 读取、真实页面 E2E、服务运行验证和 Git 收尾的授权要求、未执行原因和授权后动作。该清单确认当前开发期证据已较完整，但最终验收仍未 ready。

最终验收准备清单刷新：`final-evidence-readiness.json` 已补充当前开发证据摘要：后端证据 17 组、1298 indexed tests，前端证据 11 个顶层节点，包含 14 个三流程静态脚本、26 个权限/历史静态脚本、12 个上传/文控/详情静态脚本和 `pnpm build:local` PASS；交叉覆盖 8 组、验收矩阵 30 条。该摘要只用于收尾前审计，不改变真实 DB/Flowable/E2E/runtime/Git 仍缺的最终门禁。

BDD-26 至 BDD-28 前端静态证据刷新：审批中心、详情、权限、签名、审阅矩阵、查看矩阵、分发摘要与生命周期/历史相关 26 个静态脚本当前态全部 PASS，并已纳入 `frontend-evidence-index.json` 的 `permissionHistoryStaticContracts` 分组。该证据补强权限、页面一致性、签名/下载入口、矩阵预览和历史/时间线静态覆盖，仍不替代真实多账号页面访问、长部门名 UI 或历史列表 E2E。

权限/预览/签名后端索引刷新：`test-evidence-index.json` 新增 `permissionPreviewSignatureAggregation`，将已通过的 111-test 权限/预览下载/签名授权相邻后端回归落为机器可读证据，逐类记录 surefire XML 路径、测试数和 0 failures/errors/skipped。该索引证据补强 BDD-26 至 BDD-28 的后端权限边界复查，但仍不替代真实多账号页面访问、签名下载权限和历史列表 E2E。

验收交叉审计刷新：`evidence-coverage-crosscheck.json` 的 BDD-26 至 BDD-28 permission/UI/history 分组已补充引用 `test-evidence-index.json#permissionPreviewSignatureAggregation`，确保 111-test 后端权限/预览/签名证据与现有矩阵访问、前端静态证据共同挂接到验收追踪。该文件仍只证明 traceability，不改变最终 `not_complete` 判断。

当前源码复跑刷新：权限/预览/签名后端定向回归重新 PASS 111 tests；`frontend-evidence-index.json#permissionHistoryStaticContracts` 的 26 个审批中心、详情、权限、签名、审阅矩阵、查看矩阵和历史静态合同重新全部 PASS。该刷新增强 BDD-26 至 BDD-28 的开发期证据，但不改变真实多账号页面 E2E、运行环境和 Git 收尾门禁。

前端证据索引全量复验：按 `frontend-evidence-index.json` 当前声明逐项执行 14 个三流程/工作台脚本、26 个权限/历史脚本和 12 个上传/文控/详情脚本，共 52 个静态合同全部 PASS；`pnpm ts:check` PASS；`pnpm build:local` PASS，输出 `Build successful. Please see dist directory`。本轮确认索引中的脚本均存在且当前源码合同可通过，但仍不替代真实账号页面 E2E、运行态权限和业务流程验收。

开发验证收口复核：17 个后端聚合组逐类回读 surefire XML，1298 indexed tests 的类级/组级计数一致且 failures/errors/skipped 全为 0；8 个 crosscheck 分组的证据引用全部解析；`final-evidence-readiness.json` 保持 17 组 / 1298 tests 和 `final_evidence_not_ready_authorization_required`；设计文档验证器、TDD、manifest 哈希、尾随空白、scoped `git diff --check` 和任务状态 `in_progress` 均 PASS。真实 DB、正式 Flowable、运行态、页面 E2E 和 Git 收尾仍是未授权的最终门禁。

## Remaining Work

- 逐部门义务已按 controlledFileId/stageCode/departmentId/assigneeUserId/obligationId 落库，DCC BPM 多实例候选列表已保留重复负责人，且办理任务时已按 task local `obligationId` 回写 Flowable runtime `taskId/nodeInstanceId` 到义务快照；正式部署表已核验，但仍缺真实流程实例中多部门任务办理证据。
- 三套独立 BPMN 已完成真实数据库迁移和正式 Flowable 部署表核验；完整真实运行态仍需先配置租户 1/category 908710 的 NEW、REVISION、OBSOLETE action route 及唯一有效部门负责人。
- 作废 FormCenter 独立 key、历史迁移和运行态切换窗口的代码边界已验证；真实作废页面流程尚未执行，因为缺少 OBSOLETE action route。
- 上传入口已有 `needTraining` 字段，上传/升版培训节点、分发节点和文控审核终态均已完成后端单元级触发/闭环；真实 Playwright 已完成上传预览和升版检入，但提交、培训、分发、文控审核和最终生效仍待配置完成后重跑。
## 2026-09-23 Runtime Follow-up

- PASS: task-owned `int_qms` runtime worktree slot 1 started on frontend `8062` and backend `48062`; backend build completed successfully with Java 17/Maven 3.9.11.
- PASS: real Playwright page actions completed upload approval signoff and approval for `DCC-E2E-ADMIN-1790136634742`; evidence: `approval-2026-09-23T04-57-11-561Z/result.json`.
- PASS: read-only MySQL verification shows `PENDING_APPLICANT_TRAINING_RECORD` with training required.
- BLOCKED: the real frontend training workbench did not show the task, and direct detail navigation was redirected to controlled browsing. No API/DB business-action bypass was used. Training-record upload, distribution, doc-control review, and controlled effectiveness remain unverified; task status stays `in_progress`.

## 2026-09-24 Finalization Regression And Current E2E Gate

Production fix and regression scope:

- `DccControlledFileFinalizationServiceImpl.finalizeOrdinaryApproval` no longer applies the legacy pre-stamped-PDF prerequisite to native upload/revision `PENDING_DOC_CONTROL_REVIEW`; the native route must have both published/stamped ids absent at review time, then finalization generates and binds the approved-source stamped artifact. Legacy `PENDING_DOC_CONTROL_APPROVAL` still requires both ids before transition.
- `DccFrozenApprovalSignatures` uses a 3-stage set for upload/revision and retains the existing 4-stage set for legacy. For `DEPT` stages, the frozen owner list is a multiset aligned with unique department ids; successful signatures are counted by unique `taskId`, so one user leading two departments must sign two distinct tasks. Ordinary user/position candidate stages retain unique-user semantics.
- RED was reproduced twice: first `Current controlled file cannot be published` before stamping; after making the fixture match the real three-stage route, `DCC electronic signature evidence prerequisite is missing` because the verifier still required the legacy fourth stage. A second RED modeled one same-user signature for two department obligations and was required to fail before the status CAS and PDF stamp.
- GREEN: `DccControlledFileFinalizationServiceImplTest` 52 tests pass; `DccControlledFileWorkflowServiceImplTest` 145 tests pass; same-round combined regression 197 tests pass. The two-department same-leader positive case asserts two distinct task signatures and generated file binding; the negative case asserts one task signature cannot satisfy both obligations. Legacy no-artifact rejection remains covered.
- Refreshed aggregation verification from current surefire XML: core Expected Verification 203 tests; normal-flow 143; training/distribution/finalization 88; obsolete/FormCenter replay 247. All reported failures/errors/skipped are zero. `test-evidence-index.json` now records 17 groups and 1308 grouped/indexed tests; crosscheck and acceptance entries remain `not_complete` pending live lifecycle E2E.

Runtime and page boundary:

- Task-owned registered worktree `int_qms slot=1` serves frontend `8062`, backend `48062`; full backend package completed successfully across 31 Maven reactor modules. The running nested DCC module hash matches the just-built module jar; both finalization and frozen signature classes are present. Backend health is `UP`; shared `8061/48061` was never restarted.
- Real upload-page read-only preflight for category 908710 returned `ready=true`, with a 3-stage route and exactly two DEPT signoff obligations. This does not count as upload submission.
- Two real UI upload attempts failed before any DCC master/file was inserted. One reused a template filename already reserved by an active name claim; another selected an existing project with no valid enabled MDM product master. Both exact submitted file numbers were read-only checked and have zero DCC master/file rows. Failure result JSONs are retained with temporary session/upload ticket fields redacted.
- A task-owned test project/product was then created and approved through the real DCC product-catalog page, and a unique category-matched template item was saved through its project template editor. The real upload page now returns `ready=true` for projectCodeId 267 and the unique template item. No DCC file was submitted in this prepared run.
- Remaining page blocker: the department-owner snapshot resolves to `dccE2EProd0922v1` (user 910325), but a read-only account check shows `password_credential_status=RESET_REQUIRED`. Its frozen signoff tasks cannot be actioned until the account owner completes the first password change through the real login UI. No reset, credentials edit, API/DB business bypass or substitute admin-as-owner was used.
- Test runtime stdout/application logs included the test login password due server request logging. The verified task backend alone was stopped; exact occurrences in its two task-owned logs were redacted, zero remaining matches were verified, and the same task backend package was restarted to `48062` health `UP`. The shared `48061` process/logs were untouched.
- Final real page lifecycle E2E (four upload/revision checkbox combinations, department signoff, training, distribution, doc-control activation, obsolete), final service business-path proof, and Git/cleanup closeout remain incomplete. Task status remains `in_progress`; no `ready_for_closeout` or `completed` claim is made.

Documentation and evidence status correction:

- `task.md` distinguishes the cleared 2026-09-22 route configuration blocker from the current 2026-09-24 leader login prerequisite. Existing 2026-09-22/23 evidence remains historical; the latest status does not label old route absence as current.
- The task-owned DCC project/product application was created and approved through the real product-catalog UI. One unique task-owned file-template row was saved through the real template editor and read back in the project detail; this is fixture preparation only. A later fresh upload-page readiness check returned `ready=true`. No successful DCC file submission or approval action occurred in this latest fixture.
- A previous template save click timed out because the editor row was not selected in the Cascader model; a subsequent page run asserted the leaf radio checked, retained one valid row, received PUT HTTP 200/business code 0, closed the editor and saw the saved filename. The document records that only the latter operation passed.
- `test-evidence-index.json` was re-parsed against every referenced current surefire XML: 17 groups, 1308 group-indexed tests, all zero failures/errors/skips; refreshed affected aggregates are core 203, normal-flow 143, training/distribution/finalization 88, and obsolete/FormCenter replay 247. Acceptance and crosscheck statuses remain final-blocked.
- `source-anchor-audit.json` now includes the native three-stage/legacy four-stage finalization and per-department unique-task signature behavior. Final source audit passed for 8 anchors / 17 evidence items and all recorded lines are in-range; 7 focused semantic assertions passed for stage sets, department multiset/task-id evidence, generated artifact gate, and positive/negative/legacy regressions.
- Two E2E failure JSON files have session ids and upload tickets redacted. Task backend logs emitted the test credential through request logging; both affected task-owned log batches were redacted while the verified worktree backend was stopped, then the same package restarted on `48062` with health `UP`. The shared `48061` runtime was not touched.
- Superseding leader prerequisite result: at the user's explicit request, the task-created RESET_REQUIRED owner was replaced before any new DCC document was submitted. Real `/system/dept` Playwright actions saved `910333 -> shanglei/尚磊 (1468)` and `910334 -> xujianhai/徐建海 (1524)`. Refreshed rows displayed each owner; read-only MySQL verification confirmed both exact bindings and both accounts as enabled, unlocked and `ACTIVE`. No role/password mutation and no admin-as-owner substitution occurred. Full page lifecycle E2E and Git/cleanup closeout remain open; current task state is `in_progress`.
- Follow-up real upload readiness result: after the owner update, the task-owned project 267/category template was selected in the real upload page. The page's upload preview returned HTTP 400 with eight blockers: both owners lack a system post, current-stage DCC permission, e-signature authorization and valid signature image. The submit endpoint was not called; read-only DB checks found zero project-267 master/file rows and zero source-PDF temporary-upload rows in the last 30 minutes. This is a real configuration/qualification blocker, not a product-code regression.
- Read-only signer qualification inventory found no non-admin tenant-1 user satisfying all four readiness checks while enabled, unlocked and credential status `ACTIVE`; `zhaojie` has signature authorization/image but no post. No role, post, authorization, password or signature-image changes were made. Admin cannot be used as department owner/signatory. Further end-user E2E requires an already-qualified real production/quality signer or explicit authorization for formal onboarding with genuine user-supplied signature images.
- Runtime was restored after scrubbing the credential from the two task-owned log files: the same `int_qms slot 1` backend on 48062 is health `UP` under Java PID 55052; no shared runtime was touched. The new preview failure JSON and leader-replacement script/result are preserved in the task manifest.
- Final documentation gates: 12 task JSON files parse; 17 surefire aggregation groups were checked against 148 current XML class reports (1,308 group-indexed tests, all zero failures/errors/skips); all 8 evidence crosscheck groups and 30 acceptance rows remain `not_complete`; all 16 explicit Cleanup Keep paths exist; all 20 manifest hashes match. Task status first line remains `in_progress`.
- TDD compliance verifier passes for the three changed backend source/test paths; Node `--check` passes for all 7 retained/updated DCC E2E scripts; `git diff --check` reports no whitespace errors (only Git LF/CRLF normalization warnings). No full frontend build was rerun because this continuation changed no frontend production source.
- Read-only Git preflight is current: branch `int_qms...origin/int_qms`, 472 paths changed in the full worktree, staged count 0. Six preserved ignored scripts/result files will need explicit `git add -f` if retained at eventual closeout; nothing was staged or committed. The task-owned backend was restarted after exact credential redaction and is health `UP` on `48062`; shared `48061` remains untouched.

## 2026-09-25 Current Development Verification Refresh

- RED/GREEN test repair: the current targeted Maven run first failed at test compilation because `DccApprovalTaskAdapterTest` used `assertTrue` without its JUnit static import. After adding the import, strict Mockito reported one unused `task.getName()` stub in the new ACTIVE final-review projection test; removing that stub produced GREEN.
- GREEN backend targeted regression: `mvn -pl yudao-module-bpm,yudao-module-dcc -am "-Dtest=DccApprovalTaskAdapterTest,DccControlledFileFinalizationServiceImplTest,DccControlledFileWorkflowServiceImplTest,DccControlledFileObsoleteServiceTest,DccControlledFileApprovalRouteAssigneeResolverTest,DccThreeWorkflowBpmnMigrationTest,DccTaskAssigneeSnapshotMigrationTest,BpmDccRequiredCandidatesTest,BpmNativeApprovalTaskProviderTest,BpmUserTaskActivityBehaviorTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, BPM 42 tests + DCC 245 tests = 287 tests, failures/errors/skipped all zero.
- GREEN frontend verification: seven targeted DCC static contracts PASS, `pnpm ts:check` PASS, seven retained task Playwright scripts pass `node --check`, and `pnpm build:local` ends with `Build successful. Please see dist directory`.
- PASS runtime UI source alignment: the task frontend on `8062` was confirmed to run from `C:\IntRuoyi\20260923-dcc-three-workflows-runtime`; the missing `CategoryPermissionRulesTab.vue` chain was synchronized from the current worktree. After page refresh, the authenticated admin UI visibly shows and opens the `类别权限` tab for `技术调研报告`. The newly drafted `OBSOLETE / USER / admin / GLOBAL` rule remains unsaved and is not counted as effective configuration.
- BLOCKED final runtime dependency: `intruoyi-onlyoffice` and the `onlyoffice/documentserver:latest` image were absent; the isolated start command was rejected by the execution policy, so `127.0.0.1:8080/healthcheck` remains unavailable. No backend restart, database write, API business-action bypass, signature fabrication, or Git mutation was performed in this refresh.
- Final status remains `in_progress`; real upload/revision/obsolete page lifecycle, OnlyOffice-backed revision finalization, saved category obsolete permission, qualified human signers, and Git/cleanup closeout remain incomplete.
- RED/GREEN frontend index refresh: the first full rerun of the 52 scripts declared in `frontend-evidence-index.json` exposed a stale assertion in `dcc-approval-upload-view-static.spec.js`; the current production code intentionally permits management recovery actions for `FINALIZATION_FAILED` while approval-upload handling still hides them through `showFullDetailSections`. The static contract was narrowed to those two semantic requirements, and the individual script plus all 52 indexed scripts then passed.
- PASS/current-state evidence reconciliation: all 149 class-level surefire XML references in `test-evidence-index.json` now match current report counts, including `BpmNativeApprovalTaskProviderTest` 36, `DccApprovalTaskAdapterTest` 24, and `DccDirectoryControllerTest` 11; indexed aggregate total is 1324 tests with zero failures/errors/skipped. `final-evidence-readiness.json` now records the current blockers as signer qualification and Git/cleanup closeout; the B/1 permission/OnlyOffice blockers are cleared.
- PASS/current-state OnlyOffice runtime recovery: after proxy activation, `onlyoffice/documentserver:latest` pulled successfully and Compose started `intruoyi-onlyoffice` with `8080 -> 80`, health `healthy`; container `docservice` and `converter` are RUNNING, and host `/healthcheck` returns HTTP 200/body `true`. OnlyOffice is no longer a current blocker; B/1 revision finalization page recheck remains pending.
- 2026-09-26 current continuation: real UI qualification setup completed for the explicitly authorized test environment. Admin electronic-signature authorization was already ENABLED; a new synthetic PNG was uploaded and enabled through `/signature-governance/my-signature`; both task departments were reassigned to admin through `/system/dept`. Route preview returned `ready=true` with two department obligations resolved as `[1,1]`, plus admin for MATRIX_APPROVAL and DOC_CONTROL_REVIEW.
- 2026-09-26 real MDM prerequisite: category `908710` is `DCC_FVM_DHF_002`, so the service correctly requires an enabled MDM product. Real `产品建档申请` UI submission and approval created request `8`, product binding `productMasterId=9`, and task-owned project code `269 / DCC-E2E-DCFINAL-20260926-01`.
- 2026-09-26 RED/GREEN frontend repair: a real template-editor attempt showed server taxonomy leaf `技术调研报告` id `13`, while the visible Element Plus cascader rendered index-like values and did not open children; no template PUT was sent. The editor now adds explicit `value: row.id` / `label: row.name` fields and consumes them through cascader props. `node tests/e2e/dcc-project-file-template-static.spec.js` PASS, `pnpm ts:check` PASS, and the focused BPM/DCC regression PASS with 221 tests and zero failures/errors/skips.
- 2026-09-26 remaining blocker: after the code fix, Vue setup state contains the correct tree and leaf value `13`, but the visible 8062 runtime cascader still renders the stale index panel and the real template PUT has not yet been observed. Consequently real upload/revision/obsolete lifecycle E2E and Git/cleanup closeout remain incomplete; task stays `in_progress`.
- 2026-09-26 real upload multi-instance acceptance: the task-owned project template was saved through the real UI with leaf `13`; real Playwright upload with `needTraining=false` created controlled file `2054545668044084021` under `dcc-controlled-file-upload`. Route preview resolved two department obligations `[910334,910333]` to the same admin user `[1,1]`. Read-only Flowable verification showed two concurrent `MATRIX_REVIEW` tasks, `nrOfInstances=2`, and distinct obligation IDs/task IDs, proving same-user multi-department duties remain separate.
- 2026-09-26 real upload full lifecycle acceptance: both signoff tasks and the approval task were completed through the real approval center page; manual distribution was completed through the real controlled-file detail page; doc-control review was completed through the real detail page with task context. Final read-only state is `ACTIVE` with `published_file_id=9198354931261`, `stamped_file_id=9198354931261`, and zero active Flowable tasks. Upload/without-training path is therefore GREEN through finalization; revision, training-enabled upload, obsolete path, and Git/cleanup closeout remain open.
- 2026-09-26 real revision full lifecycle acceptance: after saving directory `913869` admin query/content-view permission through the real directory authorization UI, the current-directory browser displayed A/1. Real Playwright completed checkout, major check-in to B/1 (`2054545668044084022`), submit, two department signoff tasks, approval, manual distribution, and doc-control review. Final read-only state: A/1 `SUPERSEDED`, B/1 `ACTIVE`, master current active points to B/1, and B/1 published/stamped IDs are `9198354931263`.
- 2026-09-26 native obsolete status: not passed. The task runtime frontend on 8062 is intermittently serving the old category-page module without the current “类别权限” tab; source/runtime hashes match, but frontend restart was rejected by execution policy and HMR reload was not stable. No category `OBSOLETE` rule save and no obsolete workflow submission were performed. The task remains `in_progress`; Git/cleanup closeout is also open.
- 2026-09-26 obsolete prerequisite correction: real Playwright eventually saved the category rule `OBSOLETE / USER / admin / GLOBAL` for category `908710` (HTTP 200/code 0); read-only MySQL confirms rule `id=14499`, `subject_id=1`, `active=1`. A fresh real B/1 detail read-only projection reports `allowedActions` includes `OBSOLETE`, but the visible 8062 “更多” menu still omits “作废当前版本”. Native obsolete submission remains unverified because the current runtime frontend menu is stale; no API/DB business-action substitute was used.
# 2026-09-26 Test-image substitution addendum

- Generated task-owned PNG signature materials for `admin`, `shanglei`, and `xujianhai`; the assets are retained under the task artifacts directory and do not create database records by themselves.
- Through real UI, `shanglei`/`1468` and `xujianhai`/`1524` received test password resets, the `文控` role, the `项目经理` post, and enabled electronic-signature authorization. Their real login attempts then returned `首次或重置后必须修改密码`; no API/DB bypass was used.
- Through real UI, `admin` uploaded and enabled `test-signature-admin-20260926.png`; upload and enable both returned HTTP 200/business code 0, and the page displayed the image as `已启用`.
- Real 8062 Playwright read-only probe found B/1 `DCC-E2E-ADMIN-1790431784325` visible with generated published/stamped files, but the row's `更多` menu still rendered only `修改基础信息`; `作废当前版本` was absent. The obsolete submission therefore remains `BLOCKED` on the runtime frontend entry projection, with no API/DB business-action substitute.
- 2026-09-27 final native obsolete acceptance supersedes the preceding stale-menu note: B/1 `2054545668044084022` is now `OBSOLETE`, the master is `OBSOLETE_CHAIN`, and the active obsolete permission rule remains `OBSOLETE / USER / admin / GLOBAL` (`id=14499`). Real UI verification artifact `obsolete-2026-09-27T03-50-01-966Z/result.json` is `PASS`; detail page shows `OBSOLETE`, the obsolete reason, and the action is locked/closed. No API or database business-action substitute was used.

## 2026-09-27 负责人快照/部门义务详情审计缺口修复

- Backend detail VO now returns `candidateSourceIds` plus ordered `candidateSourceNames` for DEPT route snapshots.
- Each DEPT route snapshot now returns `departmentObligations` with `snapshotId`, `departmentId/name`, `assigneeUserId/name`, `leaderConfigDigest`, `obligationId`, and `bpmTaskId`. The conversion reads the task-assignee snapshot table read-only and associates only obligations belonging to the route snapshot's stage and candidate departments.
- The detail route-snapshot table and approval-progress dialog now show `部门义务` and `负责人快照`, including the stable identifiers needed to distinguish two obligations assigned to the same user.
- RED/GREEN evidence: the new static contract first failed on the missing columns; after implementation, `DccControlledFileQueryServiceTest` passed 154/154, `pnpm ts:check` passed, all 14 DCC detail static contracts passed, and the scoped `git diff --check` passed.
- This change is read-only at query/UI level and does not change workflow actions, route resolution, task creation, or database schema. A live-page check must be run after the task-owned backend/frontend runtime is rebuilt or hot-reloaded; it was not claimed in this source-only verification.
