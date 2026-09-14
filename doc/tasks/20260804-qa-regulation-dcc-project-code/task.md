# QA 规程按 DCC 项目代码配置

## Task Goal

将 `QA 规程配置` 页面从固定的压力泵示例页改为 DCC 项目代码驱动的通用配置页。用户必须先选择正式 DCC 项目代码，页面再带出项目名称和产品主数据关系；当前的“按压式球囊扩充压力泵”仅作为所选项目对应的产品，不再写死为页面结构。

## Scope

- 修改 `QaRegulationPage.vue`，接入现有 DCC 项目代码分页查询 API。
- 项目代码、项目名称、产品主数据 ID 由 DCC 数据只读带出。
- 页面按 QA 规程状态区分已配置和待配置的 DCC 项目，便于 QA 识别后续配置缺口。
- 新增正式后端状态接口，按 DCC 项目绑定的 `productMasterId` 查询 `mes_qa_inspection_regulation.product_id` 配置状态。
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
- [x] M3：实现 DCC 项目代码选择、配置状态分区、加载、错误和发布门禁。
- [x] M4：实现后端 QA 规程产品配置状态接口并接入前端。
- [x] M5：完成 GREEN、回归、类型检查和技能证据验证。
- [ ] M6：提交、推送和任务收尾。

## Expected Verification

- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs`
- `node tests/e2e/mes-edhr-qa-menu-static.spec.js`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest" test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-qa-regulation-dcc-project-code/backend-api-evidence.md`
- `node scripts/preflight/login-preflight.mjs` with env-sourced local default login and target `/mes/pro/process-pool/qa-regulation`
- `node tests/e2e/mes-edhr-qa-menu-real.e2e.js`
- `node tests/e2e/role-matrix-qa-regulation-original-excerpt-real.e2e.cjs`
- `pnpm e2e:qa-regulation:dcc-status:real`
- `git diff --check`

## Applicable Experience Gate

- DCC 项目代码必须是产品关系的正式入口，页面不得仅从前端 payload、项目名或空值推断产品。
- `productMasterId` 必须来自正式 DCC 项目代码与 MDM 产品绑定；缺少绑定时页面明确显示未绑定，不生成默认产品。
- 查询失败必须可见，不得切换到压力泵示例数据或返回默认成功。
- QA 配置状态必须来自后台 QA 规程记录；状态接口失败时页面显示错误，不把项目静默归为待配置。

## Baseline

- 任务开始前工作区存在其它任务改动，已按规则建立独立基线提交：`516ef63a1`。
- 基线提交后出现的并行改动保持不动，不纳入本任务提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以 DCC 项目代码作为 QA 配置产品范围的正式入口。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked

- 实现、静态合同、类型检查、后端单测和只读真实页面入口/压力泵原文验证已完成。
- 状态分区真实 E2E 被正式数据前置阻塞：本机 DCC 项目代码 `IDI`（id 129）返回 `productMasterId=null`，无法调用后端 `project-statuses` 对产品级 QA 规程配置状态做真实页面验收。
- 未使用产品名称、固定 IDI、前端假数据或默认产品补齐绑定；需要先补齐正式 DCC 项目代码到 MDM 产品的绑定数据，或由用户明确授权创建/修复本机测试数据。
