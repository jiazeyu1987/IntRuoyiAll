# 任务：展厅 release 脏标记与防抖合并发布

## Goal

- 将当前“产品/公司一发布就必须手工重建 showroom release 才能让 Website 看见最新内容”的链路改为后台自动合并发布。
- 发布内容变更时只标记 showroom release 为 `dirty`，不立即全量重建。
- 后台定时任务仅在存在脏变更且超过防抖窗口时才执行一次全局 release 重建，并切换 current pointer。
- 没有新发布内容时，不执行 release 重建，不增加无意义运行压力。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\release\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\job\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\release\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-release-dirty-debounce\**`

## Non-Scope

- 不修改 `Website` 前端的 release 轮询逻辑。
- 不改数据库 schema，不新增 release 表。
- 不把单个产品改成绕过 global release 直接匿名读取 current revision。
- 不顺手调整既有 version center、封面批任务、讲解批任务的业务规则，除非它们直接依赖新的 release 自动发布契约。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\worktrees\codex-showroom-release-dirty-debounce\doc\tasks\20260524-showroom-prompt-template-garbled-text-fix\task.md`
- Status before this task: `Completed`
- Impact on this task:
  上一同仓任务已完成，不阻塞本次实现；当前新建 worktree 干净，可在隔离分支内独立推进并验证。

## Milestones

- [x] M1：检查前序任务状态，建立本任务文档、执行日志和后端证据骨架。
- [x] M2：梳理当前手工 release、版本中心重发、产品/公司发布入口与 scheduler 现状，明确可复用边界。
- [x] M3：先补 RED，锁定“发布只标脏、未到防抖窗口不重建、到窗口后只重建一次”的行为。
- [x] M4：实现 release 脏状态服务、定时调度与发布入口接线，保持 fail-fast 和多租户执行语义。
- [x] M5：执行定向测试和必要回归，更新任务文档与证据，确认仅本任务文件进入提交边界。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 如新增 scheduler/service 定向测试：
  `mvn -pl yudao-module-showroom "-Dtest=<新增测试类>" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 如需要运行态验证：
  - `mvn -pl yudao-server -am -DskipTests package`
  - 本地运行库接口或定向 probe

## Current Status

Completed

- 已完成：
  - 已在隔离 worktree `codex/showroom-release-dirty-debounce` 完成自动发布主改动，并已快进合入 `int_main`。
  - 新增 `ShowroomReleaseAutoPublishService`，使用 `ConfigService` 持久化 release 脏状态、防抖窗口、最近发布与最近失败信息。
  - 新增 `ShowroomReleaseAutoPublishScheduler`，按租户每分钟检查一次，只有在 dirty 且超过防抖窗口时才执行一次全局 release 发布。
  - 将 `ShowroomPersistentContentService` 的 live 变更入口接到 `markDirty(...)`：
    - `publishCompanyRevision`
    - `publishProductRevision`
    - `createHall / updateHall / replaceHallProductMappings / deleteHall`
    - `deleteProduct`
  - 将立即切 current release 的入口继续保留同步发布语义，并改为在成功后清理 dirty 状态：
    - 手工 `publish release`
    - version center `republish`
  - 将当前 live narration 发布路径补了“仅当 source revision 等于当前 live revision 时标脏”的安全判断，避免未来 revision 的预发布媒体提前触发全局 release。
  - 已修复合入 `int_main` 后暴露的主分支冷启动回归：
    - `ShowroomReleaseAutoPublishService` 多构造函数场景未显式声明 Spring 注入构造器，导致打包后按无参构造创建 bean 失败。
    - `ShowroomPersistentContentService` 在构造阶段提前 `getIfAvailable()`，主分支冷启动和运行态 `markDirty(...)` 都存在顺序风险；已改为运行时延迟解析。
  - 已完成定向测试、release/版本中心回归、`yudao-server` 联编打包，以及 `48081 + Website 4173` 主分支联调探活。

## Final Verification Result

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAutoPublishServiceTest,ShowroomPersistentContentServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAutoPublishServiceTest,ShowroomPersistentContentServiceTest,ShowroomVersionCenterServiceTest,ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductNarrationAudioShouldProcessPublishedProductsAndExposeAutoCheckState+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `Invoke-WebRequest http://127.0.0.1:48081/showroom/release/current` -> PASS，主分支后端冷启动后可直接返回 current release
- `Invoke-WebRequest http://127.0.0.1:4173/showroom/release/current` -> PASS，`Website` 代理口与主分支后端返回同一 release
- `Invoke-WebRequest http://127.0.0.1:4173/showroom/display/website-config` -> PASS，返回公司 `瑛泰`、展厅数 `8`、首个展厅 `心内介植入展厅`

## Risks / Blockers

- 当前无阻塞。
- 已知边界：
  - 本次只改 `IntRuoyi` 后端自动合并发布；`Website` 页面打开后的持续轮询仍未改，用户重新进入页面或重新初始化 runtime 时才能拿到新的 `releaseId`。
