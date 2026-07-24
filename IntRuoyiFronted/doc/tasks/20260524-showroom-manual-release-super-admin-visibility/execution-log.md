# 执行日志：修复展厅手动发布按钮对超级管理员不可见

BDD: 超级管理员也应看到手动发布展厅按钮 -> Given 当前用户角色包含 `super_admin` 且进入 `/showroom/company` / When 公司工作台头部动作区渲染 / Then “手动发布展厅”按钮必须可见

BDD: 企宣角色与超级管理员共享同一发布入口 -> Given 当前用户角色包含 `showroom_publicity` 或 `super_admin` / When 点击“手动发布展厅” / Then 前端都必须允许触发 `/showroom/release/publish`

BDD: 非企宣且非超级管理员仍不得看到按钮 -> Given 当前用户既不是 `showroom_publicity` 也不是 `super_admin` / When 公司工作台渲染 / Then 发布按钮仍必须隐藏

INVESTIGATION: 2026-05-24 -> 已确认后端 `ShowroomAdminController` 发布权限为 `showroom_publicity || super_admin`。
INVESTIGATION: 2026-05-24 -> 已确认前端 `CompanyWorkbench.vue` 当前只按 `showroom_publicity` 判断按钮可见性，因此 `芋道源码 / admin` 看不到按钮而测试企宣账号可见。
RED: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> FAIL，按钮条件仍为 `isShowroomPublicity`，测试要求的 `canPublishShowroomRelease`、`SUPER_ADMIN_ROLE_CODE` 与 `roles.includes('super_admin')` 均缺失。
GREEN: `node --test scripts/showroom-admin-manual-release-button.test.mjs` -> PASS，按钮条件已对齐为 `showroom_publicity || super_admin`。
GREEN: `node node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src\views\showroom-admin\company\CompanyWorkbench.vue scripts\showroom-admin-manual-release-button.test.mjs` -> PASS。
GREEN: 代码契约核对 -> PASS，后端 `ShowroomAdminController` 使用 `securityFrameworkService.hasRole(SHOWROOM_PUBLICITY_ROLE_CODE) || securityFrameworkService.hasRole(RoleCodeEnum.SUPER_ADMIN.getCode())`；前端现在也使用同样的两个角色码。
BLOCKED: Playwright 本地真实登录 `芋道源码 / admin / admin123` -> FAIL，当前本地临时前端 `http://127.0.0.1:18082/login?redirect=%2Fshowroom%2Fcompany` 未能走过登录页，页面未回显明确错误，因此无法在本机完成该账号的最终 UI 可见性实测。
