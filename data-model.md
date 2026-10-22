# 偏差管理数据模型设计

## Purpose and Scope

定义一批多偏差、一偏差至多一份处理记录，以及一张不合格评审关联同批多条关键偏差的目标合同。名称为新设计建议，不是运行库现状。

## Evidence Reviewed

- MesProEdhrBatchExecutionDO、MesProEdhrNonconformanceReviewDO 及现有 migration。
- OQ/PQ 校验偏差具有 package/case/run 关联，语义与本功能不同，保持原样。
- prd.md 的 FR-01 至 FR-12 和常规/转审两条关闭分支。

## Entities

| 实体（建议表名） | 核心字段与约束 |
| --- | --- |
| Deviation / mes_pro_edhr_deviation | id、tenant_id、deviation_code、batch_execution_id（不可改绑）、批记录业务编号快照、level=NORMAL/CRITICAL、PRD发起字段、status=OPEN/CLOSED、close_reason、closed_at、nonconformance_review_id、version。 |
| Handling / mes_pro_edhr_deviation_handling | id、tenant_id、deviation_id、调查/根因/风险/措施/CAPA/验证/结论字段、content_version、content_hash、version。tenant_id+deviation_id 唯一；第一次保存才创建。 |
| 月序列 / mes_pro_edhr_deviation_sequence | tenant_id+year_month 唯一、last_value；PC-YYYYMM-0001 起，四位不足补零、超过自然扩位。 |
| 签名关联与审计 | 复用统一签名及本领域投影；subjectId/actionCode/contentVersion/hash/signatureId/signer/服务器时间/时区/意图/有效性和失效原因。动作审计保存修改原因、before/after。 |
| 幂等动作事实 | tenant/action/key 唯一、规范化业务载荷hash、结果对象ID；复用现有可信设施或专门记录。不能从状态猜测上次请求已成功。 |

发起字段范围和处理字段范围以 PRD 为准；不同日期、人员、选择项和长文本分别保存，不把整张处理表压成一个无法校验的“说明”。

## Relationships

- BatchExecution 1 → N Deviation；每条偏差有且只有一份正式批记录。
- Deviation 1 → 0..1 Handling；初始或提前转 NCR 可无处理行，正常关闭必须有完整唯一处理行。
- NCR 1 → N 关键偏差（同批）；单条偏差通过 nonconformance_review_id 最多指向一个 NCR。不需要一个新的“多轮处理”表。
- 发起、处理内容和签名证据是独立数据；修订审计不属于第二份处理结果。
- NCR 内部 activeOrderId/workOrderId 继续依据正式来源关系保存；不成为偏差主关联。

## State Models

| 状态 | 必要条件 | 页面归属 |
| --- | --- | --- |
| OPEN | 正式发起；处理中、验证未通过、缺任何必要签署均保持 OPEN。 | 全部、未处理。 |
| CLOSED + NORMAL_COMPLETED | 处理结论为闭环完成；发起编制签名、完整处理记录与处理编制签名、验证合格签名、部门负责人、QA关闭确认、质量负责人均有效；CRITICAL 另需管理者代表。 | 全部、已处理；显示“常规闭环”。 |
| CLOSED + TRANSFERRED_TO_NCR | QA签署发起同批关键偏差NCR成功，关联ID已保存；不要求偏差表处理完成、验证或批准。 | 全部、已处理；显示“转不合格审批关闭”和评审状态。 |

QA 与质量负责人可任意先后签署，最后必要签署完成时才常规关闭。处理正文contentVersion不包括新增签名数组、验证/部门结论和批准意见；这些是绑定当前版本的签署事实，投影在唯一处理记录中。首次验证签署不会改正文版本而废止编制签名。正文修改使该版编制、验证、部门、QA、质量、管理者签名均失效；验证结论修订只失效后续部门/批准，部门结论修订只失效后续批准，所有旧证据保留。QA、质量、管理者同版并列签署互不失效。发起原文锁定，发起签名不随处理修订失效。

业务事实 OPEN 与 NCR pending_review 分别产生批次阻断。NCR 处置完成不改变偏差的 close_reason，更不会把“转审关闭”改写为“验证合格”。首版关闭后只读。

## Migration Notes

- 目标 MySQL migration 先核对依赖、字符容量、tenant过滤、幂等DDL、唯一索引和菜单ID；本轮不写/执行SQL。
- 索引至少：tenant+code 唯一、tenant+batch_execution_id+status、tenant+nonconformance_review_id、tenant+deviation_id（处理唯一）、tenant+year_month（序列唯一）。
- 保留旧 OQ/PQ 偏差、历史NCR、批记录和签名资料，不做三级等级自动迁移、不凭编号回填关联。
- 新 NCR source_type 使用明确批记录偏差来源；已有来源、冻结前快照、历史材料和操作事实语义不变。

## Data Integrity Rules

- 同租户、同正式批记录验证贯穿创建、读、签署、评审及追溯。发起后编号、批记录关联和发起原文锁定。
- 常规与转审关闭原因必填，关闭时间只用受控服务器时间；转审关闭必须有真实NCR ID。
- 正式编号提交后不回收；失败事务未发布编号不算占用。首次月份并发及跨月边界必须测试。
- 数据修改审计保留原值和新值；签名失效不删除旧签名，也不显示伪造的通过结论。
- 评审创建、批次冻结、关联、偏差关闭事务原子性；已选集合为空/跨批/普通/已关闭整体拒绝。
- 关闭/NCR建立与提交/放行动作使用统一串行化合同，保证没有两者都成功的竞态。
- 不提供删除正式偏差、改绑批记录或关闭后编辑接口。

## Open Questions

现有 schema 的最终列类型/迁移依赖由 P1 核验。首版只支持创建评审时一次选多条；事后追加既有已签评审和偏差重开不实现，保留明确拒绝。

## Design Blockers

部署前必须核对正式schema及签名适配器注册；无运行证据不能声称数据库模型已上线。P1必须证明未推送PQC场景具有正式批记录身份。
