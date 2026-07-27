# Bug Regression Evidence

## Bug Summary

用户新建工艺路线草稿后，流转关系图“批记录表单”仍对全部工序显示“未配置”。当前关系图读取逻辑已只读取正式 `batchRecordReports`，但批记录单独升版没有生成正式路线候选绑定，且批记录版本的路线关联在连续升版中丢失。

## Expected Behavior

- Given 已存在唯一工艺路线、激活路线版本和对应工序。
- When 用户只选择升版“批记录表单”，不选择重建工艺流程。
- Then 系统生成或更新路线草稿候选版本，并按当前路线工序写入正式 `batchRecordReports`。
- Then 新批记录版本保留正式 `routeId`。
- Then `formBindings` 和 `batchRecordAttachmentOwners` 不参与批记录表单绑定。

## Reproduction

- 真实页面：工艺路线 `922119 / RT000028 / 球囊扩张压力泵`，新建草稿 V18 后选择“批记录表单”，14 个工序均显示“未配置”。
- 只读数据库：V18 的 14 个 `batchUseConfigs.batchRecordReports` 均为空；正式路线工序批记录绑定为 0；批记录版本 `130 / V14.0 / APPROVED` 有 15 张报表但没有进入路线逐工序绑定。

## Root Cause

- 批记录导入页只有在用户勾选“工艺流程”产品时才确认并提交路线候选版本参数。
- 后端只有 `routeRebuildRequested=true` 时才调用路线生成服务；只升版批记录表单不会生成正式绑定候选。
- 新批记录版本创建时没有继承来源版本 `routeId`，连续升版后路线关联变为空。

## Regression Tests

- 后端 DB 测试：仅升版批记录表单时必须生成路线草稿候选，并写入逐工序正式 `batchRecordReports`。
- 后端 DB 测试：新批记录版本必须继承来源版本 `routeId`。
- 前端静态合同：仅勾选批记录表单且存在唯一当前路线时，也必须确认并提交路线候选参数。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，草稿保存后 `batchRecordReports=[]` 被清空，且显式草稿快照中的正式报表名称无法解析。
- RED: `node tests/e2e/mes-route-flow-batch-record-form-source-static.spec.js` -> FAIL，前端关系图仍通过合并链路构建“批记录表单”，未建立正式 `batchRecordReports` 专用显示契约。
- RED: 真实数据路线 `922119` 新建 V18/V19 草稿时，当前正式绑定为 `0`；批记录版本 `130 / V14.0 / APPROVED` 有 `15` 张 MAIN 报表，但未写入路线候选逐工序 `batchRecordReports`。

## GREEN

- GREEN: `MesProBatchRecordReportServiceImplDbTest` 覆盖“仅升版批记录表单也生成路线 DRAFT 候选，并写入逐工序正式 `batchRecordReports`；同时保留 `formBindings` 且不改写生效工序设置”。
- GREEN: `MesProRouteVersionLifecycleSchemaTest` 和 `test_mes_route_version_snapshot_mediumtext_sql.py` 覆盖 `route_snapshot_json` 支持大快照。
- GREEN: `node IntRuoyiFronted\tests\e2e\mes-batch-record-import-formal-route-binding-static.spec.js` -> PASS，前端只勾选“批记录表单”时也会明确确认路线候选并提交当前路线/版本。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionLifecycleSchemaTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordRouteGovernanceContractTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`154` tests。
- GREEN: 真实页面 V19 草稿 `flow-config?useType=BATCH&routeVersionId=390` 返回 `rows=14`、`totalReports=14`、`formBindingRows=4`；点击关系图“批记录表单”显示正式生产记录名称。

## Risk And Regression Scope

- 仅修改批记录 Word 导入到路线候选正式绑定的生命周期。
- 不修改关系图读取来源，不使用名称匹配、`formBindings`、默认 `MAIN` 或工序开始负责人作为展示 fallback。
- 不直接改写生效路线；正式绑定先进入路线候选版本，发布后再由既有投影链路写入当前工序设置。

## Blockers And Follow-up

- 已通过正式 API 将现有 V19 候选快照补齐为 `totalReports=14`，用于本地复验用户截图路径；该操作不是产品修复的唯一证据，最终完成证据以 DB 回归测试、静态合同、schema 迁移测试和真实页面只读复核共同构成。
