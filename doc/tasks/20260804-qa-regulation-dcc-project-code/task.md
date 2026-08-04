# QA 规程按 DCC 项目代码配置

## Task Goal

将 `QA 规程配置` 页面从固定的压力泵示例页改为 DCC 项目代码驱动的通用配置页。用户必须先选择正式 DCC 项目代码，页面再带出项目名称和产品主数据关系；当前的“按压式球囊扩充压力泵”仅作为所选项目对应的产品，不再写死为页面结构。

## Scope

- 修改 `QaRegulationPage.vue`，接入现有 DCC 项目代码分页查询 API。
- 项目代码、项目名称、产品主数据 ID 由 DCC 数据只读带出。
- 未选择 DCC 项目代码时阻塞 QA 规程发布前检查。
- DCC 查询失败时显示明确错误，不使用固定压力泵数据或默认项目降级。
- 更新 QA 规程页面静态契约。

## Non-Goals

- 本任务不新增 QA 规程正式保存/发布后端接口。
- 本任务不修改 `mes_qa_inspection_regulation` 数据库结构。
- 本任务不通过产品名称模糊匹配、默认项目代码或前端假数据补齐产品关系。

## Milestones

- [x] M1：核对现有 QA 页面、DCC 项目代码 API 和产品绑定模型。
- [x] M2：记录 BDD 并完成专用静态契约 RED。
- [ ] M3：实现 DCC 项目代码选择、加载、错误和发布门禁。
- [ ] M4：完成 GREEN、回归、类型检查和技能证据验证。
- [ ] M5：提交、推送和任务收尾。

## Expected Verification

- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/mes-edhr-qa-menu-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/frontend-feature-evidence.md`
- `git diff --check`

## Applicable Experience Gate

- DCC 项目代码必须是产品关系的正式入口，页面不得仅从前端 payload、项目名或空值推断产品。
- `productMasterId` 必须来自正式 DCC 项目代码与 MDM 产品绑定；缺少绑定时页面明确显示未绑定，不生成默认产品。
- 查询失败必须可见，不得切换到压力泵示例数据或返回默认成功。

## Baseline

- 任务开始前工作区存在其它任务改动，已按规则建立独立基线提交：`516ef63a1`。
- 基线提交后出现的并行改动保持不动，不纳入本任务提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以 DCC 项目代码作为 QA 配置产品范围的正式入口。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
