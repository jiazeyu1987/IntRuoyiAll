# Task: 展厅产品封面改为 Codex CLI 直接生成

## Goal

将 `POST /showroom/product/generate-cover-image` 与批量封面生成链路从默认 SiliconFlow 图片模型切换为本机 Codex CLI 直接生成封面图，并继续保持：

- 只有基础信息已通过审核/已发布的产品才允许生成；
- 生成结果必须上传到文件中心并返回可直接回填前端 `coverImage` 的地址；
- 缺少 Codex CLI 配置、生成文件路径、文件内容或上传结果时必须 fail-fast，不得 fallback 回 SiliconFlow。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- 需要时补充的 `yudao-module-showroom` 定向测试
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\**`

## Non-Scope

- 不改前端按钮、审批入口或接口路径。
- 不修改产品讲解稿、公司讲解稿或 TTS 链路。
- 不保留 SiliconFlow 作为封面生成 fallback。
- 不修改数据库 schema。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-directory-tree-backend\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 最近同仓任务已闭环，不阻塞本次 showroom 封面生成链路切换。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在与本任务无关的已修改与未跟踪文件，且 `ShowroomApiRuntime.java` / `ShowroomAdminController.java` / `ShowroomDisplayController.java` 已有在途变更。
- Impact: 本次只在封面生成服务、定向测试和当前任务目录范围内追加改动，避免覆盖无关在途工作。

## Dependencies

- 本机 `codex` / `codex.cmd` 可执行，并且当前运行环境具备原生图片生成能力。
- `YudaoAiProperties.CodexCli` 可提供命令、工作目录、模型和超时配置。
- `FileApi` 继续负责最终图片上传与 URL 回填。

## Milestones

1. 创建任务文档、执行日志、后端证据骨架，并冻结切换目标。
2. 先补 RED 测试，锁定封面生成必须走 Codex CLI 本地图片文件路径，不再依赖 SiliconFlow。
3. 以最小改动实现 Codex CLI 生成、文件读取、上传回填与 fail-fast 校验。
4. 跑通定向测试、证据校验与 closeout preview，更新任务记录。
5. 仅提交本任务相关改动。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldRequireApprovedProductBeforeGeneration+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-codex-cli-generation --mode preview`

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - 已核对最近同仓任务完成状态。
  - 已创建本次任务目录和记录骨架。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-directory-tree-backend\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\task.md`
- Remaining blockers:
  - RED 测试、实现与定向验证尚未完成。

### Milestone 2

- Status: Completed
- Completed work:
  - 已新增独立回归测试 `ShowroomProductCoverImageServiceTest`，先锁定“封面必须直接走 Codex CLI”的成功与失败契约。
  - 已执行 RED，确认旧实现仍报 `siliconflow api key is required`。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`（RED）
- Remaining blockers:
  - 需要将生产代码切换到 Codex CLI 并回跑 GREEN。

### Milestone 3

- Status: Completed
- Completed work:
  - 已将 `ShowroomProductCoverImageService` 从 SiliconFlow 图片模型切换为 Codex CLI 本地 PNG 生成。
  - 已增加命令、超时、路径、非 PNG、空文件、上传结果为空等 fail-fast 校验。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- Remaining blockers:
  - 需要完成定向 GREEN、证据校验与 closeout preview。

### Milestone 4

- Status: Completed
- Completed work:
  - 已回跑独立 `ShowroomProductCoverImageServiceTest` 并通过。
  - 已确认 showroom 主源码编译通过。
  - 已通过后端证据校验与 closeout preview。
- Verification evidence:
  - `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\backend-api-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-codex-cli-generation --mode preview`
- Remaining blockers:
  - 待完成任务范围提交。

### Milestone 5

- Status: Completed
- Completed work:
  - 已将提交范围收敛到封面服务、独立回归测试与本任务文档。
  - 已创建本任务独立 commit `4b2c21cd71`。
- Verification evidence:
  - `git status --short -- doc/tasks/20260521-showroom-product-cover-codex-cli-generation yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/cover/ShowroomProductCoverImageService.java yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/cover/ShowroomProductCoverImageServiceTest.java`
  - `git commit -m "任务: 展厅封面改为Codex生成"`
- Remaining blockers:
  - None.

## Current Status

- Status: Completed
- Completed work:
  - 已确认现有封面生成服务已切换为 Codex CLI 直接生成本地 PNG，再上传文件中心。
  - 已通过本机 probe 验证 `codex exec` 可生成真实本地 PNG 路径。
  - 已通过独立单元回归测试验证成功与 fail-fast 路径。
  - 已完成后端证据校验与 closeout preview。
  - 已完成本任务范围提交：`4b2c21cd71`。
- Remaining blockers:
  - 模块既有 `ShowroomHttpApiIntegrationTest` 当前受工作树内历史契约漂移阻塞，尚不能作为本次最终集成 GREEN。
  - None within the committed Codex CLI cover-generation scope.

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
- PASS: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-codex-cli-generation --mode preview`
- PASS: `git commit -m "任务: 展厅封面改为Codex生成"` -> `4b2c21cd71`
- BLOCKED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldRequireApprovedProductBeforeGeneration+productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test`，当前工作树仍受既有 `ShowroomHttpApiIntegrationTest` 编译错误阻塞，不构成本次已提交 Codex CLI 切换的新增缺陷。
