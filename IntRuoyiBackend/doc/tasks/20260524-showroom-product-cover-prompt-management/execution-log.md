# 执行日志：展柜产品封面提示管理

BDD: 保存产品封面提示词版本 -> Given 企宣角色位于提示管理页并提交合法 PRODUCT_COVER 模板 / When 保存新提示词版本 / Then 系统必须创建新的版本号、保留旧版本历史，并把新版本作为当前生效版本。

BDD: 非法占位符与缺少产品名占位符必须失败 -> Given 用户提交空模板、未知占位符或未包含产品名占位符的模板 / When 保存提示词版本 / Then 系统必须返回明确校验错误，不得保存半成品版本。

BDD: 单图封面生成默认使用当前提示词版本 -> Given PRODUCT_COVER 已存在当前生效版本 / When 用户在产品管理中触发单图 AI 封面生成 / Then 服务端必须渲染当前版本模板并记录该版本 usage，而不是继续使用硬编码 prompt。

BDD: 批量封面任务必须锁定创建时提示词版本 -> Given 用户启动批量封面任务后又保存了更新的提示词版本 / When 旧任务继续执行 / Then 该任务必须继续使用创建时锁定的 promptVersionId，不得中途切换到新模板。

BDD: 历史版本只能查看不能直接生成 -> Given 系统已有多个 PRODUCT_COVER 提示词版本 / When 用户在提示管理页查看历史版本 / Then 页面必须允许只读查看完整模板，但不提供直接用旧版本生成图片的入口。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest,ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomSchemaMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，首次编写 `ShowroomImagePromptVersionServiceTest` 时多处 `assertThrows(...)))` 语法闭括号错误，测试编译阶段即失败。

GREEN: 修正 `ShowroomImagePromptVersionServiceTest` 语法并补齐 prompt service / mapper / runtime / batch-task / controller 适配后，`mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest,ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomSchemaMapperContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#imagePromptManagementShouldSaveNewCurrentVersionAndExposeHistory+imagePromptManagementShouldRejectNonPublicityUsers+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，提示词当前/历史接口、403 权限、单图封面生成与批量封面回归全部通过。

GREEN: `python -m pytest script/tests/test_showroom_sql_scripts.py script/tests/test_showroom_prompt_version_sql.py -q` -> PASS，base schema、menu seed、mysql prompt migration 和 backfill 断言全部通过。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest,ShowroomProductCoverImageServiceTest,ShowroomApiRuntimeProductCoverPersistenceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductCoverBatchTaskServiceTest,ShowroomSchemaMapperContractTest,ShowroomApiRuntimeBatchPublishTest,ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，新增/修改的 prompt、批量任务、批量发布与讲解回归测试在当前工作区全部通过。

GREEN: `python -m pytest script/tests/test_showroom_sql_scripts.py script/tests/test_showroom_prompt_version_sql.py script/tests/test_showroom_prompt_menu_sql.py -q` -> PASS，提示管理菜单 seed / 运行库菜单修补 SQL 与 prompt 版本 SQL 断言全部通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260524-showroom-product-cover-prompt-management --mode preview` -> PASS，预览结果仅保留 `task.md` 与 `execution-log.md`，未发现 blocked 项。

GREEN: 真实本地运行库迁移 -> PASS，`python -X utf8 -c "..." | docker exec -i int-ruoyi-mysql mysql -uroot -p123456 ruoyi-vue-pro` 已成功执行 `sql/mysql/20260524_showroom_prompt_version.sql`。

GREEN: 本地前后端重启 -> PASS，`mvn -pl yudao-server -am -DskipTests package` 与 `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` 完成后，`http://127.0.0.1:48081/v3/api-docs` 与 `http://127.0.0.1:8081` 均返回 `200`。

BLOCKER: Playwright 真实单图 `AI生成` 响应窗口 -> FAIL，`playwright-cli --session showroom-prompt-management run-code ...verify-showroom-prompt-management.mjs` 在等待 `/admin-api/showroom/product/generate-cover-image` 响应 120 秒后超时。

INFO: 后端访问日志 `output/runtime/backend-20260524-020047.out.log` 已确认上述真实请求最终完成，`/admin-api/showroom/product/generate-cover-image` 两次分别耗时 `764819 ms` 与 `1054806 ms`。

INFO: API 最终核对 -> PASS，使用真实测试租户登录本地运行库后，`GET /admin-api/showroom/prompt/current?sceneCode=PRODUCT_COVER` 返回当前版本 `V3`、`useCount=5`、`changeNote=playwright verify 1779559450331`；`GET /admin-api/showroom/product/get?id=1` 返回当前产品 revision `2551`，封面已更新为 `/admin-api/infra/file/28/get/showroom/product/cover/20260524/product-product_001-cover.png`。

INFO: 用户于 2026-05-24 明确要求提交当前前后端代码；本次后端提交边界锁定为 `showroom prompt management` 相关代码、SQL、测试与任务文档，不混入其他 `doc/tasks/*cover-image*` 与 `output/imagegen/*` 产物。
