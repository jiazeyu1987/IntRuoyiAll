# 工序损耗原因维护

## Task Goal

实现 AC-D04：生产组长在生产组长工作台内，通过标准列表维护“工序设置列表下的工序”的损耗原因。权限来自工艺路线“工序开始”配置；同一工艺路线配置多个生产组长时，多个组长共享同一份工序损耗原因数据。

## Milestones

- [x] 建立 BDD/TDD 设计文档，明确前端、后端、数据库和 E2E 验收口径。
- [x] 创建并登记新的 `D:\IntRuoyiWorktree\` worktree。
- [x] 编写 RED/GREEN 测试覆盖权限、共通数据、CRUD、报工下拉、禁用/跨工序拒绝和历史快照。
- [x] 实现数据库、后端接口和前端标准列表区域。
- [x] 运行 GREEN 与回归验证，记录证据。
- [x] 真实写入型 Playwright E2E：使用任务自有模拟环境完成两个生产组长共享维护验证。
- [ ] 完成 cleanup、经验沉淀、提交和推送。

## Expected Verification

- `bdd-tdd-design.md` 可 UTF-8 读取并覆盖用户列出的 7 项验收。
- 后端定向 Maven 测试覆盖工序可见范围、多个组长数据共通、损耗原因新增/修改/删除、报工提交校验和历史快照。
- 前端静态合同覆盖生产组长工作台、标准列表、损耗原因独立列和操作面板。
- 迁移策略门禁通过，证明 `20260805_mes_process_loss_reasons.sql` 元数据、依赖和风险声明有效。
- 真实 Playwright E2E 使用任务自有生产组长/员工账号、授权/未授权路线工序和损耗原因样本完成验收。

## Current Status

ready_for_closeout

任务自有模拟环境已建立，`8093/48093` 运行态、运行时 API 和真实 Playwright 页面路径均已通过。剩余收尾项为 cleanup preview/apply、经验沉淀、提交和推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按工艺路线“工序开始”与“工序设置列表下的工序”正式来源建模。
- `是否存在临时补丁或绕过`：否。

## Worktree

- Branch: `codex/20260805-process-loss-reasons`
- Path: `D:\IntRuoyiWorktree\20260805-process-loss-reasons`
- Runtime profile: `int_main`
- Slot: `12`
- Frontend port: `8093`
- Backend port: `48093`

## Cleanup Keep

- doc/tasks/20260805-process-loss-reasons/bdd-tdd-design.md
- doc/tasks/20260805-process-loss-reasons/backend-api-evidence.md
- doc/tasks/20260805-process-loss-reasons/frontend-feature-evidence.md
- doc/tasks/20260805-process-loss-reasons/database-schema-evidence.md
- doc/tasks/20260805-process-loss-reasons/verification-report.md
- doc/tasks/20260805-process-loss-reasons/acd04_simulate_environment.py
- doc/tasks/20260805-process-loss-reasons/acd04_verify_runtime_api.py
- doc/tasks/20260805-process-loss-reasons/acd04_verify_frontend_ui.e2e.cjs
- doc/tasks/20260805-process-loss-reasons/fixture-summary.json
- doc/tasks/20260805-process-loss-reasons/runtime-api-verification.json
- doc/tasks/20260805-process-loss-reasons/frontend-ui-verification.json
