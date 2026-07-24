# 执行日志：修复产品资料导入 product001 图片差异被判相同

BDD: product001 导入图片不同必须算变化 -> Given 系统已有 `product001` 且当前封面与 Excel `产品图` 内容不同 / When 导入 `产品资料正式版.xlsx` 中的 product001 行 / Then `product001` 不出现在 `skippedProductCodes`，并发布新封面版本。

BDD: 空产品图仍保留当前封面 -> Given 导入行无嵌入产品图 / When 导入产品 / Then 保留当前封面，若其他字段也无变化才允许跳过。

BDD: 同一产品图仍跳过无变化 -> Given 导入图片内容与当前封面完全一致 / When 导入产品 / Then 不上传新封面、不增加 revision。

REPRO: 解析 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料正式版.xlsx` -> `产品列表` 表头包含 `产品图`，`product_001` 位于 Excel 第 2 行，图片锚点为 row0=1/col0=12，即第 2 行第 13 列 `产品图`。

ROOT CAUSE: `ShowroomProductCoverImageService.uploadImportedCoverImage(...)` 使用固定文件名 `product-<code>-imported-cover.<ext>`；同一天同产品导入不同图片时返回的 `cover_image` URL 可保持不变，`hasImportChanges(...)` 只比较字段 URL，导致不同图片被判为无变化。

RED: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL，`uploadImportedCoverImageShouldUseDifferentFileNamesForDifferentImageBytes` 捕获不同图片仍生成同一导入封面文件名；`uploadImportedCoverImageShouldUploadBytesAndReturnProxyFileUrl` 在期望 hash 文件名时无法匹配旧固定文件名。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，10 tests。不同图片字节生成不同导入封面文件名，相同导入路径不再遮蔽图片差异。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，33 tests。导入空单元格保留当前值、封面变化判定和导入封面上传组合行为通过。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，54 tests。相关展厅产品内容、持久化内容和基础契约回归通过。

NOTE: 本地 48082 后端进程使用的 jar 启动时间为 2026-05-31 17:34，早于当前修复提交；若直接在 `http://localhost:8081` 用该进程导入，弹窗仍可能展示旧行为，需要重启为包含本修复的后端构建后再验。

GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260531-showroom-product-import-product001-image-diff/bug-regression-evidence.md -> PASS，Bug regression evidence is valid。

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-product-import-product001-image-diff --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json -> PASS，预览保留 `task.md` 与 `execution-log.md`，仅识别临时 `bug-regression-evidence.md` 为可清理，无 blocked/warnings。

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-product-import-product001-image-diff --mode apply --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json -> PASS，已清理临时 `bug-regression-evidence.md`，保留正式任务记录。
