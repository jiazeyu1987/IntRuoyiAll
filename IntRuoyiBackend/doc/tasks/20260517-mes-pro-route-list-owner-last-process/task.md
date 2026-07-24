# Task: MES 工艺流程列表显示负责人与末道工序

## Goal

调整 MES 工艺流程分页接口，使列表页不再依赖 `路线说明` 与 `备注` 两列，而是返回并展示 `负责人` 与 `末道工序`。

## Scope

- 只修改 MES 工艺流程分页接口的展示字段聚合逻辑与对应测试。
- 不修改工艺路线详情表单、导入逻辑、删除逻辑或权限规则。
- 保持现有接口路径不变，仅扩展分页响应字段。

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-dcc-file-category-list-columns-actions/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this route-list response change.

## BDD

BDD: route page returns owner and last process -> Given the MES 工艺流程分页包含已恢复的真实路线, When the frontend requests `/mes/pro/route/page`, Then each route row contains `ownerName` and `lastProcessName` derived from real route-product and route-process data.

BDD: route page keeps existing base fields -> Given the MES 工艺流程分页接口被调用, When the service returns the page payload, Then the existing `code/name/status/createTime` fields remain available and unchanged.

## Milestones

- [x] M1: Add failing backend tests for `ownerName` and `lastProcessName`.
- [x] M2: Implement backend page enrichment for owner and last process.
- [x] M3: Run targeted backend tests and record RED/GREEN evidence.
- [x] M4: Finalize task evidence and status.

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplDisplayFieldsTest test`
- `mvn -pl yudao-server -am -DskipTests package`
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`

## Current Status

Completed. The MES route page backend now returns `ownerName` and `lastProcessName` in the paged response.

## Final Verification

- `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplDisplayFieldsTest test` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS

## Blockers

None.
