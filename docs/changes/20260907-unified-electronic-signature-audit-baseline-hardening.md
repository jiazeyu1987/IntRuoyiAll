# 统一电子签名审查基线加固

## Request Summary and Source

- 来源：用户 2026-09-07 要求优化统一电子签名文档，并在优化后重新判断按文档开发是否可以满足检查项 4.1 至 4.12。
- 请求实质：把“机制可实现但参数和管理控制待定”的设计基线，升级为可直接指导开发和审查取证的完整基线。

## Current Baseline Reviewed

- `doc/tasks/20260907-unified-electronic-signature-interface-design/docs/product/`
- `doc/tasks/20260907-unified-electronic-signature-interface-design/docs/system/`
- `doc/tasks/20260907-unified-electronic-signature-interface-design/docs/acceptance/`
- `docs/adr/ADR-0003-unified-electronic-signature-kernel.md`
- 上一轮独立审查发现：4.12 口径、密码/锁定参数、账户发放、可读前后快照、可信时间阻断、周期复核 SOP 和密码历史算法仍不足以直接放行。

## Classification

- 类型：合规验收基线增强、产品策略定稿、系统设计补强和测试范围扩展。
- 不属于生产代码修复；本轮只更新文档和验证结论。

## Product Impact

- 冻结首版密码和锁定基线：至少 8 位，必须包含大小写字母、数字、特殊字符；90 天有效期；禁止复用当前密码和前 5 次历史密码；15 分钟滚动窗口内累计失败 5 次锁定 30 分钟。
- 明确检查项 4.12 按用户提供清单的验收口径为“当前实名账户会话 + 签名时重新输入本人密码”；文档不得将其误称为密码学意义上的 MFA。若审查方要求独立第二因子，生产放行转为阻塞并升级方案。
- 增加账户实名发放、本人领取、禁止共享、离职停用和季度账户复核要求。
- 周期复核固定为季度自动建批，并在重大策略、密钥、时间或篡改事件后触发专项复核。

## Design Impact

- 签名记录绑定最近 5 分钟内有效、同步正常且偏差不超过 1 秒的可信时间证据；否则正式签名失败关闭。
- 增加不可变、可读的签名主题快照和字段差异，不再只保存前后哈希。
- 密码历史比较明确覆盖当前密码和前 5 次历史哈希，并在账户行锁内原子完成。
- 周期复核增加调度、逾期升级、SOP 版本、培训证据、责任替补和证据保留规则。

## Data Impact

- 用户名唯一键不包含逻辑删除字段，账户标识永久不可复用。
- 增加签名主题快照、字段差异、可信时间证据引用、复核 SOP/培训/逾期字段。
- 既有用户 `must_change_password` 采用明确回填规则，不能以默认值使所有历史账号意外锁死或放行。

## API Impact

- 签名内部命令不接受 `signedAt`，返回并持久化 `timeEvidenceId`。
- 验证详情和导出返回受权限保护的可读前后快照/字段差异。
- 周期复核由调度和受控手工补建入口创建，客户端不能上传样本或结论。

## Test Impact

- 增加精确策略边界、跨登录/签名累计锁定、当前密码复用、账户标识永久不可复用、可信时间 5 分钟/1 秒边界、可读前后差异、季度自动建批和逾期升级测试。
- 真实 E2E 仍必须由实施当轮用户明确授权，只能走真实页面完成业务动作。

## Release and Operations Impact

- 生产启用必须配置企业批准的 NTP 地址、秘密密钥、季度复核责任人和替补人员。
- 必须完成账户发放/停用 SOP、电子签名使用 SOP、周期复核 SOP、培训和审批记录。
- 文档完成不等于生产合规；代码、配置、数据迁移、运行证据和质量签署均为放行条件。

## Decision

ACCEPT：接受加固并同步更新产品、系统、验收、ADR 和验证报告。以上数值作为首版最低安全基线；组织可以批准更严格值，不能批准更弱值而仍宣称满足本检查清单。

## Required Approvals

- 质量负责人批准 SOP、复核责任和历史不可验证记录处置。
- 安全负责人批准密码/锁定最低基线、密钥管理和 4.12 解释。
- 运维负责人提供企业受控 NTP 地址并证明时间配置、权限和持续巡检有效。
- 若外部审查方要求独立第二因子，必须另行批准真正 MFA 方案后才能放行。

## Downstream Skill Reruns

- `product-requirements-docs`：更新 PRD、用户流程和验收标准。
- `system-design-docs`：更新接口、数据、安全部署和迁移设计。
- `bdd-tdd-acceptance-planner`：更新 BDD、TDD、E2E 和测试数据。
- `security-privacy-compliance-review`：形成逐项 PASS 条件和剩余非软件门禁。

## Blockers and Next Action

- 当前文档优化无阻塞。
- 下一步更新下游文档、执行结构校验和独立逐项审查。
