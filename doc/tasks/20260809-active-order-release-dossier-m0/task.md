# 活跃订单放行资料 M0 契约冻结

## Task Goal

基于 V4 最终开发方案和当前代码实现，冻结活跃订单放行资料开发所需的接口契约、三类 writer 边界、fixture manifest、运行时顺序与关键工程决策，为后续 A1-A6 开发提供唯一、无歧义的 M0 输入。

## Milestones

- [x] M0-00：建立任务文档并确认只执行契约冻结，不修改生产代码。
- [x] M0-01：核对并冻结前后端接口、DTO、状态、幂等与 blocker 契约。
- [x] M0-02：核对当前领域模型并冻结批记录、过程检验单、损耗单 writer 契约。
- [x] M0-03：冻结 fixture 可执行入口清单、manifest 和真实页面验收路径。
- [x] M0-04：冻结运行时顺序、事务边界、来源快照和负责人规则，完成结构验证。

## Expected Verification

- `m0-contract-freeze.md` 覆盖 M0-01 至 M0-04，且每项均引用当前代码或明确记录阻塞。
- 接口请求、响应、blocker、状态和幂等字段与当前 Controller、VO、Service、前端类型及静态合同一致。
- 三类 writer 均有唯一正式数据源、正式目标载体、输入、输出、完成条件和失败 blocker；禁止使用替代来源。
- fixture manifest 的每类数据都有页面、正式领域 service 或正式 API 入口；缺失入口必须阻塞。
- 运行时顺序、事务边界、`sourceSnapshotHash`、`RELEASE_APPROVE` 负责人和签名规则无歧义。
- 任务目录全部 Markdown 可按 UTF-8 读取，文档结构检查和 `git diff --check` 通过。

## Current Status

completed

M0 契约、结构验证和本任务 closeout preview/apply 均已通过。未启动 A1-A6，未修改生产代码、数据库或运行环境。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少正式载体、来源、映射、签名、负责人或 fixture 入口时记录 blocker，不使用替代来源。
- `是否从根因和长期维护角度解决`：是。M0 以当前正式领域模型和现有入口为依据冻结契约，避免后续 Agent 重复实现或各自猜测。
- `是否存在临时补丁或绕过`：否。

## Source Documents

- `doc/tasks/20260808-active-order-release-dossier-design/v4-final-agent-development-plan.md`
- `doc/tasks/20260808-active-order-release-dossier-design/docs/system/backend-api-design.md`
- `doc/tasks/20260808-active-order-release-dossier-design/docs/system/data-model.md`
- `doc/tasks/20260808-active-order-release-dossier-design/docs/acceptance/test-data.md`

## Applicable Experience Gates

- `docs/backend-development.md#edhr-放行负责人来源门禁`：放行负责人统一来自路线级 `RELEASE_APPROVE`。
- `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`：后端校验双 100%、正式资料来源、负责人和幂等。
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`：PQC 使用发布 QA 规程、结构化逐件明细和最终确认汇集。
- `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`：申请成功与刷新失败分层处理。

## Cleanup Keep

- doc/tasks/20260809-active-order-release-dossier-m0/task.md
- doc/tasks/20260809-active-order-release-dossier-m0/execution-log.md
- doc/tasks/20260809-active-order-release-dossier-m0/m0-contract-freeze.md
- doc/tasks/20260809-active-order-release-dossier-m0/verification-report.md
