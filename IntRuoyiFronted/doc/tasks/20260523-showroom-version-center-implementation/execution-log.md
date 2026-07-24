# Execution Log: 20260523-showroom-version-center-implementation

BDD: 公司和产品都应通过独立静态隐藏路由进入版本中心 -> Given 用户位于公司工作台或产品列表 / When 点击“进入版本中心”或“版本中心” / Then 系统必须跳到各自的静态隐藏路由，并保持正确的 activeMenu 与返回行为。

BDD: 版本中心页面必须同时显示当前内容版本、当前线上版本和当前 release -> Given history/detail 接口返回三套状态 / When 页面渲染头部与左栏 / Then 用户必须能区分当前内容、当前线上和当前 release，不允许混用单一“当前版本”概念。

BDD: 版本中心页面必须展示历史版本预览与当前内容 diff -> Given 用户选中某个历史版本 / When detail 接口返回字段、图片、语音与 diff 数据 / Then 页面必须渲染双语字段、内容图片、公开 preview 资产摘要、双语语音和当前内容 diff。

BDD: republish 必须显式展示 target blocker 与 global release blocker -> Given 用户准备执行历史版本重发 / When 页面打开确认弹窗 / Then 弹窗必须展示一步到位发布说明、target 级 blocker 和 GLOBAL_RELEASE blocker，并在成功后刷新 history/detail，不离开页面。

RED: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-product-version-browser.test.mjs` -> FAIL, 产品详情仍保留 revision selector，产品列表/壳页尚未接入版本中心路由，新版本中心页面与合同文件尚不存在。

GREEN: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-product-detail-entry.test.mjs` -> PASS

REGRESSION: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-product-detail-entry.test.mjs` -> PASS

REGRESSION: `node --test scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-detail-entry.test.mjs` -> PASS

REGRESSION: `node --test scripts/showroom-admin-version-center.test.mjs scripts/showroom-admin-version-center-interaction.test.mjs scripts/showroom-admin-product-version-browser.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-company-dashboard-history.test.mjs scripts/showroom-admin-product-detail-entry.test.mjs` -> PASS

GREEN: `pnpm install --frozen-lockfile` -> PASS，frontend worktree 已补齐本地依赖。

GREEN: `pnpm exec vue-tsc --noEmit -p tmp/tsconfig.version-center.json` -> PASS，已对本次改动相关文件完成局部类型检查；全量 `tsconfig.relaxed.json` 仍会因工作区规模触发 Node OOM，不作为本次版本中心实现 blocker。
