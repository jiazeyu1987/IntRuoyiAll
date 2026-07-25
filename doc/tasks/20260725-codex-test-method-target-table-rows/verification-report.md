# Verification Report

## Result

- Status: ready_for_closeout, verification passed and waiting for cleanup plus final commit/push.
- User-visible outcome: 测试方法项和测试目标项均按表格行展开，黄色范围两条核验描述归入测试目标项。
- Backend outcome: 检查点重复替换改为物理删除旧行后重建，避免软删除唯一键冲突。

## Commands

- PASS: `node tests/e2e/system-codex-test-management-static.spec.js`
- PASS: `node --check tests/e2e/system-codex-test-management-real.e2e.js`
- PASS: `node --check ..\doc\tasks\20260725-test-management-manual-replan-881mo\test-management-manual-replan-full.e2e.cjs`
- PASS: `mvn -pl yudao-module-system -am -Dtest=CodexTestCaseServiceImplTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `pnpm ts:check`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260725-codex-test-method-target-table-rows/frontend-feature-evidence.md`

## Data And Runtime Notes

- Local sample case id `1` in `int-ruoyi-mysql` was corrected once; affected rows = `1`。
- Corrected method text now contains only the two ordered operation steps.
- Existing checkpoints retain four target rows: 重排成功、仅目标工单产品编号变橙色、最近一次成功排产时间更新、生产排产甘特图仅包含目标工单。
- The workspace still contains many non-task dirty files from concurrent work; they are excluded from this task's staging boundary.

## Experience Consolidation

- Updated existing long-term rule: `docs/backend-development.md#2026-07-25-子表集合替换软删除唯一键门禁`。
- Updated route index: `docs/experience-index.md` keyword route for 子表集合替换 / 软删除唯一键 / `deleteByCaseId`。
