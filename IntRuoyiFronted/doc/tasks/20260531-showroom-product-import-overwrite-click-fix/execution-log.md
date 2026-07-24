# 执行日志：修复产品导入点击覆盖未生效

BDD: 点击覆盖提交覆盖动作 -> Given 用户打开展厅产品导入弹窗 / When 点击“覆盖”并提交导入 / Then 前端提交 `sameProductAction=OVERWRITE`，后端按覆盖发布处理。

BDD: 默认跳过保持不变 -> Given 用户打开展厅产品导入弹窗 / When 未点击覆盖直接提交 / Then 前端提交 `sameProductAction=SKIP`。

RED: node scripts/showroom-admin-product-import-form.test.mjs -> FAIL, 新增断言要求 `el-radio-button value="SKIP"` 和 `value="OVERWRITE"`，当前组件仍使用 `label`，覆盖按钮取值未符合项目当前 Element Plus 写法。

GREEN: node scripts/showroom-admin-product-import-form.test.mjs -> PASS, tests 5, pass 5。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

根因记录：导入弹窗使用旧式 `label` 作为单选按钮值；项目内 Element Plus 2.11.1 的同类组件主要使用 `value`，导致用户点击覆盖时可能未把 v-model 更新为 `OVERWRITE`。

修复记录：将“跳过”“覆盖”两个 `el-radio-button` 改为 `value="SKIP"` 和 `value="OVERWRITE"`。
