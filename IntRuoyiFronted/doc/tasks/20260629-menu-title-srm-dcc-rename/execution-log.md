# Execution Log：SRM 与文控中心菜单改名（前端）

- BDD: SRM 菜单测试使用新标题 -> Given 前端 E2E 通过菜单标题定位导航路径 / When 执行相关 SRM 用例 / Then 顶级菜单标题应使用 `SRM`。
- BDD: 文控中心断言使用新标题 -> Given 前端静态与真实菜单断言依赖 DCC 顶级菜单标题 / When 执行相关测试 / Then 顶级菜单标题应使用 `文控中心`。
- GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-upload-size-policy-management-static.spec.js` -> PASS
- GREEN: `rg -n -F "供应商关系管理"` / `rg -n -F "DCC文控中心"` 定向扫描前端测试与脚本 -> PASS，旧标题已切换为 `SRM` / `文控中心`。
