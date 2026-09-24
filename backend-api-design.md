# 偏差管理后端与 API 设计

## Purpose and Scope

以 PRD 为业务依据，新增批记录偏差服务，复用统一电子签名、不合格评审及权限体系。本文件中的新 API/字段是目标合同，不代表当前实现已经存在。

## Evidence Reviewed

- IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrNonconformanceReviewServiceImpl.java：create、dispose、ensureBatchNotFrozen；当前创建分支需要区分 PQC 来源和批记录来源。
- 同目录 MesProEdhrBatchActiveOrderDetailService.java：按批记录或活跃订单读取详情；需补充正式批记录上下文。
- 同目录 MesProBatchRecordExecutionSignatureService.java：已有签署能力；新动作须注册到统一签名适配器，不能只增常量。
- 同目录 MesOrderReleaseCompletenessServiceImpl.java：偏差关闭检查当前读取工单质量异常，须增加本模块来源，保留原检查。
- service/pro/productionrelease/pqc/MesPqcProductionReleaseServiceImpl.java：当前 PQC 生产放行写服务；不能仅改旧 eDHR 放行路径。
- service/pro/frontline/MesFrontlinePqcContextServiceImpl.java：一线 PQC 正式写入口。
- 当前 OQ/PQ 的 MesProEdhrValidationDeviationDO 不承担本次生产偏差。

## Modules

新增 Deviation Controller/Service/Mapper/DO、编号序列、处理记录和审计投影。现有 NCR 服务新增显式批记录偏差来源（建议 BATCH_DEVIATION），复用其评审单、处置、冻结和签名，不要求伪造 PQC_RELEASE sourceId。

查询偏差按 batchExecutionId；NCR 内部如需 activeOrderId/workOrderId，仍通过已有正式来源关系解析和保存，保留现有操作事实链。用户只选批记录，禁止用批号模糊匹配或“最新订单”补关系。

新增偏差限制只在四项正式写动作执行；不要直接塞入会拦截查询、签名、偏差关闭或评审处置的通用“批次全部操作”门禁。

## API Contracts

建议基础路由 /mes/pro/edhr-deviation，响应遵循 CommonResult/PageResult，Long ID 在前端使用十进制字符串。签名密码仅在请求内短暂传递，不持久化或进入幂等哈希。

| 方法/目标路径 | 输入 | 输出及校验 |
| --- | --- | --- |
| GET /page | status=ALL/OPEN/CLOSED，编号、批记录号、level、发起时间，pageNo/pageSize，白名单排序 | 当前租户有 query 权限范围内全部偏差；closeReason、reviewStatus 分列。 |
| GET /get | deviationId | 发起、0..1 处理记录、签名、审计、关闭原因、关联 NCR 与处置结果、allowedActions。 |
| GET /batch-options | 批记录编号等搜索条件 | 真实可关联批记录；无需已有 PQC 放行申请；已上市放行不可创建。 |
| POST /create | batchExecutionId、发起字段、idempotencyKey、signaturePassword | 验证 create 权限/租户/放行状态；发起签名和编号原子生成，返回 ID/code/version。 |
| PUT /handling | deviationId、expectedVersion、处理字段、changeReason | 首次保存创建唯一处理记录；之后更新同一行；更新内容版本并使该版处理签名失效。 |
| POST /sign | deviationId、actionCode、expectedContentVersion、签署结论/意见、idempotencyKey、signaturePassword | 明确动作：处理编制、验证、部门负责人、QA确认、质量负责人、管理者代表。角色独立授权。满足 FR-06 时自动常规关闭。 |
| POST /create-ncr | batchExecutionId、deviationIds、expectedVersions、评审原因、idempotencyKey、signaturePassword | QA 发起，列表非空且全为同批开放关键偏差；直接转审，不要求偏差表验证/批准或处理完成。 |
| GET /by-batch | batchExecutionId、分页 | 当前批记录的正式偏差；所有页可查，不截断为“前若干条”。 |
| 既有 NCR /dispose | 评审 ID、处置、材料、意见、签名 | QA 或获处置授权的 PQC 放行人员可用；其他权限不自动获得处置权。 |

