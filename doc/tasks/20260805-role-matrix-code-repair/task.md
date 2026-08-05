# AC-M22 批记录完整性预检修复

## Task Goal

修复 `AC-M22 | 检查批记录完整性` 的代码不符合项：放行预检必须按正式来源证明批记录、PQC、调拨/库存、签名和阻塞异常完整；缺任一正式来源或状态不闭环时不得放行。

## Milestones

- [x] 记录 AC-M22 BDD/TDD 场景与当前门禁
- [x] 补充回归：PQC 聚合后确认、调拨缺必备来源、调拨数量/状态无效、activeOrder 路线版本不匹配
- [x] 实施最小后端修复，不引入 fallback、默认成功或吞异常
- [x] 运行目标后端回归与相邻回归
- [x] 更新验证报告、剩余风险和当前阻塞状态

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 若目标 Maven 因既有 Windows `target` 文件系统异常阻塞，记录精确 blocker，不用静态扫描冒充 JUnit 通过。

## Experience Gates

- `docs/backend-development.md#edhr-批次任务配置来源门禁`：正式批记录表单不得用 `formBindings`、默认 `MAIN` 或工序开始配置替代。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 事实必须来自发布规程、结构化逐件明细和正式提交/复核链路。
- `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：PowerShell 下 Maven `-D...` 参数必须整体加双引号。
- `docs/powershell-memory.md#maven-目标目录文件系统异常门禁`：遇到 `target/classes` 损坏或删除卡住时 fail fast，不叠加 Maven 命令。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在正式来源适配器和放行预检入口收紧完整性判断。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

AC-M22 后端代码修复、目标 JUnit、静态校验、cleanup preview/apply 和经验沉淀均已完成。目标 Maven 命令 `mvn -pl yudao-module-mes -am "-Dtest=MesOrderReleaseCompletenessServiceTest,MesPqcProcessInspectionAggregationServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过，Surefire 合计 41 tests / 0 failures / 0 errors。实现与验证记录已被共享分支提交 `ba81bdfe3` 吸收；本次仅补最终 completed 收尾记录。

## Remaining Out Of Scope

- 真实页面全量 `ACCEPTED` 仍需 M6 E2E 逐 AC 覆盖，本任务只修复后端可单元闭环的不符合项。

## Cleanup Keep

- doc/tasks/20260805-role-matrix-code-repair/bug-regression-evidence.md
