# 执行日志：展厅 release 脏标记与防抖合并发布

BDD: 发布内容变更只标记 release 为脏 -> Given 产品或公司内容已成功发布到 current revision / When 发布入口完成业务发布 / Then 系统只记录 showroom release 为 dirty 并刷新最后变更时间，不立即重建全局 release

BDD: 无脏变更时 scheduler 不得重建 release -> Given 当前没有任何待同步的 showroom 脏变更 / When 后台定时任务执行检查 / Then 系统必须直接跳过，不得生成新 release、切换 pointer 或写入无意义快照

BDD: 防抖窗口内的多次发布应合并为一次全局 release -> Given 同一防抖窗口内连续有多个产品发布成功 / When scheduler 在窗口结束后执行 / Then 系统只应生成并切换一次新的 current release，吸收窗口内所有最新 live 内容

BDD: 未到防抖窗口时 scheduler 不得提前切换 current release -> Given showroom 已标记 dirty 但距离最后一次变更尚未超过防抖窗口 / When 后台定时任务执行检查 / Then 系统必须保持当前 release pointer 不变，并保留 dirty 状态等待下一轮

INVESTIGATION: 2026-05-24 -> 已确认当前 Website 自动识别的是 `/showroom/release/current` 返回的 `releaseId`，不是单产品 current revision。
INVESTIGATION: 2026-05-24 -> 已确认匿名 `GET /showroom/display/website-config` 依赖 current release pointer 对应的 legacy projection。
INVESTIGATION: 2026-05-24 -> 已确认现有 `ShowroomReleasePublisherService.publishRelease(...)` 会立即 resolve snapshot、materialize release、persist release 并切 current pointer，适合保留为“真正发布动作”。
INVESTIGATION: 2026-05-24 -> 已确认现有 showroom 定时任务与后台状态持久化模式可复用 `ConfigService + @Scheduled + TenantUtils` 实现。
RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAutoPublishServiceTest,ShowroomPersistentContentServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增 `ShowroomReleaseAutoPublishServiceTest` / `ShowroomPersistentContentServiceTest` 初始阶段无法编译，直接暴露缺少自动发布状态服务与内容发布后标脏接线。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAutoPublishServiceTest,ShowroomPersistentContentServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，已验证 dirty 状态、防抖合并、立即发布清脏，以及产品发布后触发 `markDirty("PRODUCT_REVISION_PUBLISHED", operatorId)`。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest,ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，现有手工 release、release 组装与 version center 重发链路未被破坏。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#productCoverImageShouldGenerateAndUploadAiCoverForApprovedProduct+batchGenerateProductNarrationAudioShouldProcessPublishedProductsAndExposeAutoCheckState+batchGenerateProductCoverImageShouldCreatePublishedRevisionAndSummarizeSkippedProducts" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，产品封面生成、批量语音自动补齐、批量封面续跑摘要等相邻 runtime 行为回归通过。
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，自动发布改动已成功联编进 `yudao-server.jar`。

BDD: 合入主分支后的打包后端必须可冷启动并保留自动标脏行为 -> Given `int_main` 已合入自动发布改动并重新打包 `yudao-server.jar` / When Spring 创建 `ShowroomReleaseAutoPublishService` 与 `ShowroomPersistentContentService` 并触发展厅更新 / Then 主分支后端必须成功启动，且 `updateHall(...)` 仍会写入 `showroom.release.auto-publish.state`

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增主分支冷启动回归测试后，Spring 报 `ShowroomReleaseAutoPublishService` 无默认构造器，且 `ShowroomPersistentContentService` 在构造阶段提前 `getIfAvailable()` 触发 bean 创建失败。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`ShowroomReleaseAutoPublishService` 已显式声明 Spring 注入构造器，`ShowroomPersistentContentService` 已改为运行时延迟解析自动发布服务，主分支上下文成功启动并验证 `updateHall(...)` 会写入 `dirty=true` 与 `HALL_UPDATED`。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAutoPublishServiceTest,ShowroomPersistentContentServiceTest,ShowroomVersionCenterServiceTest,ShowroomReleaseAdminPublishIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，自动发布状态服务、内容发布标脏、版本中心与主分支冷启动回归全部通过。
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，主分支重新打包后的 `yudao-server.jar` 成功生成。
GREEN: `Invoke-WebRequest http://127.0.0.1:48081/showroom/release/current` -> PASS，主分支后端在 `2026-05-24 19:11:46` 冷启动成功后可返回 current release `20260524T100623Z-316b86ad1758`。
GREEN: `Invoke-WebRequest http://127.0.0.1:4173/showroom/release/current` / `Invoke-WebRequest http://127.0.0.1:4173/showroom/display/website-config` -> PASS，`Website` 代理口与主分支后端返回同一 release，且网站配置返回公司 `瑛泰`、展厅数 `8`、首个展厅 `心内介植入展厅`。
