# Execution Log: 展厅产品封面改为 Codex CLI 直接生成

BDD: 已审核产品使用 Codex CLI 生成封面 -> Given 产品基础信息已审核通过且具备真实基础字段 / When 管理员触发产品封面生成 / Then 后端必须调用本机 Codex CLI 生成本地 PNG、上传到文件中心，并返回可直接回填的 `coverImage` 地址。

BDD: 未审核产品仍禁止生成封面 -> Given 产品基础信息尚未通过审核 / When 管理员触发产品封面生成 / Then 后端必须 fail-fast 返回“需要产品基础信息经过审核之后才可以AI生成封面”，且不得触发 Codex CLI。

BDD: 批量生成封面只处理已发布产品 -> Given 当前筛选命中已发布与未发布产品混合结果 / When 企宣用户批量生成封面 / Then 后端必须只对已发布产品调用 Codex CLI，为成功项发布只更新 `cover_image` 的新版本，并汇总失败与跳过结果。

INFO: 本机预探针已确认 `codex exec` 可以使用原生图片生成能力，并把生成 PNG 落到 `C:\Users\BJB110\.codex\generated_images\<session>\*.png` 后返回绝对路径。

RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> FAIL，旧实现仍强依赖 `yudaoAiProperties.getSiliconflow()` 与 `AiModelFactory.getOrCreateImageModel(AiPlatformEnum.SILICON_FLOW, ...)`，导致成功路径报错 `SHOWROOM_COVER_GENERATION_FAILED: siliconflow api key is required`，不满足“直接走 Codex CLI”的新契约。

GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> PASS，3 tests green，已验证 Codex CLI 命令构造、生成 PNG 路径上传成功、缺少配置 fail-fast、返回缺失文件 fail-fast。

GREEN: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile` -> PASS，showroom 主源码在当前模块下可正常编译。

GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\backend-api-evidence.md` -> PASS，evidence contract 校验通过。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-codex-cli-generation --mode preview` -> PASS，preview 状态 `ready`，默认保留 `task.md` / `execution-log.md`，若 apply 会删除 `backend-api-evidence.md`。

GREEN: `git commit -m "任务: 展厅封面改为Codex生成"` with `TDD_TASK_DIR=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation` -> PASS，创建 commit `4b2c21cd71`。

BLOCKER: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldRequireApprovedProductBeforeGeneration+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test` 仍被当前工作树内既有的 `ShowroomHttpApiIntegrationTest` 编译错误阻塞，报错集中在 `displayRevision()` 与 `adminController.getProduct(..., ...)` 契约漂移；该阻塞先于本次切换已存在，不是本次 Codex CLI 封面改造引入。
