# 执行日志：电子签名侧边栏子菜单仍不可见

- `BDD: 动态菜单可见性不被隐藏静态直达路由覆盖 -> Given 后端下发电子签名子菜单 hidden=false / When 前端与 hidden=true 的静态直达路由合并 / Then 合并后的菜单仍保持 hidden=false 并在侧边栏显示。`
- `BDD: 电子签名侧边栏显示子菜单 -> Given 用户登录测试租户 / When 打开电子签名菜单 / Then 侧边栏显示 8 个电子签名子菜单。`

## 记录

- `INFO: scope -> 修复前端 route merge，不修改电子签名业务 API。`
- `RED: node scripts\signature-governance-page-contract.test.mjs -> FAIL, hidden static route merge keeps dynamic sidebar visibility 断言失败，permission.ts 未保留动态菜单 hidden=false。`
- `GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 6 tests passed。`
- `GREEN: experience-preflight -> PASS, 已读取登录门禁；真实 E2E 仅使用本机 http://localhost:8081 与测试租户 aoteman。`
- `GREEN: Playwright real E2E 登录测试租户并访问 http://localhost:8081/signature-governance/overview -> PASS, 侧边栏可见 总览、文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包、统一策略，failedApi=[]。`
- `GREEN: root-cause-check -> PASS, 后端菜单与角色绑定正确；前端 hidden 静态路由合并已改为保留动态菜单 hidden/alwaysShow。`
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260624-signature-governance-sidebar-children\frontend-feature-evidence.md -> PASS。`
- `GREEN: npm run ts:check -> PASS。`
- `GREEN: node scripts\signature-governance-page-contract.test.mjs -> PASS, 6 tests passed after evidence update。`
- `GREEN: task-closeout-cleanup preview/apply -> PASS, 无临时产物需删除。`
