# Execution Log

## User Intent

用户要求按照已确认的根因进行设计、开发和验证：流转关系图“批记录表单”必须读取工序设置中对应工序绑定的正式批记录表单，不能读取表单槽位。

## BDD

- BDD: 关系图显示对应工序的正式批记录表单 -> Given 某路线工序在工序设置中绑定正式批记录表单且 `formBindings` 为空，When 在流转关系图选择“批记录表单”并点击该工序，Then 右侧显示正式批记录表单名称和链接，节点状态为已配置。
- BDD: 表单槽位不能替代批记录表单 -> Given 某路线工序只有 `formBindings` 动态表单槽位且没有正式批记录表单，When 在流转关系图选择“批记录表单”，Then 该工序显示“未配置”且节点状态为缺失。
- BDD: 同一基础工序的路线实例保持独立 -> Given 同一 `processId` 在路线中存在两个 `routeProcessId` 且分别绑定不同批记录表单，When 查看关系图，Then 每个节点只显示自己的正式批记录表单。
- BDD: 特殊节点负责人来源不受影响 -> Given 工序开始配置了 `batchRecordAttachmentOwners`，When 查看或执行特殊节点，Then 上传人仍来自工序开始配置，不受批记录表单和表单槽位改动影响。

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

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 2 个回归用例按预期失败：
  - 草稿保存后的 `batchRecordReports=[]`，证明正式批记录表单被清空。
  - 同一 `processId` 的两个 `routeProcessId` 可读到各自报表 ID，但报表名称为 `null`，证明显式草稿快照元数据未加载。
- RED: `node tests/e2e/mes-route-flow-batch-record-form-source-static.spec.js` -> FAIL，当前不存在只读取 `selectedLegacyBatchRecords` 的正式批记录表单专用构建函数。

## Blockers

- None.
