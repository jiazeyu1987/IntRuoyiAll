# 任务：展柜名称后缀统一为展柜和 Showcase

## 任务目标

- 将展柜管理中 `showroom_hall.name` 的中文名称后缀从“展厅”统一改为“展柜”。
- 将 `showroom_hall.name_en` 的英文名称后缀从 `Hall` 统一改为 `Showcase`。
- 提供可复跑 SQL 修正脚本，并同步种子 SQL 基线，避免重新初始化后回到旧后缀。

## 非目标

- 不修改前端展示逻辑，不在前端做显示层替换。
- 不修改展柜编码、产品映射、发布、审批、讲解或资产逻辑。
- 不改描述字段中的普通语义文字。
- 不对缺失英文名做自动兜底生成。

## 前置任务检查

- 最近同仓任务：`20260525-showroom-release-asset-tombstone-revival`。
- 上一任务状态：`completed`。
- 影响：上一任务已完成；当前仓库仍有该任务相关未提交改动，本任务只暂存和提交本次直接产生的文件。

## BDD 场景

- BDD: 展柜管理中文名称后缀统一 -> Given `showroom_hall.name` 中存在以“展厅”结尾的展柜名称, When 执行后缀修正脚本, Then 这些名称必须改为以“展柜”结尾，且非后缀文字保持不变。
- BDD: 展柜管理英文名称后缀统一 -> Given `showroom_hall.name_en` 中存在以 `Hall` 结尾的英文展柜名称, When 执行后缀修正脚本, Then 这些名称必须改为以 `Showcase` 结尾，且非后缀文字保持不变。
- BDD: 种子基线不再写入旧后缀 -> Given 使用 Excel 种子生成脚本重建 `20260519_showroom_excel_seed.sql`, When 比对提交基线, Then 种子 SQL 中 8 条展柜中文名称应使用“展柜”后缀，不再写入“展厅”后缀。

## 里程碑

- [x] M1：建立任务记录并确认上一同仓任务状态。
- [x] M2：补充 RED 回归测试，锁定 SQL 修正脚本与种子基线后缀规则。
- [x] M3：实现 SQL 修正脚本并更新 Excel 种子生成规则与提交基线。
- [x] M4：执行 GREEN 验证并只读核对本地运行库。
- [ ] M5：更新证据、运行 closeout 预览并按策略提交本任务变更。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py::test_showroom_hall_name_suffix_patch_updates_only_name_suffixes`
- `python -X utf8 -m pytest script/tests/test_showroom_excel_seed_tooling.py::test_showroom_excel_seed_script_matches_committed_sql`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -e "SELECT id, hall_code, name, name_en FROM showroom_hall WHERE deleted = 0 ORDER BY display_order, id;"`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-hall-name-suffix-showcase/bug-regression-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-hall-name-suffix-showcase --mode preview`

## Current Status

completed

## 当前状态

- 状态：completed
- 已完成：
  - 已用只读 SQL 确认本地运行库 8 条展柜名称仍以“展厅”和 `Hall` 结尾。
  - 已建立任务记录。
  - 已补充并通过 SQL 脚本与 Excel 种子基线回归测试。
  - 已新增可复跑 SQL 修正脚本，并同步 Excel 种子生成脚本与提交基线。
  - 已在本地 MySQL 执行修正脚本，8 条展柜均更新为“展柜 / Showcase”尾缀。
- 已通过 Playwright 真实前端路径 `/showroom/hall` 验证列表显示新尾缀。
- 阻塞与影响：
  - 暂无阻塞。

## Final Verification Result

- PASS: `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py::test_showroom_hall_name_suffix_patch_updates_only_name_suffixes`
- PASS: `python -X utf8 -m pytest script/tests/test_showroom_excel_seed_tooling.py::test_showroom_excel_seed_script_matches_committed_sql`
- PASS: 本地 MySQL 执行 `sql/mysql/20260525_showroom_hall_name_suffix_showcase.sql`
- PASS: `old_suffix_count = 0`
- PASS: Playwright 真实前端路径 `/showroom/hall`
- PASS: bug regression evidence 校验
- PASS: task-closeout-cleanup preview/apply

## Cleanup Keep

- `doc/tasks/20260525-showroom-hall-name-suffix-showcase/task.md`
- `doc/tasks/20260525-showroom-hall-name-suffix-showcase/execution-log.md`
- `doc/tasks/20260525-showroom-hall-name-suffix-showcase/bug-regression-evidence.md`

## Cleanup Candidates

- `doc/tasks/20260525-showroom-hall-name-suffix-showcase/scripts/verify-showroom-hall-name-suffix-page.mjs`
- `doc/tasks/20260525-showroom-hall-name-suffix-showcase/showroom-hall-name-suffix-failure.png`
