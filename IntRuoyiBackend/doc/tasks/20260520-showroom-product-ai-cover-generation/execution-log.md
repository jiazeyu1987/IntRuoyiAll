# Execution Log: 展厅产品 AI 封面生成接口

BDD: 已审核产品可以生成 AI 封面 -> Given 产品基础信息已保存且审批状态为 `APPROVED` 或等价可生成状态 / When 调用产品 AI 封面生成接口 / Then 后端必须基于真实产品基础信息生成封面，并返回上传后的封面文件地址供前端回填。

BDD: 未审核产品禁止生成 AI 封面 -> Given 产品基础信息未通过审核 / When 调用产品 AI 封面生成接口 / Then 后端必须 fail-fast 返回明确错误，阻止生成动作，并提示需要产品基础信息经过审核之后才可以 AI 生成封面。

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `ShowroomProductCoverImageService`、`/showroom/product/generate-cover-image` 入口与请求/响应契约。

GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldRequireApprovedProductBeforeGeneration+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，新增两条封面生成 integration tests 通过。

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-ai-cover-generation\backend-api-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-product-ai-cover-generation --mode preview` -> PASS，preview 状态 `ready`。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，新的 showroom 封面生成逻辑与 `YudaoAiProperties.SiliconFlow.imageModel` 已打进 `yudao-server.jar`。

BLOCKER: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，现有 `appConfigShouldAggregateCompanyHallProductAndBilingualMedia` 与 `appConfigShouldFailFastWhenLiveProductPreviewAssetIsMissingInsteadOfFallingBackToCoverImage` 仍受 `SHOWROOM_PREVIEW_STATIC_ASSET_MISSING` 影响，属于与本次封面生成功能无关的既有 preview asset 契约失败。

INFO: 真实前端闭环只读排查确认运行库角色绑定缺失。`docker exec int-ruoyi-mysql ...` 查询显示：账号 `aoteman` 为 `tenant_id=122`，最初仅绑定角色 `tenant_admin`；角色 `showroom_publicity` 为 `tenant_id=1`，当前绑定给用户 `gaoxin`。随后已在本地运行库为 `tenant_id=122` 补齐 `showroom_publicity` 角色与 `aoteman` 绑定。

INFO: 真实闭环排查确认 live MySQL `showroom_change_request.submitter_dept_id` 与源码正式 SQL 基线不一致：源码 `20260519_showroom_v1_schema.sql` 与测试 schema 都允许 `DEFAULT NULL`，live 表初始为 `NOT NULL`；随后已按基线修回允许 `NULL`。

BLOCKER: 真实浏览器最终点击 `AI生成` 已经穿透到上游 SiliconFlow 图片服务，但服务返回 `403 {"code":30001,"message":"Sorry, your account balance is insufficient"}`。当前真实封面图产出阻塞已收敛为外部账户余额不足。
