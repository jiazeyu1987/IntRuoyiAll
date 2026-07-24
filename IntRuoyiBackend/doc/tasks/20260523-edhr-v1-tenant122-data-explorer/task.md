# 任务：租户 122 eDHR E2E 真实数据现状盘点

## Goal

只读检查本地数据库中测试租户 `122` 的 eDHR E2E 真实数据准备度，重点确认：

- 是否具备 `product/item`、`route`、`route_process`、`work_order`、`task`、`workstation` 基础数据
- 是否存在已绑定 `batchRecordReportId` 的 `route_process`
- 是否能拼出最小可复用数据链，或必须新建整套数据

## Scope

- `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-tenant122-data-explorer\**`
- 本地数据库只读查询
- 仓库内与数据库连接、MES 表结构、eDHR 执行链相关的只读代码检索

## Non-Scope

- 不修改数据库数据
- 不修改业务代码
- 不执行 E2E

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro\doc\tasks\20260523-edhr-v1-execution-m3-snapshot-backend\task.md`
- Status before this task: `Completed on 2026-05-23`
- Impact: 上一任务已完成，本任务可独立进行只读数据盘点。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro`
- Current state: 允许只读分析，不回滚、不修改并行改动
- Impact: 仅新增任务文档记录本次盘点结论与证据

## Milestones

- [x] M1: 创建任务文档并确认本地数据库连接来源
- [x] M2: 只读盘点租户 122 的基础主数据与关系数据
- [x] M3: 识别可复用最小数据链和缺口
- [x] M4: 回写结论、SQL 查询思路与验证证据

## Expected Verification

- 能给出租户 `122` 各层数据是否存在的只读结论
- 能给出 `route_process.batch_record_report_id` 绑定现状
- 能给出“最小可复用链”或“需整套新建”的判断依据

## Current Status

Completed on 2026-05-23. 已完成本地数据库只读盘点：租户 `122` 仅存在 `mes_pro_batch_record_report` 15 条报表数据，`item / route / route_product / route_process / work_order / task / workstation` 均为 0；不存在已绑定 `batchRecordReportId` 的租户 122 `route_process`，也不存在可直接复用的最小 eDHR 执行链。

## Final Verification Result

- 只读连接验证：`python + pymysql` 连接 `127.0.0.1:23306 / ruoyi-vue-pro` -> PASS
- 只读事实核对：
  - `system_tenant.id = 122` 存在，名称为“测试租户”，`system_users` 下有 4 个未删除用户
  - `mes_pro_batch_record_report` 在租户 `122` 下有 15 条，样本为 `EBR_TN122_A_T01` ~ `EBR_TN122_A_T15`
  - `mes_md_item / mes_pro_route / mes_pro_route_product / mes_pro_route_process / mes_pro_work_order / mes_pro_task / mes_md_workstation` 在租户 `122` 下均为 `0`
  - 租户 `122` `mes_pro_route_process` 总数为 `0`，因此已绑定 `batch_record_report_id` 的数量也为 `0`
  - 以 `work_order -> item -> route_product -> route -> route_process -> batch_record_report` 的弱链查询结果为 `0`
  - 以 `task -> work_order -> item -> route -> route_product -> route_process -> workstation -> batch_record_report` 的全链查询结果为 `0`
- 交叉参考：
  - 当前库中上述 MES 核心表的真实业务数据仅存在于 `tenant_id = 1`
  - `tenant_id = 1` 仅有 `1` 条已绑定 `batch_record_report_id` 的 `route_process`，但同库仍查不到可跑通的弱链或全链
- Closeout 预览：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi-worktrees\20260523-edhr-v1-execution\ruoyi-vue-pro --task-id 20260523-edhr-v1-tenant122-data-explorer --mode preview` -> BLOCKED，linked worktree 无法对 `int_main` 做 ff-only 预览，且主工作树与当前 worktree 存在并行未提交改动。
