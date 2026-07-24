# 任务：展厅产品 Excel 到 Website 发布验证（后端）

## 任务目标

在新的 worktree 中，使用测试租户通过真实前端导入 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx`，发布产品并手动发布展厅 Website 包；验证本地 Website 展示内容与 Excel 一致。随后修改 Excel 中一个产品的产品名，再重复导入、发布、Website 验证，确认 Website 对应产品名随 Excel 更新。

## 里程碑

- [x] M1：创建独立验证 worktree 并建立任务文档。
- [x] M2：启动新 worktree 后端并连接真实测试租户数据源。
- [x] M3：通过测试租户真实前端导入正式 Excel。
- [x] M4：执行全部产品发布与手动发布展厅 Website 包。
- [x] M5：读取本地 Website 发布结果，与 Excel 做产品内容一致性校验。
- [x] M6：修改一个产品名后重复导入、发布和 Website 校验。
- [x] M7：记录验证证据并收尾。

## BDD 场景

- BDD: 正式 Excel 首次发布到 Website -> Given 测试租户导入正式 Excel / When 发布产品并手动发布 Website 包 / Then 本地 Website 对应产品内容与 Excel 一致。
- BDD: Excel 产品名变更传播到 Website -> Given 修改正式 Excel 中一个产品的 `产品名-中文` / When 再次导入、发布产品并手动发布 Website 包 / Then 本地 Website 对应产品名更新为修改后的值。
- BDD: 验证隔离 -> Given 本任务只做验证 / When 需要临时 Excel 或发布产物 / Then 不修改正式业务代码，不影响芋道源码租户数据。

## 预期验证

- Playwright 使用测试租户真实登录路径。
- 导入 Excel 走“展厅 / 产品管理”真实 UI。
- 发布展厅 Website 包走真实 UI。
- Website 校验读取本地 Website 运行结果，至少覆盖 `160` 行产品编码与产品名一致性，以及被修改产品名的二次传播。

## 当前状态

Completed: 已在测试租户完成正式 Excel 首次导入、全部发布产品、手动发布 Website 包和本地 Website 比对。随后原地修改 `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版-补充产品资料.xlsx` 中 `product_001` 的 `产品名-中文`，再次导入、全部发布、手动发布并验证本地 Website 对应产品名同步变化。前后端与 Website 快进融合后，又使用测试租户在融合后的主 Website `http://127.0.0.1:4176` 完整复测通过。

最终证据：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260530-showroom-website-excel-publish-verify\output\playwright\showroom-website-excel-publish\website-publish-flow-result.json`。

融合后复测证据：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260530-showroom-website-excel-publish-verify\output\playwright\showroom-website-excel-publish-after-merge\website-publish-flow-result.json`。
