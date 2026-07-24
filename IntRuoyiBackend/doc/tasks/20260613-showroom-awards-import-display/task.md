# 20260613 展厅奖项导入与展柜展示

## 任务目标

在展厅产品管理中支持从 `E:\QmS\产品资料修改版-补充产品资料.xlsx` 的 `奖项` 页签导入奖项资料，奖项可编辑中英文名称、讲解、封面和语音，并可作为展柜展项与产品一起发布到展厅前台。

## 前置任务检查

- 最近后端任务：`20260612-runtime-control-server-host-defaults`。
- 状态：`COMPLETED`。
- 结论：允许开始本任务。

## 里程碑

1. M1 审计：确认现有产品导入、展柜映射、发布快照和 Website 协议。
2. M2 RED：新增奖项导入、奖项模型、混合展项和发布失败路径测试。
3. M3 GREEN：实现奖项持久化、导入、管理 API、展柜混合项、发布和 Website 配置输出。
4. M4 REGRESSION：运行 showroom 模块目标测试和必要编译检查。
5. M5 收尾：记录证据、运行 task-closeout-cleanup 预览并提交本任务改动。

## 预期验证

- 后端 showroom 目标 Maven 测试通过。
- 缺少 `奖项` 页签、奖项序号、中文名或首图封面时明确失败。
- 产品与奖项可同时绑定展柜并输出到发布快照。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少必要页签、字段、封面、发布版本或布局时直接失败。
- `是否从根因和长期维护角度解决`：是；新增正式奖项模型和混合展项模型，不用产品字段或临时 JSON 绕过。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：奖项导入、奖项模型、混合展项、发布快照、租户字段修复、发布讲解音频复用规则修复、奖项语音管理入口、奖项发布 `revisionId` 绑定与中英文语音门禁、Website 奖项详情字段与发布文档哈希契约修复。
- 验证证据：`mvn -pl yudao-module-showroom test` 曾通过，327 tests / 0 failures / 0 errors；本轮新增回归 `mvn -pl yudao-module-showroom "-Dtest=ShowroomHallMixedItemContentTest,ShowroomReleaseAdminPublishIntegrationTest#publishReleaseShouldExposeAwardDocumentFieldsRequiredByWebsiteRuntime+publishAwardEndpointShouldFailFastWhenAwardNarrationAudioMissing+publishAwardEndpointShouldPublishTheNarrationBackedDraftRevisionWithoutCreatingAnotherRevision" test` 通过，5 tests / 0 failures / 0 errors；`mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` 通过。
- 真实 E2E：测试租户 `aoteman` 已导入 `E:\QmS\产品资料修改版-补充产品资料.xlsx`，奖项 46 行全部成功；更新 NLS token 后 `AWARD-001` 真实生成中文语音 `9198354891941`、英文语音 `9198354891942`，奖项发布到修订版 `50`；展柜 `hall_id=10` 已保存 `AWARD-001` 混合展项；后台真实点击 `手动发布展厅` 成功发布 release `20260614T055216Z-cdf9733a057e-21bd7d57e98a`。
- 前台验证：公开 manifest 中 `award-detail-1` 使用 `awardId=1` 且不含 `productId`，奖项详情文档 `contentHash=bd6bcfadc9eda0a87e19c072ad7c7ed385b8a321fef929b2babc62d0895324b8` 与 Website 完整性算法一致；Playwright 打开 `http://localhost:5173`，切换展柜后可见 `社会贡献奖` 奖项卡片，点击进入奖项详情，展示颁发单位 `嘉定区江桥镇人民政府` 和语音面板，无 `SHOWROOM_RELEASE_DOCUMENT_HASH_MISMATCH`。
- 管理端与 Website 回归：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过；`node tests\e2e\showroom-award-audio-static.spec.js` 通过；`npm test` 在 Website 通过，11 files / 142 tests。

## Cleanup Keep

- `doc/tasks/20260613-showroom-awards-import-display/backend-api-evidence.md`
