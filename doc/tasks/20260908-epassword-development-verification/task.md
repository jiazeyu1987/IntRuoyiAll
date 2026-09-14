# 统一电子签名开发验证

## Task Goal

在 `D:\IntRuoyiWorktree\epassword_20260908` 中按已批准的统一电子签名文档完成开发与验证，落实审查项 4.1 至 4.12 对应的账户、密码、重新认证、签名绑定、审计追踪、复核、审批顺序和身份验证控制。

## Scope

- 统一电子签名内核、账户/密码控制、签名重新认证、签名证据、历史验证、周期复核、可信时间、日封存链、特权审计和归档恢复验证。
- BPM、DCC、MES/eDHR 目标正式签名链路迁入统一签名内核。
- 本地自动化验证、静态合同、后端打包和收尾证据。

## Non-Scope

- 不执行真实数据库写入、真实生产迁移、远程服务器操作、发布或真实前端 E2E。
- 不宣称外部法律认证已完成；生产审查仍需真实可信时间源、WORM/不可改写存储、调度、迁移、备份恢复和真实环境证据。
- 4.12 当前满足“实名账户会话 + 签名时本人密码重新认证”的最低口径；若审查方要求独立 OTP/硬件/短信等第二因子，需追加 MFA 集成。

## Milestones

- [x] M0：建立任务文档，读取项目规则和交付技能。
- [x] M1：T1 账户唯一、密码强度、首次/重置改密、密码历史和有效期。
- [x] M2：T2 统一失败锁定与签名重新认证。
- [x] M3：T3 统一签名内核最小领域与持久化模型。
- [x] M4：T4-T6 DCC、BPM、MES/eDHR 适配与旧写入删除。
- [x] M5：T7 周期复核与历史查询。
- [x] M6：T8 可信时间、日封存链、特权审计和归档恢复核验。
- [x] M7：主工作区 baseline 提交、任务分支合入、后端打包验证和本地合并。

## Expected Verification

- T1-T8 对应测试和静态合同均产生有效 RED/GREEN，且 `Tests run > 0`。
- 新增或修改的生产代码有对应测试或静态合同。
- 数据库迁移有静态合同和 H2 测试 schema。
- 业务模块不再直接验证正式签名密码或直接写正式签名表。
- 检查项 4.1 至 4.12 均能追踪到实现、测试和证据。

## Design Constraints Check

- 统一服务端签名内核，不新增浏览器可直接指定业务对象的通用签名写接口。
- 正式签名人、租户、时间和权威内容哈希均来自服务端。
- 缺少账户、授权、密码状态、内容快照、密钥、时间证据、WORM 或审计前置时失败关闭。
- 不改写历史签名，不长期双写新旧签名事实源。

## Current Status

completed：T1-T8 代码与任务级证据已完成；实现提交、主工作区 baseline 提交、任务分支合并、本地 `int_main` 合并和 `origin/int_main` 推送均已完成。最终后端打包验证通过，任务收尾完成。

## Cleanup Keep

- doc/tasks/20260908-epassword-development-verification/task.md
- doc/tasks/20260908-epassword-development-verification/execution-log.md
- doc/tasks/20260908-epassword-development-verification/verification-report.md
