# Task: MES 工艺流程负责人编辑与关键工序产品列

## Goal

扩展 MES 工艺流程展示与编辑能力：

- 列表将 `末道工序` 改为 `关键工序`
- 列表新增 `关联产品` 列，显示所有关联产品编号
- 编辑工艺流程时允许修改该工艺流程的负责人

## Scope

- 修改 MES 工艺流程后端响应聚合逻辑与对应测试
- 不修改数据库 schema
- 仅在现有接口结构上扩展 `ownerName`、`keyProcessName`、`productCodes`
- 负责人通过现有 route remark 持久化，不引入 fallback 存储

## Previous Task Check

- Previous backend task: `doc/tasks/20260517-mes-pro-route-list-owner-last-process/task.md`
- Status before this task: completed.
- Impact: no unfinished backend task blocks this follow-up route enhancement.

## BDD

BDD: route page returns key process and product codes -> Given the MES 工艺流程分页包含已恢复的真实路线, When the frontend requests `/mes/pro/route/page`, Then each row contains `keyProcessName` and a joined `productCodes` string from all related products.

BDD: route detail supports editable owner -> Given the operator opens an existing MES 工艺流程 for edit, When the form loads and later saves `ownerName`, Then the backend returns the owner field and persists it through the existing route save flow.

## Milestones

- [x] M1: Add failing backend tests for `keyProcessName` / `productCodes` / `ownerName` round-trip.
- [x] M2: Implement backend response enrichment and owner persistence.
- [x] M3: Run targeted backend tests, package, and restart verification.
- [x] M4: Record RED/GREEN evidence and finalize status.

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplDisplayFieldsTest test`
- `mvn -pl yudao-server -am -DskipTests package`
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`

## Current Status

Completed.

## Final Verification

- `mvn -pl yudao-module-mes -Dtest=MesProRouteServiceImplDisplayFieldsTest test` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS

## Blockers

None.
