BDD: 普通工序只配置生产填写人 -> Given 用户在批记录用途配置页查看工序表单人员配置 / When 查看行内状态或打开配置弹窗 / Then 页面只展示生产填写人和电子签名流转说明，不展示审核人或批准人配置。
BDD: 工序级保存不携带审核批准规则 -> Given 用户保存普通工序填写人设置 / When 前端提交人员设置 / Then 保存请求仍保留 fillRule，但 signatureRules 固定为空数组，审核和批准留给放行阶段。
RED: node tests/e2e/edhr-production-filler-only-static.spec.js -> FAIL, 旧页面缺少 `工序表单生产填写人设置`，仍展示工序级审核/批准入口。
GREEN: node tests/e2e/edhr-production-filler-only-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-role-assignment-ui-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-form-permission-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/edhr-production-filler-only-static.spec.js tests/e2e/edhr-role-assignment-ui-static.spec.js tests/e2e/edhr-process-form-permission-static.spec.js -> PASS
GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS
