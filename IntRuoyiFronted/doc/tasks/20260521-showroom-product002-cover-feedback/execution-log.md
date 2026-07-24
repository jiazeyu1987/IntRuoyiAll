# Execution Log: product_002 封面生成反馈修复

BDD: 不允许生成时必须明确提示 -> Given `product_002` 当前不满足 AI 封面生成条件 / When 用户点击 `AI生成` / Then 页面必须明确提示当前不能生成的原因，而不是无反应。

BDD: 请求完成后必须有结果提示 -> Given 产品允许真实 AI 封面生成 / When 用户点击 `AI生成` 且接口返回成功或失败 / Then 页面必须分别显示成功提示或错误提示。

RED: 真实 Playwright 复现 `product_002` -> FAIL，按钮可见且未禁用，但点击后 120 秒内未捕获到 `/showroom/product/generate-cover-image` 响应，页面也没有任何 `.el-message` 提示，用户体感为“没有反应”。

GREEN: `node --test scripts/showroom-admin-product-cover-field.test.mjs` -> PASS，源码已锁定“开始生成”和“正在生成中”两条反馈文案。

GREEN: 真实 Playwright 复验 `product_002` -> PASS，点击 `AI生成` 后 1.5 秒内已出现消息：
- `AI封面生成中，请稍候`
