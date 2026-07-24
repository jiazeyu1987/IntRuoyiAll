# 执行日志：展厅产品导入相同产品选择覆盖或跳过（前端）

BDD: 导入弹窗默认跳过相同产品 -> Given 用户打开展厅产品管理导入弹窗 / When 未调整相同产品处理方式 / Then 默认选择跳过相同产品。

BDD: 用户选择覆盖相同产品 -> Given 用户打开展厅产品管理导入弹窗 / When 选择覆盖相同产品并提交 / Then 前端随导入请求发送覆盖参数。

BDD: 导入中禁用选择 -> Given 导入请求正在提交 / When 弹窗处于 loading / Then 相同产品处理选择不可编辑，避免提交中改变语义。

RED: node scripts/showroom-admin-product-import-form.test.mjs -> FAIL, 新增静态契约要求“相同产品处理”和 sameProductAction，旧弹窗未包含该选择，断言 /相同产品处理/ 未命中。

GREEN: node scripts/showroom-admin-product-import-form.test.mjs -> PASS, tests 5, pass 5。

RED: pnpm ts:check -> FAIL, Node 默认堆限制下 exit code 134，报 JavaScript heap out of memory，非本任务类型错误。

GREEN: $env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check -> PASS。

实现记录：导入弹窗新增 sameProductAction 状态，默认 `SKIP`；新增单选按钮组“跳过/覆盖”；提交时追加 FormData 字段 `sameProductAction`；resetForm 重置为跳过。

验证记录：导入中控件使用 `:disabled="formLoading"` 禁用，避免请求提交期间修改处理语义。

E2E-BLOCKED: 浏览器打开 http://localhost:8081 和 /login 后均停留在启动页，未进入真实产品管理页面。检查本机运行状态发现后端 48081/48082 无监听；`output/runtime/backend-20260531-201110.out.log` 记录 DCC 下载加密 base64-key 配置缺失，前端日志 `output/runtime/frontend-20260531-201110.err.log` 还记录 Vite 依赖更新 `EMFILE: too many open files`。未使用 mock 或临时控件绕过。
