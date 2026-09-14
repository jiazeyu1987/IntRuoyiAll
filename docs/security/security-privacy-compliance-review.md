# Security Privacy Compliance Review

## Scope

本次审查覆盖 `D:\IntRuoyiWorktree\20260909_epassword` 中电子签名合规整改相关内容：账号唯一性、密码策略、首次/重置改密、密码有效期、历史密码限制、账户锁定、签名内容绑定、签名时间、审计追踪、定期合规审查、多人审批顺序和签名人身份验证。

Reviewed artifacts:

- `IntRuoyiBackend/sql/mysql/20260908_system_signature_password_t1.sql`
- `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserPasswordPolicy.java`
- `IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/user/AdminUserServiceImpl.java`
- `IntRuoyiBackend/yudao-module-signature/src/main/java/cn/iocoder/yudao/module/signature/service/ElectronicSignatureServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProBatchRecordExecutionSignatureService.java`
- `IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/approval/service/signature/ApprovalSignatureRecordServiceImpl.java`
- `docs/security/electronic-signature-periodic-compliance-review-sop.md`
- `doc/tasks/20260909-epassword-compliance-hardening/verification-report.md`

## Data

电子签名证据属于受控电子记录，包含签名人、业务对象、业务内容快照、内容 hash、签名原因、签名时间、认证方式、审批上下文和审计 diff。该类数据应按受控质量记录处理，不得通过草稿、模拟或登录会话记录替代正式电子签名记录。

## Authentication

正式电子签名要求当前登录账号提供电子签名密码并通过 `reauthenticateForSignature` 重认证。重认证会拒绝禁用账号、锁定账号、错误密码、初始/重置待改密凭据和过期密码。

## Authorization

正式签名必须绑定当前签名人和业务上下文。BPM 原生待办审批签名要求具备流程实例、任务 ID、节点编码和顺序证据；非流程直签业务保留业务对象证据，不被 BPM 顺序门禁误伤。

## Secrets

审查文档不记录明文密码、token、私钥或连接密钥。测试和任务日志只记录命令、结果和结构化证据，不提交 `node_modules`、运行日志或密钥文件。

## Logging, Audit, And Retention

统一电子签名记录保存签名人、签名时间、签名原因、认证方式、签名算法、key/policy 版本、内容 hash、前后内容 hash、字段 diff、证据 hash 和验证状态。定期合规审查 SOP 要求审查包归档账号、密码策略、账户锁定、正式签名抽样、审计追踪、多级审批顺序和整改闭环证据。

## Findings

| ID | Requirement | Status | Evidence | Residual Action |
|---|---|---|---|---|
| 4.1 | 账户唯一性 | PASS | `canonical_username` 与租户唯一键，迁移前重复检查 | 生产执行迁移前需处理已存在重复账号 |
| 4.2 | 密码强度 | PASS | `AdminUserPasswordPolicy` 要求长度、大小写、数字、特殊字符 | 无 |
| 4.3 | 首次/重置后改密 | PASS | 签名前重认证拒绝 `INITIAL` / `RESET_REQUIRED` | 无 |
| 4.4 | 密码有效期 | PASS | 90 天有效期策略，登录和签名前检查 | 无 |
| 4.5 | 历史密码限制 | PASS | 最近 5 次密码不可复用 | 无 |
| 4.6 | 账户锁定 | PASS | 登录失败计数、锁定状态和签名前锁定检查 | 无 |
| 4.7 | 签名与内容绑定 | PASS | `contentHash`、前后内容 hash、证据 hash 与 canonical JSON | 无 |
| 4.8 | 签名时间不可手填 | PASS | 正式签名使用服务端时间，人工时间字段 fail fast | 无 |
| 4.9 | 审计追踪完整性 | PASS | 签名人、时间、原因、算法、认证方式、内容 diff 和 hash | 无 |
| 4.10 | 定期合规审查 | PASS FOR DESIGN / OPS EVIDENCE REQUIRED | `docs/security/electronic-signature-periodic-compliance-review-sop.md` 定义周期、责任、证据包和判定规则 | 生产审计前必须形成已执行并批准的审查记录 |
| 4.11 | 多人审批顺序 | PASS | BPM 待办签名保留流程实例、任务 ID、节点编码和顺序；缺关键上下文 fail fast | 无 |
| 4.12 | 签名人身份验证 | PASS | 正式签名统一账号 + 密码重认证；草稿/模拟模式不进入正式证据口径 | 无 |

## Blockers

当前 worktree 内容已经补齐 4.10 的设计与制度文档证据。但如果审查目标是“生产运营已持续执行”，仍需首期或当期已执行审查记录、证据包、整改闭环和批准记录；该运营证据不能由代码仓库自动证明。

主线融合仍受 `E:\IntRuoyi` 主工作区 dirty 状态影响，收尾脚本此前阻止 ff-only 合并。
