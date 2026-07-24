# 任务：发布后三角色 E2E 门禁后端收口归档

## 任务目标

对 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 中仍未挂到独立任务包的“发布后三角色真实 E2E 门禁”后端改动做正式归档与收口，覆盖以下余量：

1. 发布迁移与 SQL 契约：角色账号/密码、smoke 账号密码新鲜度、ERP 手动触发权限、A03388 设备工序产能冲突、路线 `900026` 产线绑定、排产工序快照缺列等正式发布 SQL。
2. 发布契约与脚本：showroom runtime schema 的 release metadata、required SQL 打包、scheduler smoke compose/.env/wrapper 变量透传与 route-ready 默认值。
3. 排产接口与控制器契约：`/mes/pro/auto-schedule/dependencies` 支持 POST body，避免真实长工单范围依赖线查询命中 414。

本任务用于承接上述“未单独成包但必须随最终后端提交一起提交”的余量改动；已完成的四个子任务仍分别保留其独立任务记录：

- `20260619-post-release-role-e2e-gate-route-900026-line-fix`
- `20260619-post-release-role-e2e-gate-autocode-counter-recovery`
- `20260619-post-release-role-e2e-gate-edhr-batch-trigger-gate`
- `20260619-post-release-role-e2e-gate-feedback-approver-identity`

## 上一任务检查

- 已核对上述四个发布门禁后端子任务目录当前状态均为 `COMPLETED`。
- 当前剩余工作不是继续修复新缺陷，而是为这些已完成链路补齐后端“总收口任务”归档，承接仍未挂任务的发布契约、SQL 与接口改动，便于本仓做选择性提交。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\release-backup-restore.md`：所有测试服/正式服发布链路问题都必须沉淀为正式 release SQL、required SQL 或发布脚本契约，不能靠手工修补留在环境里。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`：测试服目标环境固定为 `172.30.30.58`，最终是否闭环以远端运行态 `.env`、镜像 tag 和真实 smoke/e2e 证据为准。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：涉及 `芋道源码` 租户真实账号验收时，失败必须记录真实租户、账号、入口和失败位置；最终是否通过以真实浏览器路径与真实 smoke 结果为准。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。所有修复都通过正式 SQL、发布契约、接口契约和后端 fail-fast 逻辑完成；未增加 silent fallback。
- `是否从根因和长期维护角度解决`：是。把测试服暴露的问题落到正式发布迁移、发布脚本契约、控制器契约与回归测试，不依赖手工环境修补。
- `是否存在临时补丁或绕过`：否。没有通过 mock、隐藏按钮、跳过审批或手工改运行结果伪装通过。

## BDD 场景

- `BDD: 发布后角色账号与 smoke 运行契约 -> Given 测试服每次发布后都要用芋道源码租户真实账号做三角色验收 / When 发布链执行数据库迁移并写出运行时 .env / Then 角色账号、密码、smoke 账号密码新鲜度与 ERP 手动触发权限必须由正式 SQL 和发布契约保证，而不是依赖环境残留。`
- `BDD: 展厅运行态 schema 发布契约 -> Given 展厅代码已经依赖 showroom 产品附件表与大厅背景字段 / When 构建并部署发布包 / Then required SQL 必须包含对应 showroom schema，后端启动前 preflight 缺表或缺字段必须 fail fast。`
- `BDD: 智能排产依赖查询长范围 POST 契约 -> Given 生产排产页会加载大量工单依赖线 / When 查询当前甘特图依赖线 / Then 后端必须支持 POST request body，避免真实路径因 URL 过长命中 414。`
- `BDD: 排产工序快照 schema 契约 -> Given 智能排产 smoke 会把工单工序写入 mes_pro_schedule_order_process 快照表 / When 后端持久化 process_code 与 process_name / Then 发布迁移必须先补齐列并完成历史回填。`
- `BDD: A03388 同设备同工序唯一产能契约 -> Given 芋道源码租户智能排产 smoke 会读取 A03388 的设备工序产能 / When 同设备同工序存在重复高产能记录 / Then 发布迁移必须逻辑删除冲突高产能，只保留保守容量，避免自动排产预览 500。`
- `BDD: 最终测试服发布闭环 -> Given 后端 release SQL、发布契约与调度服务修复全部完成 / When 维护仓重新构建发布并运行三角色真实 E2E / Then 最终测试服 evidence 必须显示 gaomin、zhaojie、wangsiyu 全部通过，且智能排产 smoke 全链路 PASS。`

## 里程碑

1. M1：盘点已完成子任务与仍未挂任务的后端 release gate 改动。`DONE`
2. M2：补齐 release SQL / publish contract / controller POST body / schedule precondition 的 BDD 与 RED/GREEN 证据。`DONE`
3. M3：用维护仓最终 PASS evidence 复核测试服闭环，并把剩余改动纳入后端总收口任务。`DONE`
4. M4：完成后端收口归档任务文档并准备选择性提交。`DONE`

## 预期验证

- `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py script\tests\test_scheduler_smoke_release_contract.py script\tests\test_showroom_release_sql_contract.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_mes_scheduling_closed_loop_sql.py -q`
- `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql`
- `mvn -pl yudao-module-mes "-Dtest=MesMdAutoCodeSerialNumberPartStrategyTest,MesProFeedbackImportRecordServiceImplTest,MesProAutoScheduleControllerContractTest,MesProAutoScheduleServiceImplTest,MesProAutoScheduleAlgorithmContractTest,MesProAutoScheduleContractTest" test`
- 维护仓最终 PASS 证据：
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\runtime-console-build-deploy-1781882335880.json`
  - `D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260618-post-release-role-e2e-gate\evidence\post-release-role-e2e-1781882555897.json`

## 当前状态

COMPLETED：本仓此前未成包的 release SQL、showroom required SQL、scheduler smoke 发布契约、`/mes/pro/auto-schedule/dependencies` POST body 契约以及排产工序快照/A03388 产能冲突门禁，均已通过正式代码与测试固化，并由维护仓最终发布 `release-20260619-2230-role-e2e-gate-feedback-approver-wrapper` 在测试服 `172.30.30.58` 完成真实闭环验证。

## 最终验证

- `script/tests/test_post_release_role_e2e_gate_sql.py`、`test_scheduler_smoke_release_contract.py`、`test_showroom_release_sql_contract.py`、`test_publish_int_ruoyi_to_test_tooling.py`、`test_mes_scheduling_closed_loop_sql.py` 覆盖本任务涉及的 SQL、发布脚本与排产 schema 契约。
- `MesProAutoScheduleControllerContractTest` 覆盖 `/mes/pro/auto-schedule/dependencies` 的 POST body 合同；`MesMdAutoCodeSerialNumberPartStrategyTest`、`MesProFeedbackImportRecordServiceImplTest`、`MesProAutoScheduleServiceImplTest`、`MesProAutoScheduleAlgorithmContractTest`、`MesProAutoScheduleContractTest` 覆盖本次门禁链路相关后端回归。
- 维护仓最终 PASS evidence 已证明以上改动成功随发布进入测试服，且三角色真实 E2E / 智能排产 smoke 全部通过。
