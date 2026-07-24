# Execution Log: eDHR Word 导入绑定工艺路线产品

BDD: Word 导入绑定多个产品编码 -> Given 上传 Word 且第一个工序为产品信息，并选择多个生产工单产品名称 / When 确认导入 / Then 生成批记录表单、工艺路线、路线工序、工艺批记录路线，并把这些产品名称对应的全部产品编码去重绑定到路线。

BDD: 产品名称必填 -> Given 上传 Word 后未选择工艺路线对应产品名称 / When 确认导入 / Then 前端阻止确认，后端收到空数组时也报错并回滚所有生成内容。

BDD: 产品候选来自生产工单 -> Given 生产工单中存在多个同名或近似产品 / When 用户输入部分产品名 / Then 下拉返回生产工单实际使用过的去重产品名称。

BDD: 单个产品名称无编码跳过 -> Given 选择的部分产品名称在生产工单中查不到可绑定产品编码 / When 导入成功 / Then 这些产品名出现在 `skippedProductNames`，其余可解析产品正常绑定。

BDD: 全部产品名称无编码回滚 -> Given 选择的所有产品名称都查不到可绑定产品编码 / When 确认导入 / Then 报错并回滚批记录、路线、路线工序、用途绑定和产品绑定。

- RED: `mvn.cmd -pl yudao-module-mes -DskipTests test-compile` -> FAIL，旧测试仍按 4 参数调用 `recognizeUploadedRoute`，尚未覆盖产品名称必填与路线产品绑定返回字段。
- GREEN: experience-preflight -> PASS，真实登录已进入目标页：baseUrl=http://localhost:8081，tenant=测试租户，username=aoteman，targetPath=/mes/pro/batch-record-template。
- GREEN: `mvn.cmd -pl yudao-module-mes -DskipTests test-compile` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordReportServiceImplDbTest,MesProBatchRecordReportControllerTest" test` -> PASS，覆盖产品名称必填、产品编码去重绑定、全部无编码回滚和返回字段。
- GREEN: `node tests/e2e/electronic-batch-record-word-import-entry-static.spec.js` -> PASS，前端静态契约覆盖 .doc/.docx 入口和 productNames 提交。
- GREEN: `pnpm.cmd ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProWorkOrderServiceImplTest,MesProWorkOrderMapperTest,MesProRouteUseConfigControllerPermissionTest,MesProEdhrBatchExecutionServiceTest" test` -> PASS，72 tests。
- GREEN: real-e2e-word-import-product-binding -> PASS，使用真实文件 `C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`、测试租户 `aoteman`，导入生成 reports=15、routeProcesses=14、batchBindings=14、boundProductCodes=1；最新通过记录 batchRecordName=E2E-WORD-1783433099306，routeCode=RT000006。
- GREEN: task-closeout-cleanup preview -> PASS，在 `ruoyi-vue-pro` 仓库预览无删除项，保留 `task.md` 与 `execution-log.md`。
