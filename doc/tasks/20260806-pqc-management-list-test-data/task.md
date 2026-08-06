# PQC 管理列表测试数据

## Task Goal

- 在本机 `芋道源码/admin` 可访问的 PQC 管理列表中添加一条可追踪测试数据。
- 测试数据必须进入正式提交列表读模型，不使用前端 mock、不改生产代码、不污染远端环境。
- 测试数据必须带任务标识，便于后续只读核验和清理。

## Milestones

- [x] 建立任务目录、BDD 场景和数据写入边界。
- [ ] 核对 PQC 管理列表 API、读模型、表结构和 admin 可见范围。
- [ ] 写入一条本机测试租户 PQC 提交事件及必要关联记录。
- [ ] 验证 `芋道源码/admin` PQC 管理列表可查询到该测试数据。
- [ ] 记录验证结果、数据主键和清理方式。

## Expected Verification

- 只读核对真实库表结构：`mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task`、PQC 组长 scope。
- 写入后用只读 SQL/API 核对任务标识、事件类型、实际员工、今天提交日期、PQC 任务和原始 payload。
- 本机运行态可用时，用 `芋道源码/admin` 登录态或对应列表 API 验证 PQC 管理列表能看到该记录。

## BDD Scenarios

- BDD: PQC 管理列表显示测试提交 -> Given 本机 `芋道源码/admin` 打开 PQC 管理列表 / When 今天存在一条 admin 负责范围内的 PQC 测试提交 / Then 列表能显示生产工单、工序、PQC 检验员、检验项、检验数量、损耗数量和逐件样本值。
- BDD: 测试数据可追踪可清理 -> Given 测试提交写入正式库 / When 后续需要清理 / Then 可通过任务标识定位并删除本次事件和关联 PQC 记录，不影响其它业务数据。

## Applicable Gates

- 数据库写入门禁：写 SQL 前必须核对真实库 schema、目标租户、影响范围和清理方式。
- PQC 项目级检验快照门禁：PQC 列表展示应来自结构化 `pqcItemDetails/itemResults` 和 rawPayload 快照。
- 登录访问门禁：仅使用本机 `localhost:8081` / `127.0.0.1:48081` 和 `芋道源码/admin` 标签，不记录密码。
- No fallback：不得用 mock、默认成功、前端硬编码或 API-only 伪造页面数据。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，测试数据写入正式列表读模型所需数据源。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

- 已开始核对 PQC 管理列表正式数据来源和写入边界。
