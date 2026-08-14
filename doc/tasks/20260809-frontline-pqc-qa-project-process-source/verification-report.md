# 一线 PQC QA 项目工序来源验证报告

## M9 Update

BLOCKED。用户最新反馈“当前只显示一个，需要显示 QA 对应所有工序”后，重新用真实登录态只读诊断订单 `881MO090935` 与 `881MO090889`：接口均只返回 `清洗工序`。同时，路线候选产品和精确 DCC 项目候选中只有 `productId=902149` 存在 1 条 `MES_QA/PUBLISHED` QA 规程，发布版本 `54` 只对应 `routeProcessId=980647` / `processId=922987` / `清洗工序`；其它候选产品未配置 QA 规程。后端回归 `shouldPreparePqcPieceDetailContextWithBulkQueriesOnly+shouldDisplaySubmittedPqcTaskProcessWithoutTaskContextAndKeepPendingProcess` 通过，证明正式 QA 数据有两个工序时接口能返回两个。当前阻塞点是正式 QA 发布数据只有一个工序，不是前端列表渲染或代码去重错误。

## Verification Result

PASS。新代码已加载本机 `48081`。一线 PQC 工序来源为：活跃订单产品定位正式路线 -> 路线绑定代码定位唯一启用 DCC 项目 -> 路线绑定物料 ID 与 DCC `productMasterId` 共同作为 QA 产品候选 -> 当前路线/版本唯一命中正式 `MES_QA/PUBLISHED` QA 产品 -> 从检验项目提取并按 `routeProcessId + processId` 去重工序。用户确认的“工序列表优先”已实现：历史 `inspectionTool/samplingPlanText` 为空不阻断列表，详情和提交继续严格拦截。

## Verified Behavior

- 订单产品代码只用于确认当前路线；DCC 项目从该路线全部正式产品绑定物料代码中唯一解析，未绑定当前路线的其它 DCC 项目不参与。
- 路线存在额外工序、但 QA 检验项目未覆盖时，额外工序不进入一线 PQC 列表。
- 同一 QA 工序存在重复规程结果或多个检验项目时，只返回一次工序。
- `PENDING` 任务只附着到已有 QA 候选工序，不扩展候选集合。
- 正式任务上下文使用唯一命中的 QA 产品查询规程，并继续校验 `MES_QA/PUBLISHED`、路线、版本和工序身份。
- 项目代码、DCC 产品主数据、正式规程、检验项目或工序身份缺失时显式失败，不回退到活跃订单快照、路线全集或前端补齐。
- QA 产品候选同时包含路线正式绑定物料 ID 和唯一 DCC 项目的 `productMasterId`，再按当前路线/版本过滤发布规程并要求唯一命中一个 QA 产品；真实数据唯一命中 `productId=902149`。
- 历史展示原文为空时，工序列表原样携带空值；打开检验方法详情或提交时明确失败，不补默认值。

## Evidence

- BDD、两轮 RED、GREEN 和阻塞解除证据：`execution-log.md`。
- 目标 GREEN：`MesFrontlinePqcContextServiceTest#shouldListDistinctQaInspectionItemProcessesFromRouteProjectCode`，1/1 通过。
- 标准 Maven 相邻回归：11 个一线 PQC 工序来源、任务附着、空展示字段和上下文场景，11/11 通过。
- 前端边界及相邻静态合同：3/3 通过。
- 前端类型检查：`pnpm ts:check` 通过。
- 生产编译：`mvn -pl yudao-module-mes -DskipTests compile`，BUILD SUCCESS。
- 长期门禁：`docs/backend-development.md#PQC 待检准入与工序选择必须分离` 与 `docs/experience-index.md` 已更新。

## Independent Verification Audit

2026-08-10 重新按当前工作区与当前运行态验收，结果仍为 PASS：

- 代码核对：后端“工序列表”路径使用 `toProcessListInspectionItem` 原样映射展示字段；详情/提交路径仍使用严格字段校验。前端详情打开、签名前校验和提交 payload 构造均调用正式展示字段校验。
- 运行态核对：`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`；监听 PID `47284`；运行 Jar 为 `backend-report-shared-allocation-20260810-final-20260810-002331.jar`。
- 前端静态合同：`frontline-pqc-process-list-display-field-boundary-static.spec.cjs`、`frontline-pqc-sampling-equipment-dialog-static.spec.cjs`、`frontline-pqc-fact-dialog-static.spec.cjs` 全部 PASS。
- 前端类型检查：`pnpm ts:check` PASS。
- 后端回归：一线 PQC 11 个相关场景 PASS，`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`，完成时间 `2026-08-10T01:06:51+08:00`。

## Verification Scope

未执行整个 MES 模块全量测试；验证范围为目标场景及 10 个相邻一线 PQC 场景、3 个前端静态合同、前端类型检查和真实 E2E。

## Git

用户未要求 Git 操作；未暂存、未提交、未推送。

## Closeout

`task-closeout-cleanup` 最终 apply 已通过。一次性 E2E/evidence/旧热补丁产物已清理；核心三份任务记录、正式回归测试和生产代码已保留。2026-08-10 又对当前最终运行态完成一次真实页面复验，临时复验脚本已删除。

## E2E Extension

### Result

`E2E PASS`，通过真实前端用户路径完成，不是 API-only 替代验证。

### Real Path Evidence

- 入口：`http://127.0.0.1:8081/mes/pro/feedback/edhr-batch-pqc-fill`。
- 登录：真实前端 `芋道源码/admin` 登录成功；密码和 token 未写入证据。
- 活跃订单：`GET /admin-api/mes/pro/feedback/frontline/device-account/pqc/active-orders` -> HTTP 200、业务码 0。
- 当前运行态已加载路线项目代码和 QA 产品候选修复；原业务码 `1040760103`、`matchedProjectIds=[]` 已消失。
- 登录态路线只读复核：路线 `922119` 正式绑定代码为 `ID`、`AW.107.02.01.2010`、`AW.107.02.01.1009`、`AW.107.02.01.2036`。
- 登录态 DCC 只读复核：上述路线代码中唯一启用精确匹配为 `ID`，`projectId=147`、`productMasterId=11`；无需创建任何 `AW.*` 同名 DCC 项目。
- 工序接口：HTTP 200、业务码 0；页面与接口均只有 `1. 清洗工序`，正式身份 `980647:922987`，无重复。
- 历史展示字段边界：检验详情未打开，提交签名弹窗未打开。
- 写入保护：仅调用非持久化运行上下文 `/pqc/switch-employee`；`persistentMesWrites=[]`。
- 浏览器状态：`targetFailures=[]`、`pageErrors=[]`、`consoleErrors=[]`。
- 当前最终运行态复验：`backend-report-shared-allocation-20260810-final-20260810-002331.jar`、PID `47284`、health `UP`；订单 `881MO090935` 的接口工序为 `["清洗工序"]`，页面工序为 `["1. 清洗工序"]`，展示字段缺失边界仍为 `detailBlocked=true`、`submitBlockedBeforeSignature=true`，且 `persistentMesWrites=[]`。

### Blocking Precondition

无阻塞前置。`48081` 当前运行 `backend-report-shared-allocation-20260810-final-20260810-002331.jar`，PID `47284`，health `UP`。

严格 no-fallback 生效：没有改用活跃订单工序快照、路线全部工序、待检任务集合、模拟数据、SQL 修补或 API-only 成功来掩盖缺失前置。

### Current Closeout State

任务状态为 `completed`。当前为主工作区，用户未要求 Git 操作；未提交、未合并、未推送。
