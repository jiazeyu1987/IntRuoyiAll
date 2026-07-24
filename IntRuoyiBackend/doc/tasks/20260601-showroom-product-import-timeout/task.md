# 任务：定位展厅产品导入耗时与前端超时

## 任务目标

使用真实文件 `D:\Downloads\产品资料修改版-补充产品资料.xlsx` 复现并计量展厅产品导入耗时，确认后端是否出现异常慢路径；如根因在后端处理链路，则补充回归测试并做最小修复。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260531-showroom-product-import-actual-image-diff-still-skipped/task.md` 状态为 completed。
- 当前后端仓库仅有无关未跟踪 `runtime/`，本任务不触碰、不提交。

## BDD 场景

- BDD: 真实产品资料导入耗时可见 -> Given 使用 `产品资料修改版-补充产品资料.xlsx` 通过真实导入接口导入 / When 后端处理完成 / Then 记录接口耗时、成功数、跳过数和失败数。
- BDD: 后端导入错误必须快速可见 -> Given Excel 缺少必需字段或处理过程中发生真实错误 / When 调用导入接口 / Then 后端返回明确失败，不静默成功、不降级。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现真实文件导入耗时并记录结果。
- [x] M3：判断是否存在后端可修复慢路径。
- [x] M4：如需后端改动，补充 RED 测试、最小修复并运行 GREEN/回归。
- [x] M5：记录最终验证结果。

## 预期验证

- 真实接口导入耗时、结果数量和异常信息被记录到执行日志。
- 如后端需要改动：相关 `ShowroomProductExcelImportExportIntegrationTest` 回归通过。
- 如后端无需改动：明确记录前端请求超时为本次修复边界。

## 当前状态

status: completed

## Current Status

completed

已完成修复与验证。后端 multipart 默认上限已从 `100MB/120MB` 调整为有限的 `256MB/300MB`；新运行包可接收并处理 `D:\Downloads\产品资料修改版-补充产品资料.xlsx`。

## 最终验证

- `mvn -pl yudao-server "-Dtest=UploadMultipartLimitConfigTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。
- `mvn -pl yudao-server -am "-DskipTests" package` -> PASS。
- 新运行包直接导入真实文件 -> PASS，`totalRows=165`、`successCount=89`、`skippedCount=75`、`failureCount=1`。
- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，26 tests。
