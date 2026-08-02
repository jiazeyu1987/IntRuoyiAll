# Verification Report

## Summary

本轮先按用户要求完成静态分析与本地代码修复确认；用户后续授权后，已在测试服完成 `ROUTE-XLSX-00002` 路线工序工作站绑定，并通过真实后端接口执行重排。不发布代码到测试服。

## Results

- 复现：静态链路确认测试服问题数据属于“路线工序未绑定工作站 + 排产工序旧班时快照”的组合，原逻辑会沿用旧 `shift_hours=10.5`。
- 修复：`MesProAutoScheduleServiceImpl` 已改为无工作站手工/小时产能工序读取工作台统一班时，缺失或不统一时失败，不再静默沿用旧快照。
- 回归：已新增/调整 `MesProAutoScheduleServiceImplTest` 用例覆盖 7 小时刷新旧 10.5 小时快照、缺工作台班时失败。
- 静态检查：`git diff --check` 对本任务修改文件通过；调用链检查确认刷新后的 `shiftHours` / `shiftCapacityTotal` 会进入快照落库和持续时间/日产能计算。
- 数据绑定：测试服芋道源码租户 `tenant_id=1` 的 `route_id=900026` 已备份 26 条路线工序，并将 26 条路线工序全部绑定到唯一匹配的启用工作站；复核 `null_workstation_count=0`、`distinct_workstation_count=26`。
- 重排应用：真实接口预览后携带 `calendarContextToken` 应用重排，`applied=true`、生成任务 640 个、删除旧任务 472 个、保留任务 4 个、阻塞问题 0 个、缺料提示 164 个。
- 工单时间：目标排产工单 `127,128,129,130,131,136` 的计划开始/完成时间均已更新；例如工单 127 更新为 `2026-08-04 09:31:00` 至 `2026-09-29 08:41:00`。
- 资源落库：通过 `mes_pro_task_schedule_ext -> mes_pro_task` 复核 6 个目标排产工单任务全部绑定有效工作站，`null_task_ws_count=0`、`invalid_task_ws_count=0`，每个工单覆盖 26 个工作站；最近重排快照 `mes_pro_replan_explanation_snapshot.id=12`。

## Remaining Risks

- 未发布代码到测试服，测试服仍是当前已部署版本。
- 当前测试服目标路线工作站班时全部为 `10.50`，本次重排证明绑定资源链路和重排应用成功，但未证明 7 小时 / 14 小时完成时间差异；若要验证差异，需要先把目标工作站班时改为 7 或 14 后再重排对照。
- 历史 `mes_pro_schedule_order_process.workstation_id` 快照仍存在空/旧工作站字段，当前测试服代码未回写该快照表；实际任务资源以 `mes_pro_task_schedule_ext -> mes_pro_task.workstation_id` 验证为准。
- 本地 Maven 目标 JUnit 因 Windows 构建卡住未取得明确 PASS；后续发布前必须先跑出目标测试 PASS。
- 当前仓库存在大量非本任务脏改和并发 Maven 进程，后续提交/发布前需先隔离任务改动并确认构建环境干净。
