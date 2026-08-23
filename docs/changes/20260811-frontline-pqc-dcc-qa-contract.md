# 一线 PQC 按 DCC 项目代码读取 QA 规程契约变更

## Request Summary And Source

- Request source: 当前线程用户连续确认并最终批准“其他按推荐值”。
- Request: 一线 PQC 工序列表必须按 `活跃订单 -> 已锁定生产路线 -> 正式路线-DCC关系 -> 唯一DCC项目代码 -> QA规程 -> 订单锁定QA发布版本 -> QA自有工序 -> QA检验项目` 读取。
- QA 规程只对应 DCC 项目代码；QA 不再对应产品、工艺路线、路线版本、MES 工序或路线工序，也不得对这些对象做存在性校验。
- 全部有效活跃订单均应显示，PQC 任务状态只能叠加展示，不能过滤订单、QA 工序或检验项目。

## Current Baseline Reviewed

- 当前工作树已存在 `mes_qa_inspection_regulation.dcc_project_code_id`、QA process、按inspectionType多行item、task的qaProcessId/versionId和QA五个DCC-ID接口；本设计必须复用，不能重新创建。
- 一线active-order列表仍缺activeOrderId，并存在PENDING筛选、workOrder/route去重及当前产品路线再准入；工序查询仍接workOrderId+routeId。
- active-order尚无DCC/regulation/version不可变快照；现有路线-DCC正式关系及管理页面尚缺。
- QA管理前端仍残留产品/路线payload与循环发布；DCC项目列表需要通过MES batch status组合QA列。
- PQC人员switch无状态，submit controller把actualEmployeeId覆盖为loginUserId；QA发布NUMERIC与提交/纠正/放行的NUMBER存在断裂。
- removed active order会复用同一ID恢复但不重建task；历史迁移和恢复语义必须显式冻结。

## Classification

- Requirement change.
- Domain boundary correction.
- Persistent data-contract migration.

## Impact Analysis

- Product Impact: 三个有效活跃订单均进入选择列表；选择订单后显示订单锁定 QA 版本中的全部 QA 自有工序和检验项目。
- Design Impact: MES 生产路线只负责定位 DCC 项目代码；QA 领域从 DCC 项目代码开始，禁止反向依赖 MES 产品与工序。
- Data Impact: 只新增路线-DCC正式关系及active-order三个QA快照；复用现有QA-DCC/process/item/equipment/task结构。本次保留历史产品/路线/工序审计列，不建DCC-QA binding、item-type或订单context表。
- API Impact: 一线工序查询只接收activeOrderId；QA五接口保持现有DCC-ID合同；新增路线-DCC GET/PUT/DELETE；DCC后端响应不加QA字段，由前端批量调用MES project-statuses组合。
- Test Impact: 删除PENDING过滤、workOrder/route去重、当前产品路线再准入和产品/路线QA断言；新增路线配置UI、版本锁定/重新激活、规则真值表、实际员工签名、NUMERIC、事件归属、幂等和迁移回填回归。
- Release Impact: 需要维护窗口内完成正式迁移和代码切换；禁止双读、兼容推算或旧接口 fallback。
- Operations Impact: 发布前必须盘点无法唯一迁移的历史 QA 工序和 PQC 任务，使用经批准的显式映射清单；缺少清单时阻塞迁移。

## Decision

- Accepted.
- Decision owner: 当前线程用户。
- Approved defaults: 路线级唯一 DCC 关系；一个 DCC 项目代码一个有效 QA 规程；新订单激活时锁定 QA 发布版本；旧/重新激活订单保持原锁定版本；QA 工序使用独立稳定身份；缺配置显式阻塞；DCC 列表增加 QA 规程列和跳转。
- 当前历史经验中的产品/路线版本/MES工序 QA 关联口径被本变更替代；实施完成后必须通过 `project-experience-consolidation` 更新长期经验，避免旧规则继续被引用。

## Required Approvals

- 业务规则和推荐值已由用户明确批准。
- 本文不授权数据库执行、运行环境修改、远程发布或真实业务数据迁移；这些操作必须在后续实施任务中按项目门禁单独取得授权或满足既定任务范围。

## Downstream Skill Reruns

- `system-design-docs`: 重建前端、后端、数据和安全部署设计。
- `bdd-tdd-acceptance-planner`: 重建 BDD、严格 TDD、测试数据和真实 E2E 计划。
- 后续实施按任务切片分别使用 `database-schema-delivery`、`backend-api-delivery`、`frontend-feature-delivery`、`milestone-tdd-delivery` 和 `independent-verification-gate`。
- 实施收尾使用 `project-experience-consolidation` 更新冲突的长期经验。

## Blockers And Next Action

- Design blockers: 业务方向已确认，但实施设计未放行。当前需修正 `PATROL_AM/PATROL_PM` 双巡检规则身份、同任务并发提交原子闭环，以及不依赖DCC当前启用状态的锁定历史QA读取合同。
- Implementation blockers: 未冻结真实迁移清单、测试租户/账号、实际运行库 schema 或发布窗口时，不得执行迁移和真实 E2E。
- Next action: 不得启动14个Agent实施。先开启新的文档修订和独立评审闭环，解决最终评审阻塞项并取得明确PASS，再把设计包作为开发基线。
