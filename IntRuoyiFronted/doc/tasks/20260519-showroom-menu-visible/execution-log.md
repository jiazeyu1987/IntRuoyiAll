# 执行记录：展厅父级菜单可见

BDD: 展厅父级菜单可见 -> Given 用户进入系统菜单 When 路由模块加载 Then “展厅”父级路由不应配置隐藏，并保留 `alwaysShow` 以展示子页签。

RED: `node --test scripts/showroom-admin-frontend.test.mjs` -> FAIL, `src/router/modules/showroom.ts` 仍存在 `hidden: true`，展厅父级菜单会被隐藏。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs` -> PASS

GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/router/modules/showroom.ts scripts/showroom-admin-frontend.test.mjs` -> PASS
