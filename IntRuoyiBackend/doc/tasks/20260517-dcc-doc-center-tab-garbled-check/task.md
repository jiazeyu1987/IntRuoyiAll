# Task: 检查 DCC 文控中心子页签文案是否存在乱码

## 目标
检查 IntRuoyi 前端中 DCC 文控中心页签及其子页签的文案、标题、按钮、占位符和标签是否存在乱码、编码异常或明显错别字。

## Milestones
- [ ] M1: 定位 DCC 文控中心相关前端页面、路由和子页签入口。
- [ ] M2: 逐页检查相关文案是否存在乱码或异常字符。
- [ ] M3: 如发现问题，记录受影响页面与具体文案，并补充验证结果。

## Verification
- 直接检查相关前端源码文案。
- 如需页面态验证，使用本地前端入口 `http://localhost:8081` 复核。

## Status
- Completed.

## Completed Work
- 定位到 DCC 文控中心菜单种子文件 `sql/mysql/20260513_dcc_base_schema.sql`。
- 确认子页签 `6816 / controlled-file/training-mine` 的名称在基础脚本里写成了乱码 `DCC鎴戠殑鍩硅`。
- 确认后续补丁 `sql/mysql/20260516_dcc_training_closed_loop_menu.sql` 里写的是正确名称 `DCC我的培训`，但它使用 `WHERE NOT EXISTS`，不会覆盖基础脚本里已存在的脏数据。

## Final Verification
- UTF-8 读取 `sql/mysql/20260513_dcc_base_schema.sql`，第 582 行仍是 `DCC鎴戠殑鍩硅`。
- UTF-8 读取 `sql/mysql/20260516_dcc_training_closed_loop_menu.sql`，第 3 行是正确的 `DCC我的培训`。
- 结论：当前初始化脚本链路里，DCC 文控中心“我的培训”子页签存在乱码风险。

## Remaining Blockers
- 无代码阻塞；如需消除初始化风险，需要补一个显式 `UPDATE` 修复脚本或直接修正基础种子。
