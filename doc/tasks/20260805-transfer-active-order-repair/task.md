# 调拨写入口与活跃订单边界修复

## Task Goal

修复岗位矩阵代码分析中可闭环的不符合项：禁止 MES 侧手工调拨写入口；班组长加入活跃订单必须基于已确认生产工单；活跃订单列表必须按当前班组长隔离。

## Milestones

- [x] 记录 BDD/TDD 场景与当前门禁
- [x] 补充 RED 回归：确认态校验、班组长范围隔离、调拨写接口禁用
- [x] 实施后端最小修复，不引入 fallback、默认成功或吞异常
- [x] 实施前端只读入口修复，移除调拨手工写按钮
- [x] 运行可执行验证并记录阻塞项

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesWmTransferManualWriteControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/mes-wm-transfer-readonly-static.spec.cjs`

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；后端写入口 fail fast，前端移除手工写入口。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

修复已写入并完成定向验证；前端静态契约通过，后端目标 JUnit 已在主工作区复跑通过：`MesTeamLeaderActiveOrderServiceTest`、`MesWmTransferManualWriteControllerTest`、`MesActiveOrderTransferTraceServiceTest` 合计 21 tests / 0 failures / 0 errors / 0 skipped。`task-closeout-cleanup` preview/apply 均通过且无删除项，任务专属验证 worktree 已删除。当前仅剩共享分支并行改动、提交和推送收尾未处理。

## Remaining Out Of Scope

- QA 规程发布写链路、PQC 检验任务自动生成、AC-M10 无订单 SOP 事实报工仍需独立任务修复。
