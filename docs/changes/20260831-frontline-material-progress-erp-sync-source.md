# 工序多物料进度与批号来源变更记录

## Request Summary And Source

- 来源：用户 2026-08-31 当前线程确认。
- 进度口径：多物料完成数量不一致时直接取最小值。
- 批号口径：不直连 ERP；系统每天已同步 ERP 表单，页面只从系统内同步数据读取；系统没有批号就保持没有。

## Current Baseline Reviewed

- PRD 原设计把批号描述为 ERP 回填，并预留了内部回填接口方向。
- 实施设计原建议按 `完成数量 / BOM 比例` 取最小值。
- 当前代码库每日同步表中，只有 `erp_kingdee_production_pick_list_item` 同时具备 `production_order_no + material_number + lot_number`；库存表缺生产订单身份，库存移动表也缺生产订单身份，不能准确归属本次报工。
- 当前未提交迁移曾在 `mes_pro_feedback_material` 预留 ERP 批号回填字段，尚未融合或部署。

## Classification

- Requirement change：工序进度从“待确认/BOM 比例折算”明确为“直接取完成数量最小值”。
- Data source constraint：批号来源限定为系统内每日同步事实，不允许实时 ERP 调用或本地模拟回填。

## Impact Analysis

### Product Impact

- 弹簧 `5`、杠杆 `3` 时，本次工序进度为 `3`。
- 页面批号可能为空；空值是正式同步现状，不显示占位或错误成功。
- 同一订单物料若同步表存在多个非空批号，保留全部去重批号，避免任取一个。

### Design Impact

- 删除内部 ERP 回填接口设计。
- `mes_pro_feedback_material` 只保存一线报工事实，不复制 ERP 批号。
- 查询报工物料详情时，按生产工单编号和物料编码读取同步领料单明细。
- 工序需要哪些批记录物料不再从产品 BOM 或用料比例自动推导；MVP 在工艺路线候选版本的当前工序“批记录表单”字段明细中维护 `frontlineReportMaterialIds`，随路线版本审批和发布形成快照。

### Data Impact

- 新增事实表移除 `erp_batch_code/erp_receipt_no/erp_batch_status/erp_batch_returned_at`。
- 不写入、不更新 ERP 同步表。
- 无同步数据时返回空批号集合。

### API Impact

- 正式提交仍一次携带全部物料。
- 物料详情响应增加只读 `batchCodes[]`，来源为系统内部同步表。
- 不新增 ERP 写入或回调 API。

### Test Impact

- 进度测试必须覆盖直接最小值以及 `0` 最小值。
- 批号读取测试覆盖单批号、多批号去重、空数据、其它订单/物料隔离。
- 迁移测试必须确认不再包含本地 ERP 回填字段。

### Release And Operations Impact

- 不需要 ERP 网络、凭据或回调配置。
- 依赖现有每日 ERP 表单同步任务；同步失败时批号自然为空，不切换数据源。
- 发布仍需要新增事实表迁移和应用代码同时上线。

## Decision

ACCEPT。用户是当前需求提出人，并明确确认两个业务口径；本次变更直接纳入当前 MVP，不拆分到后续版本。

## Required Approvals

- 产品口径：用户当前消息已批准。
- 无远端数据库或 ERP 外部写入授权需求；本任务只读系统内部同步表。

## Downstream Skill Reruns

- Product requirements：更新 PRD、用户流程和验收标准。
- BDD/TDD：增加直接最小值和内部同步批号读取场景。
- Database schema delivery：更新迁移和迁移合同。
- Backend API delivery：实现最小值进度、同步表批号读取和物料详情响应。
- Frontend feature delivery：展示只读批号集合；无数据保持空。
- Playwright：真实页面验证物料页签、最小值提交请求和同步批号展示。

## Blockers And Next Action

- 当前无业务口径阻塞。
- 下一步先更新下游文档和 RED 测试，再实现正式提交与内部同步批号读取。
