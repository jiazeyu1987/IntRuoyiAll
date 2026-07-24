# 同步工单快速过滤与资源入池校验修复

## Task Goal

修复排产同步工单 `admission-diff` 查询与入池校验缺陷，覆盖快速过滤、入池状态筛选、资源快照默认工作站和人工数量缺失阻断。

## Current Status

completed

## Milestones

- [completed] M1 建立回归场景与 RED 证据：覆盖 admission-diff 快速过滤、ALREADY_ADMITTED 状态筛选、默认工作站资源快照、人工数量缺失阻断。
- [completed] M2 实现最小正式修复：后端解析 quickFilter，前端快速过滤映射显式查询字段，资源快照使用工序启用工作站，人工数量缺失阻断入池。
- [completed] M3 运行目标后端与前端静态验证，记录 GREEN 证据。
- [completed] M4 收尾记录 verification-report 与经验沉淀。

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderAdmissionDiffServiceTest test`
- `node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js`
- 如前端依赖可用，补充运行相关快速过滤静态测试。

## Experience Gate

- `docs/experience-index.md`：任务初始化时缺失；后续已出现并读取。
- 命中 `docs/release-build-preflight-lessons.md#2026-07-19-build-release-mes-companion-contract-编译门禁`：模块编译必须以 Maven 结果为准，不删除调用或注释实现来规避编译失败。
- `docs/powershell-memory.md`：索引指向的权威文档不存在；本任务未执行发布、服务器、数据库或真实 E2E 等高风险动作。
- 本次经验沉淀检查：没有适合的既有长期经验归宿，且未产生需要新增长期规则的通用结论，因此不新建长期经验文档。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修复查询参数接收/映射、资源快照选择和阻断原因计算。
- `是否存在临时补丁或绕过`：否。

## Verification Evidence

- 后端 RED：`mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderAdmissionDiffServiceTest test` 曾因 `quickFilter` 缺失 setter 失败。
- 后端 GREEN：同一命令通过，`9` 个测试全部通过。
- 前端 RED：`node tests/e2e/mes-pro-schedule-order-admission-default-static.spec.js` 因 admission 快速过滤未映射 `workOrderCode` 失败。
- 前端 GREEN：同一命令通过；定向 ESLint 通过。
- 全量前端类型检查：`pnpm ts:check` 被 `src/views/dcc/controlled-file/browser/index.vue` 的既有类型错误阻断，与本任务文件无关。
- 收尾：`task_closeout.py --mode preview` 与 `--mode apply` 均通过，已清理三份任务附属技能证据文件。

## Remaining Blockers

- 无阻断。本任务已具备收尾条件；全量前端类型检查的 DCC 既有错误见 `verification-report.md`。
