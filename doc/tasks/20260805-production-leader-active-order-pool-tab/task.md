# 20260805 生产组长活跃订单池 Tab

## Task Goal

将生产组长页面的“活跃订单池”作为独立功能 Tab；Tab 内容使用统一标准列表模板展示全部活跃订单，并提供“新增活跃订单”按钮。新增时只输入生产工单“订单号”，通过远程候选下拉选择真实生产工单编号，后端按唯一有效排产解析正式路线和路线版本。

## Milestones

- [x] 识别生产组长页面、统一列表模板和现有活跃订单接口
- [x] 编写并运行聚焦 RED 静态合同
- [x] 记录订单号加入需求变更、BDD 和 RED/GREEN 证据
- [x] 实现候选搜索端点、workOrderId-only 新增接口和服务端路线解析
- [x] 实现单字段远程下拉弹窗，并拆除新增动作中的调拨关联输入
- [x] 更新静态合同和真实 E2E 脚本，拆分调拨追溯只读验收
- [x] 修复未选择真实候选时加入活跃订单可能向后端提交空 `workOrderId` 的回归
- [x] 修复只输入完整订单号但未点候选时仍向后端提交空 `workOrderId` 的截图回归
- [x] 使用 `芋道源码/admin` 走生产组长页签聚焦真实 E2E，证明新增请求体仅包含 `workOrderId`
- [x] 修复本机 `48081` 旧运行 Jar 缺少 `/process-config/list` 导致“请求地址不存在”的运行态回归
- [x] 候选下拉按可加入前置条件排序，并用绿色“符合要求”标识可加入工单
- [x] 修复候选下拉 eligibility 逐条查询导致 loading 长时间不结束的运行态回归
- [x] 在生产组长页签右侧显示当前生产组长负责的工艺路线名称
- [x] 复制按压式球囊扩充压力泵路线并关联目标产品
- [ ] 完成写入型真实 E2E、证据归档、清理、提交与推送

## Expected Verification

- `workdir=IntRuoyiFronted; node tests/e2e/production-leader-active-order-pool-tab-static.spec.js`
- `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/team-leader-process-config-unified-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `workdir=IntRuoyiFronted; node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js`
- `workdir=IntRuoyiFronted; node --check tests/e2e/team-leader-workbench-real-flow.e2e.js`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=IntRuoyiFronted; node tests/e2e/team-leader-workbench-real-flow.e2e.js`，必须使用测试生产组长和任务自有已确认工单；当前缺少 `TLW_*` 前置时记录 BLOCKED。
- `workdir=IntRuoyiFronted; ACTIVE_ORDER_E2E_BASE_URL=http://127.0.0.1:8081 ACTIVE_ORDER_E2E_WORK_ORDER_CODE=881MO093613 node tests/e2e/production-leader-active-order-focused.e2e.js`，使用 `芋道源码/admin` 真实页面路径验证订单号下拉选择和新增请求体；当前本机无完整 QA 规程覆盖候选时记录 BLOCKED。
- `workdir=IntRuoyiFronted; node test-results/process-config-route-focused/process-config-route-focused.e2e.cjs`，使用 `芋道源码/admin` 真实页面路径验证“工序配置”页签和 `/process-config/list` 运行态路由。
- `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderProcessConfigServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs/changes/20260806-active-order-code-input.md`
- `git diff --check`

## Current Status

blocked - 本轮已修复“加入活跃订单池提示 `请求参数不正确:不能为null`”回归，并补齐“只输入完整订单号未点候选”精确解析路径；活跃订单聚焦静态合同、`pnpm ts:check` 与目标 `git diff --check` 已通过。2026-08-06 17:18 使用 `芋道源码/admin` 在生产组长页签完成聚焦真实 Playwright 路径：远程下拉选择 `881MO093613` 后新增请求体为 `{"workOrderId":925868}`，旧 null 参数校验已消失；后端进入正式 PQC 任务生成前置并因缺少已发布 QA 规程阻塞，事务回滚后活跃订单、工序快照、PQC 任务残留均为 0。2026-08-06 19:04 又修复本机 `48081` 旧运行 Jar 缺少 `/mes/pro/process-pool/team-leader/process-config/list` 的运行态回归：新运行 Jar `backend-runtime-process-config-list-autowired-20260806-183405.jar` 已启动，健康检查 `UP`，只读 Playwright 登录 `芋道源码/admin` 后点击“工序配置”得到 HTTP 200、业务码 0。2026-08-06 后续用户复现 `activeOrderId=33` 同一 PQC 报错；只读 DB 确认候选 `881MO093613` 的 `吹球囊成型` 工序没有任何匹配已发布 QA 规程且排产工序计划日期为空，`activeOrderId=33` 未残留。2026-08-06 20:31 已补齐候选 eligibility 只读评估：候选接口返回 `eligible/ineligibleReason`，可加入候选稳定排在前面，前端下拉用绿色“符合要求”标识；目标前端静态合同、后端 JUnit 29 tests、`pnpm ts:check` 和目标 `git diff --check` 均通过。2026-08-06 21:36 修复候选 eligibility 逐候选逐工序 N+1 查询导致下拉长时间 loading 的风险：后端改为一批加载候选排产、排产工序、QA 规程版本和项目后内存判定，新增回归单测从缺少批量 item mapper 的 RED 转为 GREEN；本机 `48081` 已刷新至 `backend-runtime-active-order-candidate-batch-20260806-213525.jar`，真实页面输入 `88` 后候选接口 HTTP 200、业务码 0、约 3.2 秒返回 20 条、`loadingCount=0`。本机只读统计显示已确认工单 4,338 条、唯一有效排产 55 条、完整 QA 规程覆盖可新增候选 0 条；按无 fallback / 无造数规则，完整新增 PASS 仍阻塞，未执行 cleanup apply、提交或推送。2026-08-06 追加数据变更：已将 `球囊扩张压力泵` 工艺路线复制为 `RT000028-IDI / 按压式球囊扩充压力泵`（routeId=980091），并将 3 个同名目标产品全部关联到该新路线；只读复核显示这些目标产品当前仍无生产工单和排产工单。

- 2026-08-06 追加完成生产组长页签右侧负责路线名称展示：路线名来自正式 `/process-config/list` 的 `routeName` 并去重，不使用 `formBindings`、活跃订单、路线编码或路线 ID 推断；目标静态合同、`pnpm ts:check` 和目标 `git diff --check` 已通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，将活跃订单维护从班组配置职责中拆出，并让新增流程由后端从唯一有效排产解析正式路线，不再信任客户端路线/调拨输入。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#统一列表复合工具栏布局门禁`：活跃订单池必须显式复用 `UnifiedListTemplate`，新增操作放在模板 actions 区域，不使用页面级临时工具栏替代标准模板。
- `docs/frontend-development.md#前端角色内容页签拆分口径门禁`：本需求是生产组长页面内部功能模块 Tab，不新增主导航路由或 PQC 组长入口。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小静态合同完成 RED/GREEN，并单独记录全量类型检查结果。
- `docs/e2e-rules.md#element-plus-下拉选择门禁`：真实流程脚本必须点击 Element Plus 真实候选，不用自由文本或隐藏字段替代下拉选择。
- `docs/powershell-memory.md`：提交前按脏工作区基线、选择性暂存和提交后残余改动门禁执行。

## Cleanup Candidates

- doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md
- doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md
- IntRuoyiFronted/test-results/team-leader-workbench-real-flow/
