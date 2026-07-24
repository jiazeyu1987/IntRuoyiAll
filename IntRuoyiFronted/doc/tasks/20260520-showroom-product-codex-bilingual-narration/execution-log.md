# 执行日志：展厅产品 Codex CLI 讲解稿与双语语音生成

BDD: 产品详情展示讲解稿编辑区 -> Given 用户打开 `展厅 / 产品管理` 的产品详情 / When 详情加载完成 / Then 页面必须展示讲解稿文本框、`生成讲解稿` 按钮与 `生成语音` 入口，不得隐藏为 fake 状态标签。

BDD: 生成讲解稿后回填当前文本框 -> Given 产品基础资料可读 / When 用户点击 `生成讲解稿` / Then 前端必须调用真实产品讲解稿生成接口，并把返回的中文讲解稿回填到当前文本框。

BDD: 生成语音使用当前讲解稿 -> Given 用户已生成或编辑当前中文讲解稿 / When 用户点击 `生成语音` / Then 前端必须先保存当前中文讲解稿，再调用真实产品双语语音生成接口，不得直接沿用旧英文稿或跳过保存。

RED: `node --test scripts/showroom-admin-product-narration-editor.test.mjs` -> FAIL，产品详情弹窗尚未包含讲解稿文本框与生成按钮，`ShowroomAdminApi` 也没有产品脚本生成与 `getNarration` 契约。

GREEN: `node --test scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-product-detail-entry.test.mjs scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs` -> PASS。

GREEN: `npx.cmd eslint src/api/showroom-admin/index.ts src/views/showroom-admin/product/ProductDetailDialog.vue scripts/showroom-admin-product-narration-editor.test.mjs` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\frontend-feature-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-showroom-product-codex-bilingual-narration --mode preview` -> PASS，preview 状态 `ready`。

BLOCKED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-codex-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\scripts\verify-product-narration-dialog.mjs` -> FAIL-FAST，初次真实 `/showroom/product` 页面被无关 `CompanyHistoryWorkbench.vue` ESLint overlay 拦截，`详细信息` 按钮未渲染到可操作状态。

BLOCKED: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-codex-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\scripts\verify-product-narration-dialog.mjs` -> FAIL-FAST，第二次真实验证命中新后端接口未进入运行 jar，`/admin-api/showroom/product/generate-narration-script` 返回 `No static resource ...`。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，后端最新代码已重新打入 `yudao-server.jar`。

GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS，前后端本地运行态已重启到最新 jar。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-codex-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-codex-bilingual-narration\scripts\verify-product-narration-dialog.mjs` -> PASS，真实页面完成产品详情打开、Codex CLI 中文讲解稿生成、中文稿保存以及产品级双语语音生成，返回 `generatedNarrationVersionId=10`、`savedNarrationVersionId=11`、`zhNarrationVersionId=11`、`enNarrationVersionId=12`、`scriptLength=166`。
