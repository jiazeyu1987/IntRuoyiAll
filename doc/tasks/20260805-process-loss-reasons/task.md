# 工序损耗原因维护

## Task Goal

按用户确认口径实现 AC-D04：生产组长在工作台 Tab 内用标准列表维护“工序设置列表下的工序”的损耗原因，并按 BDD + 严格 TDD 在新的 IntRuoyi worktree 中开发验证。

## Milestones

- [ ] 建立 BDD/TDD 设计文档，明确前端、后端、数据库和 E2E 验收口径。
- [ ] 创建并登记新的 `D:\IntRuoyiWorktree\` worktree。
- [ ] 编写 RED 测试覆盖权限、共通数据、CRUD、报工下拉、禁用/跨工序拒绝和历史快照。
- [ ] 实现数据库、后端接口和前端标准列表 Tab。
- [ ] 运行 GREEN 与回归验证，记录证据。
- [ ] 完成 cleanup、经验沉淀、提交和推送。

## Expected Verification

- BDD/TDD 设计文档可 UTF-8 读取并覆盖用户列出的 7 项验收。
- 后端定向 Maven 测试覆盖工序可见范围、多个组长数据共通、损耗原因新增/修改/删除、报工提交校验和历史快照。
- 前端静态合同覆盖生产组长工作台 Tab、标准列表模板、损耗原因独立列和操作面板。
- 真实 Playwright E2E 使用生产组长路径验证可见范围、跨组长共通、报工下拉、禁用/删除、跨工序拒绝和历史快照。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按工艺路线工序设置正式来源和后端权限校验建模。
- `是否存在临时补丁或绕过`：否。

## Worktree Plan

- Branch: `codex/20260805-process-loss-reasons`
- Path: `D:\IntRuoyiWorktree\20260805-process-loss-reasons`
- Base: 当前 `int_main` HEAD，主工作区已有并行脏改动不纳入本任务。
