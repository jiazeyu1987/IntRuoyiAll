# 正式服企业荣誉展柜布局发布后 Website 未变化分析

## 任务目标
- 分析用户在正式服调整“企业荣誉展柜 1/2”布局并发布后，Website 前台布局没有跟随变化的原因。
- 本轮先做只读定位；除非根因明确且用户授权修复，否则不写业务数据。

## 里程碑
1. 建立任务记录并读取正式服/PowerShell 门禁。- 已完成
2. 核正式服展柜、映射布局、最新 release、manifest 与 website-index 文档。- 已完成
3. 对比后台当前布局与 release 文档布局，定位卡在发布链路还是前台渲染链路。- 已完成
4. 输出原因、影响范围和修复建议。- 已完成

## 预期验证
- 能指出“企业荣誉展柜 1/2”对应 hall / item 数据、release 文档字段和前台渲染字段之间的差异。
- 不使用猜测或 fallback 解释。

## 经验门禁
- 正式服读取已由当前问题授权；本轮默认只读分析。
- PowerShell 使用 UTF-8；远程脚本用 UTF-8 base64；不使用 `&&`。
- 不操作 `/mnt/nas` 根目录、挂载或 fstab。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，本轮定位到发布包已带布局字段，但 Website 对 `AWARD` 项走荣誉墙网格渲染，未使用发布包坐标。
- 是否存在临时补丁或绕过：否。

## 只读定位结论
- 正式服当前指针已更新：`showroom_release_pointer` 指向 `20260705T084043Z-be276b74dfa8-5cdcefdb51e7`，不是前台仍停留在旧 release。
- 正式服数据库中 `hall_09` / `hall_10` 即“企业荣誉展柜1/2”，`showroom_hall_item` 的 AWARD 布局在 2026-07-05 16:40:12 / 16:40:38 已更新。
- 最新 `website-index` 文档已经包含 `hall_09` / `hall_10` 的 `layoutX/layoutY/layoutWidth/layoutHeight`；其中 `hall_10` 从上一版 `b5145...` 变为当前 `c22105...`，说明发布包确实带出了调整后的布局。
- Website 前端映射层会把 release layout 映射成 `product.layout`，但展示层对 `itemType === "AWARD"` 单独走 `createAwardsWallMarkup()` / `createAwardCardMarkup()`；该路径没有调用 `createProductCardMarkup()` 或 `createProductLayoutStyle()`，因此不会使用 `layout.x/y/width/height`。
- 根因：企业荣誉项是 `AWARD`，发布包有坐标，但 Website 前台把 AWARD 渲染为按年份分组的荣誉墙网格，绕开了画布坐标布局。

## 修复建议
- 在 Website 前台修复 `AWARD` 渲染路径：当展柜以 canvas 布局展示时，`AWARD` 应与 PRODUCT 一样使用发布包中的 `layout` 坐标定位。
- 保留 `AWARD` 详情与音频逻辑，不改变发布包契约；修复点应集中在 `D:\ProjectPackage\Website\src\medical-kiosk.js` 的 gallery 渲染路径。
- 增加回归测试：构造 `企业荣誉展柜` 中 AWARD 的非默认坐标，断言渲染出的卡片带 `left/top/width/height`，避免再次退回固定荣誉墙网格。

## 当前状态
- 状态：已完成只读原因分析，待用户确认是否继续修复 Website 前端渲染。
- 当前步骤：等待修复授权。
