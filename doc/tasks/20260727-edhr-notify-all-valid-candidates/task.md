# 任务：eDHR 工作任务通知全部有效候选人及 MES 完整回归全绿

## 任务目标

1. 将 eDHR 批次执行工作任务的站内信收件人规则统一为：同一工作任务通知其候选快照中的全部有效候选账号；同一任务内账号去重；不把不同业务任务的候选人混成一个任务。
2. 按用户 2026-07-27 明确批准的范围变更，修复 `mvn -pl yudao-module-mes test` 暴露的全部 failure/error，直到完整 MES 模块回归 `BUILD SUCCESS`。

## 里程碑

1. 现状与影响范围确认
2. BDD 场景和回归测试先行，形成 RED
3. 后端通知收件人实现
4. 定向测试、模块回归和证据校验
5. 完整 MES 回归失败簇规划与前置条件治理
6. 按失败簇修复 schema/fixture/装配/契约/业务回归
7. 完整模块独立复验
8. 任务文档收尾、提交并推送

## 预期验证

- 候选快照包含多个有效账号时，每个账号收到一次站内信。
- 填写任务和审核任务均按自身候选快照通知，不使用单一负责人覆盖候选人。
- 候选快照中的重复账号不会重复通知。
- 现有模板编码、通知参数和任务创建链路保持不变。
- 定向 Maven 测试、MES 模块回归测试和任务证据脚本通过。

## Current Status

ready_for_closeout

原通知行为、标准定向 GREEN、同类服务测试、生产代码编译、T4/T5/T6 目标失败簇修复与相邻回归均已通过。2026-07-28 用户明确确认 Sheet1 Excel 真实样本覆盖“不需要覆盖这个”，因此该缺失 fixture 不再作为验收前置；已删除依赖缺失真实 Excel 的导入/DB 回滚测试入口，保留 `Sheet1RouteExcelParserTest` 中 4 个合成 fail-fast/契约测试。

最终验收已完成：2026-07-28 08:53:25 +08:00 在 `IntRuoyiBackend` 执行 `mvn -pl yudao-module-mes test`，结果 `BUILD SUCCESS`，2530 tests、0 failures、0 errors、18 skipped。任务实现和 required verification 已完成，进入收尾阶段。

收尾阻塞：当前共享工作区存在非本任务脏改动（`20260728-scheduler-route-flow-list-permission` 相关 SQL/测试/任务产物及并发源码改动）。按“不得混入非本任务改动”和项目脏工作区基线门禁，当前不执行本任务提交/推送，待用户确认基线处理或并发任务清理后再完成 closeout commit/push。

共享分支状态：并发任务于 2026-07-27 18:41:23 创建并推送基线提交 `f18927b9`，其中已包含本任务 Java 实现、测试和当时的初始任务文档。完整回归后的复核确认本地 `HEAD` 与 `origin/int_main` 已对齐且都包含 `f18927b9`；共享分支仍由并发任务持续推进，本次完整回归后的任务证据更新尚未提交。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。候选人来源仍以任务创建时的有效候选快照为准。
- 是否从根因和长期维护角度解决：是。收件人解析集中在工作任务通知入口，避免各任务类型分别维护单收件人逻辑。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- eDHR 候选填写人必须来自任务/填写人快照，不得从当前登录人、创建人、角色 ID 或空列表推断。
- 后端行为变更使用 BDD -> RED -> GREEN -> REGRESSION。
- Maven 多模块验证使用 `-pl yudao-module-mes -am`，避免兄弟模块产物边界误判。
- 本任务不修改数据库 schema、菜单、权限和租户绑定，不执行 SQL 或远端操作。
- 用户已明确扩大范围至 MES 完整回归全绿；任何 schema、fixture 或测试基础设施改动仍必须先按对应项目门禁确认，禁止跳过测试、伪造 fixture 或放宽断言。
- 真实 fixture 缺失默认阻塞；若用户明确取消该真实样本覆盖，只能删除该覆盖入口并保留可执行的合成 fail-fast/契约测试，禁止 `@Disabled`、Maven excludes、assumptions、伪造 fixture 或合成 workbook 冒充真实样本。

## 范围变更

- 决策证据：`docs/changes/20260727-mes-full-regression-green.md`
- 规划产物：`request-analysis.md`、`prd.md`
- 最终验收：在 `IntRuoyiBackend` 执行 `mvn -pl yudao-module-mes test`，退出码 0、`BUILD SUCCESS`、0 failures、0 errors。
- 用户后续范围变更：Sheet1 Excel 真实样本覆盖不再要求；最终验收以剩余 MES 正式测试套件完整通过为准。

## 初始证据

- 既有脏工作区基线提交：`868893b0`。
- 基线文件清单和提交前状态记录在 `execution-log.md`。

## Cleanup Keep

- doc/tasks/20260727-edhr-notify-all-valid-candidates/backend-api-evidence.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/bug-regression-evidence.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/dev-plan.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/failure-inventory.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/prd.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/request-analysis.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/task-state.json
- doc/tasks/20260727-edhr-notify-all-valid-candidates/test-plan.md
- doc/tasks/20260727-edhr-notify-all-valid-candidates/test-report.md
