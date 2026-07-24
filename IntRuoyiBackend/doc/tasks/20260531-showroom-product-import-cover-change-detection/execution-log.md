# 执行日志：展厅产品导入产品图参与变化判定

BDD: 相同产品图不触发新版本 -> Given 产品已有封面且 Excel 只填写 `展品编码` 并嵌入与当前封面相同的 `产品图` / When 导入产品 / Then 导入结果为跳过无变化，不上传新封面，不增加 revision。

BDD: 不同产品图触发封面变化 -> Given 产品已有封面且 Excel 只填写 `展品编码` 并嵌入不同 `产品图` / When 导入产品 / Then 导入结果成功发布，新 revision 使用新封面。

BDD: 当前封面文件缺失快速失败 -> Given 当前封面是本系统文件 URL 但文件内容无法读取 / When 导入产品图需要比较 / Then 导入行失败并返回清晰错误，不静默按变化处理。

RED: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, `importProductExcelShouldSkipWhenOnlyImportedProductImageMatchesCurrentCover` 捕获旧逻辑在 `ShowroomApiRuntime.resolveImportCoverImage` 中仍调用 `uploadImportedCoverImage`，同图导入没有在上传前与当前封面内容比较。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，23 个导入/导出集成测试通过，同图导入跳过且不上传，异图导入发布新封面。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，9 个封面服务测试通过，覆盖本系统文件 URL 内容比较、外部 URL 不比较、缺失当前封面快速失败。

GREEN: mvn -pl yudao-module-showroom -am "-Dtest=ShowroomProductExcelImportExportIntegrationTest,ShowroomProductCoverImageServiceTest,ShowroomProductContentTest,ShowroomPersistentContentServiceTest,ShowroomFoundationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，53 个后端回归测试通过。

GREEN: python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260531-showroom-product-import-cover-change-detection/backend-api-evidence.md -> PASS，后端交付证据格式有效。

CLEANUP PREVIEW: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260531-showroom-product-import-cover-change-detection --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json -> PASS，keep: task.md/execution-log.md，delete candidate: backend-api-evidence.md，blocked: none。
