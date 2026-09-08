# GxP 审计追踪统一设计任务

## Task Goal

形成可实施、可验证、可持续治理的统一 GxP 审计追踪设计，覆盖检查清单 2.1 至 2.10，并明确现有 eDHR、DCC、BPM、Showroom、权限、配置、发布迁移和可信时间能力如何接入统一审计账本。

本任务只交付文档，不修改生产代码、数据库、运行环境或远程服务。

## Milestones

- [x] M1：确认当前代码证据、缺口和设计边界。
- [x] M2：记录统一审计架构决策及被拒绝方案。
- [x] M3：完成前端、后端、数据模型、配置安全部署设计。
- [x] M4：完成分阶段开发计划、BDD 场景和严格 TDD 验证计划。
- [x] M5：记录质量、法规、运维和系统负责人必须确认的外部业务参数；参数确认和代码实施转入后续任务。
- [x] M6：接受独立复审提出的六项合规设计整改并记录变更影响。
- [x] M7：补齐完整法规归档、自动周期审查、可执行覆盖登记、特权防篡改、状态信封和系统变更清单。
- [x] M8：重新执行 2.1 至 2.10 独立放行审查和收尾验证。
- [x] M9：补充实施合规门禁，明确开发完成、持续满足和最终审查放行条件。

## Expected Verification

- 系统设计四份文档包含技能要求的全部结构章节。
- ADR 包含状态、上下文、驱动因素、选项、决策、后果、迁移、回退/重审条件和验证影响。
- 检查清单 2.1 至 2.10 均能追溯到设计控制和测试场景。
- 文档明确无 fallback、审计失败回滚、客户端不能提供审计身份或正式时间、审计记录不可更新删除。
- `system-design-docs` 和 `architecture-decision-records` 校验脚本通过；若脚本只支持固定默认文件名而不能校验任务特定文件，则记录精确阻塞并执行结构检查。
- 六项复审缺口均有明确需求、数据/API/安全控制和自动测试，不再只停留在目标描述。
- 独立复审逐项给出 2.1 至 2.10 的“设计可满足/不可满足”结论，并区分软件设计与生产运行证据。
- 合规证据矩阵为 2.1 至 2.10 分别定义软件证据、运行/SOP 证据和 PASS 条件。
- 实施合规门禁明确“统一内部审计接口 + 统一策略/覆盖门禁”，并区分 `PASS FOR DESIGN`、`PASS FOR SOFTWARE` 和 `PASS FOR OPERATIONAL COMPLIANCE`。

## Design Constraints Check

- 使用 Java 17、Spring Boot、Maven 多模块和 Vue 3/Vite/TypeScript 现有技术栈。
- 统一的是审计写入契约、账本、查询和治理门禁，不统一或代理全部业务接口。
- 业务写入与审计事件在同一事务中提交；审计失败必须回滚业务写入。
- 不使用 API 访问日志替代 GxP 审计，不从 HTTP 请求自动猜测变更理由或前后值。
- 不进行长期双写、静默降级、异步补记、默认成功或兼容 fallback。
- 现有专业审计明细继续作为证据，由统一事件通过稳定身份和哈希显式关联。
- 当前用户已批准总体方案；生产部署、远程配置、数据库写入和 E2E 仍需后续单独授权。

## Current Status

completed：六项设计整改、实施合规门禁、结构校验、2.1 至 2.10 独立复审和 task-closeout-cleanup 均已完成；用户已确认继续按合规要求收尾，Git 提交/推送纳入最终收尾步骤。

## Open Business Decisions

- 各记录类别的法定/质量保存期限、起算事件和 legal hold 规则。
- 周期审查频率、抽样规则、质量负责人、逾期升级路径。
- 企业批准的 NTP 地址、最大允许偏差和证据采集周期。
- 正式服与审查服 WORM/Object Lock 存储目标、独立故障域副本和恢复责任人。
- 首批纳入 GxP 的业务对象清单以及明确不适用项的批准人。

## Cleanup Keep

- doc/tasks/20260907-gxp-audit-trail-unification/prd.md
- doc/tasks/20260907-gxp-audit-trail-unification/development-plan.md
- doc/tasks/20260907-gxp-audit-trail-unification/test-plan.md
- doc/tasks/20260907-gxp-audit-trail-unification/compliance-evidence-matrix.md
- doc/tasks/20260907-gxp-audit-trail-unification/implementation-compliance-gate.md
