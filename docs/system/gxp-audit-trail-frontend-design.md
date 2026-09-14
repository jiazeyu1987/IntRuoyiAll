# 统一 GxP 审计追踪前端设计

## Purpose and Scope

为质量审查员和授权管理员提供统一、只读、可筛选、可验证和可导出的审计中心；同时规定受控业务页面如何采集原因、展示电子签名含义并处理审计失败。前端不生成正式操作人、正式时间、前后值或 hash。

## Evidence Reviewed

- 现有系统操作日志页面提供分页查询和导出模式。
- DCC 受控文件审计已提供操作人、动作、目的、结果、原因和时间视图。
- eDHR 字段审计已提供字段级前后值、原因、签名和链验证能力。
- 统一签名治理查询已覆盖 DCC、eDHR、Showroom 和 BPM。

## Pages and Routes

- `/system/gxp-audit/events`：标准事件列表，默认显示最近事件和完整性状态。
- `/system/gxp-audit/events/:id`：事件详情，展示对象、动作、原因、前后差异、证据、签名、时间和 hash。
- `/system/gxp-audit/reviews`：周期审查批次、到期状态、发现项和整改。
- `/system/gxp-audit/integrity`：链、每日清单、WORM、备份恢复和可信时间状态。
- `/system/gxp-audit/archive`：法规归档包、完整内容清单、主副本回执、保存期限和恢复演练。
- `/system/gxp-audit/coverage`：策略登记版本、实际写入口、覆盖缺口、不适用批准和测试映射。
- `/system/gxp-audit/privileged`：特权审计外送状态、未封存水位、DML/DDL/授权及关闭审计告警。
- `/system/gxp-audit/policies`：当前策略版本和批准依据，只读；策略维护另受高风险权限控制。

## Components

- `GxpAuditFilterBar`：租户、领域、对象、动作、结果、操作人、签名和时间范围筛选。
- `GxpAuditTable`：固定列宽显示时间、对象、动作、操作人、原因摘要、签名和完整性。
- `GxpAuditDiffViewer`：结构化字段差异，明确旧值、新值和脱敏状态。
- `GxpEvidenceLinks`：通过稳定证据类型和 ID 打开领域只读详情。
- `GxpSignatureEvidence`：签名人、签署时间、含义、内容 hash 和验证状态。
- `GxpIntegrityPanel`：事件 hash、前序 hash、清单和归档回执。
- `GxpReviewWorkspace`：冻结范围、发现项、整改、质量签名和关闭门禁。
- `GxpReviewScheduleStatus`：上次/下次周期、漏期、补扫、失败和逾期状态。
- `GxpArchivePackageDetail`：展示完整归档包 required item、数量/hash、主副本和仅包恢复结果，明确区别数据库备份与法规归档。
- `GxpCoverageReport`：按写入口类型显示登记、测试、owner、不适用批准和缺口。
- `GxpStateEnvelopeViewer`：将 ABSENT、PRESENT、VOIDED、REDACTED 转为明确业务状态，不把 null 显示为空白正常值。
- `GxpSystemChangeManifest`：展示 commit、镜像/产物、OpenAPI diff、Migration 顺序、配置 diff、批准和回滚关系。
- `GxpReasonDialog`：嵌入受控业务操作，使用原因分类加必填说明，不提供通用默认原因。

## State and Data Flow

1. 业务页面先取得当前对象版本，只在用户确认动作时收集原因和签名挑战。
2. 前端调用原业务 API；后端负责真实 before/after、正式身份、正式时间和审计事务。
3. 返回成功只表示业务与审计已共同提交；`GXP_AUDIT_*` 错误必须显示失败且不得刷新成成功状态。
4. 审计中心读取统一只读 API；专业证据按后端返回的稳定链接打开，不按名称、时间或文本搜索猜测。
5. 导出采用服务端固定查询快照；前端轮询明确任务状态，不在浏览器拼接证据包。

## Error States

- 缺少原因：保持确认框打开并定位原因字段。
- 签名缺失或无效：显示签名错误，不重复提交原业务请求。
- 审计写入失败：显示“业务未保存”，刷新对象确认未变更。
- 完整性失败：以高风险状态显示首个异常位置，禁用“验证通过”或“关闭审查”。
- 可信时间、WORM 或备份证据缺失：显示 `阻塞`，禁止使用绿色正常状态。
- 证据无权访问：保留事件摘要并显示权限错误，不把证据缺失当作不存在。
- 归档包只含 hash/manifest 或缺少 required item：显示 `归档不完整`，不能显示“已备份”。
- 应审周期没有唯一批次、特权审计外送断链或未封存水位超期：显示 `阻塞` 并禁止关闭审查和合规导出。
- 覆盖报告有未登记入口或过期不适用决定：显示精确 operationId 和源码入口，不允许隐藏在汇总数字中。

## Accessibility and Responsive Behavior

- 状态不仅依赖颜色，必须包含明确文本和图标。
- 差异表支持键盘导航、焦点可见和屏幕阅读器标签。
- 表格在窄屏保持关键身份、动作、时间和状态可见，其余字段进入详情抽屉；不截断唯一对象编号。
- 原因输入、签名确认和高风险错误使用可聚焦标题及明确字段错误。

## Open Questions

- 是否允许跨租户质量总览；默认不允许。
- 导出采用同步下载还是现有受控任务机制，需结合预计数据量决定。
- 哪些专业证据页面已有合格只读路由，哪些需要新增只读详情。

## Design Blockers

- 未批准质量审查角色、跨租户权限和职责分离矩阵。
- 未批准周期审查频率、严重度和整改时限。
- 未完成后端统一错误码和证据链接合同前，前端不能宣称完整闭环。
