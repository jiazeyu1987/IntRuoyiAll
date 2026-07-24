# Execution Log

BDD: 奖项页签导入 -> Given 上传的 Excel 同时包含 `产品列表` 与 `奖项` 页签，且奖项行具备序号、中文名和 E 列首图 / When 用户在产品管理执行导入 / Then 系统导入并发布奖项资料，返回奖项导入统计和额外图片 warning。

BDD: 奖项导入缺必要数据必须失败 -> Given `奖项` 页签缺失、奖项序号缺失、中文名缺失或 E 列首图缺失 / When 用户导入 Excel / Then 后端返回明确错误，不静默跳过、不生成默认封面。

BDD: 展柜可选择奖项 -> Given 产品和奖项均有已发布资料 / When 用户在展柜选择器保存展项 / Then 展柜保存 `PRODUCT` 与 `AWARD` 混合项，并按 `itemType + itemId` 拦截重复项。

BDD: 发布包含奖项详情 -> Given 展柜中包含奖项且奖项具备名称、封面、讲解、语音和布局 / When 用户发布展厅 / Then 发布快照输出混合 `items`，并生成奖项详情文档。

GREEN: mvn -pl yudao-module-showroom -DskipTests compile -> PASS。

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomHallContentTest,ShowroomHallMixedItemContentTest" test -> PASS。

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomProductExcelImportExportIntegrationTest" test -> PASS。

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseWebsiteIndexAssemblyTest,ShowroomReleaseProductDetailAssemblyTest,ShowroomReleaseDocumentApiTest,ShowroomReleaseDocumentErrorSemanticsTest" test -> PASS。

RED: mvn -pl yudao-module-showroom test -> FAIL, full module still has legacy integration fixtures without hall canvas layout and workflow/role-binding test prerequisites; strict release publishing now fails fast with `SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required` instead of generating incomplete release.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomAssignmentWorkflowTest" test -> PASS.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomSchemaMapperContractTest" test -> PASS.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomVersionCenterBackfillContractTest" test -> PASS.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" test -> PASS.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest#productPublishShouldCarryForwardNarrationAudioWhenLatestDraftScriptIsUnchanged" test -> PASS.

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest#productRowGenerateNarrationAudioShouldCompleteEnglishDraftForLatestProductDraftBeforePublish" test -> PASS.

GREEN: mvn -pl yudao-module-showroom test -> PASS, 327 tests / 0 failures / 0 errors.

GREEN: docker exec int-ruoyi-mysql mysql ... < sql/showroom/20260613_showroom_award_and_hall_item_schema.sql -> PASS, local Docker schema now has `showroom_award`, `showroom_award_revision`, `showroom_hall_item`.

GREEN: Playwright real login/import -> PASS, `http://localhost:8081/login?redirect=/showroom/product` 使用测试租户 `aoteman` 真实登录，打开 `/showroom/product`，切换 `奖项` 页签，上传 `E:\QmS\产品资料修改版-补充产品资料.xlsx`；导入响应 `awardTotalRows=46`、`awardSuccessCount=46`、`awardFailureCount=0`，返回 9 条额外图片 warning。

GREEN: mvn -pl yudao-server -am "-Dmaven.test.skip=true" package -> PASS，停止锁定 `yudao-server\target\yudao-server.jar` 的本机 48081 旧进程后重新打包，并用 runtime copy 启动 `http://127.0.0.1:48081/actuator/health` -> `UP`。

GREEN: Playwright award audio UI -> PASS，真实登录测试租户后进入产品管理奖项页签，打开 `AWARD-001` 编辑弹窗，确认 `中文语音`、`英文语音` 和 `生成中英文语音` 控件可见。

BLOCKED: Playwright award audio generation -> BLOCKED，真实点击 `生成中英文语音` 后 `/admin-api/showroom/narration/generate-audio` 返回 HTTP 200 业务失败：`SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=400 body={"task_id":"...","status":40000001,"message":"Meta:ACCESS_DENIED:The token '****' is invalid!"}`。本机只读配置查询显示 `infra_config.config_key='yudao.ai.tts.aliyun-nls.access-token'` 存在，长度 32，更新时间 `2026-06-06 13:03:03`，但已被阿里云拒绝。

BLOCKED: Playwright award publish -> BLOCKED，由于奖项中英文语音未生成，点击 `保存并发布` 只调用 `/showroom/award/draft` 保存草稿，没有调用 `/showroom/award/publish`；按奖项发布与 release 必需字段规则，缺语音时不得继续发布。

RED: mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishAwardEndpointShouldFailFastWhenAwardNarrationAudioMissing+publishAwardEndpointShouldPublishTheNarrationBackedDraftRevisionWithoutCreatingAnotherRevision" test -> FAIL，新增测试先暴露奖项发布请求缺少 `revisionId`，且发布流程会在保存奖项草稿后创建新修订版，无法绑定已生成语音的源修订版。

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishAwardEndpointShouldFailFastWhenAwardNarrationAudioMissing+publishAwardEndpointShouldPublishTheNarrationBackedDraftRevisionWithoutCreatingAnotherRevision" test -> PASS，奖项发布接口要求提交 `revisionId`，校验请求字段与该修订版一致，并要求 AWARD 中文/英文讲解均已有音频；通过后发布同一个语音来源修订版，不再生成新的未配音修订版。

