# 岗位矩阵代码修复

## Task Goal

修复岗位需求分解矩阵代码分析中已确认且可闭环的 MES 不符合项：禁止 MES 侧手工调拨写入口，收紧班组长活跃订单必须来自已确认生产工单并按当前班组长隔离查询。

## Milestones

- [ ] 记录 BDD/TDD 场景与当前门禁
- [ ] 为活跃订单确认态校验、班组长范围隔离、调拨写入口禁用补 RED 回归
- [ ] 实施最小代码修复，不引入 fallback 或默认成功
- [ ] 运行目标后端回归与必要静态验证
- [ ] 更新验证报告与剩余未修复项

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesWmTransferControllerTest,MesWmTransferLineControllerTest,MesWmTransferDetailControllerTest" test`
- 若 Maven 因既有 `target_corrupt_m4_20260802_1327` 或 Windows target 删除异常阻塞，记录精确 blocker，不用静态扫描冒充 JUnit 通过。

## Applicable Gates

- Backend BDD/TDD：先记录 BDD，再执行 RED/GREEN。
- Strict no-fallback：缺少正式来源或权限边界时 fail fast，不返回默认成功。
- PowerShell Maven `-D` 参数：所有 `-D...` 参数整体加双引号。
- Maven target 异常：不得叠加 Maven 命令或把环境失败写成业务通过。
- 前端静态契约隔离：如触碰前端，只做当前行为专用静态验证，不用无关全量 blocker 替代。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；通过服务/控制器边界阻断不符合入口。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

## Remaining Out Of Scope

- QA 规程发布写链路、PQC 检验任务自动生成、AC-M10 无订单 SOP 事实报工仍为较大业务链路，需单独任务补正式数据链路与 E2E。
