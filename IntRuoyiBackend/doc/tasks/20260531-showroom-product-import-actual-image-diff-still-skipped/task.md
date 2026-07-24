# 任务：修复旧导入封面 URL 导致产品图仍提示相同

## 任务目标

针对用户反馈“导入弹窗仍提示 `product_001` 为相同产品，但实际产品图与系统封面不同”，修复旧式固定导入封面 URL 被浏览器缓存后，即使服务端文件内容已经匹配 Excel 图片，导入仍被判定为无变化而无法刷新封面地址的问题。

## 前序任务检查

- 已确认上一后端任务 `doc/tasks/20260531-showroom-product-import-product001-runtime-retest/task.md` 状态为 completed，不阻塞本任务。
- 后端仓库当前仅有无关未跟踪 `runtime/`，本任务不触碰、不提交。
- 前端仓库存在无关改动和未跟踪任务目录，本任务先限定为后端导入实际图片判定，不触碰前端改动。

## BDD 场景

- BDD: 实际图片内容不同必须算变化 -> Given 系统当前封面实际文件内容与 Excel `产品图` 嵌入图片字节不同 / When 导入该产品行 / Then 产品不能进入跳过无变化列表。
- BDD: 仅 URL 或文件名相同不能代表图片相同 -> Given 当前封面 URL 与导入封面 URL 文本相同或相似 / When 实际文件内容不同 / Then 导入必须识别为变化并发布新版本。
- BDD: 旧导入封面 URL 已匹配内容也要刷新地址 -> Given 当前封面使用旧式 `product-编码-imported-cover.png` 地址且该地址可能被前端缓存 / When Excel 嵌入图片内容与当前文件内容相同 / Then 导入仍发布一次新版本并把封面改为带内容哈希的导入封面 URL。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复核本地后端运行状态和真实 workbook/product_001 图片字节。
- [x] M3：比对系统当前封面实际字节与导入图片字节。
- [x] M4：补充 RED 回归测试。
- [x] M5：最小修复并运行 GREEN/回归。
- [x] M6：收尾清理预览并提交本任务直接相关改动。

## 预期验证

- RED：回归测试证明旧式固定导入封面 URL 在内容匹配时仍会被跳过，无法刷新前端缓存地址。
- GREEN：旧式固定导入封面 URL 会被规范化为带内容哈希的导入封面 URL，并产生新版本。
- REGRESSION：`ShowroomProductExcelImportExportIntegrationTest` 和封面相关测试通过。

## Current Status

completed

## 当前状态

status: completed

已完成后端最小修复与目标回归验证。最终验证命令：

- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 37, Failures: 0, Errors: 0, Skipped: 0`。
- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 47, Failures: 0, Errors: 0, Skipped: 0`。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence ruoyi-vue-pro\doc\tasks\20260531-showroom-product-import-actual-image-diff-still-skipped\execution-log.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-product-import-actual-image-diff-still-skipped --mode preview` -> PASS，`delete: <none>`，`blocked: <none>`。
- `mvn -pl yudao-server -am -DskipTests package` -> PASS，生成新的本地后端包。
- 本地 48081 已切换到 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260531-233551-import-cover-fix.jar`，`GET http://127.0.0.1:48081/admin-api/system/auth/get-permission-info` -> HTTP 200。
