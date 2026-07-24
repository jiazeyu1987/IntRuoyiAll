# 执行日志：展柜名称后缀统一为展柜和 Showcase

BDD: 展柜管理中文名称后缀统一 -> Given `showroom_hall.name` 中存在以“展厅”结尾的展柜名称, When 执行后缀修正脚本, Then 这些名称必须改为以“展柜”结尾，且非后缀文字保持不变。

BDD: 展柜管理英文名称后缀统一 -> Given `showroom_hall.name_en` 中存在以 `Hall` 结尾的英文展柜名称, When 执行后缀修正脚本, Then 这些名称必须改为以 `Showcase` 结尾，且非后缀文字保持不变。

BDD: 种子基线不再写入旧后缀 -> Given 使用 Excel 种子生成脚本重建 `20260519_showroom_excel_seed.sql`, When 比对提交基线, Then 种子 SQL 中 8 条展柜中文名称应使用“展柜”后缀，不再写入“展厅”后缀。

INFO: 只读 SQL 复现 -> `showroom_hall` 当前 8 条记录分别为 `心内介植入展厅 / Cardiac Intervention Implant Hall`、`心脏植入展厅 / Cardiac Implant Hall`、`外周介植入展厅 / Peripheral Intervention Implant Hall`、`神经介植入展厅 / Neuro Intervention Implant Hall`、`外泌体与超声聚焦展厅 / Exosome and Focused Ultrasound Hall`、`骨科与泌尿产品展厅 / Orthopedics and Urology Products Hall`、`非介入类产品展厅 / Non-interventional Products Hall`、`医疗标准件展厅 / Medical Standard Components Hall`。

RED: `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py::test_showroom_hall_name_suffix_patch_updates_only_name_suffixes` -> FAIL, 缺少 `sql/mysql/20260525_showroom_hall_name_suffix_showcase.sql` 修正脚本。

RED: `python -X utf8 -m pytest script/tests/test_showroom_excel_seed_tooling.py::test_showroom_excel_seed_script_matches_committed_sql` -> FAIL, 当前 Excel 种子生成结果仍包含 `心内介植入展厅`，缺少 `心内介植入展柜`。

INFO: 已新增 `sql/mysql/20260525_showroom_hall_name_suffix_showcase.sql`，只更新 `showroom_hall.name` 与 `showroom_hall.name_en` 的尾缀；已更新 Excel 种子生成脚本并重新生成 `20260519_showroom_excel_seed.sql`。

GREEN: `python -X utf8 -m pytest script/tests/test_showroom_sql_scripts.py::test_showroom_hall_name_suffix_patch_updates_only_name_suffixes` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_showroom_excel_seed_tooling.py::test_showroom_excel_seed_script_matches_committed_sql` -> PASS。

GREEN: 本地 MySQL 执行 `sql/mysql/20260525_showroom_hall_name_suffix_showcase.sql` 并只读核对 -> PASS，8 条记录更新为 `心内介植入展柜 / Cardiac Intervention Implant Showcase`、`心脏植入展柜 / Cardiac Implant Showcase`、`外周介植入展柜 / Peripheral Intervention Implant Showcase`、`神经介植入展柜 / Neuro Intervention Implant Showcase`、`外泌体与超声聚焦展柜 / Exosome and Focused Ultrasound Showcase`、`骨科与泌尿产品展柜 / Orthopedics and Urology Products Showcase`、`非介入类产品展柜 / Non-interventional Products Showcase`、`医疗标准件展柜 / Medical Standard Components Showcase`，`old_suffix_count = 0`。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-name-suffix-showcase open http://127.0.0.1:8081/login?redirect=%2Fshowroom%2Fhall` + `run-code --filename doc/tasks/20260525-showroom-hall-name-suffix-showcase/scripts/verify-showroom-hall-name-suffix-page.mjs` -> PASS，真实前端 `/showroom/hall` 列表 8 条展柜均显示“展柜 / Showcase”新后缀，旧后缀不可见。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-showroom-hall-name-suffix-showcase/bug-regression-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-hall-name-suffix-showcase --mode preview` -> PASS，预览仅删除本次临时 Playwright 验证脚本，保留 `task.md`、`execution-log.md`、`bug-regression-evidence.md`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-showroom-hall-name-suffix-showcase --mode apply` -> PASS，已删除临时 Playwright 验证脚本。
