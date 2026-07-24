# 任务：展厅产品 Excel 到 Website 发布验证（前端）

## 任务目标

在新的 worktree 中，通过测试租户真实前端路径完成展厅产品 Excel 导入、产品发布、手动发布展厅 Website 包，并验证本地 Website 内容与 Excel 一致；再修改一个产品的中文名并重复流程，确认 Website 名称随 Excel 更新。

## 里程碑

- [x] M1：创建独立验证 worktree 并建立任务文档。
- [x] M2：启动新 worktree 前端并连接新 worktree 后端。
- [x] M3：用 Playwright 走测试租户导入正式 Excel。
- [x] M4：用 Playwright 执行产品发布与展厅手动发布 Website 包。
- [x] M5：用 Playwright 或发布接口读取本地 Website 展示内容并与 Excel 比对。
- [x] M6：修改一个产品名后重复验证更新传播。
- [x] M7：记录 E2E 证据并收尾。

## BDD 场景

- BDD: 测试租户真实导入发布 -> Given 用户以测试租户登录 / When 在展厅产品管理导入正式 Excel 并发布 / Then 产品数据进入待发布或已发布状态。
- BDD: 手动发布 Website 包 -> Given 产品数据已发布 / When 用户在展厅页面点击手动发布展厅按钮 / Then 本地 Website 能读取新发布内容。
- BDD: 产品名二次更新 -> Given Excel 中一个产品名被修改 / When 重复导入和发布 / Then Website 中相同产品编码的名称变为修改后的名称。

## 预期验证

- 真实浏览器登录测试租户：`测试租户 / aoteman / admin123`。
- 真实 UI：导入、发布产品、手动发布展厅。
- 本地 Website：验证产品编码、中文名与 Excel 一致，二次修改产品名后可见变化。

## 当前状态

Completed: 已通过测试租户真实 UI 完成正式 Excel 导入、全部发布产品、手动发布展厅、本地 Website 比对；随后原地修改正式 Excel 中一个产品名并重复流程，Website 对应产品名已同步变化。前后端与 Website 快进融合后，使用测试租户在融合后的主 Website `http://127.0.0.1:4176` 再次完整复测通过。E2E 结果文件：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260530-showroom-website-excel-publish-verify\output\playwright\showroom-website-excel-publish\website-publish-flow-result.json`。融合后复测文件：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260530-showroom-website-excel-publish-verify\output\playwright\showroom-website-excel-publish-after-merge\website-publish-flow-result.json`。
