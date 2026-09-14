# Test Plan

## Task-Level Validation

### test_case_id: TC-F1

mapped_task_ids: F1
mapped_acceptance_ids: AC-01, AC-02, AC-03, AC-10, AC-17
environment or setup: 后端 MES 模块，测试数据库或 MyBatis 测试上下文。
steps: 运行 T01-T04 的 Maven/SQL 契约测试。
expected_result: 新工序池模型、提交事件、PQC 入池、服务端时间、签名和上下文校验通过。
evidence: Maven 输出、SQL 契约输出、execution-log。

### test_case_id: TC-F2

mapped_task_ids: F2
mapped_acceptance_ids: AC-04, AC-05, AC-10, AC-17
environment or setup: F1 已融合或等效基础模型可编译。
steps: 运行 T05-T08，验证组合提交事务、payload 拆分、超限原始值保留、路线不阻断。
expected_result: 报工、记录本、工序池事件同事务创建，失败回滚无部分数据。
evidence: Maven 输出、接口契约测试、execution-log。

### test_case_id: TC-F3

mapped_task_ids: F3
mapped_acceptance_ids: AC-06, AC-07, AC-05, AC-17
environment or setup: F1 基础模型，前端依赖安装完整。
steps: 运行 T09-T11，验证模板目录、字段契约、PQC 模板、前端渲染。
expected_result: 固定模板字段和 UI 与文档一致，切换员工后模板和 payload 隔离。
evidence: Maven 输出、前端静态/单元测试、execution-log。

### test_case_id: TC-F4

mapped_task_ids: F4
mapped_acceptance_ids: AC-08, AC-09, AC-02, AC-10
environment or setup: 设备账号、路线、工序、员工绑定测试数据。
steps: 运行 T12-T14。
expected_result: 设备账号只能看到绑定路线工序，工序只能切换绑定员工，签名身份与实际员工一致。
evidence: Maven 输出、前端静态测试、execution-log。

### test_case_id: TC-F7

mapped_task_ids: F7
mapped_acceptance_ids: AC-11, AC-12, AC-13, AC-14
environment or setup: F1 数量片段和生产工单计划开始时间字段已确认。
steps: 运行 T15-T17。
expected_result: 生产工单按计划开始时间 FIFO 分配，缺时间阻塞，明细可追溯，已分配片段锁定。
evidence: Maven 输出、SQL 契约输出、execution-log。

### test_case_id: TC-F8

mapped_task_ids: F8
mapped_acceptance_ids: AC-15, AC-16, AC-02, AC-03, AC-13
environment or setup: F1/F2/F3/F4/F7 已融合，存在工序池事件测试数据。
steps: 运行 T18-T20。
expected_result: 时间轴按提交时间、多条件过滤、只读详情和追溯状态全部通过。
evidence: Maven 输出、前端测试、Playwright 证据、test-report。

## System-Level Validation

- 运行 `scripts\preflight\branch-runtime-port-guard.ps1`。
- 运行 `pnpm --dir IntRuoyiFronted ts:check`。
- 运行 `docs/acceptance/production-line-process-pool/tdd-plan.md` 中 GREEN Commands 的后端定向套件。
- 若本机登录、数据库、签名和测试数据齐备，运行真实 Playwright E2E：一线报工组合提交、设备账号内切换员工、PQC 提交、时间轴查询。
- 若真实 E2E 前置缺失，必须在 `test-report.md` 记录 blocker 和影响，不能宣称 E2E 通过。

## Regression Checks

- 现有 `mes:pro-feedback` 报工 create/update/page/detail 不应被破坏。
- 现有 eDHR 批记录表单、表单槽位、工序开始三类配置链路不应混用。
- 现有生产工单页面和字段不应引入排产系统依赖。
- 时间轴接口不得产生写请求。
