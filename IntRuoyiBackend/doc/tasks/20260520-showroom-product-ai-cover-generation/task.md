# Task: 展厅产品 AI 封面生成接口

## Goal

为 `展厅 / 产品管理` 提供真实的产品 AI 封面生成接口。接口必须基于产品当前表单中的基础信息构造生成请求，调用现有 AI 图片生成能力产出封面，并返回可直接写回前端 `封面` 表单的文件地址。只有基础信息已通过审核的产品才允许生成；未审核时必须 fail-fast 返回明确错误，不得 mock 成功、静默跳过或降级到人工占位图。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\config\YudaoAiProperties.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\src\main\resources\application.yaml`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-ai-cover-generation\**`

## Non-Scope

- 不改公司、展厅或讲解稿的生成链路。
- 不新增 fallback 图片、默认图片或兼容分支。
- 不修改与本次封面生成无关的 workflow 审批规则。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\task.md`
- Status before this task: `Completed on 2026-05-20`
- Impact: 上一条 showroom 产品 AI 能力任务已完成，本次可以在同一产品链路上继续补封面生成接口。

## Milestones

- [x] M1: 记录本次后端任务文档、BDD 场景和验证口径。
- [x] M2: 先补 RED 测试，锁定“仅已审核产品可生成封面”和“生成结果返回 coverImage”契约。
- [x] M3: 实现产品封面生成服务、接口与 fail-fast 校验。
- [x] M4: 完成后端验证、证据更新与 cleanup preview。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 需要时补充的 showroom 产品封面定向测试
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-ai-cover-generation\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-product-ai-cover-generation --mode preview`

## Current Status

Completed on 2026-05-20. `POST /showroom/product/generate-cover-image` 已落地：接口会先校验产品审批状态，再调用默认 SiliconFlow 图片模型生成封面，并把上传后的文件地址返回给前端回填。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldRequireApprovedProductBeforeGeneration+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-ai-cover-generation\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-product-ai-cover-generation --mode preview`
- PASS: 前端定向 `node --test scripts/showroom-admin-product-cover-field.test.mjs`

## Blockers

- 外部 blocker：`mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 仍存在与本次功能无关的既有失败：`appConfigShouldAggregateCompanyHallProductAndBilingualMedia` 与 `appConfigShouldFailFastWhenLiveProductPreviewAssetIsMissingInsteadOfFallingBackToCoverImage` 当前受 `SHOWROOM_PREVIEW_STATIC_ASSET_MISSING` 影响。该问题来自现有 preview asset 静态文件契约，不影响本次新增的封面生成接口结论。
- 已解除：测试租户 `aoteman` 的企宣审批角色绑定与 `showroom_change_request.submitter_dept_id` live schema 漂移问题均已在本地运行库修复，真实审批闭环已可推进到图片生成请求。
- 当前最终外部 blocker：真实点击 `POST /showroom/product/generate-cover-image` 时，SiliconFlow 返回 `403 {"code":30001,"message":"Sorry, your account balance is insufficient"}`。当前阻塞已明确为上游图片账户余额不足，而不是本地代码或运行库错误。
- 当前后端目标文件 `ShowroomApiRuntime.java`、`ShowroomAdminController.java`、`ShowroomHttpApiIntegrationTest.java` 在任务开始前已存在未提交改动。按“不得混提无关改动”规则，本次未自动执行 Git commit，避免把当前线程之外的修改一起提交。
