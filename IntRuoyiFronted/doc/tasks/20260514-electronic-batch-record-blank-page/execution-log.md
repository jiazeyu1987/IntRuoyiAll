BDD: 电子批记录菜单指向真实组件 -> Given 运行时菜单仍暴露 `电子批记录` 菜单且路径为 `/mes/pro/batch-record-template`, When 前端解析菜单中的 `component` 字段, Then `mes/pro/batchrecordtemplate/index` 必须在当前分支存在真实视图文件，页面不能静默空白。

BDD: 后端接口缺失时页面必须失败直出 -> Given 当前环境未部署 `/admin-api/mes/pro/batch-record-template/**`, When 用户打开电子批记录入口页, Then 页面必须直接显示缺少的后端前置条件和影响，而不是渲染空白区域。

RED: python login + permission-info menu inspection -> FAIL, runtime menu returned `电子批记录` with component `mes/pro/batchrecordtemplate/index`, but `Test-Path D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3\\src\\views\\mes\\pro\\batchrecordtemplate\\index.vue` was `False`.

RED: live backend endpoint probe -> FAIL, `GET /admin-api/mes/pro/batch-record-template/page?pageNo=1&pageSize=10` returned `{"success":false,"message":"No static resource admin-api/mes/pro/batch-record-template/page.","code":500,...}`.

GREEN: node --test scripts/electronic-batch-record-route.test.mjs -> PASS, the live menu target now resolves to a real frontend view file.

GREEN: pnpm exec eslint scripts/electronic-batch-record-route.test.mjs src/views/mes/pro/batchrecordtemplate/index.vue -> PASS.

GREEN: pnpm build:local -> PASS, Vite local build completed successfully.

GREEN: page behavior contract -> PASS, the restored entry page no longer depends on a missing dynamic component and explicitly reports the missing backend route family.
