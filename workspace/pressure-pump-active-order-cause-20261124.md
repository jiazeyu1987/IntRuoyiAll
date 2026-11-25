# 测试服58 / 测试Z：加入活跃订单异常（只读调查）

- 账号：zhangfeihong，用户ID911750，租户170，账号启用。
- 订单：881MO083056，测试Z工单ID1188139。
- 日志：2026-11-24 00:37:52 请求 `/admin-api/mes/pro/process-pool/team-leader/active-order/add`，参数 workOrderId=1188139。
- 实际异常：写入 `mes_pqc_inspection_task` 被数据库约束 `chk_mes_pqc_inspection_rule_key` 拒绝（MySQL3819）。
- 调用链：新增活跃订单按钮 → MesProcessPoolTeamLeaderController.addActiveOrder → MesTeamLeaderActiveOrderServiceImpl.addActiveOrder → insertPqcInspectionTasks → insertPqcInspectionTask → Mapper.insert。
- 当前程序 MesPqcInspectionRulePlan 的规则为 FIRST、PATROL、FINAL；创建活跃订单时生成首检和巡检任务。
- 现场数据库 CHECK 仅允许 FIRST、PATROL_AM、PATROL_PM、FINAL，不允许 PATROL。程序和数据库规则不一致。
- 当前仓库动态巡检迁移 `20261104_mes_pqc_dynamic_patrol_rounds.sql` 仅增加 pqc_qa_source_version_ids_json 列，没有更新上述 CHECK。
- 失败状态核验：该工单有效活跃订单0条；PQC任务0条。addActiveOrder 为回滚事务，现场结果与整体回滚一致。
- 本次只读调查，没有修改数据库、部署或重启服务，没有代替用户加入活跃订单。
- 此前订单/单据复制核验不包含加入活跃订单的业务验收。
