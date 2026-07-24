# 任务：刷新当前 DR readiness 文档

## 任务目标

根据根任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260604-dr-recovery-rollback-gap-audit` 的 P1 口径漂移缺口，新增当前 DR readiness 汇总文档，明确已完成的代码门禁、已验证证据、仍阻塞的真实恢复/回滚/接管动作、RTO/RPO 与 owner 要求，避免继续用 2026-05-24 或局部任务证据判断当前系统是否 ready。

## Previous Task Check

- 上一个同服务仓库任务：`doc/tasks/20260605-runtime-control-owner-matrix-alert-routing/task.md`
- 状态：`completed`
- 处理：上一任务已完成并提交 `be63394046`；本任务只新增当前 DR readiness 文档和文档合同测试，不操作服务器、不执行真实恢复/回滚。

## BDD 场景

- BDD: 当前 DR 文档收敛证据口径 -> Given 2026-05-24、2026-06-03、2026-06-04、2026-06-05 证据同时存在 / When reviewer 查看当前 DR readiness / Then 文档必须列出最新状态、已完成门禁和仍阻塞项，不得把旧证据当作 ready。
- BDD: 当前 DR 文档保留阻塞门禁 -> Given 真实 rollback-app、备份服接管和 with-data DCC 读回尚未执行 / When reviewer 查看 readiness / Then 文档必须保持 BLOCKED，并写明授权/验证缺口。
- BDD: 当前 DR 文档覆盖恢复合同 -> Given DR readiness 技能要求 recovery scope、RTO/RPO、retention、restore procedure、owners 和 external dependencies / When 文档合同测试运行 / Then 必须包含这些章节和关键证据引用。

## Milestones

- [x] M1：确认上一任务 completed，并确认后端仓库缺少当前 DR readiness 汇总文档。
- [x] M2：新增 RED 文档合同测试。
- [x] M3：新增 `docs/recovery/current-dr-readiness.md`。
- [x] M4：运行文档合同测试、DR validator 和 diff check。
- [x] M5：cleanup 预览并提交本任务改动。

## Expected Verification

- `python -m pytest script/tests/test_current_dr_readiness_doc.py -q`
- `python C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence docs/recovery/current-dr-readiness.md`
- `git diff --check -- docs/recovery/current-dr-readiness.md script/tests/test_current_dr_readiness_doc.py doc/tasks/20260605-current-dr-readiness-doc-refresh`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。文档明确保持 DR readiness 为 `BLOCKED`，不以旧证据或局部代码门禁替代真实恢复/回滚验证。
- `是否从根因和长期维护角度解决`：是。新增当前状态汇总页，并用合同测试保护关键章节与最新 blocker。
- `是否存在临时补丁或绕过`：否。不操作服务器、不伪造恢复/接管证据。

## 当前状态

completed

## Cleanup Keep

- `doc/tasks/20260605-current-dr-readiness-doc-refresh/task.md`
- `doc/tasks/20260605-current-dr-readiness-doc-refresh/execution-log.md`
