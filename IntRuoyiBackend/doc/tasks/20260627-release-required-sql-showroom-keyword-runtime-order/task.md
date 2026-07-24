# 任务：修复发布链路 showroom keyword runtime SQL 依赖顺序失真

## 任务目标

- 修复测试服真实发布在 `20260626_showroom_keyword_bu_seed_runtime.sql` 阶段因 `showroom_keyword` 表不存在而失败的问题。
- 收敛并修复 `deploy-release` 阶段 required SQL 应用顺序与 `dependsOn` 契约不一致的根因，确保 runtime-scanned showroom keyword schema/seed SQL 能按依赖顺序执行。
- 保持发布 fail-fast 契约，不引入 fallback、静默跳过、人工补库或手工改服绕过。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260627-release-required-sql-dcc-fvm-retain-other-baseline\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已完成 DCC FVM retain-other completion 测试环境收敛；本任务继续处理真实测试服发布推进到 showroom keyword runtime SQL 后暴露出的新 blocker。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`
- 适用强制门禁：
  - 服务器状态核对、发布排障前必须按 `server-access.md` 使用既定目标主机与访问方式，不得把测试服问题误判为本机构建或其他环境问题。
  - 发布代码入口与发布链路修改统一落在 `D:\ProjectPackage\Int\IntRuoyiMaintance`；`D:\ProjectPackage\Int\IntRuoyi` 仅作为业务源码、SQL 与发布输入，必须避免在错误仓位做临时发布绕过。
  - 发布、构建、required SQL、schema preflight 失败必须 fail fast；缺少 manifest、依赖顺序证明或前置条件时不得继续测试服/正式服/备份服发布。
  - 高风险真实发布动作前，`execution-log.md` 必须记录 `GREEN: experience-preflight -> PASS`，否则不得继续执行测试服/正式服/备份服写入动作。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。当前根因收敛为 `deploy-release` 执行 required SQL 时未保持 preflight 依赖顺序，额外排序把 `20260626_showroom_keyword_bu_seed_runtime` 提前到了 `20260626_showroom_keyword_schema_seed_runtime` 之前。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: required SQL 执行顺序必须服从 dependsOn 契约 -> Given release package 同时包含 showroom keyword schema runtime SQL 与 BU seed runtime SQL 且 BU seed dependsOn schema / When deploy-release 读取 preflight-plan 并执行 APPLY 项 / Then schema runtime SQL 必须先于 BU seed runtime SQL 执行，不能再被额外排序重排到后面。`
- `BDD: 测试服真实 blocker 必须以发布链路正式修复收敛 -> Given 测试服 v9 发布日志显示 2026-06-27 真实执行时先跑了 20260626_showroom_keyword_bu_seed_runtime.sql 并报 Table 'ruoyi-vue-pro.showroom_keyword' doesn't exist / When 我们修复 deploy-release required SQL 排序逻辑 / Then 新发布包在测试服应先创建 showroom_keyword 表后再执行 BU seed，而不是依赖人工补表或改库绕过。`

## 里程碑

1. M1：记录真实测试服失败证据与根因收敛。`COMPLETED`
2. M2：补充发布脚本/预检链路 RED 契约测试。`COMPLETED`
3. M3：完成最小正式修复并跑通定向回归。`COMPLETED`
4. M4：更新证据、提交后端修复并重新进入发布链路。`COMPLETED`

## 预期验证

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q`
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_release_preflight_plan.py -q`
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql`

## 最终验证结果

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "preserves_preflight_dependency_order_for_non_priority_required_sql or executes_dcc_view_matrix_test_tenant_prereq_before_seed_on_test" -q` -> PASS（`2 passed`）
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS（`94 passed`）
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_release_preflight_plan.py -q` -> PASS（`11 passed`）
- `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` -> PASS（`status=passed`, `migrationCount=218`）
- 修复结果：`Sort-RequiredDatabaseSqlApplyItems` 在测试环境仅保留 DCC prerequisite 的显式优先级，其余 required SQL 保持 `preflight-plan` 产出的依赖顺序，不再把 `20260626_showroom_keyword_bu_seed_runtime` 排到 `20260626_showroom_keyword_schema_seed_runtime` 前面。
