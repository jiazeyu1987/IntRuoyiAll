# 展厅产品正式版导入封面旧文件缺失修复

## Task Goal

修复导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` 正式版时，Excel 行已携带封面图片但当前产品旧封面文件读取失败，导致发布失败的问题。

## Previous Task Check

- 上一个展厅导入任务 `20260602-showroom-product-import-owner-company-124` 已标记 `completed`。
- 当前仓库存在 DCC/infra 相关未提交改动和未跟踪任务目录，和本任务无关，本任务不接管、不回滚、不提交。

## Milestones

- [x] M1: 建立任务记录，确认上一展厅任务状态，并记录设计约束。
- [x] M2: 用回归测试复现当前封面文件缺失时导入失败。
- [x] M3: 修复导入封面解析逻辑，使 Excel 已携带封面时可替换不可读取的旧封面。
- [x] M4: 运行目标测试、相关回归测试和缺陷证据校验。
- [x] M5: 收尾预览、提交本任务改动。

## Expected Verification

- `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-showroom-product-import-cover-current-file-missing\execution-log.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-showroom-product-import-cover-current-file-missing --mode preview`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本修复将“Excel 携带封面图片时上传该图片替换旧封面”定义为导入主流程；旧封面不可读取时不得继续复用旧引用。
- `是否从根因和长期维护角度解决`：是。根因是封面对比步骤强依赖旧文件可读，阻断了 Excel 封面作为权威导入数据的发布流程。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Completed Work

- 已建立任务文档。
- 已确认上一展厅导入任务完成，且当前 DCC/infra 改动不属于本任务。
- 已新增回归测试并复现失败：当前旧封面读取异常会导致行导入失败。
- 已修复导入封面解析逻辑：Excel 携带封面时，如果旧封面读取失败，则上传 Excel 封面替换旧引用。
- 已运行目标测试和相关回归测试，均通过。
- `bug-regression-fix-loop` 证据校验通过。
- `task-closeout-cleanup` 预览结果：delete `<none>`、blocked `<none>`、warnings `<none>`。

## Verification Evidence

- RED: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，期望原因：导入返回失败明细 `SHOWROOM_COVER_GENERATION_FAILED: failed to read current product cover image`，成功数为 0。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，1 个测试通过。
- REGRESSION: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，39 个测试通过。
- EVIDENCE: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-showroom-product-import-cover-current-file-missing\execution-log.md` -> PASS。
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260602-showroom-product-import-cover-current-file-missing --mode preview` -> ready，delete `<none>`，blocked `<none>`。

## Remaining Blockers

- 无。
