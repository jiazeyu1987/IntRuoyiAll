# 20260730 eDHR 一线填写页签

## Task Goal

在 eDHR 批记录页面级页签中新增 `生产填写` 和 `PQC填写` 两个独立页签，并把现有一线简化填写 UI 接入真实 Vue 前端页面。

## Milestones

- [x] 建立任务记录与 BDD/TDD 验收合同。
- [x] 新增 eDHR 批记录共享页签组件和两个填写路由。
- [x] 将一线简化填写组件拆成固定 `production` / `pqc` 模式，防止员工切换时跨模板自动换 UI。
- [x] 完成静态合同、类型检查和可用验证记录。
- [ ] 接入正式一线设备账号路线绑定来源，解除 `芋道源码/admin` E2E 上下文阻塞。
- [ ] 使用 Playwright 在 `芋道源码/admin` 真实页面复验 `生产填写` 与 `PQC填写`。

## Expected Verification

- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`
- `node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js`
- `pnpm ts:check`
- 本机运行态可用时：用 Playwright 打开 `生产填写` 与 `PQC填写` 两个页签并保存 1920×1080 截图。

## Current Status

in_progress

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，采用共享页签组件和固定模式的一线填写组件，避免静态 HTML 或重复实现。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端功能按 BDD + strict TDD 执行，先新增最小静态合同，再实现。
- 页面复刻只允许改前端展示和路由，不修改后端 API、DTO、数据库或业务数据。
- 缺少正式提交上下文时必须显式阻塞，不 mock、不默认成功、不静默切换模板。
- PowerShell 写中文文档必须使用 UTF-8 路径；本任务文档通过 `apply_patch` 写入。

## Dirty Workspace Baseline

- Baseline commit: `4158334f chore: baseline dirty workspace before edhr frontline tabs`
- Purpose: 隔离本任务开始前已存在的未提交/未跟踪改动。

## Implementation Summary

- 新增共享页签组件 `EdhrBatchRecordTabs.vue`，统一 `批次执行 / 历史批记录 / 生产填写 / PQC填写` 页签入口。
- 新增 `BatchProductionFillPage.vue` 与 `BatchPqcFillPage.vue`，分别锁定 `FrontlineFixedTemplatePanel` 的 `production` 与 `pqc` 模式。
- 将 `FrontlineFixedTemplatePanel.vue` 调整为固定模式渲染，员工切换只记录后端模板类型，模板不一致时显式阻塞提交，不自动切换 UI。
- 生产页隐藏工单/生产订单，仅保留工序、员工、主页、数量、设备参数、最多 3 个设备卡片和无设备状态。
- PQC 页显示生产订单、工序、员工、主页和可输入检验内容，去除检验方法、成功/失败、巡检摘要等非必需内容。

## Experience Consolidation

- 已按 `project-experience-consolidation` 规则搜索 `docs/` 与 `docs/experience-index.md`。
- 本任务未产生新的通用工程门禁；一线填写业务背景已在 `docs/inception/` 与 `docs/acceptance/production-line-process-pool/` 中存在，未新增长期经验文档。

## Cleanup Keep

- doc/tasks/20260730-edhr-frontline-fill-tabs/frontend-feature-evidence.md

## Git Integration Blocker

- `git status --short --branch` 当前显示 `int_main...origin/int_main [ahead 10, behind 8]`，且工作区存在调度、签名、权限等并行任务脏改动。
- 本任务实现和验证已完成，cleanup 已通过；最终 push/远端同步需先由共享分支负责人完成分支分叉与并行脏改动协调。
- 为避免触碰并行任务改动，本任务不执行 pull、rebase、merge、force push 或宽泛暂存。

## Yudao Source E2E Blocker

- `芋道源码/admin` 登录预检可以打开 `生产填写` 和 `PQC填写` 两个页签。
- 完整页面 E2E 被正式接口阻塞：`/admin-api/mes/pro/feedback/frontline/device-account/processes` 返回 `1040760100`，消息为 `设备账号工艺路线绑定来源未接入，无法加载一线报工上下文`。
- 源码核对显示当前只有 `MesFrontlineDeviceAccountRouteBindingSource` 接口，没有生产实现 bean；按 no-fallback 规则不伪造设备账号路线绑定。

## Continuation Scope

- 用户要求继续直到 E2E 验证通过。
- 正式绑定来源限定为现有主数据链路：登录账号岗位、工作站人力、路线工序工作站、工作站设备、启用工艺路线。
- 真实 E2E 若需要数据，只允许使用可清理的任务自有夹具，不永久改写 admin 基线数据。
