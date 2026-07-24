# Execution Log：排产员工作台全量数据包按钮

BDD: 工作台显示全量包按钮 -> Given 用户进入排产员工作台 / When 页面渲染设置区 / Then 可见导出全部数据包与导入全部数据包按钮。
BDD: 工作台保留独立文件选择器合同 -> Given 用户点击导入全部数据包 / When 页面准备选择文件 / Then 页面仍通过隐藏文件选择器接收单个数据包文件并触发导入。
BDD: 前端 API 指向正式聚合接口 -> Given 工作台执行全量包导出或导入 / When 前端发起请求 / Then 请求命中新的后端聚合接口，而不是前端自行串调多个已有接口。
GREEN: experience-preflight -> PASS，已读取 `docs/login-access.md`，本机入口 `http://localhost:8081` 与后端 `http://127.0.0.1:48081` 可访问，准备执行真实 Playwright 按钮链路验证。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-route-import-export-static.spec.js` -> FAIL，初始状态缺少“导出全部数据包 / 导入全部数据包”按钮、隐藏文件选择器与 full-config API 合同。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-route-import-export-static.spec.js` -> PASS，工作台按钮、隐藏文件选择器与 full-config API 合同通过。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-scheduler-workbench-static.spec.js` -> PASS，工作台静态结构校验通过。
BLOCKER: local-runtime-stability -> FAIL，本机 `48081` 曾被旧缓存包自动切回，前端真实点击阶段一度出现 `/full-config/export` 404 与导入超时噪音，因此不能直接把早期探针结果当正式结论。
GREEN: local-runtime-stability -> PASS，本轮先稳定 `48081` 到最新源码运行态，再执行真实浏览器验证。
GREEN: real-admin-export-button -> PASS，真实浏览器读取到“导出全部数据包”按钮可见、可点，点击后命中 `GET http://127.0.0.1:48081/admin-api/mes/pro/scheduler-workbench/full-config/export`，HTTP `200`。
GREEN: real-admin-export-import-roundtrip -> PASS，真实浏览器在 `芋道源码/admin` 下导出 `D:\ProjectPackage\Int\IntRuoyi\output\playwright\admin-scheduler-workbench-full-config-export.json`，文件大小 `1750192` bytes；随后通过隐藏文件选择器 `index=1` 导入同一文件，命中 `POST http://127.0.0.1:48081/admin-api/mes/pro/scheduler-workbench/full-config/import`，HTTP `200`，响应 `{"code":0,"msg":"","data":{"userRoleBindingCount":27,"assignedRoleCount":41}}`，页面 toast 为 `导入完成；用户角色绑定 27 条；分配角色 41 条`。
