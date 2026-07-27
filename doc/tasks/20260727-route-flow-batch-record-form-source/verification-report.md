# Verification Report

## Summary

- 结论：已修复“新建草稿后批记录表单为空”的正式链路问题；关系图“批记录表单”只读取逐工序正式 `batchRecordReports`，不读取 `formBindings`、表单槽位或工序开始配置。
- 范围：批记录 Word 升版/导入候选生成、草稿快照读写、`route_snapshot_json` 容量、前端导入确认和关系图展示。
- 当前线程目标补充结论：尚未完成“球囊扩张导管路线绑定成功”的最终验收。精确 `球囊扩张导管` 路线缺少正式批记录表单来源；`PTCA球囊扩张导管` 已有 14/14 数据库正式绑定，但缺少测试租户可登录账号，未完成真实页面点击验证。

## Runtime Evidence

- 本地 schema 迁移：`mes_pro_route_version.route_snapshot_json` 从 `text` 升级为 `mediumtext`，用于保存包含正式批记录、表单槽位和附件负责人配置的大快照。
- 正式 API 保存后复核：路线 `922119 / RT000028 / 球囊扩张压力泵`，V19 草稿 `routeVersionId=390` 的 `flow-config?useType=BATCH` 返回 `rows=14`、`reportRows=14`、`totalReports=14`、`formBindingRows=4`。
- 真实页面复核：`http://127.0.0.1:8086` 以 `芋道源码/admin` 打开 V19 草稿流转关系图，点击“批记录表单”后，粗洗、精洗、大包装工序分别显示 `粗洗工序生产记录`、`精洗工序生产记录`、`大包装工序生产记录`，每个字段均有正式报表链接。
- 页面写入约束：真实页面验证期间 `MES` 写请求数为 `0`；截图证据保存在 `output/playwright/route-flow-batch-record-form-source/batch-record-form-field-v19.png`。

## 导管专项证据

- 默认租户 `芋道源码`：`900025 / ROUTE-XLSX-00001 / 球囊扩张导管`，DRAFT V10 `routeVersionId=272`，正式 `flow-config?useType=BATCH` 返回 `rows=23`、`rowsWithReports=0`、`totalReports=0`、`formBindingRows=0`。
- 测试租户 `测试租户`：`922046 / ROUTE-XLSX-00001 / 球囊扩张导管`，数据库正式绑定表当前为 `23` 个工序、`0` 个正式批记录报表；V2 草稿快照 `batchUseConfigs=23`，但 `batchRecordReports=0`、flat `batchRecordReportId=0`。
- 测试租户 `测试租户`：`922220 / RT000043 / PTCA球囊扩张导管`，数据库正式绑定表为 `14` 个工序、`14` 个正式报表，样例包括 `粗洗工序生产记录`、`精洗工序生产记录`、`大包装工序生产记录`。
- 测试租户页面验证阻塞：默认本机账号只能登录 `芋道源码`；用 `visit-tenant-id=122` 访问 `922220`/`922046` 的 `flow-config` 返回 `没有该操作权限`，测试租户默认 `admin` 同密码登录失败。
- 来源文件阻塞：本地仅发现 `resource\批记录压力泵.doc` 和 `resource\过程检验记录.docx`，未发现精确 `球囊扩张导管` 的 MAIN 批记录 Word 源文件；数据库也未发现精确 `球囊扩张导管` 的 MAIN 批记录版本。

## Regression Evidence

- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_route_version_snapshot_mediumtext_sql.py -q` -> PASS，`3 passed`。
- `node IntRuoyiFronted\tests\e2e\mes-route-flow-batch-record-form-source-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\mes-route-flow-batch-record-detail-slot-filter-static.spec.js` -> PASS。
- `node IntRuoyiFronted\tests\e2e\mes-batch-record-import-formal-route-binding-static.spec.js` -> PASS。
- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProRouteVersionLifecycleSchemaTest,MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordRouteGovernanceContractTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`154` tests。
- `mvn.cmd -pl yudao-server -am "-DskipTests" package` -> PASS，`BUILD SUCCESS`。

## Known Unrelated Blockers

- `pnpm ts:check` 仍失败于既有无关文件 `src/views/form-center/template/index.vue`，缺少 `isTemplateWorkspaceMode`、`isTemplateSimulationMode`、`templateWorkspaceMode`、`returnToTemplateList`、`templateRouteLoadError` 等属性。
- `pnpm build:local` 仍失败于同一无关文件 `src/views/form-center/template/index.vue (624:3): Invalid end tag`。
- 本任务未修改表单中心模板页面；上述 blocker 与批记录表单正式来源链路无关。

## Goal Blockers

- 若用户目标中的“球囊扩张导管路线”指精确名称 `球囊扩张导管`，需要先提供或导入该产品对应的正式 MAIN 批记录表单来源，并通过正式导入/升版链路生成逐工序 `batchRecordReports`。
- 若用户目标指 `PTCA球囊扩张导管`，需要提供测试租户可登录账号或授权以测试租户登录，才能完成关系图真实页面点击验证；当前只能证明数据库正式绑定为 `14/14`。

## Data Safety

- 未使用 `formBindings`、表单槽位、特殊表单、默认 `MAIN` 推断、工序开始配置或前端文案补空。
- 本地 V19 修复通过正式登录态 `flow-config/batch-record/save` API 写入候选快照；未直接 SQL 写业务绑定数据。
- `formBindings` 在 V19 中仍单独保留 `4` 行，未替代也未被正式批记录表单覆盖。
