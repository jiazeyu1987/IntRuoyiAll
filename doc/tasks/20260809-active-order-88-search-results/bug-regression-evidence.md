# Bug Regression Evidence

## Bug Summary And Expected Behavior

生产组长在“新增活跃订单”弹窗输入宽关键词 `88` 时，符合正式路线、DCC 项目和已发布 QA 条件的目标工单未显示。期望宽关键词搜索在候选上限内优先返回符合资格的匹配工单，不能在资格判定前被其它匹配项挤出。

## Reproduction

- 运行态：本地前端 `http://127.0.0.1:8081`、后端 `http://127.0.0.1:48081`，后端健康状态为 `UP`。
- 真实路径：登录后进入“生产组长工作台”，点击“新增活跃订单”，在“订单号/产品”输入 `88`。
- 实际结果：下拉返回 20 条候选，全部显示“已取消”；`881MO090935`、`881MO090972`、`881MO090973`、`881MO090974` 均未出现。
- 数据证据：租户 `1` 中 `88` 共匹配 1053 条工单，其中 810 条已确认；目标工单在 mapper 的 ID 倒序前 20 之外。

## Root Cause

- `MesProWorkOrderMapper.selectCandidatesByKeyword` 在正式资格解析前执行 `ORDER BY id DESC LIMIT 20`。
- `MesTeamLeaderActiveOrderServiceImpl.searchActiveOrderCandidates` 在 mapper 返回之后才构建路线/DCC/QA 上下文并按 `eligible` 排序，因此被截断的目标工单永远不会进入资格判断。
- 仅将已确认状态提前排序不足以解决：真实数据已有 810 条已确认匹配项，仍会在正式资格判断前错误截断。

## Verification

- 定向回归：2 tests，0 failures，0 errors。
- 相邻回归：46 tests，0 failures，0 errors。
- 完整后端包：30/30 reactor modules `SUCCESS`。
- 最终 JAR 字节码：mapper 不再资格前截断，service 在资格排序后执行 `limit(20)`。
- Playwright 真实页面：PASS；正式迁移补齐数据库列后，在包含本次修复的稳定运行态输入 `88`，四个目标订单均显示“符合要求”；精确搜索已取消工单仍显示“生产工单已取消”。

## RED:

- `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProWorkOrderMapperTest#testSelectCandidatesByKeyword_doesNotTruncateBeforeEligibilityEvaluation,MesTeamLeaderActiveOrderServiceTest#shouldApplyCandidateLimitAfterEligibilityEvaluationForBroadKeyword" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL。
- mapper 回归失败：`expected: <24> but was: <20>`。
- service 回归失败：`expected: <20> but was: <24>`。

## GREEN:

- RED 同命令重跑 -> PASS：mapper 与 service 共 2 tests，0 failures，0 errors。
- 相邻回归 -> PASS：`MesTeamLeaderActiveOrderServiceTest`、`MesTeamLeaderActiveOrderErpPlannedStartTest`、`MesProcessPoolTeamLeaderControllerTest` 共 46 tests，0 failures，0 errors。

## Risk And Regression Scope

- 影响生产组长活跃订单候选搜索的排序和数量边界；不得改变路线/DCC/QA 资格规则或新增写入幂等性。

## Blockers And Follow-up Actions

- 同一后端目录有 3 个非本任务 Maven 进程并发编译，且一个进程正在验证同一个 `MesTeamLeaderActiveOrderServiceTest`；共享 `target` 使当前 GREEN 结果不可信。
- 外部构建结束后必须重新执行两条 RED 对应测试、相邻 MES 回归、更新 `48081` 运行 jar，并通过真实页面输入 `88` 验证四个目标工单。
- 外部构建已结束，定向 GREEN 与相邻回归已完成；剩余运行包和真实页面验证。
- 运行包构建和字节码核验已完成；真实页面验证的唯一当前前置是按正式迁移 `IntRuoyiBackend/sql/mysql/20260809_mes_qa_inspection_item_display_fields.sql` 补齐本机共享数据库列 `inspection_tool`、`sampling_plan_text`。必须先取得共享数据库变更授权，再应用迁移并重跑 Playwright；不得以 fallback 绕过。
- 用户已授权迁移，且并行 QA 任务已按同一正式脚本成功补齐并复核两个字段；数据库前置解除。当前仅等待该任务释放共享 `48081`，随后继续本任务运行包与真实页面验证。
- 本任务再次只读复核 schema 为 GREEN；但共享 `48081` 仍由并行 QA 任务的专用 JAR 占用，且该 JAR 经反编译确认不含本次候选截断修复。真实页面复验因此阻塞，不能强停其它任务或用当前旧实现冒充通过。
- 最终阻塞解除：共享运行包经反编译确认已包含本次修复，健康状态 `UP`。Playwright 独立干净会话完成宽关键词和取消工单契约复验，控制台 0 错误；未执行任何加入或业务写入操作。
