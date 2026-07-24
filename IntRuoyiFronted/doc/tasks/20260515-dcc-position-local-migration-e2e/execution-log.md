BDD: DCC 岗位分配页展示本地迁移后的岗位主数据 -> Given live 本地 MySQL 已迁入 IntAuth 当前 31 条岗位 / When 管理员通过真实登录路径进入 `DCC岗位分配` 页面 / Then 页面表格显示这 31 条本地岗位并可见代表岗位名称。

BDD: 岗位页不再受 IntAuth 运行时配置缺失影响 -> Given `GET /dcc/approval-positions` 已按设计只读本地表 / When 页面初始化请求岗位列表 / Then 页面不会再因为 IntAuth 运行时配置缺失而在 mounted 阶段报错。

RED: historical runtime evidence before migration -> FAIL, the DCC 岗位分配 runtime previously rendered `暂无数据`, and same-period DCC page verification captured `GET /admin-api/dcc/approval-positions` blocked by `IntAuth position sync config is missing`.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli -s=dcc-position-local-migration-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-position-local-migration-e2e\scripts\verify-dcc-position-list-e2e.mjs` -> PASS, real login reached `/dcc/controlled-file/positions`, visible row count was `31`, representative names were visible, and the screenshot was captured.
