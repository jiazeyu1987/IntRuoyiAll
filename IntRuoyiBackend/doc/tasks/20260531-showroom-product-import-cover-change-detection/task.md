# 任务：展厅产品导入产品图参与变化判定

## 任务目标

调整展厅产品管理 Excel 导入变化判定：导入行包含嵌入 `产品图` 时，先与当前产品封面做内容对比；相同则视为无变化并保留当前封面，不上传、不发布新版本；不同则作为封面变化参与导入发布。

## 前序任务检查

- 已检查 `doc/tasks/20260531-showroom-product-import-blank-keep-current/task.md`，状态为 completed，不阻塞本任务。
- 本任务只改本机仓库，不操作测试服、正式服或远程服务器。
- 当前仓库存在无关未跟踪运行态文件 `runtime/runtime-control/runtime-ops/*.json`，本任务不触碰、不提交。

## 里程碑

- [x] M1：建立任务文档、BDD 场景、预期验证和证据文件。
- [x] M2：添加 RED 回归测试，证明相同产品图会被旧逻辑先上传。
- [x] M3：实现产品图与当前封面内容对比。
- [x] M4：运行目标后端测试与回归测试并记录证据。
- [x] M5：收尾清理预览并提交本任务直接相关改动。

## BDD 场景

- BDD: 相同产品图不触发新版本 -> Given 产品已有封面且 Excel 只填写 `展品编码` 并嵌入与当前封面相同的 `产品图` / When 导入产品 / Then 导入结果为跳过无变化，不上传新封面，不增加 revision。
- BDD: 不同产品图触发封面变化 -> Given 产品已有封面且 Excel 只填写 `展品编码` 并嵌入不同 `产品图` / When 导入产品 / Then 导入结果成功发布，新 revision 使用新封面。
- BDD: 当前封面文件缺失快速失败 -> Given 当前封面是本系统文件 URL 但文件内容无法读取 / When 导入产品图需要比较 / Then 导入行失败并返回清晰错误，不静默按变化处理。

## 预期验证

- RED：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 先失败，原因是旧实现只按上传后的封面 URL 判定变化。
- GREEN：同一命令通过；额外单测 `ShowroomProductCoverImageServiceTest` 覆盖封面内容比较。
- REGRESSION：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。

## 当前状态

status: completed

已完成代码实现、GREEN 验证、后端回归验证与 task-closeout-cleanup 预览；提交仅包含本任务直接相关文件。

## 最终验证结果

- RED：导入集成测试先失败，证明旧逻辑同图导入仍会上传封面。
- GREEN：导入集成测试与封面服务测试通过。
- REGRESSION：产品导入、封面服务、内容服务与基础契约回归通过，共 53 个测试。
- CLEANUP PREVIEW：`task-closeout-cleanup` 预览无阻塞，建议保留 `task.md` 与 `execution-log.md`，额外证据文件不纳入提交。
