# Execution Log

## User Intent

用户要求在 worktree 中实现已设计的一线生产活跃订单归属功能，验证成功后融合进 `int_main`。业务口径是允许员工超出订单数量报工，超出部分由生产组长在现有报工管理和订单分配流程中调整。

## BDD Evidence

BDD: 一线生产顶部展示三个选择入口 -> Given 一线生产页面已加载 / When 员工进入页面 / Then 顶部依次显示活跃订单、工序、员工和最大化。

BDD: 生产活跃订单选择体验与 PQC 一致 -> Given 存在多个活跃订单 / When 打开活跃订单选择 / Then 支持订单号搜索、候选详情及完整加载状态。

BDD: 正式报工归属选中订单 -> Given 选中订单 A、工序 P、员工 E / When 正式提交 / Then 报工记录和工序池事件的 `workOrderId` 都是 A。

BDD: 超过订单数量仍允许报工 -> Given 订单数量 100 且本次完成 200 / When 正式提交 / Then 提交成功且不截断、不自动分给其他订单。

BDD: 生产组长红色识别并调整超报 -> Given 订单 A 数量 100 且报工 200 / When 组长查看报工管理 / Then 显示红色待调整数量 100，并可通过现有分配入口调整至其他订单。

BDD: 订单工序不匹配时阻塞 -> Given 订单 A 不包含工序 P / When 正式提交 / Then 明确提示上下文不一致且不创建记录。

## Milestone Updates

- M1：已完成。worktree 为 `D:\IntRuoyiWorktree\frontline-production-active-order`，分支为 `codex/frontline-production-active-order`。
- M2：已完成。三个前端验收合同均先失败，失败原因与设计预期一致。
- M3：已完成。生产活跃订单接口、选择态、提交归属校验和组长待调整展示均已落地。
- M4：已完成可执行验证。自动化、编译和只读真实页面均通过；写入型 E2E 因缺少任务专用前置而阻塞。

## Verification Evidence

RED: `node tests/e2e/frontline-production-active-order-picker-static.spec.cjs` -> FAIL, 一线生产顶部缺少活跃订单选择入口。

RED: `node tests/e2e/frontline-production-active-order-submit-attribution-static.spec.cjs` -> FAIL, 正式提交仍从运行配置读取订单而非用户选中订单。

RED: `node tests/e2e/team-leader-report-overage-highlight-static.spec.cjs` -> FAIL, 生产组长报工管理缺少超报红色待调整标识与选中订单分配预填。

RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesFrontlineActiveOrderControllerTest#getProductionActiveOrders_returnsOnlyResponsibleLeaderOrders test` -> FAIL, 生产设备账号控制器缺少正式生产活跃订单接口。

RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesProFrontlineFeedbackSubmitServiceTest#shouldAssignProductionSubmitToSelectedActiveOrderWithoutQuantityCap test` -> FAIL, 提交授权服务缺少选中订单有效性与工序映射校验合同。

RED: `node tests/e2e/frontline-production-active-order-real-readonly.e2e.cjs` -> FAIL, 活跃订单入口未纳入顶部统一选择区域契约，真实页面只能识别工序和员工两项。

GREEN: `node tests/e2e/frontline-production-active-order-picker-static.spec.cjs` -> PASS。

GREEN: `node tests/e2e/frontline-production-active-order-submit-attribution-static.spec.cjs` -> PASS。

GREEN: `node tests/e2e/team-leader-report-overage-highlight-static.spec.cjs` -> PASS。

GREEN: `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS，PQC 原有活跃订单切换未回归。

GREEN: `node tests/e2e/team-leader-report-allocation-static.spec.cjs` -> PASS，现有组长订单分配链路未回归。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesFrontlineActiveOrderControllerTest,MesProFrontlineFeedbackSubmitServiceTest,MesFrontlineSubmitAuthorizationTest,MesFrontlineSubmitIdentityTraceTest" test` -> PASS，23 tests，failures 0，errors 0。

GREEN: `pnpm.cmd ts:check` -> PASS。

GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS，30/30 reactor modules。

GREEN: `node tests/e2e/frontline-production-active-order-real-readonly.e2e.cjs` -> PASS，工作树 `8100/48100`，真实登录 `芋道源码/admin`，一线生产订单弹框与生产组长报工管理页面可见，MES 写请求为 0，页面错误为 0。

REGRESSION: 扩展静态回归中 `frontline-production-repeat-submit-static.spec.cjs`、`frontline-production-maximize-runtime-cache-static.spec.cjs`、`team-leader-report-allocation-dialog-hide-static.spec.cjs` 失败；核对为当前基线既有合同与现有实现不一致，本任务未改动对应行为，未作为本任务回归处理。

E2E-BLOCKED: 写入型真实 200 件报工与组长调整 -> BLOCKED，当前进程未提供 `TLW_TENANT`、`TLW_USERNAME`、`TLW_PASSWORD`、任务自有订单/任务/路线/工序/物料/员工/设备/记录本/签名/审批人/报工类型等真实 E2E 前置；影响是未在浏览器中产生真实超报和分配写入，不影响单元测试已覆盖的 100 件订单报 200 件仍成功、归属选中订单的服务行为。

## Blockers

- 写入型真实 E2E 前置缺失，不能使用默认 admin 基线数据替代；已保留只读真实页面证据和精确缺失清单。