BLOCKED: NLS credential recheck -> BLOCKED，`ALIYUN_NLS_ACCESS_TOKEN`、`ALIYUN_ACCESS_KEY_ID`、`ALIYUN_ACCESS_KEY_SECRET`、`ALIBABA_CLOUD_ACCESS_KEY_ID`、`ALIBABA_CLOUD_ACCESS_KEY_SECRET` 均未在进程/用户/机器环境变量中配置；真实库 `infra_config` 仍为 `yudao.ai.tts.aliyun-nls.access-token` 长度 32、掩码 `a86d****a02c`、更新时间 `2026-06-06 13:03:03`。本机 48081/48082 后端健康，但缺少有效 NLS token，无法完成奖项中英文语音生成和最终发布 E2E。

GREEN: NLS credential update -> PASS，用户更新 token 后只读复核 `infra_config.config_key='yudao.ai.tts.aliyun-nls.access-token'` 长度 32、掩码 `6c2a****789d`、更新时间 `2026-06-14 10:15:24`。

GREEN: Playwright award audio generation -> PASS，真实登录测试租户 `aoteman`，进入奖项页签编辑 `AWARD-001`，点击 `生成中英文语音` 后后端生成中文语音文件 `9198354891941`、英文语音文件 `9198354891942`。

GREEN: Playwright award publish -> PASS，`AWARD-001` 保存并发布成功，发布修订版 `currentRevisionId=50`，奖项中文/英文讲解与语音门禁通过。

GREEN: Playwright hall mixed item -> PASS，展柜 `hall_id=10` 保存混合展项，`showroom_hall_item` 中存在 `item_type=AWARD`、`item_id=1`、`display_order=24`；画布截图 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\showroom-hall-award-canvas-saved.png` 显示 `社会贡献奖` / `AWARD-001`。

RED: Website release document integrity -> FAIL，release `20260614T051818Z-cdf9733a057e-c9c87d12d97a` 前台加载时报 `SHOWROOM_RELEASE_DOCUMENT_HASH_MISMATCH`，原因是后端奖项详情 `contentHash` 纳入了 `awardCode/subtitle/bilingualPublicFields/attachments`，而 Website 正式完整性负载只包含奖项详情展示必需字段。

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldExposeAwardDocumentFieldsRequiredByWebsiteRuntime" test -> PASS，奖项详情发布测试新增断言：manifest 中 `award-detail` 使用 `awardId` 且 `contentHash` 等于 Website 契约负载 SHA-256。

GREEN: mvn -pl yudao-server -am "-Dmaven.test.skip=true" package -> PASS；第一次 repackage 因本机旧 48081 进程锁定 `yudao-server\target\yudao-server.jar` 失败，停止该锁定进程后重跑通过，并启动新 48081，健康检查为 `UP`。

GREEN: Playwright manual release publish -> PASS，真实登录 `http://127.0.0.1:8081` 测试租户，点击公司信息工作台 `手动发布展厅`，发布 release `20260614T055216Z-cdf9733a057e-21bd7d57e98a`，manifestHash `73930c07ec3c532f011ed3958aaf1038ed6c7b2d9fe25fa16fda890f7c0b3c09`。

GREEN: Public manifest award hash verification -> PASS，公开 manifest 中 `award-detail-1` 为 `awardId=1` 且不含 `productId`；按 Website `createDocumentIntegrityPayload` 同口径计算得到 `bd6bcfadc9eda0a87e19c072ad7c7ed385b8a321fef929b2babc62d0895324b8`，与 manifest `contentHash` 一致。

GREEN: Playwright Website award detail -> PASS，打开 `http://localhost:5173`，切换展柜后可见 `社会贡献奖` 奖项卡片，点击后进入 `[data-product-detail-id][data-item-type="AWARD"]`，详情展示 `社会贡献奖`、`嘉定区江桥镇人民政府` 与语音面板，控制台无 `SHOWROOM_RELEASE_DOCUMENT_HASH_MISMATCH`。

GREEN: mvn -pl yudao-module-showroom "-Dtest=ShowroomHallMixedItemContentTest,ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldExposeAwardDocumentFieldsRequiredByWebsiteRuntime+publishAwardEndpointShouldFailFastWhenAwardNarrationAudioMissing+publishAwardEndpointShouldPublishTheNarrationBackedDraftRevisionWithoutCreatingAnotherRevision" test -> PASS，5 tests / 0 failures / 0 errors。

RED: pnpm ts:check -> FAIL，默认 Node heap 约 4GB 下 `vue-tsc` OOM，退出码 134；未发现类型错误输出。

GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS。

GREEN: node tests\e2e\showroom-award-audio-static.spec.js -> PASS。

GREEN: npm test (D:\ProjectPackage\Website) -> PASS，11 files / 142 tests。
