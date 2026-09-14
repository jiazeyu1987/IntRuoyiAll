# 一线 PQC 按项目代码下 QA 检验项目展示工序

## Task Goal

修正一线 PQC 的“选工序”列表来源：从当前活跃订单定位生产工单产品、产品对应工艺路线及路线对应项目代码，再读取该项目代码下正式 QA 检验项目，从检验项目中提取并去重工序后展示。

## Milestones

- [x] M1：确认用户要求、现有实现和历史门禁冲突。
- [x] M2：补充 BDD 场景与 RED 回归测试。
- [x] M3：实现项目代码到 QA 检验项目工序的正式读取链路。
- [x] M4：完成目标测试和相关回归验证。
- [x] M5：完成验证记录、经验沉淀与任务清理。
- [x] M6：通过本机 Playwright 真实页面验证一线 PQC 工序列表来源。
- [x] M7：按用户最新澄清改为“订单产品定位路线、路线绑定定位 DCC 项目代码”；RED/GREEN、相邻回归和真实运行态加载均已完成。
- [x] M8：按用户确认的“工序列表优先”拆分校验边界：列表原样返回空展示字段，检验详情和正式提交继续严格拦截。
- [ ] M9：按用户最新反馈修正“只显示一个工序”的回归，确保一线 PQC 显示 QA 对应的全部不重复工序，并完成回归与 E2E 验证。

## Expected Verification

- 后端回归证明候选工序只来自活跃订单产品对应路线、对应项目代码下的正式 QA 检验项目。
- 同一工序包含多个 QA 检验项目或重复规程记录时，候选列表只返回一次。
- 活跃订单工序快照或路线中存在、但项目代码下 QA 检验项目未覆盖的工序不进入列表。
- 项目代码、正式 QA 规程或工序身份缺失时显式失败，不使用活跃订单快照、路线全集或前端过滤补齐。
- 目标 Maven 测试、相关回归、bug evidence validator 与任务 cleanup 通过。
- 本机 `int_main` 运行态加载本次后端实现后，通过 Playwright 登录一线 PQC 页面，选择真实待检活跃订单并核对页面工序卡片与 `active-order/processes` 响应一致；不得调用 `/pqc/submit` 或其它持久化写接口。页面选择工序所必需的 `/pqc/switch-employee` POST 仅构造运行上下文，必须单独记录且确认后端无持久化。

## Cleanup Candidates

- `doc/tasks/20260809-frontline-pqc-qa-project-process-source/frontline-pqc-process-source.e2e.cjs`
- `doc/tasks/20260809-frontline-pqc-qa-project-process-source/frontline-pqc-qa-process-diagnose.mjs`
- `doc/tasks/20260809-frontline-pqc-qa-project-process-source/runtime-inspection/`
- `output/playwright/20260809-frontline-pqc-qa-project-process-source/`
- `output/runtime/int_main/backend-report-shared-allocation-20260809-v3.pre-pqc-route-project-20260809-215150.jar`
- `output/runtime/int_main/backend-report-shared-allocation-20260809-v4-pqc-route-project-20260809-220156.jar`
- `output/runtime/int_main/backend-report-shared-allocation-20260809-v4-pqc-route-qa-product-20260809-223211.jar`
- `output/runtime/int_main/backend-report-shared-allocation-20260809-v4-pqc-route-qa-legacy-fields-20260809-225237.jar`
- `output/runtime/int_main/backend-report-shared-allocation-20260809-v4-pqc-route-qa-legacy-fields-20260809-225651.jar`
- `output/runtime/int_main/hotpatch-work-pqc-route-qa-legacy-fields-20260809-225237/`
- `output/runtime/int_main/hotpatch-work-pqc-route-qa-legacy-fields-20260809-225651/`
- `output/runtime/int_main/hotpatch-work-pqc-process-list-priority-20260809-234242/`
- `output/runtime/int_main/logs/pqc-route-qa-legacy-fields-20260809-225651.stdout.log`
- `output/runtime/int_main/logs/pqc-route-qa-legacy-fields-20260809-225651.stderr.log`

## Cleanup Keep

- `output/runtime/int_main/backend-report-shared-allocation-20260809-v4-pqc-process-list-priority-20260809-234242.jar`
- `output/runtime/int_main/logs/pqc-process-list-priority-20260809-234242.stdout.log`
- `output/runtime/int_main/logs/pqc-process-list-priority-20260809-234242.stderr.log`

## Applicable Experience Gates

- 命中 `docs/backend-development.md#MES PQC 项目级检验快照门禁`：QA 检验项目必须来自正式发布规程和项目级结构化数据。
- `docs/backend-development.md#PQC 待检准入与工序选择必须分离` 已要求按 QA 检验项目展示，但其中“当前产品物料代码直接匹配 DCC 项目代码”被用户最新澄清覆盖；正式顺序改为当前订单产品定位唯一工艺路线，再从该路线全部产品绑定中定位唯一启用 DCC 项目代码。
- 当前工作区存在大量并行改动，本任务只修改一线 PQC 工序来源、对应测试与本任务文档，不回滚或清理其他任务改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以项目代码和正式 QA 检验项目建立唯一业务来源。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked：用户最新反馈“一线 PQC 工序列表现在只显示一个，需要显示 QA 对应的所有工序”。只读诊断确认代码路径在构造两个正式 QA 已发布工序时可返回两个，但真实本机数据中订单 `881MO090935` / `881MO090889` 的路线候选产品当前只有 `productId=902149` 存在 1 条 `MES_QA/PUBLISHED` QA 规程，且只绑定“清洗工序”。因此当前阻塞不是前端渲染或列表去重代码，而是正式 QA 发布数据只有一个工序；若业务确实需要显示更多工序，需要先在 QA 规程中按对应工序正式发布更多检验项目，或由用户明确改口为“显示路线全部工序”。
