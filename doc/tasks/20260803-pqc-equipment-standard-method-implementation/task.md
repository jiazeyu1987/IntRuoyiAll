# PQC 检验设备、接收标准与检验方法开发验证任务

## Task Goal

在 `D:\IntRuoyiWorktree\20260803_pqf` 中按已完成的 PQC 修改文档实现开发验证：补齐检验项目级检验设备、设备编号、接收标准、检验方法、PQC 填写、PQC 组长复核、QA 规程配置与追溯展示闭环，并按 BDD + Strict TDD 完成验证后融合进 `int_main`。

## Milestones

- [x] 创建 `20260803_pqf` worktree 并登记运行槽位
- [x] 读取上一轮 PQC 方案、当前系统证据和经验门禁
- [x] 建立 BDD/TDD RED 基线与缺口清单
- [x] 实现后端 schema、服务、读模型、校验与快照链路
- [x] 实现前端 PQC 填写、组长、QA 页面交互与静态合同
- [x] 完成 GREEN、回归、文档证据和 review 优化
- [x] 提交并推送 `codex/20260803_pqf`
- [ ] 成功后融合进 `int_main`（本轮仅完成 worktree 开发验证与分支推送；本地主工作区 `E:\IntRuoyi` 脏状态阻塞默认 ff-only closeout 合并，后续需按合并门禁单独执行）

## Expected Verification

- 每个业务变更点必须映射到上一轮 PQC 文档的 BDD/TDD 场景。
- 生产代码变更必须有先 RED 后 GREEN 的后端 JUnit、schema/迁移测试、前端静态合同或真实 E2E 证据。
- 后端不得信任客户端提交的接收标准或检验方法，必须从发布规程/项目级配置生成提交快照。
- 前端不得用固定四项、整单设备或 raw payload 推断项目明细；PQC 组长和 trace 必须读取正式结构化项目明细。
- 缺正式设备主数据、数值上下限 schema、权限入口、测试数据或真实 E2E 前置时必须 fail fast 并记录影响，不得降级为 mock/API-only。

## Current Status

completed

worktree 开发验证、cleanup、实现提交和分支推送已完成；后续 `int_main` 融合需在主工作区清洁或远端快进门禁满足后单独执行。

## Applicable Experience Gates

- 前端静态契约隔离门禁：本任务新增专用 PQC 静态合同，若全量 `pnpm ts:check` 或大合同存在历史失败，只能记录为无关 blocker，不得冒充本任务通过。
- PowerShell Maven `-D` 参数门禁：目标 JUnit 使用引号包裹 `"-Dtest=..."`，避免 Windows PowerShell 把 Maven 参数解析成 lifecycle phase。
- 一对多读模型聚合门禁：PQC 组长/trace 展示项目级明细时不得按任务粗粒度一对多 JOIN 造成重复行或 total 漂移。
- 任务验证脚本保留门禁：新增 `tests/e2e/pqc-*.spec.js` 作为本任务验证产物，closeout 时不得误清理。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本任务必须按正式数据链路实现，缺前置则阻塞。
- 是否从根因和长期维护角度解决：是。目标是补齐项目级设备、编号、方法、标准、上下限、审核与追溯的统一事实模型。
- 是否存在临时补丁或绕过：否。
