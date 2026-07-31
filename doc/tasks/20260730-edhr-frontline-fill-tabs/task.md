# 20260730 eDHR 一线填写页签

## Task Goal

在 eDHR 批记录页面级页签中新增 `生产填写` 和 `PQC填写` 两个独立页签，并把现有一线简化填写 UI 接入真实 Vue 前端页面。

## Milestones

- [x] 建立任务记录与 BDD/TDD 验收合同。
- [x] 新增 eDHR 批记录共享页签组件和两个填写路由。
- [x] 将一线简化填写组件拆成固定 `production` / `pqc` 模式，防止员工切换时跨模板自动换 UI。
- [x] 完成静态合同、类型检查和可用验证记录。
- [x] 接入正式一线设备账号路线绑定来源，解除 `芋道源码/admin` E2E 上下文阻塞。
- [x] 使用 Playwright 在 `芋道源码/admin` 真实页面复验 `生产填写` 与 `PQC填写`。

## Expected Verification

- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-switch.spec.cjs`
- `node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRouteProcessTemplateBindingSourceTest,MesFrontlineWorkstationPostRouteBindingSourceTest,MesFrontlineDeviceAccountContextServiceTest,MesFrontlineEmployeeSwitchServiceTest,MesFrontlineTemplateResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node --check tests/e2e/edhr-frontline-fill-tabs-real.e2e.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-real.e2e.cjs` with registered slot URLs `http://127.0.0.1:8083` and `http://127.0.0.1:48083`

## Current Status

ready_for_closeout

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，采用共享页签组件和固定模式的一线填写组件，避免静态 HTML 或重复实现。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端功能按 BDD + strict TDD 执行，先新增最小静态合同，再实现。
- 后端只补正式一线账号路线绑定与模板绑定来源，不新增数据库迁移、不 mock、不扩大账号权限。
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
- 新增 `MesFrontlineWorkstationPostRouteBindingSource`，按登录账号岗位、工作站人力、路线工序工作站、启用路线和工作站设备解析正式一线上下文。
- 新增 `MesFrontlineRouteProcessTemplateBindingSource`，按路线工序 `check_flag` 区分生产简化模板与 PQC 简化模板。
- 更新一线设备账号上下文服务，按 `routeId + workstationId` 保留多设备工序，并允许正式无设备工序返回空设备上下文。

## Experience Consolidation

- 已按 `project-experience-consolidation` 规则搜索 `docs/` 与 `docs/experience-index.md`。
- 已将 Playwright 全新上下文登录导航竞争经验合并到现有 `docs/e2e-rules.md`，未新增长期经验文档。
- 已更新 `docs/experience-index.md`，可通过 `Execution context was destroyed`、`Playwright 登录导航竞争`、`page.evaluate localStorage.clear` 等关键词定位该门禁。

## Cleanup Keep

- doc/tasks/20260730-edhr-frontline-fill-tabs/frontend-feature-evidence.md
- doc/tasks/20260730-edhr-frontline-fill-tabs/backend-api-evidence.md

## Git Integration Status

- 当前任务已迁移到隔离 worktree `D:\IntRuoyiWorktree\20260730-edhr-frontline-e2e-runtime`，分支 `codex/20260730-edhr-frontline-e2e-runtime`。
- 实现提交：`2fea1fcd feat: add edhr frontline production and pqc runtime context`。
- 主工作区原有并行任务文档已由独立基线提交 `d1e2e44d 基线: 保存并行任务文档改动` 保留，主工作区恢复 clean。
- 已将更新后的 `int_main` 合入功能分支；两处任务证据文档冲突按语义保留已完成的 12 项后端测试和真实 E2E 结果。
- 合并后端口守卫、目标后端测试、四项前端静态合同、E2E 脚本语法检查和 `pnpm ts:check` 均通过。
- 并行任务后续更新已由主线独立提交保留；最终同步后的功能分支 HEAD 为 `2e58c44d`，已推送到 `origin/codex/20260730-edhr-frontline-e2e-runtime`。
- `04603154..2e58c44d` 在 `IntRuoyiBackend` 与 `IntRuoyiFronted` 下无新增差异，后续主线同步仅包含任务/经验文档。
- 2026-07-31 主工作区继续存在无关 NAS/DCC 并行改动；为避免把未验证改动合入本任务，已从干净 `origin/int_main` commit `57112d97` 创建临时集成目录 `D:\IntRuoyiWorktree\20260731-edhr-frontline-clean-clone`。
- 临时集成分支 `codex/20260731-edhr-frontline-clean-integration` 只包含 eDHR 一线填写任务净差异；端口守卫、目标 Maven、前端静态合同、E2E 脚本语法检查、`pnpm ts:check` 和 evidence validators 均通过。
- GitHub `origin/int_main` 后续新增 `b8251624 docs: record int_main merge closeout`；该提交仅含 `doc/tasks/20260731-merge-int-main/*`，已合入临时集成分支并复跑端口守卫、目标 Maven、前端静态合同、E2E 脚本语法检查和 `pnpm ts:check` 通过。
- GitHub `origin/int_main` 后续又合入 `011999ef merge: sync origin int_main` 及相关 NAS/DCC 并行内容；临时集成分支合并后，eDHR 前端静态合同、E2E 脚本语法检查、`pnpm ts:check` 和端口守卫仍通过，但必跑后端 Maven 门禁在 `yudao-module-dcc` testCompile 阶段失败。
- 当前状态改为 `blocked`：失败点是无关 DCC 测试 `DccProjectCodeServiceImplTest` 引用了缺失的 `DccFileCategoryMatchRuleDO` 与 `DccFileCategoryMatchRuleMapper`。在该主线编译问题修复并且远端可拉取前，不能按本任务要求推送到 `origin/int_main`、执行 closeout apply 或删除原任务 worktree。
- 用户继续后，已在同一干净临时集成分支新增受控 DCC 解阻任务 `doc/tasks/20260731-dcc-category-match-rule-compile-unblock/`，补齐正式规则表、DO、Mapper 和服务读取链路；DCC 目标测试、eDHR 后端门禁、前端静态合同、E2E 脚本语法检查和 `pnpm ts:check` 均已通过，当前状态恢复为 `ready_for_closeout`。
- 未执行 force push、rebase、reset 或历史重写。

## Yudao Source E2E Result

- PASS: `芋道源码/admin` 在隔离 worktree 运行态打开 `生产填写`、切换三设备工序、填写数量和三个设备参数，并截图 `production-three-device-1920.png`。
- PASS: `芋道源码/admin` 切换无设备生产工序后看到“本工序无设备，直接填数量”，并截图 `production-no-device-1920.png`。
- PASS: `芋道源码/admin` 打开 `PQC填写`，看到生产订单、工序、员工、主页及全部可输入检验字段，并截图 `pqc-fill-1920.png`。
- PASS: E2E fixture marker `CODX-EDHR-FRONTLINE-20260730` 清理后 `mes_pro_route`、`mes_pro_process`、`mes_md_workstation`、`mes_dv_machinery` 任务自有残留均为 0。
- PASS: 复跑过程中未发送 MES 提交写请求，页面无 console/page error。

## Continuation Scope

- 用户要求继续直到 E2E 验证通过。
- 正式绑定来源限定为现有主数据链路：登录账号岗位、工作站人力、路线工序工作站、工作站设备、启用工艺路线。
- 真实 E2E 若需要数据，只允许使用可清理的任务自有夹具，不永久改写 admin 基线数据。
