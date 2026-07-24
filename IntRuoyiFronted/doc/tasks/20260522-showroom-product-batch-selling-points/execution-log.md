# 执行日志：展厅产品管理增加一键卖点

BDD: 产品管理工具栏显示一键卖点 -> Given 企宣用户打开 `http://localhost:8081/showroom/product` 的真实产品管理页 / When 页面渲染批量操作区 / Then 工具栏中应出现 `一键卖点` 按钮，并保持与 `一键讲解 / 一键语音 / 一键封面` 一致的紧凑操作台风格。

BDD: 一键卖点按当前筛选批量补齐中英文卖点 -> Given 当前筛选命中的产品存在缺失中文或英文核心卖点 / When 用户点击 `一键卖点` 并确认执行 / Then 前端必须调用专用批量卖点接口，已有语言自动跳过，并在完成后给出真实命中/补齐/失败反馈。

BDD: 一键卖点不得静默降级 -> Given 后端返回任务执行失败、空结果或具体错误原因 / When 前端处理接口响应 / Then 页面必须展示真实错误或结果，不得用默认成功文案、mock 数据或 fallback 内容掩盖失败。

RED: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs` -> FAIL, 前端缺少 `ShowroomProductSellingPointBatchGenerateRespVO`、`batchGeneratingSellingPoints` 状态和 `一键卖点` 工具栏接线。

RED: `node tests/e2e/showroom-product-toolbar-layout.spec.js` -> FAIL, 产品管理工具栏源码中尚不存在 `一键卖点` 按钮标签与点击绑定。

GREEN: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs` -> PASS

GREEN: `node tests/e2e/showroom-product-toolbar-layout.spec.js` -> PASS

GREEN: `pnpm exec eslint src/views/showroom-admin/components/ProductListTable.vue src/views/showroom-admin/index.vue src/api/showroom-admin/index.ts scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs tests/e2e/showroom-product-toolbar-layout.spec.js --format stylish` -> PASS

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-batch-selling-points run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-selling-points\scripts\verify-showroom-product-batch-selling-points.mjs` -> PASS，真实租户 `122` 的 `aoteman` 登录后已看到 `一键卖点` 按钮，并成功弹出确认框后取消。
