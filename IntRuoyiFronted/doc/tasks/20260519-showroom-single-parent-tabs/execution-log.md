# 执行日志：展厅菜单合并为单一父级子页签

BDD: 单一展厅父菜单 -> Given 管理员维护菜单权限 When 查看前端路由结构 Then 只存在一个“展厅”父路由，展厅后台页签和数字展厅页签都作为该父路由的子路由。

BDD: 首页入口进入统一展厅路由 -> Given 用户从首页进入展厅 When 点击展厅前台或后台入口 Then 路由落到 `/showroom/home` 或 `/showroom/company`，不再进入旧的两个父级路径。

RED: 待执行 -> FAIL，尚未补充单一父级路由测试。

RED: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs scripts/home-showroom-entry.test.mjs` -> FAIL，旧实现仍使用 `showroom-admin` 与 `showroom-frontstage` 两个父路由模块，首页入口仍指向 `/showroom-admin/company` 与 `/showroom/display/home`。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-frontstage.test.mjs scripts/home-showroom-entry.test.mjs` -> PASS，16 个路由、首页入口和页签约束测试通过。

GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js ...` -> PASS，使用主工作区依赖对本 worktree 文件完成 lint。

GREEN: `node scripts/run-showroom-phase1-e2e.mjs --dry-run` -> PASS，三组 Phase 1 E2E 用例模块仍可加载。

BLOCKED: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-single-parent-tabs --mode preview` -> FAIL for automatic linked-worktree closeout，清理项为空，但脚本检测到默认主分支 `master` 没有已检出 worktree，无法自动快进合并并删除 worktree。
