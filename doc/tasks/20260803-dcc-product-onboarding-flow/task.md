# DCC 产品立项/建档闭环补全

## Task Goal

补齐 DCC 文控中“产品未在 DCC 项目代码里”时的正式产品立项/建档闭环：用户可发起建档申请，系统按审批口径完成审批通过后生成 DCC 项目代码，并与 MDM 产品主数据建立可追溯关联；后续受控文件提交必须沿用该正式 MDM 绑定。

## Scope

- 补齐 DCC 项目代码与 MDM 产品主数据之间的正式绑定字段、后端建档/审批通过服务、前端建档入口和最小验证。
- 不用 DCC 产品目录、`formBindings`、空值、默认项目代码或前端文案替代 MDM 产品主数据。
- 不实现真实 BPM 流程实例编排的占位成功；若现有 DCC/BPM 业务审批框架缺少可复用接口，本任务采用“申请单待审批 + 审批通过动作”的正式状态机，并明确保留后续接入 BPM 的边界。
- 不修改生产/测试服数据，不执行远端 SQL，不使用 API-only 冒充页面入口。

## Milestones

1. completed - 建立任务文档、适用门禁、BDD 场景和 RED/GREEN 证据结构。
2. completed - 定位现有 DCC 项目代码、MDM 产品、DCC 受控文件提交、前端基础数据入口和测试结构。
3. completed - 写 RED：后端服务/前端静态契约证明当前缺少产品建档申请、审批通过生成 DCC 项目代码、MDM 绑定和页面入口。
4. completed - 实现最小正式链路：数据库字段/表、后端 API/服务、唯一性与状态校验、受控文件提交 MDM 绑定、前端入口。
5. completed - 运行 GREEN：后端定向测试、数据库/前端静态契约和必要类型/编译验证。
6. completed - 更新验证报告、任务状态和收尾证据；真实页面 E2E 已通过，最终推送受并发本地提交与无关工作区状态阻塞，未标记 completed。

## BDD Scenarios

BDD: 产品建档申请生成待审批单 -> Given 一个产品尚未存在 DCC 项目代码 When 用户提交包含 MDM 产品信息和目标 DCC 项目代码的建档申请 Then 系统创建待审批申请 And 不立即生成正式 DCC 项目代码。

BDD: 审批通过生成正式 DCC 项目代码并绑定 MDM -> Given 产品建档申请处于待审批状态 When 审批人审批通过 Then 系统必须创建或绑定启用状态的 MDM 产品 And 生成启用的 DCC 项目代码 And DCC 项目代码记录 `productMasterId`。

BDD: 重复 DCC 项目代码必须拒绝 -> Given DCC 项目代码已存在 When 用户提交相同目标项目代码的建档申请 Then 请求被拒绝 And 不创建申请、MDM 产品或 DCC 项目代码。

BDD: 禁用 MDM 产品不能被绑定 -> Given 目标 MDM 产品存在但状态为禁用 When 用户审批通过建档申请 Then 审批动作失败并提示 MDM 产品不可用 And 不生成 DCC 项目代码。

BDD: 受控文件提交沿用 MDM 产品绑定 -> Given DCC 项目代码已由建档闭环生成并绑定 MDM 产品 When 用户基于该项目代码提交受控文件 Then 受控文件必须保存 `productMasterId`、产品编码和产品名称的正式 MDM 来源。

BDD: 页面入口暴露建档申请失败原因 -> Given 用户在 DCC 项目代码基础数据页发起产品建档 When 后端因重复编码、缺必填或禁用 MDM 产品拒绝请求 Then 页面必须展示真实失败原因，不吞掉错误或默认成功。

## Expected Verification

- 后端定向 Maven 测试覆盖申请创建、审批通过、重复编码拒绝、禁用 MDM 拒绝、受控文件提交继承 MDM 绑定。
- 数据库迁移/Schema 契约证明新增申请表、DCC 项目代码 MDM 绑定字段和关键唯一/索引约束。
- 前端静态契约证明项目代码基础数据页存在“产品建档申请”入口、表单字段、API 调用和错误暴露。
- 技能 evidence validator 通过，并将 PASS 结论复制到默认保留的验证报告。
- 真实 Playwright E2E 通过项目代码页面发起建档申请、审批通过、页面按项目代码筛选回显，并用只读 API 核验 DCC 项目代码和 MDM 产品绑定。

## Applicable Gates

- `docs/task-closeout-rules.md#任务目录`
- `docs/task-closeout-rules.md#BDD / TDD 记录`
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`
- `docs/backend-development.md#项目边界`
- `docs/frontend-development.md#前端静态契约隔离门禁`
- `docs/frontend-development.md#DCC 基础条目关联文档分类树门禁`
- `docs/database-rules.md#Schema 核对`
- `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁`
- `docs/e2e-rules.md#DCC 文控审批处理入口门禁`

## Experience Gate Summary

- DCC 基础条目和项目代码只能使用正式来源，不能用当前关联文件、空值、前端硬编码或 `formBindings` 替代正式主数据。
- DCC 项目代码与 MDM 产品绑定必须走正式建档申请/审批状态机；审批通过后 `productMasterId` 必须来自启用 MDM 产品或审批阶段正式创建的 MDM 产品。
- 审批通过时的重复项目校验必须忽略当前待审批申请自身，但继续拦截其它待审批申请和已存在 DCC 项目代码。
- DCC 审批处理必须有真实处理态入口和失败可见证据；如果缺少正式审批策略或运行态前置，应记录 BLOCKED，不能通过 SQL/API 改状态冒充审批完成。
- 数据库/schema 改动必须先核对现有迁移、DO、Mapper 和测试夹具，并为新增字段/表提供迁移与回归证据。

## Cleanup Keep

- doc/tasks/20260803-dcc-product-onboarding-flow/dcc-product-onboarding-real.e2e.cjs
- doc/tasks/20260803-dcc-product-onboarding-flow/dcc-product-onboarding-real-e2e-result.json

## Current Status

ready_for_closeout

按用户要求，本任务已切换到独立 worktree `D:\IntRuoyiWorktree\dcc-product-onboarding-flow-20260803`，登记 `int_main slot=15`，前端端口 `8096`、后端端口 `48096`，未占用主工作区 `8081/48081`。

worktree 内已完成运行态验证：DCC 定向 JUnit 通过 107 tests；前端静态契约通过；worktree DCC/MDM 相关模块 install 后重新打包 `yudao-server-exec.jar` 通过；后端 `48096` health `UP`；前端 `8096` HTTP `200`；合入最新 `origin/int_main` 和本地 `int_main` 已提交内容后再次复跑真实 Playwright E2E，通过项目代码页面发起产品建档申请并审批生成 DCC 项目代码，结果为 `requestId=7`、`projectCodeId=261`、`productMasterId=335`、`projectCode=CODXONB03074622`，`criticalNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。

剩余收尾：清理当前任务运行进程/运行产物，按 Git 门禁融合回 `int_main` 并尝试 push；若主工作区脏状态或 GitHub HTTPS 代理仍不可用，必须记录 blocker，不能标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补正式 DCC/MDM 建档与绑定链路。
- `是否存在临时补丁或绕过`：否。
