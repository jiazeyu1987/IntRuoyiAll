# 执行记录：展厅后台隐藏三个协作页签

BDD: 展厅后台隐藏三个协作页签 -> Given 用户从真实前端路径进入展厅后台菜单 / When 页面渲染展厅后台的子页签导航 / Then `版本历史`、`补充指派`、`产品讨论` 三个入口不应显示在导航中。

BDD: 隐藏入口不删除功能路由 -> Given 展厅后台仍保留既有页面实现和路由名称 / When 前端菜单过滤子页签 / Then 仅隐藏三个入口的展示，不删除对应路由与组件定义，也不引入 fallback。

REPRO: `src/router/modules/showroom.ts` 中 `ShowroomAdminHistory`、`ShowroomAdminAssignment`、`ShowroomAdminDiscussion` 三个子路由的 `meta` 只有 `canTo: true`，缺少 `hidden: true`，因此菜单系统仍会把它们渲染到展厅后台导航中。
ROOT CAUSE: 菜单过滤逻辑读取 `route.meta.hidden` 控制子菜单展示；这三个路由未显式标记隐藏，所以前端仍显示对应入口。
REGRESSION TEST: `scripts/showroom-admin-hide-three-tabs.test.mjs`
RED: `D:\Programs\node.exe --test scripts/showroom-admin-hide-three-tabs.test.mjs` -> FAIL，断言 `history / assignment / discussion` 路由块必须包含 `hidden: true` 时失败，实际源码仅有 `canTo: true`。
GREEN: `D:\Programs\node.exe --test scripts/showroom-admin-hide-three-tabs.test.mjs` -> PASS，三个路由块均包含 `hidden: true` 且仍保留 `canTo: true`。
GREEN: `D:\Programs\node.exe --test --test-name-pattern "showroom-admin route module registers the back-office shell and children" scripts/showroom-admin-frontend.test.mjs` -> PASS，展厅后台路由壳仍正常注册。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-admin-hide-three-tabs-fresh run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-admin-hide-three-tabs\scripts\verify-showroom-admin-hide-three-tabs.mjs` -> PASS，真实登录后 `http://127.0.0.1:8081/showroom/company` 左侧导航不再显示 `版本历史 / 补充指派 / 产品讨论`，同时仍显示 `展厅公司 / 产品管理 / 展厅管理 / 审批中心 / 讲解工作台`。
RISK: 复用旧 Playwright 会话时可能保留先前 HMR 失败状态；本次已使用 fresh session 完成真实路径复核。
BLOCKER: none
