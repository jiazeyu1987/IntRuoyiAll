# 一线 PQC 正式提交成功写入 E2E 验证

## Task Goal

补齐任务自有正式测试数据，并通过真实前端页面验证一线 PQC 正式提交成功写入链路，确认正式提交回执、电子签名、PQC 记录、过程池事件和重复提交保护均符合已交付方案。

## Milestones

- M1：核对运行态、登录租户/账号、PQC 正式提交流程所需 schema 与已有数据。✅
- M2：创建或补齐任务自有测试数据，覆盖 active order、pending PQC task、published QA regulation/items、formal production-submit event、授权 PQC/签名账号和清理路径。✅
- M3：通过 Playwright 操作真实前端页面完成成功提交，并采集目标写请求、页面回执和无未处理错误证据。✅
- M4：用只读 API/DB 核验持久化跨表身份，并记录重复提交不会二次写入。✅
- M5：完成任务文档、验证报告和任务自有临时产物清理。✅

## Expected Verification

- Playwright 真实页面路径：`http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-pqc-fill`。
- 数据核验：只读确认 PQC task、PQC record、process event、electronic signature、piece details 的任务自有标识和一致性。
- 重复提交核验：页面提交成功后按钮锁定，强制点击不会发出第二次 submit 请求；DB 只存在一组任务 `231` 的正式 PQC event/record。
- 技能证据：`database-schema-evidence.md` 与 `frontend-feature-evidence.md` 通过 validator，关键结论归档到 `verification-report.md`。

## Current Status

completed

实现、验证、经验沉淀与任务自有临时产物清理均已完成。未执行 Git commit/push。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复真实页面暴露的时间格式和草稿结果计算缺陷，并保留提交前严格校验。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- PQC 真实提交前置必须覆盖活跃路线全部当前工序；本轮优先使用边界清晰的任务自有单工序 fixture，不给无关工序批量补造规程或任务。
- PQC 检验项目事实必须来自发布 QA 规程和结构化 `itemResults[]`，不得用固定四项字段、raw payload、默认上下限或 API-only 展示替代。
- 提交按钮失败必须终止在可见错误边界；验证时需记录无新增 `Unhandled error during execution of native event handler`、无不可见失败、无失败后继续写入。
- 写入型 E2E 补数必须使用任务自有标识、真实账号/权限/业务对象和可清理范围；不得用默认 admin、mock 数据、前端直塞 localStorage 或 API-only 写入冒充真实 E2E。
- 写入响应不确定时必须先用稳定记录 ID 做只读断点恢复判断，禁止盲目重放导致重复提交。
- 前端 `LocalDateTime` 数字时间戳必须由正式 formatter 覆盖 API 类型与渲染路径；草稿态计算不得调用 submit-only 严格断言，严格断言保留在显式提交链路。
