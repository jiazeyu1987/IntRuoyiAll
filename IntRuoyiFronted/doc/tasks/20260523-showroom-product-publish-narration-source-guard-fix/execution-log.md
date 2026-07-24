# Execution Log: 20260523-showroom-product-publish-narration-source-guard-fix

BDD: 单条发布允许复用已保存讲解稿 source revision -> Given 产品当前待发布 revision 需要沿用已保存的中英文讲解稿版本 When 用户在产品列表点击 `发布` Then 前端必须把讲解稿 source revision 传给正式发布接口，而不是因 `sourceRevisionId != 当前待发布 revisionId` 提前拦截。
BDD: 单条发布仍需显式暴露真实讲解稿异常 -> Given 当前中英文讲解稿缺失、为空或中英文 source revision 不一致 When 用户点击 `发布` Then 前端必须显式失败并提示真实原因，不得静默跳过或默认通过。
RED: `node --test scripts/showroom-product-publish-narration-source.test.mjs` -> FAIL，`src/views/showroom-admin/index.vue` 仍保留“当前中文/英文讲解稿不属于当前待发布版本”的前端强等校验，也没有独立的讲解稿 source revision 解析逻辑。
GREEN: `node --test scripts/showroom-product-publish-narration-source.test.mjs` -> PASS，单条发布改为校验“中英文讲解稿 source revision 必须一致”，并把解析出的 `sourceRevisionId` 带入 `PUT /showroom/product/publish`。
GREEN: `pnpm exec eslint src/views/showroom-admin/index.vue scripts/showroom-product-publish-narration-source.test.mjs` -> PASS。
GREEN: `node tests/e2e/showroom-product-publish-entry.spec.js` -> PASS，列表独立 `发布` 入口结构仍保持稳定。
BLOCKED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-publish-reuse-source run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-showroom-product-publish-narration-source-guard-fix\scripts\verify-showroom-product-publish-reuse-narration-source.mjs` -> FAIL-FAST，本地 `http://127.0.0.1:8081/showroom/product` 登录后仅渲染应用壳，页面未出现 `新增` 按钮与产品表格内容，无法继续走“详细保存新 revision 后再发布”的真实点击链路。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260523-showroom-product-publish-narration-source-guard-fix --mode preview` -> PASS，预览仅包含本次任务临时 Playwright 脚本与失败截图。
INFO: cleanup apply fallback -> `task_closeout.py --mode apply` 因状态识别返回 `current status: unknown` 被工具自身阻塞；已按 preview 名单手工删除 `artifacts/missing-locator.png` 与 `scripts/verify-showroom-product-publish-reuse-narration-source.mjs`，保留 `task.md / execution-log.md`。
