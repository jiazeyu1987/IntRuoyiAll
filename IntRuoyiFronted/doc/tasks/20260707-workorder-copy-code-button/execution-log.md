# 执行日志

BDD: 复制生产工单编号 -> Given 用户打开生产工单列表并看到某行工单编号 / When 点击该编号旁边的“复制”按钮 / Then 系统将该行 `scope.row.code` 写入剪切板，并提示“工单编号已复制”。

GREEN: experience-preflight -> PASS，已读取 PowerShell 编码门禁、经验索引、统一前端样式、frontend-feature-delivery 与 frontend-contract；本轮不执行真实 E2E、服务器写入、数据库写入或发布链路操作。

RED: node tests/e2e/workorder-code-copy-button-static.spec.js -> FAIL，当前生产工单“工单编号”列缺少 `@click.stop="handleCopyWorkOrderCode(scope.row.code)"` 复制按钮。

GREEN: node tests/e2e/workorder-code-copy-button-static.spec.js -> PASS，工单编号列保留详情入口并新增复制按钮，复制处理函数写入剪切板并给出成功提示。

GREEN: pnpm.cmd exec eslint src/views/mes/pro/workorder/index.vue tests/e2e/workorder-code-copy-button-static.spec.js --format stylish -> PASS。

BLOCKER: pnpm.cmd ts:check -> FAIL，Node 默认 4GB 堆内存不足，`vue-tsc` OOM 退出，未出现业务类型错误。

GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check -> PASS。

GREEN: encoding-check -> PASS，新增任务文档与静态测试均为 UTF-8 无 BOM。
