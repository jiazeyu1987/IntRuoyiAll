# Execution Log

## User Intent

用户确认当前业务规则：一个工序可能有多个人可以填写，但系统当前没有负责人概念。默认由过程检验记录的填写人完成工序并进入下一步；如果当前工序没有过程检验记录填写人，例如灭菌记录工序，则由该工序解析出的所有填写人都可以进行下一步。要求按此规则优化当前流程，并完成完整数据真实 E2E 验证。

## BDD

- `BDD: 多填写人表单待办可见 -> Given` 当前工序有多张表单且每张表单配置不同填写人集合，`When` 任一填写人打开个人工作台，`Then` 系统应展示该用户可填写的表单任务，并通过正式入口进入对应普通批记录或 FormCenter 动态表单。
- `BDD: 过程检验记录填写人优先推进 -> Given` 当前工序存在过程检验记录且配置了填写人集合，`When` 当前工序必填表单已满足完成条件，`Then` 只有过程检验记录填写人集合内用户可以完成工序并推进下一步。
- `BDD: 无过程检验记录时所有解析填写人可推进 -> Given` 当前工序没有过程检验记录填写人集合，`When` 当前工序必填表单已满足完成条件，`Then` 当前工序解析出的全部表单填写人并集内用户可以完成工序并推进下一步。
- `BDD: 非填写人或非推进人 fail-fast -> Given` 当前用户不属于表单填写人或工序推进人集合，`When` 尝试打开填写、保存提交或推进工序，`Then` 后端返回明确权限错误，前端展示错误，不静默降级。
- `BDD: 动态表单工作台入口统一 -> Given` 个人工作台待办对应 FormCenter 动态表单且无传统 `executionId`，`When` 用户点击进入处理，`Then` 前端应调用正式批次任务打开接口并打开动态表单抽屉或统一填写工作区，不再强制要求 `executionId`。

## Milestone Updates

- in_progress: 创建任务记录，已读取 backend/frontend/e2e/database/local-runtime/login-access/task-closeout 和经验索引门禁；准备审计现有服务、测试和真实 E2E 入口。

## TDD Evidence

- pending: RED 后端测试。
- pending: RED 前端静态合同。
- pending: GREEN 目标验证。

## Verification Evidence

- pending: 后端、前端、E2E、UTF-8 和 diff 检查。

## Blockers

- 当前 `int_main` 工作区已存在非本任务脏改和 ahead 提交；本任务只新增任务自有文件和后续任务自有改动，不回滚或混入并发任务。
