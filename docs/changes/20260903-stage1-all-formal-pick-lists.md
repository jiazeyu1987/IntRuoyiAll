# Change Request: Stage1 全量正式生产领料单来源

## Request Summary And Source

- Source: 用户当前会话。
- Request: Stage1 模拟不再要求活跃订单预先绑定领料单；应在生产领料单列表中，按当前活跃订单关联生产订单编号查找全部匹配的正式生产领料单。存在多张时必须全选。输入物料仅用于系统填写批号和物料平衡；用户只填写冻结工艺路线中该工序的输出物料完成数量。设备默认选择该工序第一台正式启用设备。
- Explicit boundary: 物料身份以活跃订单冻结工艺路线中每个工序的输入/输出物料为准；禁止通过产品 BOM、产品主数据或空来源推断物料，也禁止构造模拟领料单替代正式来源。

## Current Baseline Reviewed

- `MesStage1ActiveOrderCompleteSimulationServiceImpl` 已按 `workOrder.code` 查询生产领料单明细，但把所有来源合并成一张模拟领料单，并以首张来源表头作为记录载体。
- 当未查询到正式领料单时，当前实现仍会调用 `createSyntheticPickList`，从产品主数据构造模拟领料单；这与本次禁止推断、禁止 fallback 的规则冲突。
- 绑定表迁移已具备“活跃订单 + 领料单”的多条绑定唯一键，但 Stage1 的结果、持久化校验、快照和正式领料出库仍使用 `selectByActiveOrderId` 单条读取。
- 一线物料模拟服务已区分冻结路线的输入物料（系统批号/平衡）和输出物料（用户完成数量），并按正式设备绑定稳定排序默认取第一台启用设备；本次不改变该业务口径。

## Classification

- Requirement change and defect correction: Stage1 领料来源由“预绑定或单来源合并”改为“按生产订单编号发现全部正式来源并逐张保留”。

## Decision

- Accept.
- 正式领料单来源以当前活跃订单关联生产订单编号精确匹配，查询所有未删除且来源完整的生产领料单及其明细。
- 每张来源领料单必须分别复制、分别建立活跃订单领料绑定和绑定明细，来源单据身份不得合并或以第一张替代。
- 任一匹配来源缺少表头、来源身份、状态或明细时，整个 Stage1 失败；不存在匹配来源时同样失败，并返回正式领料来源缺失错误。
- Stage1 的后续校验、快照、清理和正式领料出库必须遍历全部绑定；任何向外返回的单领料单字段不得再作为完整来源事实的唯一载体。

## Required Behavior

- Given 当前活跃订单关联生产订单 `WO-001`，且该订单有两张完整正式生产领料单，When 点击 Stage1，Then 两张领料单均被独立复制和绑定，且每张的明细、来源单号和追溯信息均可分别查询。
- Given 当前活跃订单关联生产订单没有匹配的正式生产领料单，When 点击 Stage1，Then 操作失败，不创建模拟领料单、不从产品或 BOM 推断物料。
- Given 多张匹配领料单中任一张缺少有效表头、来源身份、状态或明细，When 点击 Stage1，Then 操作整体失败，不跳过不完整来源，也不处理其余来源。
- Given Stage1 已创建多张领料单绑定，When 生成领料出库、完成校验和返回快照，Then 三个环节均覆盖全部绑定及全部绑定明细。
- Given 一线人员填写工序物料，When 提交完成数量，Then 输入物料由系统填写批号并参与物料平衡，用户只填写当前冻结工序输出物料的完成数量；默认设备为该工序第一台正式启用设备。

## Impact Analysis

- Product: Stage1 点击行为不再依赖事前领料单绑定；无正式领料来源时将明确失败，避免看似成功但来源不可追溯。
- Design: 将 Stage1 的领料来源模型、复制链和快照模型从单条绑定扩展为绑定列表；保留现有冻结路线输入/输出物料职责边界。
- Data: 沿用 `(tenant_id, active_order_id, pick_list_id, deleted)` 多来源唯一约束；需要审计每张复制后领料单及每条绑定明细，禁止合并丢失来源身份。
- API: 需要将结果和快照调整为可表达多张领料单来源；若为兼容保留单个 `pickListId`，该字段只能是展示性首项，不能参与完整性判断或下游业务处理。
- Test: 补充多来源独立复制、无来源失败、任一来源不完整整体失败、下游全量消费、重跑读取全部既有绑定的后端回归；静态合同须禁止 `createSyntheticPickList` 和单条 `selectByActiveOrderId` 用于多来源事实链。
- Release: 当前静态检查不放行。需完成定向 Maven 回归后才能进入运行态验证；本次未授权重启或发布 `int_main`。
- Operations: 错误信息应指出“当前生产订单未找到完整正式生产领料单来源”，便于业务核对领料单列表和生产订单编号。

## Implementation Boundary

- 修改范围：`MesStage1ActiveOrderCompleteSimulationServiceImpl`、相关绑定 Mapper/结果模型、正式领料出库链路及对应测试；如结果 DTO 已有多来源字段则复用，禁止无必要 schema 扩张。
- 不修改范围：冻结工艺路线输入/输出物料判定规则、用户填写输出数量规则、设备默认第一台正式启用设备规则、真实生产订单和正式领料单原始数据。
- Prohibited: 产品 BOM/产品主数据推断、空来源模拟单、按排序取第一张来源、静默跳过不完整来源、吞掉来源校验异常。

## Required Approvals

- 用户已确认：多张领料单时全选；输入物料系统填写批号用于物料平衡；输出物料由用户填写完成数量；设备默认第一台。
- 未获得授权：数据库写入、远程操作、发布或重启 `int_main`。后续实现仅可先进行本地代码和定向测试。

## Downstream Work

- behavior-driven-development / bug-regression-fix-loop: 为四个核心场景建立 BDD 和 RED/GREEN 回归。
- backend-api-delivery: 实现多来源解析、独立复制/绑定、全量校验、全量快照和全量领料出库。
- database-schema-delivery: 仅复核既有多来源唯一键是否满足正式约束；若发现 schema 缺口，须单独申请数据库变更授权。
- independent-verification-gate: 定向回归通过后，独立核验多来源事实链没有退回单条读取或 synthetic fallback。

## Blockers And Next Action

- Current blocker: 当前 Stage1 服务仍保留 synthetic fallback，并在多处以单条绑定消费多来源数据，不能按新规则放行。
- Next action: 先将上述场景写入当前任务的 BDD/RED 记录，再以严格 TDD 改造服务、测试和静态合同；完成前不得重启或发布运行环境。
