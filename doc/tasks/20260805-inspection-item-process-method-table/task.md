# 20260805 检验项目工序方法表修复

## Task Goal

修复检验项目页签的数据结构与展示口径：检验项目应按“工序 -> 检验方法”呈现，并展示每个检验方法在该工序中的接受标准、检验方法、检验器具及设备、抽样方案，表格形态参考用户提供的图 2。

## Milestones

- [x] M1 定位当前检验项目页签组件、API 契约和数据来源。
- [x] M2 记录 BDD 场景并补充 RED 静态/单元回归测试。
- [x] M3 按正式数据来源修复前端展示，不引入 fallback 或 mock 数据。
- [x] M4 运行目标验证并记录 GREEN/回归证据。
- [ ] M5 完成收尾文档、经验沉淀、清理、提交与推送。

## Expected Verification

- 目标静态/单元合同先 RED 后 GREEN，证明表格包含工序、检验项目、接受标准、检验方法、检验器具及设备、抽样方案列。
- 目标验证证明数据按工序映射检验方法，不再把检验项目误建模为独立的项目/方法/工具/标准平铺表。
- 若需要真实页面验证，按 `docs/e2e-rules.md` 使用真实前端路径；若缺少运行态或账号，则记录 blocker，不用 API-only 冒充页面通过。

## Current Status

blocked

Implementation, required targeted verification, and task-closeout cleanup are complete. Final commit/push is blocked by unrelated local ahead commits plus staged and unstaged files from other tasks; pushing now would mix task ownership.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正检验项目页签的正式展示模型与测试合同。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `前端静态契约隔离门禁`：目标验证优先使用任务专用最小静态契约；若全量 `ts:check` 或既有大契约失败在无关历史问题，必须记录无关 blocker，不能冒充本任务失败或通过。
- `PQC 检验项目事实必须来自发布规程和结构化 itemResults`：检验设备、接收标准、检验方法等字段必须来自 QA 规程正式项目模型，禁止用固定四项字段、默认上下限、空标准、raw payload 或前端文案替代正式快照。
- `QA 规程配置状态必须来自产品级规程记录`：本任务不得回退到 DCC 项目代码、产品名称或压力泵样例模板推断配置状态；保留当前已改动的正式 API/路线来源口径。
