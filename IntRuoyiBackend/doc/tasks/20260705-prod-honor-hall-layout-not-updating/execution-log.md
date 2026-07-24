# 执行日志

## BDD / TDD
- BDD: 发布后前台布局应随后台调整变化 -> Given 正式服后台调整企业荣誉展柜 1/2 布局并手动发布, When Website 前台安装目标 release, Then release `website-index` 与前台渲染应使用调整后的布局。

## 门禁
- GREEN: experience-preflight -> PASS, 已读取 PowerShell、经验索引与正式服访问门禁；本轮先只读定位。

## 只读验证证据
- GREEN: prod-release-pointer-read -> PASS, 正式服 `showroom_release_pointer` 当前指向 `20260705T084043Z-be276b74dfa8-5cdcefdb51e7`，`manifest_hash=d126e3ec4e6eb37baffe62647e7c54a963ff6f72d82a678c73a0ad4150885982`。
- GREEN: prod-honor-db-layout-read -> PASS, 正式服 `showroom_hall_item` 中 `hall_09` / `hall_10` 的 AWARD 布局均有 `layout_x/layout_y/layout_width/layout_height`，更新时间分别为 `2026-07-05 16:40:12` 和 `2026-07-05 16:40:38`。
- GREEN: prod-website-index-layout-read -> PASS, 最新 `website-index` 中 `hall_09` / `hall_10` 均包含 `layoutX/layoutY/layoutWidth/layoutHeight`；`hall_10` 当前布局摘要为 `c22105ae2ec82d65e695338652c9d311002f4e64991c45fe2105b3e7730e10d3`，上一版为 `b5145baaca6a64cdd7ae04704535a62c2a41eee9259fccff3e946206a9c904e9`。
- GREEN: website-render-path-read -> PASS, `D:\ProjectPackage\Website\src\showroom-api.js` 会映射 layout 字段，但 `D:\ProjectPackage\Website\src\medical-kiosk.js` 的 `AWARD` 渲染路径走 `createAwardsWallMarkup()` / `createAwardCardMarkup()`，没有使用 `createProductLayoutStyle()`。

## 结论
- 不是发布失败，也不是 release 指针没有更新。
- 根因是 Website 对企业荣誉 `AWARD` 项使用固定荣誉墙网格渲染，发布包中的布局坐标没有进入最终 DOM 样式。
