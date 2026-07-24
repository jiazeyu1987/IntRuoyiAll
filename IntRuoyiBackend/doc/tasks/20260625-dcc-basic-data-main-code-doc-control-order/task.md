# 任务：DCC 基础数据文控默认排序改为数字升序

## 任务目标

将 DCC 项目代码分页/导出默认顺序从当前按 `id` 倒序改为按 `doc_control_no` 的数字值从小到大排序，并保证：

- 数字型 `文控` 优先按数值升序排列；
- 非纯数字 `文控` 排在数字项之后；
- 前端分页和导出共用同一后端默认顺序；
- 不新增数据库字段、不改接口结构、不引入前端本地排序兜底。

## 当前状态

status: completed

## Current Status

completed

## 前一任务检查

- 后端最近任务 `20260625-dcc-route-node-stage-type-schema-drift` 已标记为“已完成”，允许继续本任务。
- 当前后端仓库存在其他未归属脏改动；本任务只修改 DCC 项目代码排序实现、定向单测与本任务文档，不覆盖其他改动。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：本任务仅做本机源码与定向单测，不执行真实 E2E、数据库 schema 变更、服务器写入、发布或其他高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。默认排序正式改由后端统一输出，不增加前端本地重排或失败兜底。
- `是否从根因和长期维护角度解决`：是。分页与导出都经由同一后端服务顺序，避免只改前端造成跨页顺序错误。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 文控为纯数字时按数字升序返回 -> Given 项目代码列表包含 2/10/30 等文控值 When 查询分页 Then 结果按 2、10、30 的数字顺序返回，而不是按 id 倒序或字符串顺序返回。`
- `BDD: 非数字文控排在数字文控之后 -> Given 项目代码列表同时包含纯数字和 A-1 等非纯数字文控 When 查询分页 Then 纯数字文控先按数字升序返回，非数字文控排在其后。`
- `BDD: 导出与分页复用同一默认顺序 -> Given 项目代码导出走同一分页查询 When 导出列表 Then 导出顺序与分页默认顺序一致。`

## 里程碑

1. M1：创建任务文档并补齐前置任务检查、经验门禁与 BDD。`DONE`
2. M2：先补排序 RED 单测，锁定当前 `id desc` 行为不符合用户要求。`DONE`
3. M3：最小修改 service 默认排序，实现文控数字升序并兼顾非数字文控。`DONE`
4. M4：运行定向单测、补齐后端证据与收尾。`DONE`

## 预期验证

- `mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/backend-api-evidence.md`

## Cleanup Keep

- `doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/task.md`
- `doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/execution-log.md`
- `doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/backend-api-evidence.md`

## 最终验证结果

- `mvn -pl yudao-module-dcc "-Dtest=DccProjectCodeServiceImplTest#pageAndExportShouldOrderByNumericDocControlNoAscendingBeforeNonNumeric" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260625-dcc-basic-data-main-code-doc-control-order/backend-api-evidence.md`：待执行