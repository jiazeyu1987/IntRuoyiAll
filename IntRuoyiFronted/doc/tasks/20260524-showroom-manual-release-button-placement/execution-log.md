# 执行日志：调整展厅手动发布按钮位置

BDD: 手动发布按钮位于编辑公司右侧 -> Given 企宣角色进入 `/showroom/company` / When 公司工作台头部动作区渲染 / Then “手动发布展厅”按钮必须与“进入版本中心”“编辑公司”处于同一动作区，并排在“编辑公司”的右侧

BDD: 按钮位置调整后仍保留真实发布交互 -> Given 企宣角色点击“手动发布展厅” / When 用户确认发布 / Then 前端仍必须调用 `/showroom/release/publish`，并保持确认、loading、成功与失败提示不变

BDD: 首页全局工具条不再显示发布按钮 -> Given 打开展厅管理首页容器 `showroom-admin/index.vue` / When 公司工作台单独承载发布动作 / Then 首页不再渲染独立的全局发布工具条

INVESTIGATION: 2026-05-24 -> 已确认当前代码把“手动发布展厅”按钮放在 `showroom-admin/index.vue` 顶部工具条，不符合“放在编辑公司的右边”的新要求。
INVESTIGATION: 2026-05-24 -> 已确认公司页头部动作区位于 `src/views/showroom-admin/company/CompanyWorkbench.vue`，当前动作顺序为 `状态 tag -> 进入版本中心 -> 编辑公司`。
RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，当前 `CompanyWorkbench.vue` 不包含“手动发布展厅”，首页 `showroom-admin/index.vue` 仍渲染全局发布工具条。
GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS，按钮已迁移到 `CompanyWorkbench.vue` 动作区，并通过源码顺序断言锁定在“编辑公司”右侧。
GREEN: `node node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src\views\showroom-admin\company\CompanyWorkbench.vue src\views\showroom-admin\index.vue scripts\showroom-admin-manual-release-button.test.mjs` -> PASS。
GREEN: Playwright 真实页面 `http://127.0.0.1:18082/showroom/company` -> PASS，登录 `测试租户 / aoteman / admin123` 后按钮可见；`编辑公司` 按钮坐标 `x=1008`，`手动发布展厅` 按钮坐标 `x=1116`，确认发布按钮位于右侧。
GREEN: Playwright 真实确认发布 -> PASS，页面仍实际发出 `POST /admin-api/showroom/release/publish`；当前测试租户返回 `SHOWROOM_TARGET_NOT_FOUND: live product revision not found`，前端 toast 原样显示该错误。
