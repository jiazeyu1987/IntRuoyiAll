# 排产闭环与 eDHR 边界开发计划

## 任务目标

基于最新排产需求和当前系统代码，形成可执行开发计划：明确现有 MES/ERP/排程/报工/eDHR 能力哪些可以复用，哪些会与 eDHR 或现有报工链路冲突，以及后续应如何分阶段开发，避免直接改生产工单或重排任务导致受控记录失真。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-feedback-attribution-product-model-reason/task.md`。
- 检查结果：该任务已标记 `completed`。
- 本任务复用前序 `20260609-next-scheduling-requirements`、`20260609-scheduling-order-mvp-design`、`20260609-feedback-excel-attribution-design` 的需求和设计结论，并补充当前代码现状与 eDHR 冲突分析。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务为计划和边界分析，明确缺 ERP 字段、缺路线、缺产能、缺承诺交期、报工未归属、eDHR 已执行任务重排等都必须显式阻塞或提示。
- `是否从根因和长期维护角度解决`：是。以排产工单作为排产业务边界，生产工单继续承接 ERP 来源，eDHR 继续承接受控执行记录，避免不同业务直接抢同一字段语义。
- `是否存在临时补丁或绕过`：否。本任务不改业务代码、不执行数据写入。用户已明确授权后续开发验证阶段可将 `芋道源码/admin` 中已有相关业务数据受控平移到 `测试租户`，并补齐缺失数据；该动作必须以测试租户为写入目标、保留 SQL/脚本证据，不作为绕过真实前端 E2E 的替代。

## BDD 场景

- BDD: 排产工单承接排产决策 -> Given ERP 同步生产工单已存在 / When 排产员选择生产工单并填写承诺交期 / Then 系统生成唯一排产工单，排产数量等于生产工单数量，ERP 后续变更只生成差异提示。
- BDD: eDHR 执行上下文不被重排破坏 -> Given 某生产任务已经打开 eDHR 批记录或单工序批记录 / When 夜间重排执行 / Then 该任务不被删除、移动或覆盖，相关执行快照、签名、审批、归档仍可追溯。
- BDD: 外部 MES Excel 报工先待归属 -> Given 班组长导入外部 MES Excel / When 系统解析 Excel / Then 只生成待归属记录，不直接创建正式报工，不更新排产工单进度。
- BDD: 工艺路线下维护排产资源配置 -> Given 排产员在工艺路线页面查看某路线 / When 编辑排产相关资源、人工人数、班次小时和当日维修/增减设备 / Then 系统只影响排产配置和日资源调整，不改变批处理/eDHR 基础工艺执行记录。
- BDD: 测试租户真实数据补齐 -> Given `芋道源码/admin` 中已有排产相关主数据且测试租户缺少可验证订单或报工样本 / When 开发验证需要真实数据 / Then 系统只将必要路线、产品、设备、工位、生产工单、BOM、报工样本按受控映射写入测试租户，并记录来源、目标、数量和回滚方式。

## 里程碑

- [x] M1：读取前序排产需求、排产工单池设计、报工归属设计和范围变更分析。
- [x] M2：检查当前 MES 后端/前端代码中的生产工单、自动排程、工艺路线资源、报工导入、eDHR 执行上下文。
- [x] M3：形成可复用能力、冲突点和分阶段开发计划。
- [x] M4：记录验证证据并给出下一步建议。
- [x] M5：按用户补充授权，补齐测试租户数据准备策略和当前本地数据盘点。

## 预期验证

- `rg --no-ignore -n "排产工单|eDHR|待归属|重排|生产工单|承诺交期" ruoyi-vue-pro/doc/tasks/20260610-scheduling-edhr-development-plan`
- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence ruoyi-vue-pro\docs\changes\20260609-scheduling-scope-change-edhr-impact.md`

## 当前状态

completed

## 完成记录

- 已确认当前系统已有生产工单、金蝶生产订单同步入口、工艺路线/工序/产品关联、工位设备与人工产能、自动排程预览/应用/重排、排程日历、生产任务、生产报工、第三方报工导入、设备维修和 eDHR 批记录执行能力。
- 已确认关键缺口是：缺正式排产工单业务边界、缺排产工单工序明细、缺 ERP 幂等更新差异处理、缺报工待归属链路、缺按排产工单进入排程和重排保护 eDHR 的实现。
- 已确认 eDHR 冲突集中在 `workOrderId`、`taskId`、`routeId/routeProcessId` 和受控快照：夜间重排不得删除/移动已关联 eDHR 或已归属报工的任务；排产配置不得污染批处理/eDHR 配置。
- 已新增开发计划文档：`development-plan.md`。
- 已按用户授权补充测试数据策略：开发和 E2E 写入目标为测试租户；可从 `芋道源码/admin` 只读提取必要主数据与样本订单，按租户映射平移到测试租户，缺什么补什么；最终 `芋道源码/admin` 只做只读验证。
- 当前本地数据盘点显示：路线、工序、设备、工位在 admin 与测试租户基本对齐；测试租户缺口主要在生产工单 BOM、报工导入/正式报工样本和排产工单相关新表。

## 最终验证

- GREEN: 本地数据库只读盘点 -> PASS，使用当前后端实际库 `127.0.0.2:23306/ruoyi-vue-pro` 查询关键 MES/eDHR 表在 tenant `1` 与 `122` 的有效行数，未执行写入。

## Cleanup Keep

- `doc/tasks/20260610-scheduling-edhr-development-plan/task.md`
- `doc/tasks/20260610-scheduling-edhr-development-plan/execution-log.md`
- `doc/tasks/20260610-scheduling-edhr-development-plan/development-plan.md`
