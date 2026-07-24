# 任务：展厅产品导入空单元格保留当前值（后端）

## 任务目标

调整展厅产品管理 Excel 导入语义：除 `展品编码` 必须填写用于匹配已有产品外，导入行中的空白单元格不覆盖当前产品数据。缺失表头仍快速失败，不把缺列当作空值保留。

## 前序任务检查

- 已检查 `doc/tasks/20260531-yingtai-admin-super-admin/task.md`，状态为 completed，不阻塞本任务。
- 本任务只改本机仓库，不操作测试服、正式服或远程服务器。

## 里程碑

- [x] M1：建立任务文档、BDD 场景、预期验证和证据文件。
- [x] M2：添加 RED 回归测试，证明空中文名/空文字列/空展柜名的旧行为不符合要求。
- [x] M3：实现导入 draft 的空值保留逻辑和展柜映射保留逻辑。
- [x] M4：运行目标后端回归并记录 GREEN 证据。
- [x] M5：收尾清理预览并提交本任务直接相关改动。

## BDD 场景

- BDD: 空单元格保留产品字段 -> Given Excel 行只填写 `展品编码` 且其它可导入字段为空 / When 导入产品 / Then 当前产品中英文名、生命周期、BU、在售国家、适应症、型号规格、注册证信息、卖点文案和封面均保持不变，且无变化时跳过发布。
- BDD: 局部空单元格保留当前值 -> Given Excel 行仅填写部分字段 / When 导入产品 / Then 非空字段覆盖当前值，空字段保留当前值，并只在确有变化时发布新版本。
- BDD: 空展柜名称保留映射 -> Given 产品已有展柜映射且 Excel `展柜名称` 为空 / When 导入产品 / Then 不移动、不删除该产品现有展柜映射。
- BDD: 非空非法值仍失败可见 -> Given Excel `持证公司` 与当前所属公司不一致或 `在售/在研` 为未知值 / When 导入产品 / Then 按行失败并返回清晰原因，不静默跳过或降级。

## 预期验证

- RED：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 先失败，原因是旧实现会清空空白文字字段或要求空展柜名称必填。
- GREEN：同一命令通过。
- REGRESSION：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。

## 当前状态

status: completed

已完成后端实现、目标测试、回归测试、当前源码 jar 打包、本地真实前端 E2E 和收尾清理预览。

## 验证结果

- RED：`ShowroomProductExcelImportExportIntegrationTest` 先失败，证明旧实现会清空空白中文名、拒绝空英文名/空展柜名，并丢失空 `在售国家` 的当前片段。
- GREEN：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，`Tests run: 42, Failures: 0, Errors: 0, Skipped: 0`。
- PACKAGE：`mvn -pl yudao-server -am -DskipTests package` 通过，生成包含当前 showroom 模块改动的 `yudao-server.jar`。
- E2E：测试租户 `测试租户/aoteman` 通过 `http://127.0.0.1:8081/showroom/product` 导入只填写 `展品编码=product_001` 的 xlsx；导入请求命中当前源码后端 `48082`，结果显示 `跳过无变化：1`，导入后中文名、版本号和封面保持不变。
- CLEANUP PREVIEW：`task_closeout.py --task-id 20260531-showroom-product-import-blank-keep-current --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json` 通过，`delete=[]`，`blocked=[]`。

## Cleanup Keep

- `doc/tasks/20260531-showroom-product-import-blank-keep-current/backend-api-evidence.md`
