# Backend API Evidence

## Scope

本次修复范围为 MES 工序池班组长后端服务：`MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission` 与 `MesTeamLeaderReportConfirmationServiceImpl#confirmSubmission`。

## Contract

生产报工 `PRODUCTION_SUBMIT` 的通过动作只能通过报工分配确认链路生成 `APPROVED` 复核与 allocation 明细；通用复核接口仅允许生产报工退回或非生产类提交的既有复核语义；同一事件存在终态复核时禁止再次确认。

## Validation

新增 `PRO_PROCESS_POOL_PRODUCTION_REVIEW_ALLOCATION_REQUIRED` 表达生产报工通过链路错误；报工确认入口先校验 `leaderType=PRODUCTION`，再锁定事件和最新 review，发现终态即抛 `PRO_PROCESS_POOL_SUBMISSION_REVIEW_TERMINAL_EXISTS`，不再继续 PQC/分配/插入。

## BDD:

BDD: 生产通过必须进入分配 -> Given 一个 `PRODUCTION_SUBMIT` 报工事件 When 调用通用复核接口尝试 `APPROVED` Then 后端拒绝并要求使用生产分配确认链路。

BDD: 退回后禁止继续分配 -> Given 一个生产报工已存在 `REJECTED` 终态复核 When 生产组长再调用分配确认 Then 后端拒绝且不插入 review/allocation/completion。

BDD: 生产分配只能由生产组长执行 -> Given 非 `PRODUCTION` leaderType 调用分配确认 When 请求到达后端 Then 后端在加载事件前拒绝。

## RED:

`java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-red.args` -> FAIL，旧服务实现下 3 个 AC-M16 新用例失败。

## GREEN:

`java @doc\tasks\20260805-ac-m16-report-confirmation-hardening\junit-console-green.args` -> PASS，13/13 服务测试通过。

## Verification

独立 javac 编译通过：`javac @doc\tasks\20260805-ac-m16-report-confirmation-hardening\javac-main-check2.args` 与 `javac @doc\tasks\20260805-ac-m16-report-confirmation-hardening\javac-test-check2.args`。标准 Maven 受并行 Maven 构建阻塞，多次超时，未记为通过。

## Blockers

当前阻塞项是标准 Maven 完成门禁未取得结果；机器上仍有非本任务 Maven 进程运行，按项目规则未终止不归属本任务的进程。
