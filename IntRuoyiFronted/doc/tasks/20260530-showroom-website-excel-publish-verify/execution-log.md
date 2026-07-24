# 执行记录：展厅产品 Excel 到 Website 发布验证（前端）

BDD: 测试租户真实导入发布 -> Given 用户以测试租户登录 / When 在展厅产品管理导入正式 Excel 并发布 / Then 产品数据进入待发布或已发布状态。

BDD: 手动发布 Website 包 -> Given 产品数据已发布 / When 用户在展厅页面点击手动发布展厅按钮 / Then 本地 Website 能读取新发布内容。

BDD: 产品名二次更新 -> Given Excel 中一个产品名被修改 / When 重复导入和发布 / Then Website 中相同产品编码的名称变为修改后的名称。

GREEN: `node --check doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS。

RED: `node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> FAIL，后端手动发布缺少 `showroom.release.public-website-origin`。

RED: 同一 E2E，后端发布读回指向 `http://127.0.0.1:4175` -> FAIL，Vite Website 代理路径读回超时。

RED: 同一 E2E，后端发布读回指向 `http://127.0.0.1:18085` -> FAIL，发布成功后 Website 运行时比对失败：`showrooms[0].products[8].nameCn is required`。

GREEN: Raw release vs Excel comparison -> PASS，release `20260530T121111Z-cdf9733a057e-609cb4c53071` 与正式 Excel 在 160 个产品编码和中英文名上逐项一致。

BLOCKER: Website 运行契约要求 `nameCn` 非空；正式 Excel 中 81 个已发布产品 `产品名-中文` 为空，导致本地 Website 无法展示首次发布包。证据文件：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260530-showroom-website-excel-publish-verify\output\playwright\showroom-website-excel-publish\raw-release-excel-comparison.json`。

GREEN: `node --check doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS。

GREEN: Website 修复后 `node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS，修改副本二次导入发布后 Website 与 Excel 160/160 产品一致，名称不一致 0。

GREEN: 原文件原地修改验证 `SHOWROOM_WEBSITE_PUBLISH_MODIFY_REFERENCE_IN_PLACE=1 node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS；正式 Excel 第 2 行 `product_001` 名称从 `一次性使用三通旋塞` 更新为 `一次性使用三通旋塞-E2E名称验证20260530124150`，二次发布 release `20260530T124243Z-cdf9733a057e-609cb4c53071` 与本地 Website 比对通过。

GREEN: 融合后复测 `SHOWROOM_WEBSITE_PUBLISH_WEBSITE_BASE_URL=http://127.0.0.1:4176 SHOWROOM_WEBSITE_PUBLISH_WEBSITE_WORKTREE=D:\ProjectPackage\Website SHOWROOM_WEBSITE_PUBLISH_MODIFY_REFERENCE_IN_PLACE=1 node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS；测试租户真实 UI 再次完成正式 Excel 导入、全部发布、手动发布展厅，融合后的主 Website 两轮比对均 160/160 产品一致，名称不一致 0，二次发布 release `20260530T125940Z-cdf9733a057e-609cb4c53071`。
