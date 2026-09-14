# Verification Report：一线提交到生产组长显示链路

## Result

PASS。历史生产人员组长范围缺口已通过正式幂等 migration 补齐；现有事件 `192` 和报工 `877` 未重复创建。生产组长真实页面已显示陈丽、粗洗工序、完成数量 123 件，未复核事件仍不进入报工历史。

## Root Cause And Fix

- 根因：历史人员档案没有同步到权威 `PRODUCTION/EMPLOYEE` 组长范围，正式读链路按范围过滤了正确的提交事件。
- 修复：新增 `20260809_mes_team_leader_employee_scope_backfill.sql`，按运行时一致的 `COALESCE(system_user_id, profile.id)` 身份补齐缺失范围。
- 安全边界：迁移事务内 fail fast；不更新/删除已有范围，不增加管理员全量、读时推断、自愈或默认组长 fallback。

## Verification Commands

- `python -X utf8 -m pytest script/tests/test_mes_team_leader_employee_scope_backfill_sql.py -q` -> PASS，3 passed。
- `run_migration_policy_gate("sql/mysql")` -> PASS，454 migrations。
- migration 首次/二次本机执行 -> PASS，标记行数 `14 -> 14`；missing `0`、mismatch `0`、duplicate groups `0`。
- `mvn -pl yudao-module-mes "-Dtest=MesTeamLeaderScopeServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderWorkbenchServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，24 tests，0 failures/errors。
- `node --check tests/e2e/production-leader-report-visibility-real.e2e.js` -> PASS。
- `node tests/e2e/production-leader-report-visibility-real.e2e.js` -> PASS。

## Real E2E Evidence

- Runtime：int_main 前端 `8081`、后端 `48081`，后端 health `UP`，进程归属 `E:\IntRuoyi`。
- 页面：`/mes/pro/process-pool/production-leader` > `报工管理`。
- 管理列表：HTTP `200`、business code `0`、`total=1`、event `192`、feedback `877`；可见行显示 `2026-08-09 10:30:34 / 陈丽 / 粗洗工序 / 123 件`。
- 报工历史：HTTP `200`、business code `0`、`total=0`、event `192` 不存在。
- 最终运行：MES 写请求 `0`，目标请求失败 `0`，本机非目标失败 `0`，pageerror `0`，console error `0`。
- 截图已人工检查清晰可读并列入任务清理，不作为长期敏感产物保留。

## Evidence Validators

- Bug regression validator：PASS，`Bug regression evidence is valid.`
- Database schema validator：PASS，`Database schema evidence is valid.`
- 两份临时 evidence 的 RED/GREEN 和关键结论已归档到本报告及 `execution-log.md`，可按 closeout 规则清理。

## Scope And Residual Risk

- 本次只修改本机数据库和仓库 migration/test/evidence；未执行远端发布、Git staging、commit 或 push。
- 迁移包含 `test,backup,prod` metadata，但其它环境仍需各自按正式发布流程应用；本次不宣称远端已修复。
- 事件尚未复核，报工历史为空符合当前业务状态。

## Blockers

无。

## Closeout

- Cleanup preview：PASS，blocker `0`、warning `0`。
- Cleanup apply：PASS，仅删除本任务两份临时 evidence 和 Playwright 临时输出；保留三份任务记录。
- 可复用经验已合并到既有 `docs/backend-development.md`，未新建长期经验文档。
- 未执行 Git staging、commit、merge 或 push。
