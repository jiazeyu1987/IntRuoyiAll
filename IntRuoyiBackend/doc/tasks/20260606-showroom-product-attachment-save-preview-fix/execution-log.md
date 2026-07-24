# 执行日志：展厅产品附件详情 URL 响应补齐

## BDD

- BDD: 产品附件详情返回文件 URL -> Given 产品 revision 包含图片、视频或文本附件 / When 后台查询产品详情 / Then 每个附件响应都包含 `/admin-api/infra/file/{configId}/get/{path}` 形式的正式文件 URL。
- BDD: 缺附件文件直接失败 -> Given 产品 revision 引用的附件 fileId 不存在 / When 后台查询产品详情 / Then 后端直接返回文件不存在错误，不伪造空 URL。

## TDD 记录

- RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductAttachmentTest test` -> FAIL, `ProductAttachmentRespVO` 缺少 `url` record component，`productAttachmentResponseShouldExposeFileUrl` 断言失败。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductAttachmentTest,ShowroomApiRuntimeProductMaterialMatrixTest" test` -> PASS，6 tests，覆盖附件响应 `url`、详情 URL 编码和缺文件 fail-fast。
- GREEN: `git diff --check -- yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom doc/tasks/20260606-showroom-product-attachment-save-preview-fix` -> PASS。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductAttachmentTest,ShowroomApiRuntimeProductMaterialMatrixTest" test` -> PASS，6 tests，最终复核后端附件 URL 契约。
- GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseWebsiteIndexAssemblyTest" test` -> PASS，5 tests，覆盖发布载荷中的产品附件资源与缺文件 fail-fast。
- GREEN: `git diff --check -- yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom doc/tasks/20260606-showroom-product-attachment-save-preview-fix` -> PASS。
