# 任务：展厅产品导入相同产品选择覆盖或跳过（后端）

## 任务目标

扩展展厅产品 Excel 导入接口，在导入行与系统当前产品判定为相同/无变化时，要求前端显式传入选择：跳过相同产品或覆盖并发布新版本。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260531-showroom-product-import-product001-runtime-retest/task.md` 状态为 completed，不阻塞本任务。
- 当前后端仓库仅有无关未跟踪 `runtime/`，本任务不触碰、不提交。
- 本任务只改本机仓库，不操作测试服或正式服。

## BDD 场景

- BDD: 选择跳过相同产品 -> Given Excel 行与当前产品所有导入字段一致 / When 调用导入接口并指定跳过相同产品 / Then 该产品进入跳过列表，不新增 revision。
- BDD: 选择覆盖相同产品 -> Given Excel 行与当前产品所有导入字段一致 / When 调用导入接口并指定覆盖相同产品 / Then 该产品进入成功列表，并发布一个新 revision。
- BDD: 非法相同产品处理方式失败 -> Given 导入请求传入未知处理方式 / When 调用导入接口 / Then 请求快速失败并返回明确错误。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：梳理现有导入接口和测试覆盖。
- [x] M3：补充 RED 集成测试。
- [x] M4：实现后端参数解析与覆盖/跳过语义。
- [x] M5：运行 GREEN、相关回归、证据校验和 closeout 预览。
- [x] M6：提交本任务直接相关后端改动。

## 预期验证

- RED：`ShowroomProductExcelImportExportIntegrationTest` 新增相同产品覆盖测试先失败。
- GREEN：`mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- REGRESSION：导入相关后端测试通过。

## Current Status

completed

## 当前状态

status: completed

已完成后端导入接口参数解析、相同产品跳过/覆盖语义、非法参数快速失败和集成测试覆盖；待随本任务提交落库。

## 完成工作

- `/showroom/product/import-excel` 增加必填 `sameProductAction` 请求参数，仅支持 `SKIP` 与 `OVERWRITE`，缺失或未知值快速失败。
- 导入运行时在无变化产品上按 `SKIP` 保持原跳过语义，按 `OVERWRITE` 继续发布新 revision。
- 补充集成测试覆盖相同产品覆盖发布、非法处理方式失败，以及默认测试 helper 传 `SKIP` 保持既有用例行为。

## 最终验证

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，25 tests。
- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，46 tests。
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260531-showroom-product-import-same-action/backend-api-evidence.md` -> PASS。
- 浏览器 E2E 入口检查 -> BLOCKED，当前本地 48081/48082 无后端监听；`output/runtime/backend-20260531-201110.out.log` 显示启动失败原因是 `DCC download encryption config is missing or invalid: base64-key must be valid Base64`，属于本地运行配置前置条件缺失。

## Cleanup Candidates

- doc/tasks/20260531-showroom-product-import-same-action/backend-api-evidence.md
