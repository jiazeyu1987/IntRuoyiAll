# 统一 GxP 审计追踪设计复审整改

## Request Summary and Source

- 来源：用户于 2026-09-07 要求先优化统一 GxP 审计追踪文档，再独立检查按文档开发后是否满足检查清单 2.1 至 2.10。
- 请求包含两部分：修订上一轮独立评审识别的设计缺口；修订后执行需求到设计到测试的重新放行判断。

## Current Baseline Reviewed

- `doc/tasks/20260907-gxp-audit-trail-unification/prd.md`
- `doc/tasks/20260907-gxp-audit-trail-unification/development-plan.md`
- `doc/tasks/20260907-gxp-audit-trail-unification/test-plan.md`
- `docs/system/gxp-audit-trail-backend-api-design.md`
- `docs/system/gxp-audit-trail-data-model.md`
- `docs/system/gxp-audit-trail-config-security-deployment.md`
- `docs/system/gxp-audit-trail-frontend-design.md`
- `docs/adr/ADR-0002-unified-gxp-audit-trail.md`
- `resource/相关文档/计算机化系统检查清单.md` 第二章 2.1 至 2.10。

当前基线已定义统一内部审计服务、同事务写入、只追加账本、证据关联、电子签名、周期审查、可信时间、WORM 和 CI 门禁，但复审发现六项不足：WORM 只明确保护清单、周期审查未证明自动执行、覆盖登记不可执行、特权篡改和封存窗口未闭合、CREATE/DELETE 前后状态语义不明确、系统变更缺少可重算接口和配置清单。

## Classification

- 类型：合规需求与安全架构补强。
- 风险：高。若不修订，即使代码按文档完成，2.3、2.4、2.5、2.7 和 2.8 仍可能无法通过审查。
- 范围：文档设计和验证计划；不包含生产代码、数据库写入、远程环境、E2E 或发布。

## Impact Analysis

### Product Impact

- 增加法规归档包、自动周期审查、覆盖例外批准和特权操作审查的可见状态。
- 审计中心需要区分“数据库备份可恢复”“法规归档可查阅”“清单完整性可验证”。

### Design Impact

- WORM 对象必须包含完整规范事件与必需证据包，不能只保存每日清单。
- 周期审查必须由持久化调度规则自动产生批次并保存漏期/逾期证据。
- GxP 操作登记表采用固定版本化 schema，并定义全仓发现与批准例外机制。
- 增加数据库特权操作审计、独立外送和未封存水位控制。
- CREATE/DELETE 使用显式 `exists` 状态信封。
- 发布审计增加 OpenAPI、配置、Migration、镜像/产物和批准依据的可重算清单。

### Data Impact

- 新增法规归档包及包明细、调度规则/运行记录、覆盖策略/例外、特权审计外送回执等设计实体。
- 增加 `recorded_at_utc`、事务身份、封存水位和归档包 hash 等字段要求。

### API Impact

- 增加归档包查询/验证、周期审查计划状态、覆盖报告和未封存水位只读接口。
- 不增加业务写入型公共审计 HTTP 接口，继续使用同事务内部服务。

### Test Impact

- 增加完整归档包恢复、连续周期自动执行、全写入口发现、特权篡改、封存前窗口、CREATE/DELETE 状态信封和 OpenAPI/config diff 测试。
- 最终验证必须区分文档设计 PASS 与生产运行合规证据 PASS。

### Release Impact

- 发布门禁必须验证覆盖报告、OpenAPI 差异、配置差异、Migration 顺序、镜像 digest、审计归档和可信时间状态。
- 任一必需证据缺失时阻塞发布，不生成默认通过。

### Operations Impact

- 需要正式 WORM 主副本、数据库原生特权审计外送、自动周期审查调度、NTP/chrony 和恢复演练。
- 需要质量、法规、数据库、安全和运维负责人批准参数与职责分离。

## Decision

ACCEPT：接受全部六项整改，并在原任务 `20260907-gxp-audit-trail-unification` 中续开文档阶段。修订完成后使用独立验证门禁逐项审查 2.1 至 2.10；存在未闭合设计缺口时继续判定 NO-GO。

## Required Approvals

- 用户已批准本轮文档优化。
- 保存期限、审查周期、GxP 范围和例外仍需质量/法规负责人批准。
- NTP、WORM、数据库特权审计和独立副本仍需运维、数据库及安全负责人批准。
- 后续代码、数据库、远程环境、E2E、提交和发布仍需各自当轮授权。

## Downstream Skill Reruns

- `system-design-docs`：更新前端、后端、数据、配置安全部署设计。
- `architecture-decision-records`：更新 ADR-0002 的决策、后果和验证范围。
- `independent-verification-gate`：修订后重新执行 2.1 至 2.10 放行审查。

## Blockers and Next Action

- 文档修订本身无阻塞。
- 下一步更新任务状态、PRD、设计、开发计划、测试计划和 ADR，再运行结构、语义和双向追溯校验。
- 业务与环境参数未批准时可以完成设计，但不能宣称生产运行已经合规。

