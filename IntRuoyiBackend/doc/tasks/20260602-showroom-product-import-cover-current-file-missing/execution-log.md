# Execution Log

## BDD

BDD: 正式版导入替换不可读取的当前封面 -> Given 产品当前封面 URL 指向已缺失或不可读取的内部文件，且正式版 Excel 行携带新的嵌入封面图片；When 管理端导入该正式版 Excel 并发布产品；Then 系统应上传 Excel 中的封面图片并发布新版本，不应因为旧封面文件读取失败而中断该行导入。

## TDD Evidence

- RED: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, expected reason: 当前旧封面读取失败时，导入返回失败明细 `SHOWROOM_COVER_GENERATION_FAILED: failed to read current product cover image`，成功数为 0。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS。
- GREEN: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，39 个测试通过。

## Bug Regression Evidence

- Bug summary: 导入 `产品资料正式版.xlsx` 正式版时，部分产品 Excel 行已携带封面图片，但系统先读取当前产品旧封面做内容比对；旧封面文件缺失时行导入失败，错误为 `SHOWROOM_COVER_GENERATION_FAILED: failed to read current product cover image`。
- Expected behavior: Excel 行携带封面图片时，该图片是本次导入要发布的封面数据；当前旧封面不可读取时，应上传并使用 Excel 封面，不应复用旧封面引用，也不应阻断导入。
- Reproduction command or path: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- Root cause: 导入封面解析把当前旧封面文件可读作为内容比对的强前置条件；当旧文件记录或存储对象缺失时，`importedCoverImageMatchesCurrentCover` 抛出异常并中断当前行，Excel 中已提供的封面图片没有机会上传替换。
- Regression test added or updated: 新增 `ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover`。
- RED command and expected failure: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL，期望原因：导入返回失败明细 `SHOWROOM_COVER_GENERATION_FAILED: failed to read current product cover image`。
- GREEN command and passing result: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，1 个测试通过。
- Verification: `mvn -pl yudao-module-showroom -am '-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，39 个测试通过。
- Risk and regression scope: 影响展厅产品 Excel 导入中嵌入封面图片与当前封面比对、上传、跳过无变化的判断。
- Blockers and follow-up actions: 无。

## Root Cause

导入封面解析把当前旧封面文件可读作为内容比对的强前置条件；当旧文件记录或存储对象缺失时，Excel 中已提供的封面图片没有机会上传替换。

## Verification

- 目标回归测试通过：`importProductExcelShouldReplaceMissingCurrentCoverWhenFormalExcelProvidesEmbeddedCover`。
- 相关导入和封面服务回归通过：39 个测试。
- 缺陷证据校验通过：`python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260602-showroom-product-import-cover-current-file-missing\execution-log.md`。
- 收尾预览通过：`task-closeout-cleanup` preview 为 ready，delete `<none>`、blocked `<none>`、warnings `<none>`。
