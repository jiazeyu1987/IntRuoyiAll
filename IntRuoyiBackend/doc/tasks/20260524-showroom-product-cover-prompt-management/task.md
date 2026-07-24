# 任务：展柜产品封面提示管理

## Goal

为 `展柜 -> 提示管理` 新增产品封面 prompt 管理能力，支持：

- 只管理 `PRODUCT_COVER` 场景的当前生效提示词；
- 每次保存创建新版本，并提供历史版本只读查看；
- 单图和批量封面生成都自动使用最新或锁定的提示词版本；
- v1 仅支持 `{{product_name_cn}}` 与 `{{product_name_en}}` 占位符，缺少产品名占位符或存在未知占位符时必须 fail-fast。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-cover-prompt-management\**`

## Non-Scope

- 不扩展到公司图、展柜图或 Website 前台图像生成
- 不支持用历史 prompt 版本直接触发重新生成
- 不新增 mock 成功、默认 prompt、兼容降级或静默回退
- 不修改产品封面生成前端请求体

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-dcc-nas-transfer-async-task\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact on this task:
  上一同仓任务已完成且范围在 `dcc`；本任务可在 `showroom` 模块独立推进，但不得混入其文件。

## Milestones

- [x] M1：核对同仓前置任务状态并建立本任务文档、执行日志和证据文件。
- [x] M2：补 schema / service / controller RED，锁定 prompt 版本保存、当前读取、历史读取和生成链路使用规则。
- [x] M3：实现 `showroom_image_prompt_version` 表、批量任务版本锁定字段、V1 种子和未完成任务回填。
- [x] M4：实现 prompt 运行时服务、权限校验、接口和封面生成链路接入。
- [x] M5：完成定向测试、证据更新、closeout 预览和提交边界检查。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest,ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomApiRuntimeBatchCoverModeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -m pytest script/tests/test_showroom_prompt_version_sql.py -q`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-cover-prompt-management\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-cover-prompt-management\database-schema-evidence.md`

## Current Status

- Completed on 2026-05-24.
- 已完成后端实现：
  - 新增 `showroom_image_prompt_version` 表、`prompt_version_id` 批量任务锁定字段和 `PRODUCT_COVER` V1 seed / backfill SQL。
  - 新增 prompt 版本运行时服务、当前/历史/保存接口，以及单图和批量封面生成对 prompt 版本的接入。
  - `ShowroomProductCoverImageService` 已收口为“只接收已渲染 prompt 文本再生成图片”。
- 已完成定向验证：
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest,ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomSchemaMapperContractTest,ShowroomApiRuntimeBatchPublishTest,ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#imagePromptManagementShouldSaveNewCurrentVersionAndExposeHistory+imagePromptManagementShouldRejectNonPublicityUsers+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `python -m pytest script/tests/test_showroom_sql_scripts.py script/tests/test_showroom_prompt_version_sql.py script/tests/test_showroom_prompt_menu_sql.py -q`
  - `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-showroom-product-cover-prompt-management --mode preview`
- 本地运行库已应用 `sql/mysql/20260524_showroom_prompt_version.sql` 并完成重启。
- 本次提交边界已收口为本任务相关后端代码、SQL、测试与任务文档；其余 `showroom` 图片生成任务产物继续留在工作区，不混入本次提交。

## Risks / Blockers

- 残余风险：真实前端单图 `AI生成` 请求在本地运行态下非常慢。
- 影响：
  - Playwright 在等待 `/admin-api/showroom/product/generate-cover-image` 响应时 120 秒超时；
  - 但后端访问日志已确认真实请求最终完成，耗时约 `764819 ms` 与 `1054806 ms`；
  - API 最终核对显示当前 `PRODUCT_COVER` 提示词版本已更新到 `V3`，`useCount=5`，产品 `id=1` 当前封面已变更为 `/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png`。
- closeout 预览结果：当前任务目录仅保留 `task.md` 与 `execution-log.md`，其余证据/脚本文件若后续生成可按预览结果清理。
