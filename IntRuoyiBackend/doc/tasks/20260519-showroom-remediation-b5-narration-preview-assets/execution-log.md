# B5 执行记录

## BDD

- BDD: narration version persistence and live read -> Given 一个 target/language/audience 的 narration draft 已保存并审批发布，When 后台查询 narration get 或前台读取 `/showroom/display/narration`，Then 返回的必须是持久化 live narration 的真实 script 与 audio URL。
- BDD: preview asset version persistence and display URL -> Given 一个产品或展厅的静态 preview asset 已保存并审批发布，When 前台读取 hall/product display payload，Then payload 必须带出真实 live preview image URL，而不是空字符串。
- BDD: admin narration contract completeness -> Given 后台维护讲解稿，When 调用 get/draft/generate-audio/submit 契约，Then 每个动作都必须命中真实持久化链路，不得退回内存态。
- BDD: audio generation fail-fast -> Given runtime 没有外部音频适配器实现，When 调用 generate-audio，Then 系统必须明确返回 `SHOWROOM_AUDIO_GENERATION_FAILED`，不得伪造生成成功。

## RED

- RED: `mvn -pl yudao-module-showroom clean -Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，干净 worktree `codex/showroom-remediation-b5` 在 `compile` 阶段先报现有源码契约不一致：
  - `ShowroomApiRuntime` 仍以 5 参数构造 `CompanyCurrentRespVO`
  - `ShowroomAdminController` 仍引用 `runtime.versionHistory(...)`
  - `ShowroomAdminController` 仍引用 `runtime.getProductDetail(...)`
  - `runtime.listProducts(reqVO)` / `runtime.listHalls(reqVO)` 的返回类型与 `ProductPageRespVO` / `HallPageRespVO` 不匹配

## GREEN

- GREEN: `mvn -pl yudao-module-showroom clean '-Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，先清掉共享 runtime/controller 契约阻塞，再通过 B5 目标测试集合。
- GREEN: `mvn -pl yudao-module-showroom '-Dtest=ShowroomNarrationLifecycleTest,ShowroomAudioGenerationContractTest,ShowroomPreviewAssetLifecycleTest,ShowroomHttpApiIntegrationTest,ShowroomPersistentNarrationServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS，额外验证 Spring 注入音频适配器后 `generate-audio` 可真正写回持久化 narration。

## 结果

- narration: 新增 admin `GET /showroom/narration/get`，`draft/generate-audio/submit/get` 都走持久化 `ShowroomPersistentNarrationService`。
- preview asset: 新增 `ShowroomPersistentPreviewAssetService` 与 `ShowroomPreviewAssetOperations`，live preview image file id 可从 `showroom_preview_asset_version` 读取。
- display: product card / hall product card / hall entry 可组装真实 preview image URL，不再固定空字符串。
- audio generation: runtime 不再因为构造器忽略适配器而天然失败；当无适配器 bean 时仍明确返回 `SHOWROOM_AUDIO_GENERATION_FAILED`。

## 剩余说明

- `showroom_preview_asset_version` 现有 schema 只持久化 canonical display image file id；本任务据此保证真实 display preview URL，不越权改表结构。
