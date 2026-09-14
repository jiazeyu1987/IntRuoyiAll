# 活跃订单放行资料 V4 监督交付

## Task Goal

以 V4 最终开发方案和已通过的 M0 契约为唯一实现基线，组织 A1-A6 六个子 Agent 完成前端、申请编排、三类正式资料 writer、fixture 与真实 E2E；主 Agent 逐项审查、组织独立测试和缺陷返修，最终完成集成与集成测试。

## Milestones

- [x] 复核 V4、M0、验收标准、BDD/TDD/E2E 和当前代码基线。
- [x] A2 完成“当前 apply 未调用 A3/A4/A5”严格 RED。
- [x] A3 完成批记录 writer 并独立验证。
- [x] A4 完成过程检验单 writer 并独立验证。
- [x] A5 完成损耗单 writer 与完成性检查并独立验证。
- [x] A2 完成编排、双 100%、canonical hash、事务和待办集成。
- [x] A1 完成前端契约硬化。
- [ ] A6 完成 fixture manifest、真实页面 E2E 和只读核验。
- [ ] 主 Agent 完成全量集成、独立测试、缺陷闭环和最终审计。

## Expected Verification

- 每个 AC-01 至 AC-15 均有执行证据和独立测试证据。
- 后端聚焦 JUnit、静态合同、schema、compile 通过。
- 前端静态合同和 `pnpm ts:check` 通过。
- 真实 Playwright 路径证明生产/PQC 历史可见、自然双 100%、三类正式资料、签名、唯一负责人待办和最终放行/驳回。
- 同来源重复申请不重复创建 batch、execution、审计、transaction 或 work task。
- 任一正式来源、映射、签名、QA 版本、负责人或真实页面入口缺失时 fail fast，不得记录假 PASS。

## Current Status

in_progress

A1-A5 的既有实现与聚焦回归保持通过。当前 P7/A6 目标已冻结为 tenant `1` 的 `902149 / AW.107.02.01.2010 / 球囊扩张压力泵`，路线为 `922119 / RT000028 / V29 / routeVersionId=632 / ACTIVE`，当前 14 个工序均有传统 MAIN、`form_template_id=28` 过程检验和 `form_template_id=25` 损耗表单绑定。用户已明确允许两类动态表单作为 A4/A5 正式目标载体，MAIN 仍只使用逐工序传统批记录绑定。

P7 当前为 `BLOCKED / NOT COMPLETE`。真实只读 preflight 已使用用户授权的单一 admin 模式运行，并在首次业务写前返回 `DYNAMIC_FORM_TEMPLATE_SNAPSHOT_INVALID`：14 条过程检验和 14 条损耗绑定的 `record_category_snapshot_hash`、`slot_config_snapshot_hash` 全部为空。2026-08-11 已通过正式 API 保存模板 28/25 的摘要字段规则；后续已知前置仍包括 13/14 最新 PUBLISHED QA 存在必需设备关联缺失、动态 FormCenter 自动写入/提交/原始签名/完成性证据链尚未经过真实 E2E 证明，以及绑定候选用户 `149/152` 与 admin 执行账号不一致。门禁副作用为业务页面写 `0`、业务 API 写 `0`、SQL 写 `0`、manifest `0`；真实业务 E2E 尚未开始。

P3/A4 已补齐 PROCESS_INSPECTION template 28 正式动态目标：QA reader 按 `productId + routeId + stable processId` 选择最新 PUBLISHED QA，并通过显式 DCC 项目身份校验；writer 可将当前 batch task 已关联的 FormCenter instance 写入、提交为 EFFECTIVE，并返回提交快照 ID、head hash、来源 hash 与原始 PQC/复核签名证据。标准 Maven 聚焦 `MesTeamLeaderActiveOrderReleaseProcessInspectionDynamicFormPortImplTest`、`MesTeamLeaderActiveOrderReleaseProcessInspectionReaderTest`、`MesTeamLeaderActiveOrderReleaseProcessInspectionWriterTest` 合计 `18/18` PASS。动态绑定快照空值的保存根因也已修复，标准 Maven `MesProRouteFlowConfigServiceImplTest` `43/43` PASS。该代码尚未进入当前 int_main 稳定运行包，既有 V29 不做 SQL 回填；后续需通过正式页面发布下一候选版本取得有效 hash。

