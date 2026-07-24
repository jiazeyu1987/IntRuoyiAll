# Task: DCC 文件类别要求分发和要求培训全量开启

## Goal

修复 DCC 文件类别真实 `distributionRequired`、`trainingRequired` 长期为关闭而前端列表固定显示“必须”的不一致问题，并把当前所有文件类别统一更新为要求分发、要求培训均开启，避免后续导入或编辑再次出现同类偏差。

## Scope

- 先确认并显式暂停上一个同仓后端任务，再创建本任务包。
- 严格按 BDD + TDD 先补失败回归，再做最小后端修复。
- 为当前所有 DCC 文件类别增加真实布尔值全量开启的后端修复路径。
- 修正 IntAuth 导入或后续创建默认值，避免新类别再次落成 `false/false`。
- 保持现有 DCC 规则接口、审批矩阵接口和受控文件发布链路不变。
- 不引入 fallback、mock 或静默降级逻辑。

## Previous Task Check

- Previous backend task: `doc/tasks/20260518-schedule-calendar-shortage-risk-daily-material-summary/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused schedule-calendar backend task remains isolated and does not block this DCC category requirement-alignment slice.

## Milestones

- [x] M1: Block the previous same-repository backend task and create this task package first.
- [x] M2: Record BDD scenarios and add RED verification for the category requirement mismatch and bulk-enable behavior.
- [x] M3: Implement the minimal backend data-alignment and default-value fix.
- [x] M4: Run targeted backend verification and update evidence.
- [x] M5: Preview closeout artifacts and prepare the task-scoped backend commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccCategoryTrainingRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260518-dcc-category-requirements-enable-all\bug-regression-evidence.md`

## Current Status

Completed. Backend default-value repair, regression tests, evidence validation, and closeout preview are complete.

## Final Verification Result

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccCategoryTrainingRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260518-dcc-category-requirements-enable-all\bug-regression-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260518-dcc-category-requirements-enable-all --mode preview` -> PASS

## Blocker And Impact

- None.
