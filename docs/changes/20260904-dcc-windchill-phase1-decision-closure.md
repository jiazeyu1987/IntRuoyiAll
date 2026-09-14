# DCC Windchill 第一阶段剩余决策收口

## Request Summary and Source

- 用户确认大版本超过 Z 后依次使用 AA、AB，并按相同规则继续。
- 用户授权其余第一阶段未决口径由 Codex 按最合适方案确定。
- 用户说明当前实施目标全部为测试环境，不要求生产级安全考虑。

## Current Baseline Reviewed

- `docs/product/dcc-windchill-version-phase1-prd.md` 及配套用户流程、验收标准。
- 四份 `docs/system/dcc-windchill-version-phase1-*` 系统设计和迁移盘点方案。
- `docs/adr/ADR-0001-dcc-windchill-version-model.md`。
- 当前 DCC 项目代码、类别/目录权限、版本、检出检入、审批、发布、签名和平台生命周期实现。
- 当前设计尚未确定超过 Z 的序列、项目访问权权威来源、元数据-only 检入和非当前正式来源权限。

## Classification

需求和技术约束变更。该变更收口已知开放问题，并将第一阶段部署范围限定为测试环境；不扩大到生产发布。

## Product Impact

- Revision 序列确定为 Excel 式字母进位：A 至 Z、AA 至 AZ、BA 至 BZ，并按相同规则继续。
- 小版本检入允许两类有效变化：新源文件，或至少一个允许修改的非身份业务字段变化。
- 无源文件变化的检入继续生成下一 Iteration，继承原源文件并记录相同哈希及字段差异；空操作检入必须拒绝。
- 非当前正式小版本可作为下一 Revision 内容来源，但只允许项目 OWNER 发起，并强制填写来源选择理由。
- 项目访问权以正式 `dcc_project_access_rule` 为权威来源，不使用项目负责人文本、项目修正任务或菜单权限推断。

## Design Impact

- Revision code 由 `revision_sequence` 通过确定性算法生成，不再依赖运行配置或超过 Z 的开放问题。
- 新增项目访问规则实体，支持 USER、DEPT、ROLE、POSITION 主体及 OWNER、EDIT、VIEW 级别。
- 检入 API 的 sourceUploadTicket 改为条件必填；没有新源文件时必须提交允许字段的结构化差异。
- 版本事件必须记录 CONTENT 或 METADATA 变更类型、前后文件哈希和字段差异摘要。
- ADR-0001 可以从 Proposed 更新为 Accepted，因为用户已授权剩余技术口径按推荐方案收口。

## Data Impact

- 新增 `dcc_project_access_rule`，作为项目级业务权限来源。
- Iteration 需要保存元数据快照；metadata-only 检入继承正式源文件 ID 和 SHA-256，同时产生新 Iteration。
- 旧测试数据自动映射仅适用于身份唯一、版本号唯一且可按数字顺序解释、无未完成审批、当前正式指针唯一的 Master。
- 自动映射时，每个旧业务版本映射为一个 Revision 的 `/1`；单版本链映射为 A/1，多版本链依次映射为 A/1、B/1、C/1。
- 重复版本号、驳回/撤回歧义、身份混合、缺项目/分类或多个正式版本继续列为 blocker。
- 测试环境不意味着可以自动删除歧义数据；清理必须另有精确范围和明确授权。

## API Impact

- 创建 Revision 的权限要求项目 OWNER + `dcc:controlled-file:revise` + 类别 REVISE。
- Checkout/Checkin 要求项目 EDIT 或 OWNER，并继续叠加全局动作权限和类别 EDIT。
- 普通正式版本查看要求项目 VIEW、EDIT 或 OWNER，并继续叠加当前类别/目录正文权限。
- Checkin 请求允许不传 sourceUploadTicket，但此时必须提交非空 `metadataChanges`；同时传文件和元数据变化时作为同一个新 Iteration 落库。
- 空文件变化且空元数据变化返回 `DCC_CHECKIN_NO_CHANGE`。

## Test Impact

- 增加 Z -> AA、AA -> AB、AZ -> BA 的版本序列测试。
- 增加内容检入、元数据-only 检入、内容+元数据检入及空操作拒绝测试。
- 增加项目 OWNER/EDIT/VIEW 动作矩阵测试，并证明菜单权限不能代替项目访问规则。
- 增加非当前正式来源只有 OWNER 可选、理由必填、来源不被静默替换测试。
- 增加可确定旧链自动映射及歧义链 fail-fast 测试。
- 测试环境 E2E 仍须经当轮明确授权，且只能通过真实前端页面执行验收动作。

## Release and Operations Impact

- 第一阶段只部署测试环境，不进入生产发布、生产密钥轮换、生产灾备或生产 Go/No-Go。
- 测试环境回填仍需冻结 DCC 写入口、运行 preflight/postflight 并保留可定位的测试数据快照，目的在于工程回滚而非生产合规。
- 不要求生产恢复责任人或生产维护窗口批准。
- 业务权限、审计、幂等、唯一性和发布原子性继续作为功能验收要求，不能因测试环境而取消。

## Decision

ACCEPT。接受全部上述口径，并同步更新产品需求、用户流程、验收标准、系统设计、迁移盘点和 ADR。

## Required Approvals

- 用户已批准 Revision 序列并授权其余口径按推荐方案收口。
- 测试环境正式数据库写入、服务重启、E2E、Git 提交或推送仍需要对应当轮授权；本变更不授权这些动作。
- 生产部署不在本变更范围，未来进入生产必须重新进行安全、备份恢复和发布评审。

## Downstream Skill Reruns

- Product Requirements Docs：更新 PRD、用户流程和验收标准。
- System Design Docs：更新数据、API、前端、配置安全部署和迁移盘点。
- Architecture Decision Records：更新 ADR 状态、序列、权限和迁移决策。
- 后续进入实现时重新执行 BDD/TDD 规划、数据库 schema 交付、后端和前端交付技能。

## Blockers and Next Action

- 文档同步本身无 blocker。
- 实现前仍需只读盘点目标测试库，确认哪些 Master 满足确定性自动映射条件。
- 下一动作：完成下游文档同步并运行对应 validator。
