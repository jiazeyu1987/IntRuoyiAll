# 执行日志：展厅产品语音按钮改为预览弹框

- BDD: 产品行语音按钮先打开预览弹框 -> Given 企宣人员打开展厅产品列表 / When 点击某一行的“语音”按钮 / Then 页面打开产品语音弹框，而不是立即调用语音生成接口。
- BDD: 语音弹框展示当前中英文语音现状 -> Given 产品存在已生成或未生成的中英文语音 / When 弹框加载完成 / Then 中文语音和英文语音区域都显示真实状态；有音频时可直接播放，无音频时明确显示未生成。
- BDD: 弹框内点击生成才触发真实写入 -> Given 用户已打开产品语音弹框 / When 点击“生成中英文语音” / Then 前端调用现有 `ShowroomAdminApi.generateProductNarrationAudio`，成功后刷新弹框和产品列表，失败时暴露真实错误。
- BDD: 缺少产品来源版本时直接失败 -> Given 当前产品缺少可用于生成语音的来源 revisionId / When 用户打开弹框或点击生成 / Then 页面直接暴露真实前置条件缺失，不添加兜底分支。

- INFO: task-created -> 已创建产品语音弹框任务台账，准备补 RED 静态合同并切换列表交互。
- RED: `node tests/e2e/showroom-product-row-audio-action.spec.js` -> FAIL，当前产品列表仍使用 `generate-audio` 行级事件和旧 loading，未打开语音弹框。
- RED: `node tests/e2e/showroom-product-whole-assignment.spec.js` -> FAIL，当前 `showroom-admin/index.vue` 仍绑定 `@generate-audio="handleGenerateProductNarrationAudioFromRow"`。
- RED: `node scripts/showroom-admin-product-bilingual-tabs.test.mjs` -> FAIL，产品列表源码仍要求 `generate-audio` 行级事件。
- BLOCKER: `node scripts/showroom-product-narration-action-disabled.test.mjs` -> BLOCKED，当前本地 `node_modules` 缺少 `picocolors`，导致 `@vue/compiler-sfc` 依赖链无法加载，无法进入业务断言。
- GREEN: `node tests/e2e/showroom-product-row-audio-action.spec.js` -> PASS
- GREEN: `node tests/e2e/showroom-product-whole-assignment.spec.js` -> PASS
- GREEN: `node scripts/showroom-admin-product-bilingual-tabs.test.mjs` -> PASS
- GREEN: `node scripts/showroom-admin-product-list.test.mjs` -> PASS
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- GREEN: experience-preflight -> PASS，已通过官方 `login-preflight.mjs` 真实登录本机 `http://localhost:8081/showroom/product`，允许继续执行产品语音弹框真实页面验证。
- GREEN: `node doc/tasks/20260626-showroom-product-audio-modal/verify-product-audio-dialog.mjs` -> PASS，真实登录测试租户产品页后点击首行“语音”，确认页面打开 `产品语音` 弹框并显示 `中文语音`、`英文语音` 与 `生成中英文语音` 按钮；证据输出到 `output/playwright/showroom-product-audio-modal/product-audio-dialog.png` 与 `product-audio-dialog-evidence.json`。
- BLOCKER: priority-switch -> 2026-06-26 13:39:53 +08:00 用户切换到更高优先级的 MES 手动重排按钮不可点击缺陷，本任务暂停，未进入生产代码变更。
