# Execution Log: 展厅公司菜单可见即编辑直存（前端）

BDD: 公司页菜单可见即可编辑 -> Given 用户能够进入 `/showroom/company` / When 页面加载完成 / Then `编辑公司`、`生成语音`、`保存语音` 不应再受 `showroom_publicity` 角色门控。

BDD: 公司页主动作案切换为保存 -> Given 用户打开公司编辑弹框 / When 页面渲染底部主按钮 / Then 应显示 `保存`，并继续表示这是无审批直存路径，而不是 `直接发布` 或 `提交审批`。

RED: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs` -> FAIL，`CompanyWorkbench.vue` 仍包含 `showroom_publicity / SHOWROOM_COMPANY_EDITOR_ROLE` 门控，且页面文案仍保留 `直接发布` 与 `只有企宣角色可以...`。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs` -> PASS。
GREEN: `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue src/views/showroom-admin/company/contracts.ts scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs` -> PASS。
