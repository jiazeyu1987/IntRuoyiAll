# G03 冻结文件清单

当前分支int_qms。Root最终软件验收已接受：M12有效963 + M14独立45 = 1008/1008 PASS，11865输入零漂移；F02/T04/B01均通过。生产/tests冻结，工程仅Git收尾未授权。SHA以final-checkpoint-manifest.json为准，不表示共享完整diff均由G03产生。

## 当前正式文件与自有测试

以下清单包含已授权阶段新增/修改或接管的合同、主链、类别、前端、迁移和专属测试。G04 更新过的两份 upload state/page 测试也列作联合冻结输入，归属仍为 Root/G04。

- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileDO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationExecutionContractService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccBoundWorkflowContractResolver.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitReqVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileSubmitIterationReqVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccWorkingApplicationSaveReqVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRoutePreviewReqVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRespVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkingApplicationAttributes.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccControlledFileRouteReadinessRespVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRouteReadinessService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/category/DccFileCategoryDO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileCategorySaveReqVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileCategoryRespVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileCategoryAdminServiceImpl.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyAdminService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/category/DccFileTypeTaxonomyAdminServiceImpl.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/vo/DccFileTypeTaxonomyRespVO.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/category/DccFileTypeTaxonomyController.java
- IntRuoyiFronted/src/api/dcc/controlledFile/fileCategories.ts
- IntRuoyiFronted/src/api/dcc/controlledFile/fileTypeTaxonomies.ts
- IntRuoyiFronted/src/views/dcc/controlled-file/categories/components/CategoryForm.vue
- IntRuoyiBackend/sql/mysql/20261015_dcc_category_approval_source_mode.sql
- IntRuoyiBackend/script/tests/test_dcc_category_approval_source_mode_sql.py
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalSourceModeTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/category/DccCategoryApprovalSourceProjectionTest.java
- IntRuoyiFronted/tests/unit/dcc-category-g03-approval-source.test.cjs
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrix.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixService.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApprovalMatrixTokenService.java
- IntRuoyiBackend/sql/mysql/20261015_dcc_application_custom_matrix.sql
- IntRuoyiBackend/script/tests/test_dcc_application_custom_matrix_sql.py
- IntRuoyiFronted/src/views/dcc/controlled-file/revision/revision-model.ts
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/DetailApplicationPanel.vue
- IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts
- IntRuoyiFronted/src/api/dcc/controlledFile/applicationRead.ts
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/CustomApplicationMatrixEditor.vue
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/file-type-options.ts
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixResolverTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixTokenTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccApplicationCustomMatrixWorkflowTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccCustomRejectedCheckinResubmitTest.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicRoundNativeSourceGuardTest.java
- IntRuoyiFronted/tests/unit/dcc-category-review-g03-source.test.cjs
- IntRuoyiFronted/tests/unit/dcc-detail-g03-custom-application.test.cjs
- IntRuoyiFronted/tests/unit/dcc-application-g03-attachment-manifest.test.cjs
- IntRuoyiFronted/tests/unit/dcc-upload-g03-custom-matrix-editor.test.cjs
- IntRuoyiFronted/tests/unit/dcc-upload-g03-custom-state.test.cjs
- IntRuoyiFronted/tests/unit/dcc-upload-g03-page.test.cjs
- IntRuoyiFronted/src/views/dcc/controlled-file/categories/components/CategoryReviewMatrixTable.vue

## 已转交或只读依赖

- upload/index.vue、upload/submitter.ts、upload/approval-matrix.ts：曾在 A23 接线，A32 已正式归还 Root/G04；当前 SHA 另列于 manifest.externallyOwnedUploadFiles，G03 不再写。
- DccControlledFileQueryServiceImpl.java / DccControlledFileRevisionServiceImpl.java：始终由 G06 修改；G03 仅提供 typed inheritance 合同/hunk 和真实组合验证，当前 SHA 另列于 manifest.externallyOwnedG06Files。
- 公共 create_tables.sql、旧 Category fixture 归 G05；未由 G03 并写。
- 日期策略/规则、定义 Catalog、签名确认等 Root/G01 独立保护文件不列为 G03 改动。

## 任务自有资产

全部位于 doc/tasks/20261014-dcc-gap-other-custom-matrix/：task、execution-log、verification-report、review-ready、changed-files、设计/hunk/转交记录、私有 runner、原始日志及 SHA。当前冻结整理只更新 Markdown / JSON，不改 runner、生产或 tests。

## 当前验证状态

软件验收已接受：M14 exit0、23 reactor SUCCESS，最终47份有效class报告1008/1008 PASS；G03自有53、A34 6及原父类通过，含最后NEW replay断言。F02 38/38、T04类型、B01构建通过，2429生产输入零漂移。工程仅Git未授权，Root统一清理/报告；本线程不改代码、不重复套件、不扩大范围，未要求的E2E/部署不记软件失败。
