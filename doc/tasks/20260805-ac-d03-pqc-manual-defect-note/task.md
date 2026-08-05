# AC-D03 PQC 手动不良说明

## Task Goal

按 2026-08-05 业务口径补齐 AC-D03：不再维护“不良原因”主数据；PQC 发现不良时可手动录入不良说明/原因，提交后保存原始输入快照，并能追溯到订单、工序和 PQC 记录。

## Milestones

- [x] 建立 BDD 场景和任务证据。
- [x] 用前后端聚焦测试先 RED，锁定“不合格必须有手动不良说明”和“rawPayload 保留说明”。
- [x] 前端补齐 PQC 不良说明输入、必填校验和提交载荷。
- [x] 后端补齐结构化字段、失败校验和 rawPayload 快照。
- [x] 运行定向 GREEN 与相邻回归，更新验证报告。

## Expected Verification

- 前端静态契约：PQC 页面存在“不良说明/原因”输入，失败结果提交载荷包含该字段，缺说明阻塞提交。
- 后端 JUnit：失败 PQC 提交缺少不良说明时 fail-fast；提供说明时 event/PQC record rawPayload 保留该说明。
- 结构校验：任务文档 UTF-8 可读，RED/GREEN/REGRESSION 证据完整。

## Current Status

blocked

实现、定向验证、全量前端类型检查、运行态字段检查和真实页面只读输入预检已完成；写入型 PQC E2E、详情回读、历史不覆盖专项验收仍未完成。本轮继续只读预检确认当前运行库 `mes_pro_process_pool_pqc_record` 缺 `production_submit_event_id`，而当前源码 PQC record DO/Mapper 已依赖 `productionSubmitEventId`；同时现有 active order 30 缺正式工序快照和生产提交事件，不能作为 AC-D03 写入型夹具。按 no-fallback 规则，不能用 API-only、假 eventId 或污染既有业务数据替代真实页面写入验收。完整 closeout/提交/推送仍受共享工作区大量非本任务脏改动阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按新业务口径新增正式手输字段和校验，不继续依赖旧不良原因主数据。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs\experience-index.md`；命中前后端、PQC 项目级检验快照、PowerShell UTF-8、任务 closeout 门禁。
- 适用规则：PQC 检验事实继续以发布规程和结构化 `itemResults` 为准；手动不良说明只作为不合格说明事实进入提交 payload，不替代项目级逐件明细和规程快照。

## Cleanup Keep

- doc/tasks/20260805-ac-d03-pqc-manual-defect-note/task.md
- doc/tasks/20260805-ac-d03-pqc-manual-defect-note/execution-log.md
- doc/tasks/20260805-ac-d03-pqc-manual-defect-note/verification-report.md
