BDD: 工序人员设置可理解 -> Given 用户在批记录用途配置页为某个工序绑定批记录表 / When 查看该行人员设置 / Then 页面直接展示填写人、审核人、批准人的配置状态，并提供“设置人员”入口，不再显示“审批位”“批准/复核位”“权限/派工”。
BDD: 人员设置弹窗按业务角色分组 -> Given 用户点击“设置人员” / When 弹窗打开 / Then 表单按填写人、审核人、批准人分组，字段标签明确提示选择哪些人/范围，保存按钮显示“保存人员设置”。

RED: node tests/e2e/edhr-role-assignment-ui-static.spec.js -> FAIL, 人员设置静态合同需锁定现有 API 角色值与业务术语展示。
GREEN: node tests/e2e/edhr-role-assignment-ui-static.spec.js -> PASS
GREEN: node tests/e2e/edhr-process-form-permission-static.spec.js -> PASS
GREEN: pnpm exec eslint src/views/mes/pro/route-use/RouteUsePage.vue src/api/mes/pro/edhr/batchExecution.ts tests/e2e/edhr-role-assignment-ui-static.spec.js tests/e2e/edhr-process-form-permission-static.spec.js -> PASS
GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS
