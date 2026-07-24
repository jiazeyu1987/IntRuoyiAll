# 执行日志：排查 hall_01 发布缺少画布布局

- CHECK: 上一后端任务状态 -> PASS，`doc/tasks/20260606-runtime-console-build-test-backup-release/task.md` 的 `Current Status` 为 `completed`。
- BDD: 发布阶段阻止缺失画布布局的展柜 -> Given 展柜 `hall_01` 存在产品映射但任一映射缺少 `layoutX/layoutY/layoutWidth/layoutHeight` / When 用户手动发布展厅 / Then 后端抛出 `SHOWROOM_RELEASE_HALL_BLOCKED`，不得生成默认布局。
- RED: 用户手动发布展厅 -> FAIL，expected reason：`SHOWROOM_RELEASE_HALL_BLOCKED: hallId=1 hallCode=hall_01 reason=SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required`。
- CHECK: 代码路径 -> PASS，`ShowroomReleaseAssembler` 发布阶段对每个展柜调用 `ShowroomHallCanvasLayoutPolicy.requireCanvasLayout`；该策略要求每条产品映射都有完整 `layoutX/layoutY/layoutWidth/layoutHeight`，否则抛出 `SHOWROOM_REQUIRED_FIELD_MISSING: hall canvas layout is required`，外层包装为 `SHOWROOM_RELEASE_HALL_BLOCKED`。
- CHECK: `java QueryHallLayout 1` 只读 SQL -> PASS，`hall_id=1 tenant_id=1 hall_code=hall_01 name=心内介植入展柜 mapping_count=24 complete_layout_count=0 missing_layout_count=24`。
- CHECK: `hall_01` 映射明细 -> PASS，`product_001`、`product_003` 至 `product_025` 共 24 条映射的 `layout_x/layout_y/layout_width/layout_height` 均为 `NULL`。
- BLOCKED: `hall_01` 位于 `芋道源码/admin` 租户；未经用户明确授权，不写入该租户数据。解除条件是在展柜管理中保存完整画布布局后重新发布。
- BDD: 授权后修复空画布布局 -> Given 用户明确授权修复 `芋道源码/admin` 租户空布局映射 / When 只读确认缺失展柜均为整柜布局为空并执行默认布局修复 / Then 缺失展柜的每条产品映射写入完整 `layout_x/layout_y/layout_width/layout_height`，已存在完整布局的展柜不被覆盖。
- CHECK: 用户授权 -> PASS，用户明确要求“芋道源码/admin租户的展柜的布局信息是null的都改成默认的布局信息”。
- RED: `java RepairAdminDefaultLayouts` dry-run -> FAIL，expected reason：`tenant_id=1` 中 7 个展柜整柜布局为空，`REPAIR_TARGET_COUNT=7`；`hall_01=24`、`hall_02=28`、`hall_04=17`、`hall_05=10`、`hall_06=20`、`hall_07=11`、`hall_08=27`，共 137 条映射待修复。`hall_03` 已完整，不在修复范围。
- GREEN: `java RepairAdminDefaultLayouts --apply` -> PASS，按后端默认网格算法修复 `tenant_id=1` 的 7 个整柜空布局展柜；更新行数 `UPDATED_ROWS=137`，`COMMIT=OK`。
- GREEN: `java RepairAdminDefaultLayouts` dry-run -> PASS，8 个展柜均 `complete_count = mapping_count`，`all_null_count=0`，`partial_count=0`，`REPAIR_TARGET_COUNT=0`。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260607-showroom-release-hall01-layout-missing\bug-regression-evidence.md` -> PASS。
- CHECK: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260607-showroom-release-hall01-layout-missing --mode preview` -> PASS，delete/blocked/warnings 均为 `<none>`。
