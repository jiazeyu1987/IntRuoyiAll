# 执行日志：排产工单可选列导出

- BDD: 默认可见列导出 -> Given 用户打开导出弹窗 / When 未调整列直接导出 / Then 请求携带默认可见业务列。
- BDD: 自定义列导出 -> Given 用户取消部分列 / When 确认导出 / Then 请求只携带选中列。
- BDD: 空列阻止导出 -> Given 用户取消所有列 / When 点击确认导出 / Then 前端给出明确提示且不请求后端。
- GREEN: experience-preflight -> PASS，已完成经验与前端风格门禁读取。
- RED: node tests/e2e/mes-pro-schedule-order-export-columns-static.spec.js -> FAIL, 缺少导出按钮权限、列弹窗状态、默认列清单、导出 API 和 Excel 下载调用。
- GREEN: node tests/e2e/mes-pro-schedule-order-export-columns-static.spec.js -> PASS。
- GREEN: node tests/e2e/mes-pro-schedule-order-pool-static.spec.js -> PASS。
- GREEN: node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js -> PASS。
- GREEN: pnpm ts:check:schedule -> PASS。
- RED: official login preflight -> FAIL, 默认 Playwright headless shell ICU 启动失败，改用系统 Chrome。
- GREEN: official login preflight -> PASS, 使用系统 Chrome 真实登录测试租户 `aoteman` 进入 `/mes/pro/schedule-order`。
- RED: export permission preflight -> FAIL, 页面无导出按钮；只读核验发现本机库缺少 `mes:pro-schedule-order:export` 菜单和角色授权。
- GREEN: export permission preflight -> PASS, 本机测试库应用权限迁移后 `aoteman` 权限响应包含导出权限。
- GREEN: node doc/tasks/20260708-schedule-order-export-columns/run-schedule-order-export-real-e2e.mjs -> PASS, 导出请求携带 `exportColumns[0..12]`，下载 `排产工单.xls`，表头与默认列一致，rowCountEvidence=37。