宿主只持有 activeOrderId 时，通过既有详情服务的正式来源解析返回 batchExecutionId（建议新增 deviationContext），与详情对象共同校验租户和来源一致性。这是正式数据投影，不是前端推断。若没有唯一来源，返回 MISSING/AMBIGUOUS 明确上下文原因，不调用 /by-batch；正常要求覆盖的入口必须在 P1/P5 补齐该关系。

## Error Model

至少区分 NOT_FOUND、FORBIDDEN、BATCH_CONTEXT_MISSING/AMBIGUOUS、BATCH_MARKET_RELEASED、HANDLING_CLOSED、STALE_VERSION、SIGNATURE_INVALID、REQUIRED_SIGNATURE_MISSING、INVALID_CRITICAL_SELECTION、PENDING_NCR_EXISTS、IDEMPOTENCY_CONFLICT、BATCH_BLOCKED。错误信息指明动作和可见偏差/评审编号；不泄露其他租户资料。

空查询成功仅代表真实空集合。无权限、缺 ID、数据不一致、网络失败不可转换为空集合。身份验证失败时业务零写入；允许认证模块保留正式失败审计，不记录口令。

## Transactions and Idempotency

1. 所有竞争写动作先锁同一正式批记录控制行，再按升序锁 deviation IDs、处理内容行和 NCR 行；编号创建再锁 tenant/month 序列行。与旧流程锁序核对，防止死锁。
2. /create 与上市放行采用同一串行化边界：放行先提交则创建拒绝；偏差先提交则放行拒绝。四项受限提交需在其写事务内核验当前事实，不用先前预检或页面缓存。
3. 月序列以 tenant/month 唯一行和原子递增实现，首月并发初始化须有明确数据库 upsert/锁方案；禁止 max(code)+1。正式回滚号码不对外发布，已提交号码不复用。
4. 写请求使用 tenant+action+idempotencyKey 和规范化业务载荷摘要。相同键同载荷重放返回既有结果；同键异载荷拒绝。验权后先识别已成功动作，再检验已变化的业务状态，避免成功转审后重试被误判“已关闭”。
5. 普通签署锁定内容版本。contentVersion仅覆盖处理人填写的调查、风险、处置和CAPA正文；验证结论、部门处理结论与各批准意见是绑定该版本的签署事实，经/sign提交并投影回唯一处理记录，不由/handling覆盖。新增同版签名只更新动作/状态版本，不改处理正文hash。正文修改递增contentVersion，使该处理版编制、验证及后续结论/批准全部失效；旧版本签名请求拒绝。验证结论修订只使其后部门/QA/质量/管理者签名失效，部门结论修订使其后批准失效；原处理正文未变时不反向使编制签名失效。QA/质量/管理者为同一依据上的并列签署，互不签彼此的签名数组。这样验证人第一次签结论不会自动废止前置编制签名。
6. 转 NCR 在单事务中签署评审发起、创建正式评审及冻结、关联全部选中偏差、写 CLOSED/TRANSFERRED_TO_NCR。非空同批关键偏差集整体预检；失败零部分关闭，未选中偏差保持原状。
7. 一批已有待处置 NCR 时，返回 PENDING_NCR_EXISTS 及可见评审编号，不新建第二张、不静默追加；待审处置不能被偏差拦截，处置后开放偏差继续独立阻断并可再转审。
8. NCR 处置沿用统一流程，重新计算未关闭偏差、其他评审和外部冻结；让步只恢复受既有门禁允许的动作，不自动完成 PQC/上市放行；作废终止；返工执行正式返工合同，不凭偏差关闭判断生产已完成。

## Open Questions

业务规则已收敛。具体菜单 ID、签名 action code 与新表 DDL 由实现基于当前迁移/适配器确定，不能将本文件建议名称当作已存在 API。归档导出扩展不在本次验收门禁。

## Design Blockers

- P1 必须证明正式批记录可在未推送 PQC 时用于此功能；检查来源创建时机及一线写入到批记录的正式关系，缺关系时补齐设计，不先放宽为订单关联。
- P4 复核现有返工处置和权限实现的实际效果，不能用恢复旧状态代替返工路径，也不能让新增角色字符串绕过权限体系。
- DDL、运行服务、数据库和 E2E 各自按项目规则取得当前任务授权后执行；本轮只改文档。
