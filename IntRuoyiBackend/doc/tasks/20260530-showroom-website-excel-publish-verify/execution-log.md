# 执行记录：展厅产品 Excel 到 Website 发布验证（后端）

BDD: 正式 Excel 首次发布到 Website -> Given 测试租户导入正式 Excel / When 发布产品并手动发布 Website 包 / Then 本地 Website 对应产品内容与 Excel 一致。

BDD: Excel 产品名变更传播到 Website -> Given 修改正式 Excel 中一个产品的 `产品名-中文` / When 再次导入、发布产品并手动发布 Website 包 / Then 本地 Website 对应产品名更新为修改后的值。

BDD: 验证隔离 -> Given 本任务只做验证 / When 需要临时 Excel 或发布产物 / Then 不修改正式业务代码，不影响芋道源码租户数据。

GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，生成新 worktree 后端 jar。

RED: `node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> FAIL，后端手动发布缺少 `showroom.release.public-website-origin`，返回 `SHOWROOM_RELEASE_PUBLIC_READBACK_CONFIG_MISSING`。

RED: 同一 E2E，后端配置 `showroom.release.public-website-origin=http://127.0.0.1:4175` -> FAIL，发布读回经 Vite Website 代理超时，返回 `SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED`。

RED: 同一 E2E，后端配置 `showroom.release.public-website-origin=http://127.0.0.1:18085` -> FAIL，手动发布成功并进入 Website 比对，但 Website 运行时读取发布包失败：`showrooms[0].products[8].nameCn is required`。

GREEN: `raw-release-excel-comparison.json` -> PASS，首次成功 release `20260530T121111Z-cdf9733a057e-609cb4c53071` 含 160 个产品；与正式 Excel 的产品编码、中文名、英文名逐项一致，缺失产品 0，额外产品 0，名称不一致 0。

BLOCKER: 正式 Excel 与 raw release 中 81 个已发布产品缺少 Website 必填字段 `产品名-中文/nameCn`，首个为 Excel 第 10 行 `product_010`。证据文件：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260530-showroom-website-excel-publish-verify\output\playwright\showroom-website-excel-publish\raw-release-excel-comparison.json`。

GREEN: Website 修复后 `node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS，首次发布 `20260530T123127Z-cdf9733a057e-609cb4c53071`、二次发布 `20260530T123354Z-cdf9733a057e-609cb4c53071`，使用修改副本验证 `product_001` 名称从 `一次性使用三通旋塞` 更新为 `一次性使用三通旋塞-E2E名称验证20260530123304`。

GREEN: 原文件原地修改验证 `SHOWROOM_WEBSITE_PUBLISH_MODIFY_REFERENCE_IN_PLACE=1 node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS，首次发布 `20260530T124019Z-cdf9733a057e-609cb4c53071`、二次发布 `20260530T124243Z-cdf9733a057e-609cb4c53071`；`D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 第 2 行 `product_001` 已改为 `一次性使用三通旋塞-E2E名称验证20260530124150`，Website 二次发布比对 160/160 产品通过，名称不一致 0。

GREEN: 融合后复测 `SHOWROOM_WEBSITE_PUBLISH_WEBSITE_BASE_URL=http://127.0.0.1:4176 SHOWROOM_WEBSITE_PUBLISH_WEBSITE_WORKTREE=D:\ProjectPackage\Website SHOWROOM_WEBSITE_PUBLISH_MODIFY_REFERENCE_IN_PLACE=1 node doc\tasks\20260530-showroom-website-excel-publish-verify\scripts\verify-website-publish-flow.e2e.js` -> PASS，测试租户首次发布 `20260530T125656Z-cdf9733a057e-609cb4c53071`、二次发布 `20260530T125940Z-cdf9733a057e-609cb4c53071`；融合后的主 Website 与 Excel 两轮均 160/160 产品一致，名称不一致 0，`product_001` 更新为 `一次性使用三通旋塞-E2E名称验证20260530124150-E2E名称验证20260530125844`。
