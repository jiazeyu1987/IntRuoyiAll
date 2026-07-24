# 任务：Smart Release 长期发布方案子 Agent 设计与主审

## Goal

使用子 agent 重新设计一份长期可维护的构建、发布、部署方案，并由主 agent 严格 review 后放行或退回。

## 任务目标

方案必须覆盖更快打包、构建发布包、部署发布包、数据库表结构变化、必要数据变化、资源文件变化、大文件/多文件增量、备份恢复、测试服/备份服/正式服目标选择，并明确 BDD + TDD + Subagent Driven 的实施路径。

## Scope

- 重新梳理长期 Smart Release 总体方案。
- 让子 agent 产出需求、设计、开发计划、测试计划。
- 主 agent 执行放行 review，必须确认逻辑自洽、接口清晰、无明显副作用。
- 文档必须能指导后续分阶段实现，不只是概念描述。

## Non-Scope

- 本任务不改生产代码。
- 本任务不运行真实构建、部署或服务器操作。
- 本任务不修改数据库、MinIO、NAS 或远程服务器。
- 本任务不引入 fallback、静默降级或绕过当前发布失败的临时补丁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；方案要求缺配置、缺 migration、缺 resource delta、目标不匹配时 fail fast 或明确 report-only 阶段边界。
- `是否从根因和长期维护角度解决`：是；本任务目标是长期架构与分阶段实施，不接受只修当前脚本的补丁。
- `是否存在临时补丁或绕过`：否；任何临时措施必须作为非目标或 blocker 记录。

## Milestones

- [x] M1：创建任务状态与子 agent 输入。
- [x] M2：子 agent 完成 request-analysis 与 PRD。
- [x] M3：主 agent 完成 PRD 放行 review。
- [x] M4：子 agent 完成 dev-plan 与 test-plan。
- [x] M5：主 agent 完成最终方案 review。
- [x] M6：完成验证、收尾预览和提交。

## Expected Verification

- `request-analysis.md`、`prd.md`、`dev-plan.md`、`test-plan.md`、`task-state.json`、`execution-log.md`、`test-report.md` 存在。
- Acceptance Criteria 使用稳定 `AC-xx` 编号。
- Dev Plan 使用稳定任务图 `SR-Dxx`，包含依赖、影响路径、写范围、验收映射和验证步骤。
- Test Plan 明确 BDD、RED/GREEN、集成验证、部署验证、大文件/多文件验证。
- 主审文档明确是否放行；不满足则记录不放行理由。

## Review Result

PASS。详见 `verification-report.md`。

## Current Status

completed
