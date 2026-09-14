# Execution Log

## User Intent

- 用户反馈 `PQC组长 > PQC管理` 列表为空，要求查看是否存在历史数据。

## Rule Evidence

- 已读取 `docs/database-rules.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/local-runtime.md`、`docs/login-access.md`、`docs/server-access.md`。
- 已读取 `docs/experience-index.md` 中 PQC 管理空列表经验路由，以及 `docs/backend-development.md` 对应门禁。

## Historical Evidence

- `doc/tasks/20260807-pqc-leader-management-five-records/verification-report.md` 记录了 5 条正式测试提交：task `223..227`、event `181..185`、record `104..108`。
- `doc/tasks/20260808-restore-pqc-management-test-data/verification-report.md` 记录这些数据未删除，但页面默认按当天 `submitDate` 查询时会将历史日期数据筛掉。
- `doc/tasks/20260806-pqc-management-list-test-data/verification-report.md` 记录另有 event `160`、task `189`、record `103` 的 PQC 管理测试提交。

## Milestone Updates

- M1: completed。规则、经验门禁和历史数据标识已确认。
- M2: completed。本机 MySQL `ruoyi-vue-pro` 只读核查确认共有 100 条未删除 `PQC_INSPECTION` 历史事件，日期分布为 2026-08-03 至 2026-08-08；2026-08-09 为 0 条。
- M2: completed。已知 event `160`、`181..185`、task `189`、`223..227`、record `103..108` 均存在且未删除；对应 task 状态均为 `SUBMITTED`。
- M2: completed。`芋道源码/admin`（leader user `1`）启用 PQC 人员范围为 `1,659,1500,1606`，在该范围内有 82 条历史事件，最新提交时间为 `2026-08-08 13:36:59`，当天 2026-08-09 为 0 条。
- M2: completed。检验员 `659` 的 `pqc_permission` 角色有效；leader `1` 对检验员 `659` 的 scope `980046` 启用，旧 leader `512` 的 scope `980013` 已禁用。
- M2: completed。当前前端查询仍把 `submitDate` 默认设为当天，并在无可见筛选条件时继续向列表 API 传入该日期；PQC 管理不会像生产报工页一样自动回看最近有数据日期。
- M3: completed。已形成 `verification-report.md`；本次未修改业务数据、权限、人员范围、生产代码或运行环境。

## Verification Evidence

- `SELECT DATE(server_submit_time), COUNT(*) ... event_type='PQC_INSPECTION'` -> PASS，历史总量 100，2026-08-09 数量 0。
- 已知历史 ID 联表查询 -> PASS，event/task/record 均存在且 `deleted=0`。
- PQC 组长人员范围与角色只读查询 -> PASS，admin 范围和检验员角色有效。
- 前端源代码只读核对 -> PASS，`buildSubmissionParams()` 必传 `submitDate`，默认值为当天。
- 文档结构与 UTF-8 校验 -> PASS，三份核心文档均可按 UTF-8 读取且无替换字符。
- `task-closeout-cleanup --mode preview --worktree-closeout off` -> PASS，三份核心文档均为 keep，delete/blocked/warnings 均为 `<none>`。
- `task-closeout-cleanup --mode apply --worktree-closeout off` -> PASS，无删除项。

## Experience Consolidation

- 本次结论已被 `docs/backend-development.md#MES PQC组长人员范围与管理数据可见性门禁` 和 `docs/experience-index.md` 完整覆盖：No Data 先比较 `submitDate` 与事件 `server_submit_time`，并分别核对唯一启用人员范围和 `pqc_permission`。
- 按 `project-experience-consolidation` 技能判断，无新增通用经验需要修改长期文档，也不新建经验文档。

## Blockers

- None。
