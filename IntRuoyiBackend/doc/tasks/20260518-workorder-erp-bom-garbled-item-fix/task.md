# Task: 修复 ERP 同步 BOM 后工单详情乱码

## Goal

修复点击 `ERP同步BOM` 后工单详情 `工单BOM / 物料需求` 页签出现乱码的问题，确保同步后展示的物料名称、规格型号和受影响单位使用正确中文文本，并阻止同类乱码继续写入本地 ERP/MES 主数据。

## Scope

- 先确认上一个后端任务状态，并把它按当前用户优先级切换显式标记为阻塞。
- 创建当前任务文档、执行日志与缺陷回归证据，再开始生产代码修改。
- 复现 `903245` 工单同步后的乱码现象，并确认乱码来源于本地 `erp_product` / `mes_md_item` / 关联单位主数据，而不是前端表格渲染。
- 先补失败的后端回归测试，覆盖 Kingdee 物料文本出现 UTF-8/Latin-1 混乱时的修复行为。
- 只做与 Kingdee 物料文本解码、同步链路和当前受影响主数据修复直接相关的最小改动；不引入 fallback，不改无关工单逻辑。

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-keep-only-four-process-routes/task.md`
- Status before this task: blocked by higher-priority live garbled-text defect.
- Impact: the paused route-pruning task stays isolated and does not block this production defect fix.

## Milestones

- [x] M1: Mark the previous backend task blocked and create this task package first.
- [x] M2: Record BDD scenarios and RED evidence for the garbled-text reproduction.
- [x] M3: Implement the minimal backend fix for Kingdee material text normalization.
- [x] M4: Repair the currently affected live ERP/MES master data for the reproduced work-order BOM rows.
- [x] M5: Run targeted verification, update evidence, and prepare a task-scoped commit.

## Expected Verification

- `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest,ErpKingdeeProductSyncServiceImplTest,ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `docker exec int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 -D ruoyi-vue-pro -N -e "SELECT bar_code, name, standard FROM erp_product WHERE bar_code IN ('A002.09.001.000021','A002.11.001.000012'); SELECT code, name, specification FROM mes_md_item WHERE code IN ('A002.09.001.000021','A002.11.001.000012'); SELECT id, name FROM erp_product_unit WHERE id = 57; SELECT id, code, name FROM mes_md_unit_measure WHERE id = 900055;"`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session workorder-bom-garbled-fix run-code --filename D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260518-workorder-erp-bom-garbled-item-fix\scripts\verify-workorder-bom-garbled-fix.mjs`

## Current Status

Completed for code delivery and live repair. The Kingdee material/BOM clients now normalize likely UTF-8-as-Latin-1 mojibake before downstream sync persists it, and the live master-data rows backing work order `903245` were repaired so both detail tabs render readable Chinese text again.

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-erp -Dtest=ErpKingdeeMaterialClientImplTest,ErpKingdeeProductSyncServiceImplTest,ErpKingdeeBomClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- PASS: targeted live SQL repair corrected `erp_product`, `mes_md_item`, `erp_product_unit`, and `mes_md_unit_measure` rows for `A002.09.001.000021` and `A002.11.001.000012`
- PASS: Playwright verification script `doc/tasks/20260518-workorder-erp-bom-garbled-item-fix/scripts/verify-workorder-bom-garbled-fix.mjs` opened work order `903245` and confirmed both `工单BOM` and `物料需求` tabs show `合格证（内贸INT）` / `中外标签（内贸INT）`, with the sensitive unit field restored to `张`

## Residual Risk

- Read-only inspection found many other historical `erp_product` and `mes_md_item` rows in the local runtime database that still look like mojibake from older sync runs. This task repaired the reproduced work-order path and blocked new corruption in the patched client path, but a broader backfill or full re-sync remains a separate follow-up if the user wants the entire local master-data set cleaned.

## Closeout

- PREVIEW/APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-workorder-erp-bom-garbled-item-fix --mode preview/apply`
- Result: kept `task.md` and `execution-log.md`; removed the one-off Playwright verification script and auxiliary bug evidence file after their results were captured in the task record.
