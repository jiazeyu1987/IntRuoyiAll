# Execution Log

BDD: 产品管理展示奖项页签 -> Given 用户进入展厅产品管理 / When 页面加载完成 / Then 可在 `产品` 与 `奖项` 页签间切换，并在奖项表格看到奖项字段。

BDD: 导入反馈包含奖项统计 -> Given 后端导入响应包含奖项统计和 warning / When 导入弹窗完成上传 / Then 页面展示奖项成功/失败数量与额外图片提示。

BDD: 展柜选择器提交混合展项 -> Given 用户在展柜中选择产品和奖项 / When 保存展柜映射或画布布局 / Then 请求体使用 `items`，每项包含 `itemType`、`itemId` 和布局字段。

RED: pnpm type:check -> FAIL, package script not found；改用项目现有 `pnpm ts:check`。

RED: pnpm ts:check -> FAIL, Node heap exhausted；按本机大型 Vue 工程要求设置 `NODE_OPTIONS=--max-old-space-size=8192` 后重跑。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

GREEN: NLS credential update -> PASS，用户更新 token 后奖项真实语音生成解除阻塞。

GREEN: Playwright award audio generation/publish -> PASS，测试租户 `aoteman` 真实进入奖项页签编辑 `AWARD-001`，生成中文语音文件 `9198354891941`、英文语音文件 `9198354891942`，保存并发布到修订版 `50`。

GREEN: Playwright hall award mapping -> PASS，展柜选择器保存奖项 `AWARD-001`，后端 `showroom_hall_item` 存在 `hall_id=10`、`item_type=AWARD`、`item_id=1`、`display_order=24`，画布展示 `社会贡献奖` / `AWARD-001`。

GREEN: Playwright manual release publish -> PASS，公司信息工作台真实点击 `手动发布展厅`，发布 release `20260614T055216Z-cdf9733a057e-21bd7d57e98a`。

RED: pnpm ts:check -> FAIL，默认 Node heap 下 `vue-tsc` OOM，退出码 134。

GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS。

GREEN: node tests\e2e\showroom-award-audio-static.spec.js -> PASS。

BLOCKED: NLS credential recheck -> BLOCKED，环境变量中没有 `ALIYUN_NLS_ACCESS_TOKEN` 或阿里云 AK/SK；真实库 `infra_config` 中 `yudao.ai.tts.aliyun-nls.access-token` 仍是 2026-06-06 更新的旧 token。奖项语音按钮会调用真实 `/showroom/narration/generate-audio`，当前凭证无法通过阿里云鉴权，最终发布和 Website 前台 E2E 不能继续。

GREEN: 去除展柜工作台 `productMappings` 兼容读取后，$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

RED: node tests\e2e\showroom-product-excel-import-export.spec.js; node tests\e2e\showroom-product-excel-template-static.spec.js -> FAIL, 导入弹窗说明只保留奖项字段后缺少产品列 `产品名-中文` 等既有导入契约。

GREEN: node tests\e2e\showroom-product-excel-import-export.spec.js; node tests\e2e\showroom-product-excel-template-static.spec.js -> PASS，导入弹窗同时说明产品列和奖项列，导入结果展示奖项统计、warning 和奖项失败明细。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

GREEN: node tests\e2e\showroom-award-audio-static.spec.js -> PASS，确认奖项页签、AWARD 讲解接口契约、奖项中英文语音控件、发布前语音门禁和展柜奖项类型标签均已接线。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

GREEN: Playwright real E2E login/import -> PASS，修正测试脚本的 Element Plus 租户选择方式后，真实选择 `测试租户` 并登录 `aoteman`，进入 `/showroom/product`，导入 `E:\QmS\产品资料修改版-补充产品资料.xlsx`；导入结果展示奖项 46 行、46 成功、0 失败和 9 条额外图片 warning。

GREEN: Playwright award audio controls -> PASS，真实进入奖项页签并打开 `AWARD-001` 编辑弹窗，确认 `中文语音`、`英文语音` 和 `生成中英文语音` 可见。

BLOCKED: Playwright award audio generation -> BLOCKED，真实点击 `生成中英文语音` 后 `/admin-api/showroom/narration/generate-audio` 返回业务失败 `SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed ... ACCESS_DENIED:The token '****' is invalid!`。当前阻塞是本机阿里云 NLS token 失效，不能以前端伪成功或模拟音频绕过。

GREEN: node tests\e2e\showroom-award-audio-static.spec.js -> PASS，确认奖项发布前检查中文/英文音频 URL，调用真实 `generateNarrationAudio`，生成失败时展示并抛出后端错误，发布 payload 携带 `revisionId: awardNarrationDraft.sourceRevisionId`，且发布时不再重新保存奖项草稿造成新修订版。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。