2026-08-11 主线已按用户要求完成“先对应”的只读核对：`902149 / AW.107.02.01.2010 / 球囊扩张压力泵`、路线 `922119 / V29`、14 个工序 PI/LOSS 绑定关系明确。当前 48081 已通过正式 API 保存摘要字段规则：`FORMTPL:32` total=10/workOrder=1/pqc=9，`FORMTPL:27` total=6/workOrder=1/loss=5；但 PI 绑定的 template 28/V3.0 正文标识 `PQC-IDPR-001 / 按压式球囊扩张压力泵`，与该产品从正式路线解析出的 DCC `ID / 球囊扩张压力泵` 不一致，因此该 PI 映射不可放行。后端三重 fail-fast 门禁已由主审和独立 tester 25/25 验证，A2-A5 集成回归 68/68 PASS，前端合同与 `pnpm ts:check` PASS。实时门禁再次确认正式 PQC-ID QA 与完整设备均仅 1/14（缺 44 条设备关联），PI/LOSS 两类 hash 均 0/14，业务副作用为 0。P7 需正式发布 ID-compatible PI 模板并重绑/重映射，再补 13 条 QA provenance/设备、下一候选 hash、fixture manifest 和真实页面 E2E。

2026-08-13 根据用户澄清更新 PRD：批记录单元格链接页面当前只做逐工序对应关系配置，不在配置保存或一线生产提交时生成正式批记录数据；真正的数据生成发生在生产组长点击“申请放行”时。重复行数量由目标表单实际结构和用户选择的重复行组决定，不全局写死为 4；配置阶段不以中间数量一致性或复核人时间推导作为 blocker。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。正式来源或环境前置缺失时阻塞，不使用 mock、SQL 直改、API-only 或默认 MAIN 替代；动态 formBindings 仅按本轮明确合同作为 PI/LOSS 正式目标，不替代 MAIN 批记录。
- `是否从根因和长期维护角度解决`：是。复用当前 eDHR、正式批记录、PQC 汇集、字段审计、放行事务和 RELEASE_APPROVE，不新增平行流程。
- `是否存在临时补丁或绕过`：否。

## BDD Scenarios

- BDD: 真实历史形成双 100 后申请 -> Given 生产/PQC 正式历史、签名、三类绑定和负责人完整; When 生产组长真实页面申请; Then 三类资料生成、完成性/precheck 通过并创建唯一负责人待办。
- BDD: 正式来源缺失时阻塞 -> Given 缺任一批记录绑定、PQC CONFIRMED 汇集、损耗映射、签名或负责人; When 申请; Then 返回定位 blocker，生成事务无部分资料且无待办。
- BDD: 重复申请幂等 -> Given 同一来源快照已申请; When 重复提交; Then 返回既有申请且不重复生成任何正式对象。
- BDD: 负责人处理 -> Given 申请处于 PENDING_RELEASE_APPROVAL; When RELEASE_APPROVE 负责人从真实页面批准或驳回; Then eDHR 放行事务进入 RELEASED 或 REJECTED 并保留事件审计。

## Applicable Experience Gates

- `docs/backend-development.md#活跃订单申请放行资料必须只使用正式来源`
- `docs/backend-development.md#批记录单元格链接预填落库边界`
- `docs/backend-development.md#edhr-放行负责人来源门禁`
- `docs/backend-development.md#mes-pqc-项目级检验快照门禁`
- `docs/frontend-development.md#前端写入成功与列表刷新失败分层门禁`
- `docs/e2e-rules.md` 的真实页面、任务自有数据和禁止 API-only 门禁
- `docs/powershell-memory.md#Maven-目标目录文件系统异常门禁` 与 `#Maven-javac/Lombok-class-写入长时间运行门禁`

## Cleanup Keep

- doc/tasks/20260809-active-order-release-dossier-v4-delivery/task.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/request-analysis.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/prd.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/development-plan.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/test-plan.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/task-state.json
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/execution-log.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/test-report.md
- doc/tasks/20260809-active-order-release-dossier-v4-delivery/verification-report.md
