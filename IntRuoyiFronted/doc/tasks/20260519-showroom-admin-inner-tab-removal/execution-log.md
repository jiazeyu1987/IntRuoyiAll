# 执行记录：删除展厅后台页面内部页签

BDD: 后台页面不重复显示内部 tab -> Given 用户通过“展厅”菜单子页签进入审批中心 When 页面渲染 Then 页面内容区不再出现 `el-tabs` 内部页签，仅显示当前子路由对应内容。

RED: `node --test scripts/showroom-admin-frontend.test.mjs` -> FAIL, 后台页面仍存在 `<el-tabs>`、`showroom-admin-tabs` 和 `handleAdminTabChange`。

RED: `node --test scripts/showroom-admin-frontend.test.mjs` -> FAIL, 旧测试仍用内部 tab 标签文本判断模块存在，需要改为验证业务内容行。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs` -> PASS

GREEN: `$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/index.vue scripts/showroom-admin-frontend.test.mjs` -> PASS
